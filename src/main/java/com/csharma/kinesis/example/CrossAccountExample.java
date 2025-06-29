package com.csharma.kinesis.example;

import com.csharma.kinesis.listener.KinesisListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

@Component
public class CrossAccountExample {
    
    private static final Logger log = LoggerFactory.getLogger(CrossAccountExample.class);

    @KinesisListener(streamName = "local-stream")
    public void processLocalRecord(String data) {
        log.info("Processing local record: {}", data);
    }
    

    @KinesisListener(
        streamName = "production-stream",
        targetAccountId = "123456789012"  // Uses global defaults for role ARN, external ID, etc.
    )
    public void processProductionRecord(String data) {
        log.info("Processing production record from cross-account: {}", data);
        // Process record from production account
    }

    @KinesisListener(
        streamName = "development-stream",
        targetAccountId = "987654321098",
        roleArn = "arn:aws:iam::987654321098:role/DevKinesisRole",
        externalId = "dev-external-id",
        sessionName = "dev-session"
    )
    public void processDevelopmentRecord(String data) {
        log.info("Processing development record with custom config: {}", data);
        // Process record from development account with custom configuration
    }

    @KinesisListener(
        streamName = "high-throughput-stream",
        targetAccountId = "123456789012",
        enhancedFanOut = true,
        consumerName = "cross-account-efo-consumer"
    )
    public void processHighThroughputRecord(String data) {
        log.info("Processing high-throughput record with EFO: {}", data);
        // Process record from cross-account with Enhanced Fan-Out
    }

    @KinesisListener(
        streamName = "detailed-stream",
        targetAccountId = "123456789012"
    )
    public void processDetailedRecord(KinesisClientRecord record) {
        log.info("Processing detailed record from cross-account: partitionKey={}, sequenceNumber={}", 
                record.partitionKey(), record.sequenceNumber());
        
        String data = new String(record.data().array());
        log.info("Record data: {}", data);
    }

    @KinesisListener(
        streamName = "custom-polling-stream",
        targetAccountId = "123456789012",
        maxRecords = 500,
        idleTimeBetweenReadsInMillis = 500
    )
    public void processCustomPollingRecord(String data) {
        log.info("Processing custom polling record: {}", data);
        // Process record with custom polling configuration
    }

    @KinesisListener(
        streamName = "account-a-stream",
        targetAccountId = "111111111111"
    )
    public void processAccountARecord(String data) {
        log.info("Processing Account A record: {}", data);
    }
    
    @KinesisListener(
        streamName = "account-b-stream",
        targetAccountId = "222222222222",
        roleArn = "arn:aws:iam::222222222222:role/AccountBKinesisRole",
        externalId = "account-b-external-id"
    )
    public void processAccountBRecord(String data) {
        log.info("Processing Account B record: {}", data);
    }

    @KinesisListener(
        streamName = "error-handling-stream",
        targetAccountId = "123456789012"
    )
    public void processWithErrorHandling(String data) {
        try {
            log.info("Processing record with error handling: {}", data);

            if (data.contains("error")) {
                throw new RuntimeException("Simulated processing error");
            }

            log.info("Successfully processed record");
            
        } catch (Exception e) {
            log.error("Error processing cross-account record: {}", data, e);
            // Handle the error appropriately
            // You might want to send to a dead letter queue or retry
        }
    }
} 