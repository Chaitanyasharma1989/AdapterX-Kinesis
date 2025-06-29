package com.csharma.kinesis.kinesistemplate;

import com.csharma.kinesis.common.BulkRecord;
import com.csharma.kinesis.common.BulkSendResult;
import com.csharma.kinesis.common.SendResult;
import com.csharma.kinesis.producer.*;
import com.csharma.kinesis.configuration.KinesisProperties;
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


@Component
public class KinesisTemplate {
    private static final Logger log = LoggerFactory.getLogger(KinesisTemplate.class);

    @Autowired
    private KinesisProperties kinesisProperties;

    private KinesisProducerService producerService;

    @PostConstruct
    public void init() {
        DataSerializer dataSerializer = new JsonDataSerializer();
        this.producerService = new DefaultKinesisProducerService(kinesisProperties, dataSerializer);
        log.info("KinesisTemplate initialized with producer service");
    }

    public CompletableFuture<SendResult> send(String streamName, Object data) {
        return producerService.send(streamName, data);
    }

    public CompletableFuture<SendResult> send(String streamName, String partitionKey, Object data) {
        return producerService.send(streamName, partitionKey, data);
    }

    public CompletableFuture<SendResult> sendRaw(String streamName, String partitionKey, ByteBuffer data) {
        return producerService.sendRaw(streamName, partitionKey, data);
    }

    public CompletableFuture<BulkSendResult> sendBulk(String streamName, List<Object> records) {
        return producerService.sendBulk(streamName, records);
    }

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