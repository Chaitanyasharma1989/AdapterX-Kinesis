package com.csharma.kinesis.kinesistemplate;

import com.csharma.kinesis.producer.*;
import com.csharma.kinesis.prpoerties.KinesisProperties;
import com.csharma.kinesis.serialization.DataSerializer;
import com.csharma.kinesis.serialization.JsonDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * KinesisTemplate - Facade for Kinesis operations following Facade Pattern.
 * Delegates to specialized services following Single Responsibility Principle.
 */
@Component
public class KinesisTemplate {
    private static final Logger log = LoggerFactory.getLogger(KinesisTemplate.class);

    @Autowired
    private KinesisProperties kinesisProperties;

    /**
     * -- GETTER --
     *  Get the underlying producer service for advanced operations
     *
     * @return the producer service
     */
    private KinesisProducerService producerService;

    @PostConstruct
    public void init() {
        DataSerializer dataSerializer = new JsonDataSerializer();
        this.producerService = new DefaultKinesisProducerService(kinesisProperties, dataSerializer);
        log.info("KinesisTemplate initialized with producer service");
    }

    /**
     * Send a single record to Kinesis stream
     * 
     * @param streamName the name of the stream
     * @param data the data to send
     * @return CompletableFuture with SendResult
     */
    public CompletableFuture<SendResult> send(String streamName, Object data) {
        return producerService.send(streamName, data);
    }

    /**
     * Send a single record to Kinesis stream with partition key
     * 
     * @param streamName the name of the stream
     * @param partitionKey the partition key
     * @param data the data to send
     * @return CompletableFuture with SendResult
     */
    public CompletableFuture<SendResult> send(String streamName, String partitionKey, Object data) {
        return producerService.send(streamName, partitionKey, data);
    }

    /**
     * Send raw data to Kinesis stream
     * 
     * @param streamName the name of the stream
     * @param partitionKey the partition key
     * @param data the raw data to send
     * @return CompletableFuture with SendResult
     */
    public CompletableFuture<SendResult> sendRaw(String streamName, String partitionKey, ByteBuffer data) {
        return producerService.sendRaw(streamName, partitionKey, data);
    }

    /**
     * Send bulk records to Kinesis stream
     * 
     * @param streamName the name of the stream
     * @param records the list of objects to send
     * @return CompletableFuture with BulkSendResult
     */
    public CompletableFuture<BulkSendResult> sendBulk(String streamName, List<Object> records) {
        return producerService.sendBulk(streamName, records);
    }

    /**
     * Send bulk records with custom configuration to Kinesis stream
     * 
     * @param streamName the name of the stream
     * @param records the list of BulkRecord objects with custom configuration
     * @return CompletableFuture with BulkSendResult
     */
    public CompletableFuture<BulkSendResult> sendBulkRecords(String streamName, List<BulkRecord> records) {
        return producerService.sendBulkRecords(streamName, records);
    }

    public void sendAsync(String streamName, Object data) {
        producerService.sendAsync(streamName, data);
    }

    public void sendAsync(String streamName, String partitionKey, Object data) {
        producerService.sendAsync(streamName, partitionKey, data);
    }

    public void flush() {
        producerService.flush();
    }

    @PreDestroy
    public void destroy() {
        if (producerService != null) {
            producerService.shutdown();
        }
    }
}