package com.csharma.kinesis.producer;

import com.csharma.kinesis.common.BulkRecord;
import com.csharma.kinesis.common.BulkSendResult;
import com.csharma.kinesis.common.SendResult;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Kinesis producer operations following Single Responsibility Principle.
 * Responsible for sending records to Kinesis streams.
 */
public interface KinesisProducerService {
    
    /**
     * Send a single record to Kinesis stream
     * 
     * @param streamName the name of the stream
     * @param data the data to send
     * @return CompletableFuture with SendResult
     */
    CompletableFuture<SendResult> send(String streamName, Object data);
    
    /**
     * Send a single record to Kinesis stream with partition key
     * 
     * @param streamName the name of the stream
     * @param partitionKey the partition key
     * @param data the data to send
     * @return CompletableFuture with SendResult
     */
    CompletableFuture<SendResult> send(String streamName, String partitionKey, Object data);
    
    /**
     * Send raw data to Kinesis stream
     * 
     * @param streamName the name of the stream
     * @param partitionKey the partition key
     * @param data the raw data to send
     * @return CompletableFuture with SendResult
     */
    CompletableFuture<SendResult> sendRaw(String streamName, String partitionKey, ByteBuffer data);

    /**
     * Send multiple records in a bulk transaction
     *
     * @param streamName the name of the stream
     * @param records the list of records to send
     * @return CompletableFuture with BulkSendResult
     */
    CompletableFuture<BulkSendResult> sendBulk(String streamName, List<Object> records);

    /**
     * Send multiple records with partition keys in a bulk transaction
     * 
     * @param streamName the name of the stream
     * @param records the list of records with partition keys
     * @return CompletableFuture with BulkSendResult
     */
    CompletableFuture<BulkSendResult> sendBulkRecords(String streamName, List<BulkRecord> records);
    
    /**
     * Send a record asynchronously without waiting for completion
     * 
     * @param streamName the name of the stream
     * @param data the data to send
     */
    void sendAsync(String streamName, Object data);
    
    /**
     * Send a record asynchronously with partition key without waiting for completion
     * 
     * @param streamName the name of the stream
     * @param partitionKey the partition key
     * @param data the data to send
     */
    void sendAsync(String streamName, String partitionKey, Object data);
    
    /**
     * Flush any pending records
     */
    void flush();
    
    /**
     * Shutdown the producer service
     */
    void shutdown();
} 