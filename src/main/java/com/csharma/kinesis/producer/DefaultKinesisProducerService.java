package com.csharma.kinesis.producer;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.csharma.kinesis.serialization.DataSerializer;
import com.csharma.kinesis.serialization.JsonDataSerializer;
import com.csharma.kinesis.serialization.SerializationException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Default implementation of KinesisProducerService with bulk and async capabilities.
 * Follows Open/Closed Principle by being extensible through interfaces.
 */
public class DefaultKinesisProducerService implements KinesisProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultKinesisProducerService.class);
    
    private final KinesisProducer producer;
    private final DataSerializer dataSerializer;
    private final ProducerConfiguration configuration;
    private final ExecutorService asyncExecutor;
    private final RetryPolicy retryPolicy;
    private final DeadLetterQueueHandler dlqHandler;
    private final MetricsCollector metricsCollector;
    private final BackpressureStrategy backpressureStrategy;
    private final PartitionKeyStrategy partitionKeyStrategy;
    
    public DefaultKinesisProducerService(ProducerConfiguration configuration) {
        this(configuration, new JsonDataSerializer());
    }
    
    public DefaultKinesisProducerService(ProducerConfiguration configuration, DataSerializer dataSerializer) {
        this(configuration, dataSerializer, new ExponentialBackoffRetryPolicy(),
             new LoggingDeadLetterQueueHandler(), new LoggingMetricsCollector(),
             new SimpleBackpressureStrategy(), new DefaultPartitionKeyStrategy());
    }
    
    public DefaultKinesisProducerService(ProducerConfiguration configuration, DataSerializer dataSerializer,
                                         RetryPolicy retryPolicy, DeadLetterQueueHandler dlqHandler,
                                         MetricsCollector metricsCollector, BackpressureStrategy backpressureStrategy,
                                         PartitionKeyStrategy partitionKeyStrategy) {
        this.configuration = configuration;
        this.dataSerializer = dataSerializer;
        this.producer = createProducer();
        this.asyncExecutor = Executors.newCachedThreadPool();
        this.retryPolicy = retryPolicy;
        this.dlqHandler = dlqHandler;
        this.metricsCollector = metricsCollector;
        this.backpressureStrategy = backpressureStrategy;
        this.partitionKeyStrategy = partitionKeyStrategy;
    }
    
    private KinesisProducer createProducer() {
        KinesisProducerConfiguration config = new KinesisProducerConfiguration()
                .setRegion(configuration.getRegion())
                .setMaxConnections(configuration.getMaxConnections())
                .setRequestTimeout(configuration.getRequestTimeout())
                .setRecordMaxBufferedTime(configuration.getRecordMaxBufferedTime())
                .setRecordTtl(configuration.getRecordTtl())
                .setAggregationEnabled(configuration.isAggregationEnabled())
                .setAggregationMaxCount(configuration.getMaxUserRecordsPerRequest())
                .setAggregationMaxSize(configuration.getMaxUserRecordSizeBytes());

        if (configuration.getEndpoint() != null) {
            config.setKinesisEndpoint(configuration.getEndpoint());
        }

        return new KinesisProducer(config);
    }
    
    @Override
    public CompletableFuture<SendResult> send(String streamName, Object data) {
        return send(streamName, null, data);
    }
    
    @Override
    public CompletableFuture<SendResult> send(String streamName, String partitionKey, Object data) {
        try {
            ByteBuffer serializedData = dataSerializer.serialize(data);
            return sendRaw(streamName, partitionKey, serializedData);
        } catch (SerializationException e) {
            CompletableFuture<SendResult> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    @Override
    public CompletableFuture<SendResult> sendRaw(String streamName, String partitionKey, ByteBuffer data) {
        CompletableFuture<SendResult> future = new CompletableFuture<>();
        String actualPartitionKey = partitionKey != null ? partitionKey : String.valueOf(System.currentTimeMillis());
        
        ListenableFuture<UserRecordResult> kplFuture = producer.addUserRecord(streamName, actualPartitionKey, data);
        
        Futures.addCallback(kplFuture, new FutureCallback<UserRecordResult>() {
            @Override
            public void onSuccess(UserRecordResult result) {
                SendResult sendResult = new SendResult(streamName, actualPartitionKey, 
                                                     result.getShardId(), result.getSequenceNumber());
                future.complete(sendResult);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to send record to stream: {}", streamName, t);
                future.completeExceptionally(t);
            }
        }, asyncExecutor);

        return future;
    }
    
    @Override
    public CompletableFuture<BulkSendResult> sendBulk(String streamName, List<Object> records) {
        List<BulkRecord> bulkRecords = new ArrayList<>();
        for (Object record : records) {
            bulkRecords.add(new BulkRecord(record));
        }
        return sendBulkRecords(streamName, bulkRecords);
    }
    
    @Override
    public CompletableFuture<BulkSendResult> sendBulkRecords(String streamName, List<BulkRecord> records) {
        long startTime = System.currentTimeMillis();
        List<CompletableFuture<SendResult>> futures = new ArrayList<>();
        List<SendResult> successfulResults = new ArrayList<>();
        List<FailedRecord> failedResults = new ArrayList<>();
        
        int batchSize = configuration.getMaxBatchSize();
        for (int i = 0; i < records.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, records.size());
            List<BulkRecord> batch = records.subList(i, endIndex);
            
            for (BulkRecord record : batch) {
                CompletableFuture<SendResult> future = sendRecordWithRetry(streamName, record);
                futures.add(future);
            }
        }
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        return allFutures.thenApply(v -> {
            long duration = System.currentTimeMillis() - startTime;
            
            for (int i = 0; i < futures.size(); i++) {
                CompletableFuture<SendResult> future = futures.get(i);
                BulkRecord originalRecord = records.get(i);
                
                try {
                    SendResult result = future.get(configuration.getBulkOperationTimeout(), TimeUnit.MILLISECONDS);
                    successfulResults.add(result);
                    metricsCollector.recordSendSuccess(streamName, originalRecord);
                } catch (Exception e) {
                    FailedRecord failedRecord = new FailedRecord(
                        originalRecord.getPartitionKey(),
                        originalRecord.getData(),
                        e
                    );
                    failedResults.add(failedRecord);
                    metricsCollector.recordSendFailure(streamName, originalRecord, e);
                    dlqHandler.handle(originalRecord, e);
                    metricsCollector.recordDLQ(streamName, originalRecord, e);
                }
            }
            metricsCollector.recordProcessingTime(streamName, duration);
            return new BulkSendResult(
                streamName,
                records.size(),
                successfulResults.size(),
                failedResults.size(),
                successfulResults,
                failedResults,
                duration
            );
        });
    }
    
    private CompletableFuture<SendResult> sendRecordWithRetry(String streamName, BulkRecord record) {
        CompletableFuture<SendResult> future = new CompletableFuture<>();
        sendWithRetry(streamName, record, 1, future);
        return future;
    }
    
    private void sendWithRetry(String streamName, BulkRecord record, int attempt, CompletableFuture<SendResult> future) {
        if (!backpressureStrategy.shouldAccept(((ThreadPoolExecutor) asyncExecutor).getQueue().size(), configuration.getMaxBatchSize())) {
            future.completeExceptionally(new RuntimeException("Backpressure: queue is full"));
            return;
        }
        try {
            ByteBuffer serializedData = dataSerializer.serialize(record.getData());
            String partitionKey = partitionKeyStrategy.getPartitionKey(record);
            ListenableFuture<UserRecordResult> kplFuture = producer.addUserRecord(streamName, partitionKey, serializedData);
            Futures.addCallback(kplFuture, new FutureCallback<UserRecordResult>() {
                @Override
                public void onSuccess(UserRecordResult result) {
                    future.complete(new SendResult(streamName, partitionKey, result.getShardId(), result.getSequenceNumber()));
                }
                @Override
                public void onFailure(Throwable t) {
                    if (retryPolicy.shouldRetry(attempt, t)) {
                        metricsCollector.recordRetry(streamName, record, attempt, t);
                        try {
                            Thread.sleep(retryPolicy.getBackoffDelay(attempt, t));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        sendWithRetry(streamName, record, attempt + 1, future);
                    } else {
                        future.completeExceptionally(t);
                    }
                }
            }, asyncExecutor);
        } catch (Exception e) {
            if (retryPolicy.shouldRetry(attempt, e)) {
                metricsCollector.recordRetry(streamName, record, attempt, e);
                try {
                    Thread.sleep(retryPolicy.getBackoffDelay(attempt, e));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                sendWithRetry(streamName, record, attempt + 1, future);
            } else {
                future.completeExceptionally(e);
            }
        }
    }
    
    @Override
    public void sendAsync(String streamName, Object data) {
        sendAsync(streamName, null, data);
    }
    
    @Override
    public void sendAsync(String streamName, String partitionKey, Object data) {
        asyncExecutor.submit(() -> {
            try {
                send(streamName, partitionKey, data).get();
                log.debug("Async send completed for stream: {}", streamName);
            } catch (Exception e) {
                log.error("Async send failed for stream: {}", streamName, e);
            }
        });
    }
    
    @Override
    public void flush() {
        try {
            producer.flushSync();
            log.debug("Producer flush completed");
        } catch (Exception e) {
            log.error("Failed to flush producer", e);
        }
    }
    
    @Override
    public void shutdown() {
        try {
            flush();
            producer.destroy();
            asyncExecutor.shutdown();
            if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            log.info("Producer service shutdown completed");
        } catch (Exception e) {
            log.error("Error during producer service shutdown", e);
        }
    }
} 