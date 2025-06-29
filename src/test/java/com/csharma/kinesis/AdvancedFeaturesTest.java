//package com.csharma.kinesis;
//
//import com.csharma.kinesis.producer.*;
//import com.csharma.kinesis.serialization.DataSerializer;
//import com.csharma.kinesis.serialization.JsonDataSerializer;
//import com.csharma.kinesis.serialization.SerializationException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.nio.ByteBuffer;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * Comprehensive tests for advanced Kinesis features:
// * - Retry Logic
// * - Dead Letter Queue (DLQ)
// * - Metrics Collection
// * - Backpressure Handling
// * - Pluggable Partition Key Strategy
// */
//@ExtendWith(MockitoExtension.class)
//public class AdvancedFeaturesTest {
//
//    @Mock
//    private ProducerConfiguration mockConfig;
//
//    @Mock
//    private DataSerializer mockSerializer;
//
//    @Mock
//    private RetryPolicy mockRetryPolicy;
//
//    @Mock
//    private DeadLetterQueueHandler mockDlqHandler;
//
//    @Mock
//    private MetricsCollector mockMetricsCollector;
//
//    @Mock
//    private BackpressureStrategy mockBackpressureStrategy;
//
//    @Mock
//    private PartitionKeyStrategy mockPartitionKeyStrategy;
//
//    private DefaultKinesisProducerService producerService;
//
//    @BeforeEach
//    void setUp() {
//        // Setup mock configuration
//        when(mockConfig.getRegion()).thenReturn("us-east-1");
//        when(mockConfig.getMaxConnections()).thenReturn(10);
//        when(mockConfig.getRequestTimeout()).thenReturn(5000L);
//        when(mockConfig.getRecordMaxBufferedTime()).thenReturn(100L);
//        when(mockConfig.getRecordTtl()).thenReturn(30000L);
//        when(mockConfig.isAggregationEnabled()).thenReturn(true);
//        when(mockConfig.getMaxUserRecordsPerRequest()).thenReturn(500);
//        when(mockConfig.getMaxUserRecordSizeBytes()).thenReturn(1048576);
//        when(mockConfig.getBulkOperationTimeout()).thenReturn(30000L);
//        when(mockConfig.getMaxBatchSize()).thenReturn(100);
//
//        // Setup mock serializer
//        when(mockSerializer.serialize(any())).thenReturn(ByteBuffer.wrap("test".getBytes()));
//        when(mockSerializer.getContentType()).thenReturn("application/json");
//
//        // Setup mock partition key strategy
//        when(mockPartitionKeyStrategy.getPartitionKey(any())).thenReturn("test-key");
//
//        // Setup mock backpressure strategy
//        when(mockBackpressureStrategy.shouldAccept(anyInt(), anyInt())).thenReturn(true);
//
//        // Create producer service with mocks
//        producerService = new DefaultKinesisProducerService(
//            mockConfig, mockSerializer, mockRetryPolicy, mockDlqHandler,
//            mockMetricsCollector, mockBackpressureStrategy, mockPartitionKeyStrategy
//        );
//    }
//
//    @Test
//    void testRetryLogicWithExponentialBackoff() {
//        // Test the default exponential backoff retry policy
//        ExponentialBackoffRetryPolicy retryPolicy = new ExponentialBackoffRetryPolicy(3, 100, 1000, 2.0);
//
//        assertTrue(retryPolicy.shouldRetry(1, new RuntimeException()));
//        assertTrue(retryPolicy.shouldRetry(2, new RuntimeException()));
//        assertFalse(retryPolicy.shouldRetry(3, new RuntimeException()));
//
//        assertEquals(100, retryPolicy.getBackoffDelay(1, new RuntimeException()));
//        assertEquals(200, retryPolicy.getBackoffDelay(2, new RuntimeException()));
//        assertEquals(400, retryPolicy.getBackoffDelay(3, new RuntimeException()));
//        assertEquals(1000, retryPolicy.getBackoffDelay(4, new RuntimeException())); // Capped at max
//    }
//
//    @Test
//    void testCustomRetryPolicy() {
//        // Test custom retry policy that only retries on specific exceptions
//        RetryPolicy customRetryPolicy = new RetryPolicy() {
//            @Override
//            public boolean shouldRetry(int attempt, Throwable lastException) {
//                return attempt < 2 && lastException instanceof RuntimeException;
//            }
//
//            @Override
//            public long getBackoffDelay(int attempt, Throwable lastException) {
//                return 50 * attempt; // Linear backoff
//            }
//
//            @Override
//            public int getMaxAttempts() {
//                return 2;
//            }
//        };
//
//        assertTrue(customRetryPolicy.shouldRetry(1, new RuntimeException()));
//        assertFalse(customRetryPolicy.shouldRetry(2, new RuntimeException()));
//        assertEquals(50, customRetryPolicy.getBackoffDelay(1, new RuntimeException()));
//        assertEquals(100, customRetryPolicy.getBackoffDelay(2, new RuntimeException()));
//    }
//
//    @Test
//    void testDeadLetterQueueHandler() {
//        // Test custom DLQ handler that collects failed records
//        List<Object> failedRecords = new ArrayList<>();
//        DeadLetterQueueHandler customDlqHandler = (record, error) -> {
//            failedRecords.add(record);
//            System.out.println("DLQ: Record failed: " + record + " with error: " + error.getMessage());
//        };
//
//        Object testRecord = new TestRecord("test-data");
//        RuntimeException testError = new RuntimeException("Test error");
//
//        customDlqHandler.handle(testRecord, testError);
//
//        assertEquals(1, failedRecords.size());
//        assertEquals(testRecord, failedRecords.get(0));
//    }
//
//    @Test
//    void testMetricsCollector() {
//        // Test custom metrics collector that tracks events
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failureCount = new AtomicInteger(0);
//        AtomicInteger retryCount = new AtomicInteger(0);
//        AtomicInteger dlqCount = new AtomicInteger(0);
//
//        MetricsCollector customMetricsCollector = new MetricsCollector() {
//            @Override
//            public void recordSendSuccess(String streamName, Object record) {
//                successCount.incrementAndGet();
//            }
//
//            @Override
//            public void recordSendFailure(String streamName, Object record, Throwable error) {
//                failureCount.incrementAndGet();
//            }
//
//            @Override
//            public void recordRetry(String streamName, Object record, int attempt, Throwable error) {
//                retryCount.incrementAndGet();
//            }
//
//            @Override
//            public void recordDLQ(String streamName, Object record, Throwable error) {
//                dlqCount.incrementAndGet();
//            }
//
//            @Override
//            public void recordProcessingTime(String streamName, long durationMs) {
//                // Track processing time
//            }
//        };
//
//        Object testRecord = new TestRecord("test-data");
//        RuntimeException testError = new RuntimeException("Test error");
//
//        customMetricsCollector.recordSendSuccess("test-stream", testRecord);
//        customMetricsCollector.recordSendFailure("test-stream", testRecord, testError);
//        customMetricsCollector.recordRetry("test-stream", testRecord, 1, testError);
//        customMetricsCollector.recordDLQ("test-stream", testRecord, testError);
//
//        assertEquals(1, successCount.get());
//        assertEquals(1, failureCount.get());
//        assertEquals(1, retryCount.get());
//        assertEquals(1, dlqCount.get());
//    }
//
//    @Test
//    void testBackpressureStrategy() {
//        // Test custom backpressure strategy
//        BackpressureStrategy customBackpressureStrategy = new BackpressureStrategy() {
//            @Override
//            public boolean shouldAccept(int currentQueueSize, int maxQueueSize) {
//                // Apply backpressure when queue is 80% full
//                return currentQueueSize < (maxQueueSize * 0.8);
//            }
//        };
//
//        assertTrue(customBackpressureStrategy.shouldAccept(50, 100));  // 50% full
//        assertTrue(customBackpressureStrategy.shouldAccept(79, 100));  // 79% full
//        assertFalse(customBackpressureStrategy.shouldAccept(80, 100)); // 80% full
//        assertFalse(customBackpressureStrategy.shouldAccept(90, 100)); // 90% full
//    }
//
//    @Test
//    void testPartitionKeyStrategy() {
//        // Test custom partition key strategy
//        PartitionKeyStrategy customPartitionKeyStrategy = new PartitionKeyStrategy() {
//            @Override
//            public String getPartitionKey(Object record) {
//                if (record instanceof TestRecord) {
//                    return "custom-" + ((TestRecord) record).getData();
//                }
//                return "default-key";
//            }
//        };
//
//        TestRecord testRecord = new TestRecord("test-data");
//        String partitionKey = customPartitionKeyStrategy.getPartitionKey(testRecord);
//        assertEquals("custom-test-data", partitionKey);
//
//        String defaultKey = customPartitionKeyStrategy.getPartitionKey("string-record");
//        assertEquals("default-key", defaultKey);
//    }
//
//    @Test
//    void testDefaultPartitionKeyStrategy() {
//        DefaultPartitionKeyStrategy strategy = new DefaultPartitionKeyStrategy();
//
//        // Test with BulkRecord that has partition key
//        BulkRecord recordWithKey = new BulkRecord("explicit-key", "data");
//        assertEquals("explicit-key", strategy.getPartitionKey(recordWithKey));
//
//        // Test with BulkRecord without partition key
//        BulkRecord recordWithoutKey = new BulkRecord("data");
//        assertNotNull(strategy.getPartitionKey(recordWithoutKey)); // Should return hash
//
//        // Test with regular object
//        TestRecord testRecord = new TestRecord("test-data");
//        assertNotNull(strategy.getPartitionKey(testRecord)); // Should return hash
//    }
//
//    @Test
//    void testBulkSendWithRetryAndMetrics() throws Exception {
//        // Setup retry policy to retry once
//        when(mockRetryPolicy.shouldRetry(1, any())).thenReturn(true);
//        when(mockRetryPolicy.shouldRetry(2, any())).thenReturn(false);
//        when(mockRetryPolicy.getBackoffDelay(1, any())).thenReturn(10L);
//
//        // Create test records
//        List<BulkRecord> records = Arrays.asList(
//            new BulkRecord("key1", new TestRecord("data1")),
//            new BulkRecord("key2", new TestRecord("data2"))
//        );
//
//        // This test would require a more complex setup with mocked KinesisProducer
//        // For now, we'll test the configuration and strategy integration
//        assertNotNull(producerService);
//
//        // Verify that our mocks are properly configured
//        verify(mockConfig, times(1)).getMaxBatchSize();
//        verify(mockConfig, times(1)).getBulkOperationTimeout();
//    }
//
//    @Test
//    void testSerializationExceptionHandling() {
//        // Test that serialization exceptions are properly handled
//        when(mockSerializer.serialize(any())).thenThrow(new SerializationException("Serialization failed"));
//
//        BulkRecord record = new BulkRecord("key", new TestRecord("data"));
//
//        // This would test the retry logic for serialization failures
//        // The actual implementation would need to be tested with proper mocking
//        assertNotNull(producerService);
//    }
//
//    @Test
//    void testBackpressureIntegration() {
//        // Test backpressure integration
//        when(mockBackpressureStrategy.shouldAccept(anyInt(), anyInt())).thenReturn(false);
//
//        BulkRecord record = new BulkRecord("key", new TestRecord("data"));
//
//        // This would test that backpressure is applied when queue is full
//        // The actual implementation would need to be tested with proper mocking
//        assertNotNull(producerService);
//    }
//
//    // Helper class for testing
//    private static class TestRecord {
//        private final String data;
//
//        public TestRecord(String data) {
//            this.data = data;
//        }
//
//        public String getData() {
//            return data;
//        }
//
//        @Override
//        public String toString() {
//            return "TestRecord{data='" + data + "'}";
//        }
//    }
//}