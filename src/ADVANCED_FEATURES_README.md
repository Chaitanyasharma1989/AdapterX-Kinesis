# Advanced Features Documentation

This document covers the advanced features implemented in AdapterX-Kinesis, following SOLID principles for extensibility and maintainability.

## Table of Contents

1. [Retry Logic](#retry-logic)
2. [Dead Letter Queue (DLQ) Support](#dead-letter-queue-dlq-support)
3. [Metrics/Monitoring Integration](#metricsmonitoring-integration)
4. [Backpressure Handling](#backpressure-handling)
5. [Pluggable Partition Key Strategy](#pluggable-partition-key-strategy)
6. [Custom Implementations](#custom-implementations)
7. [Configuration Examples](#configuration-examples)
8. [Testing Examples](#testing-examples)

---

## Retry Logic

### Overview
The retry logic provides configurable retry policies for failed record sends, with exponential backoff and customizable retry conditions.

### Default Implementation
```java
// Default exponential backoff retry policy
ExponentialBackoffRetryPolicy retryPolicy = new ExponentialBackoffRetryPolicy(
    3,    // max attempts
    100,  // initial delay (ms)
    5000, // max delay (ms)
    2.0   // multiplier
);
```

### Custom Retry Policy
```java
public class CustomRetryPolicy implements RetryPolicy {
    @Override
    public boolean shouldRetry(int attempt, Throwable lastException) {
        // Only retry on specific exceptions
        return attempt < 3 && lastException instanceof IOException;
    }

    @Override
    public long getBackoffDelay(int attempt, Throwable lastException) {
        // Linear backoff
        return 100 * attempt;
    }

    @Override
    public int getMaxAttempts() {
        return 3;
    }
}
```

### Usage
```java
@Bean
public RetryPolicy customRetryPolicy() {
    return new CustomRetryPolicy();
}

@Bean
public KinesisProducerService kinesisProducerService(
    KinesisProperties properties,
    DataSerializer serializer,
    RetryPolicy retryPolicy
) {
    return new DefaultKinesisProducerService(
        properties, serializer, retryPolicy,
        new LoggingDeadLetterQueueHandler(),
        new LoggingMetricsCollector(),
        new SimpleBackpressureStrategy(),
        new DefaultPartitionKeyStrategy()
    );
}
```

---

## Dead Letter Queue (DLQ) Support

### Overview
DLQ support handles permanently failed records (after all retries) by forwarding them to a configurable destination.

### Default Implementation
```java
// Default DLQ handler logs failed records
LoggingDeadLetterQueueHandler dlqHandler = new LoggingDeadLetterQueueHandler();
```

### Custom DLQ Handler
```java
public class SqsDeadLetterQueueHandler implements DeadLetterQueueHandler {
    private final AmazonSQS sqsClient;
    private final String queueUrl;

    public SqsDeadLetterQueueHandler(AmazonSQS sqsClient, String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void handle(Object record, Throwable error) {
        try {
            String messageBody = new ObjectMapper().writeValueAsString(record);
            SendMessageRequest request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .addMessageAttributesEntry("error", 
                    new MessageAttributeValue().withStringValue(error.getMessage()));
            
            sqsClient.sendMessage(request);
        } catch (Exception e) {
            // Log the failure to send to DLQ
            log.error("Failed to send to DLQ: {}", record, e);
        }
    }
}
```

### Kinesis DLQ Handler
```java
public class KinesisDeadLetterQueueHandler implements DeadLetterQueueHandler {
    private final KinesisProducerService producerService;
    private final String dlqStreamName;

    public KinesisDeadLetterQueueHandler(KinesisProducerService producerService, String dlqStreamName) {
        this.producerService = producerService;
        this.dlqStreamName = dlqStreamName;
    }

    @Override
    public void handle(Object record, Throwable error) {
        try {
            // Send failed record to DLQ stream
            producerService.sendAsync(dlqStreamName, record);
        } catch (Exception e) {
            log.error("Failed to send to Kinesis DLQ: {}", record, e);
        }
    }
}
```

---

## Metrics/Monitoring Integration

### Overview
Metrics collection provides insights into producer/consumer performance and error rates.

### Default Implementation
```java
// Default metrics collector logs events
LoggingMetricsCollector metricsCollector = new LoggingMetricsCollector();
```

### Custom Metrics Collector (Micrometer)
```java
public class MicrometerMetricsCollector implements MetricsCollector {
    private final MeterRegistry meterRegistry;

    public MicrometerMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordSendSuccess(String streamName, Object record) {
        Counter.builder("kinesis.send.success")
            .tag("stream", streamName)
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordSendFailure(String streamName, Object record, Throwable error) {
        Counter.builder("kinesis.send.failure")
            .tag("stream", streamName)
            .tag("error", error.getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordRetry(String streamName, Object record, int attempt, Throwable error) {
        Counter.builder("kinesis.send.retry")
            .tag("stream", streamName)
            .tag("attempt", String.valueOf(attempt))
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordDLQ(String streamName, Object record, Throwable error) {
        Counter.builder("kinesis.send.dlq")
            .tag("stream", streamName)
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordProcessingTime(String streamName, long durationMs) {
        Timer.builder("kinesis.send.duration")
            .tag("stream", streamName)
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

### Prometheus Metrics Example
```java
@Bean
public MetricsCollector prometheusMetricsCollector() {
    return new MicrometerMetricsCollector(meterRegistry);
}
```

---

## Backpressure Handling

### Overview
Backpressure handling prevents system overload by controlling the rate of record ingestion.

### Default Implementation
```java
// Simple backpressure: reject when queue is full
SimpleBackpressureStrategy backpressureStrategy = new SimpleBackpressureStrategy();
```

### Custom Backpressure Strategy
```java
public class AdaptiveBackpressureStrategy implements BackpressureStrategy {
    private final double threshold;
    private final AtomicLong lastBackpressureTime = new AtomicLong(0);

    public AdaptiveBackpressureStrategy(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean shouldAccept(int currentQueueSize, int maxQueueSize) {
        double utilization = (double) currentQueueSize / maxQueueSize;
        
        if (utilization > threshold) {
            lastBackpressureTime.set(System.currentTimeMillis());
            return false;
        }
        
        // Allow some recovery time after backpressure
        long timeSinceBackpressure = System.currentTimeMillis() - lastBackpressureTime.get();
        if (timeSinceBackpressure < 1000) { // 1 second recovery
            return false;
        }
        
        return true;
    }
}
```

### Rate Limiting Backpressure
```java
public class RateLimitingBackpressureStrategy implements BackpressureStrategy {
    private final RateLimiter rateLimiter;

    public RateLimitingBackpressureStrategy(double permitsPerSecond) {
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
    }

    @Override
    public boolean shouldAccept(int currentQueueSize, int maxQueueSize) {
        return rateLimiter.tryAcquire();
    }
}
```

---

## Pluggable Partition Key Strategy

### Overview
Partition key strategy allows custom logic for assigning partition keys to records.

### Default Implementation
```java
// Default strategy: use provided key or hash of record
DefaultPartitionKeyStrategy partitionKeyStrategy = new DefaultPartitionKeyStrategy();
```

### Custom Partition Key Strategies

#### Hash-Based Strategy
```java
public class HashBasedPartitionKeyStrategy implements PartitionKeyStrategy {
    @Override
    public String getPartitionKey(Object record) {
        if (record instanceof BulkRecord) {
            String key = ((BulkRecord) record).getPartitionKey();
            if (key != null) return key;
        }
        
        // Use consistent hash of record content
        return String.valueOf(Objects.hash(record.toString()));
    }
}
```

#### Round-Robin Strategy
```java
public class RoundRobinPartitionKeyStrategy implements PartitionKeyStrategy {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final int numPartitions;

    public RoundRobinPartitionKeyStrategy(int numPartitions) {
        this.numPartitions = numPartitions;
    }

    @Override
    public String getPartitionKey(Object record) {
        int partition = counter.getAndIncrement() % numPartitions;
        return "partition-" + partition;
    }
}
```

#### Field-Based Strategy
```java
public class FieldBasedPartitionKeyStrategy implements PartitionKeyStrategy {
    private final String fieldName;

    public FieldBasedPartitionKeyStrategy(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getPartitionKey(Object record) {
        try {
            Field field = record.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(record);
            return value != null ? value.toString() : "default";
        } catch (Exception e) {
            return "default";
        }
    }
}
```

---

## Custom Implementations

### Complete Custom Producer Service
```java
@Component
public class CustomKinesisProducerService implements KinesisProducerService {
    private final KinesisProducerService delegate;
    private final MetricsCollector metricsCollector;

    public CustomKinesisProducerService(
        KinesisProperties properties,
        MetricsCollector metricsCollector
    ) {
        this.metricsCollector = metricsCollector;
        
        // Create custom components
        RetryPolicy retryPolicy = new ExponentialBackoffRetryPolicy(5, 200, 10000, 2.0);
        DeadLetterQueueHandler dlqHandler = new SqsDeadLetterQueueHandler(sqsClient, dlqQueueUrl);
        BackpressureStrategy backpressureStrategy = new AdaptiveBackpressureStrategy(0.8);
        PartitionKeyStrategy partitionKeyStrategy = new HashBasedPartitionKeyStrategy();
        
        this.delegate = new DefaultKinesisProducerService(
            properties, new JsonDataSerializer(), retryPolicy, dlqHandler,
            metricsCollector, backpressureStrategy, partitionKeyStrategy
        );
    }

    @Override
    public CompletableFuture<SendResult> send(String streamName, Object data) {
        return delegate.send(streamName, data);
    }

    // Implement other methods by delegating to delegate...
}
```

---

## Configuration Examples

### Application Properties
```yaml
kinesis:
  producer:
    maxConnections: 24
    requestTimeout: 6000
    recordMaxBufferedTime: 100
    recordTtl: 30000
    aggregationEnabled: "true"
    maxUserRecordsPerRequest: 500
    maxUserRecordSizeBytes: 1048576
    bulkOperationTimeout: 30000
    maxBatchSize: 100
  consumer:
    thread-pool:
      size: 8
      queueCapacity: 1000
      shutdownTimeoutMs: 30000
  cross-account:
    enabled: true
    defaultTargetAccountId: "123456789012"
    defaultRoleArn: "arn:aws:iam::123456789012:role/CrossAccountKinesisRole"
  schema-registry:
    enabled: true
    registryName: "my-registry"
    schemaName: "my-schema"
    schemaVersion: "1"
    dataFormat: "JSON"
    validateOnConsume: true
    validateOnProduce: true
    failOnValidationError: true
```

### Spring Configuration
```java
@Configuration
public class KinesisConfiguration {
    
    @Bean
    public RetryPolicy retryPolicy() {
        return new ExponentialBackoffRetryPolicy(3, 100, 5000, 2.0);
    }
    
    @Bean
    public DeadLetterQueueHandler dlqHandler(AmazonSQS sqsClient) {
        return new SqsDeadLetterQueueHandler(sqsClient, "https://sqs.region.amazonaws.com/account/dlq");
    }
    
    @Bean
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry) {
        return new MicrometerMetricsCollector(meterRegistry);
    }
    
    @Bean
    public BackpressureStrategy backpressureStrategy() {
        return new AdaptiveBackpressureStrategy(0.8);
    }
    
    @Bean
    public PartitionKeyStrategy partitionKeyStrategy() {
        return new HashBasedPartitionKeyStrategy();
    }
    
    @Bean
    public KinesisProducerService kinesisProducerService(
        KinesisProperties properties,
        RetryPolicy retryPolicy,
        DeadLetterQueueHandler dlqHandler,
        MetricsCollector metricsCollector,
        BackpressureStrategy backpressureStrategy,
        PartitionKeyStrategy partitionKeyStrategy
    ) {
        return new DefaultKinesisProducerService(
            properties, new JsonDataSerializer(), retryPolicy, dlqHandler,
            metricsCollector, backpressureStrategy, partitionKeyStrategy
        );
    }
}
```

---

## Testing Examples

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class CustomKinesisProducerServiceTest {
    
    @Mock
    private ProducerConfiguration mockConfig;
    
    @Mock
    private MetricsCollector mockMetricsCollector;
    
    @Test
    void testCustomRetryPolicy() {
        RetryPolicy customRetryPolicy = new RetryPolicy() {
            @Override
            public boolean shouldRetry(int attempt, Throwable lastException) {
                return attempt < 2 && lastException instanceof IOException;
            }
            
            @Override
            public long getBackoffDelay(int attempt, Throwable lastException) {
                return 50 * attempt;
            }
            
            @Override
            public int getMaxAttempts() {
                return 2;
            }
        };
        
        assertTrue(customRetryPolicy.shouldRetry(1, new IOException()));
        assertFalse(customRetryPolicy.shouldRetry(2, new IOException()));
        assertEquals(50, customRetryPolicy.getBackoffDelay(1, new IOException()));
    }
    
    @Test
    void testCustomDlqHandler() {
        List<Object> failedRecords = new ArrayList<>();
        DeadLetterQueueHandler customDlqHandler = (record, error) -> {
            failedRecords.add(record);
        };
        
        Object testRecord = new TestRecord("test-data");
        RuntimeException testError = new RuntimeException("Test error");
        
        customDlqHandler.handle(testRecord, testError);
        
        assertEquals(1, failedRecords.size());
        assertEquals(testRecord, failedRecords.get(0));
    }
    
    @Test
    void testCustomMetricsCollector() {
        AtomicInteger successCount = new AtomicInteger(0);
        MetricsCollector customMetricsCollector = new MetricsCollector() {
            @Override
            public void recordSendSuccess(String streamName, Object record) {
                successCount.incrementAndGet();
            }
            
            // Implement other methods...
        };
        
        Object testRecord = new TestRecord("test-data");
        customMetricsCollector.recordSendSuccess("test-stream", testRecord);
        
        assertEquals(1, successCount.get());
    }
}
```

### Integration Tests
```java
@SpringBootTest
class KinesisIntegrationTest {
    
    @Autowired
    private KinesisTemplate kinesisTemplate;
    
    @Test
    void testBulkSendWithRetryAndDlq() throws Exception {
        List<Object> records = Arrays.asList(
            new TestRecord("data1"),
            new TestRecord("data2"),
            new TestRecord("data3")
        );
        
        BulkSendResult result = kinesisTemplate.sendBulk("test-stream", records).get();
        
        assertNotNull(result);
        assertTrue(result.getSuccessfulRecords() > 0);
        // Check metrics and DLQ if any records failed
    }
    
    @Test
    void testAsyncSend() {
        TestRecord record = new TestRecord("async-data");
        
        // Async send should not block
        kinesisTemplate.sendAsync("test-stream", record);
        
        // Wait a bit for async processing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## Best Practices

1. **Retry Logic**: Use exponential backoff for transient failures, avoid retrying on permanent failures.
2. **DLQ**: Always implement DLQ for production systems to handle permanently failed records.
3. **Metrics**: Collect comprehensive metrics for monitoring and alerting.
4. **Backpressure**: Implement backpressure to prevent system overload.
5. **Partition Keys**: Use consistent partition key strategies for even distribution.
6. **Testing**: Test all custom implementations thoroughly with unit and integration tests.

---

## Performance Considerations

1. **Batch Size**: Adjust `maxBatchSize` based on your throughput requirements.
2. **Thread Pool**: Configure thread pool size based on CPU cores and I/O patterns.
3. **Retry Delays**: Balance retry delays with latency requirements.
4. **Backpressure Thresholds**: Set appropriate backpressure thresholds to prevent overload.
5. **Metrics Overhead**: Consider the overhead of metrics collection in high-throughput scenarios.

---

This documentation provides comprehensive coverage of all advanced features. For specific implementation details, refer to the source code and unit tests. 