package com.csharma.kinesis.example;

import com.csharma.kinesis.listener.KinesisListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * Example class demonstrating schema validation with Kinesis consumer.
 * 
 * This example shows different ways to configure schema validation:
 * 1. Same-account consumer with schema validation
 * 2. Cross-account consumer with schema validation
 * 3. Schema validation with different data formats
 * 4. Schema validation with custom error handling
 */
@Component
public class SchemaValidationExample {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaValidationExample.class);
    
    /**
     * Same-account consumer with schema validation using global defaults
     * Uses the default schema registry, schema name, and version from configuration
     */
    @KinesisListener(
        streamName = "user-events-stream",
        validateSchema = true
    )
    public void processUserEvent(String data) {
        log.info("Processing validated user event: {}", data);
        // Process user event data that has been validated against schema
    }
    
    /**
     * Same-account consumer with custom schema validation configuration
     * Overrides global defaults with specific schema settings
     */
    @KinesisListener(
        streamName = "order-events-stream",
        validateSchema = true,
        schemaRegistryName = "order-schema-registry",
        schemaName = "order-schema",
        schemaVersion = "2.0",
        dataFormat = "JSON",
        failOnValidationError = true
    )
    public void processOrderEvent(String data) {
        log.info("Processing validated order event: {}", data);
        // Process order event data with custom schema validation
    }
    
    /**
     * Cross-account consumer with schema validation
     * Combines cross-account access with schema validation
     */
    @KinesisListener(
        streamName = "cross-account-events-stream",
        targetAccountId = "123456789012",
        validateSchema = true,
        schemaRegistryName = "cross-account-schema-registry",
        schemaName = "event-schema"
    )
    public void processCrossAccountEvent(String data) {
        log.info("Processing cross-account validated event: {}", data);
        // Process event from cross-account with schema validation
    }
    
    /**
     * Schema validation with Avro data format
     * Demonstrates Avro schema validation
     */
    @KinesisListener(
        streamName = "avro-events-stream",
        validateSchema = true,
        schemaName = "avro-user-event",
        dataFormat = "AVRO",
        failOnValidationError = false  // Don't fail on validation errors, just log them
    )
    public void processAvroEvent(String data) {
        log.info("Processing Avro validated event: {}", data);
        // Process Avro event data
    }
    
    /**
     * Schema validation with KinesisClientRecord parameter
     * Demonstrates accessing full record with schema validation
     */
    @KinesisListener(
        streamName = "detailed-events-stream",
        validateSchema = true,
        schemaName = "detailed-event-schema"
    )
    public void processDetailedEvent(KinesisClientRecord record) {
        log.info("Processing detailed validated event: partitionKey={}, sequenceNumber={}", 
                record.partitionKey(), record.sequenceNumber());
        
        String data = new String(record.data().array());
        log.info("Validated event data: {}", data);
        
        // Process event with full Kinesis metadata and schema validation
    }
    
    /**
     * Schema validation with custom error handling
     * Demonstrates graceful handling of validation failures
     */
    @KinesisListener(
        streamName = "error-handling-stream",
        validateSchema = true,
        schemaName = "error-handling-schema",
        failOnValidationError = false  // Don't fail, handle errors gracefully
    )
    public void processWithErrorHandling(String data) {
        try {
            log.info("Processing event with error handling: {}", data);
            
            // Simulate some processing logic
            if (data.contains("error")) {
                throw new RuntimeException("Simulated processing error");
            }
            
            // Normal processing
            log.info("Successfully processed validated event");
            
        } catch (Exception e) {
            log.error("Error processing validated event: {}", data, e);
            // Handle the error appropriately
            // You might want to send to a dead letter queue or retry
        }
    }
    
    /**
     * Schema validation with different schema versions
     * Demonstrates version-specific schema validation
     */
    @KinesisListener(
        streamName = "versioned-events-stream",
        validateSchema = true,
        schemaName = "versioned-event-schema",
        schemaVersion = "1.2"
    )
    public void processVersionedEvent(String data) {
        log.info("Processing versioned validated event: {}", data);
        // Process event with specific schema version validation
    }
    
    /**
     * Schema validation with Enhanced Fan-Out
     * Combines schema validation with Enhanced Fan-Out for high throughput
     */
    @KinesisListener(
        streamName = "high-throughput-validated-stream",
        enhancedFanOut = true,
        consumerName = "schema-validated-efo-consumer",
        validateSchema = true,
        schemaName = "high-throughput-schema"
    )
    public void processHighThroughputValidatedEvent(String data) {
        log.info("Processing high-throughput validated event: {}", data);
        // Process event with both EFO and schema validation
    }
    
    /**
     * Cross-account schema validation with Enhanced Fan-Out
     * Demonstrates the full feature set: cross-account + EFO + schema validation
     */
    @KinesisListener(
        streamName = "full-feature-stream",
        targetAccountId = "123456789012",
        enhancedFanOut = true,
        consumerName = "full-feature-efo-consumer",
        validateSchema = true,
        schemaRegistryName = "full-feature-schema-registry",
        schemaName = "full-feature-schema",
        dataFormat = "JSON",
        failOnValidationError = true
    )
    public void processFullFeatureEvent(String data) {
        log.info("Processing full-feature event (cross-account + EFO + schema validation): {}", data);
        // Process event with all features enabled
    }
    
    /**
     * Schema validation with custom polling configuration
     * Demonstrates customizing polling parameters with schema validation
     */
    @KinesisListener(
        streamName = "custom-polling-validated-stream",
        maxRecords = 500,
        idleTimeBetweenReadsInMillis = 500,
        validateSchema = true,
        schemaName = "custom-polling-schema"
    )
    public void processCustomPollingValidatedEvent(String data) {
        log.info("Processing custom polling validated event: {}", data);
        // Process event with custom polling and schema validation
    }
} 