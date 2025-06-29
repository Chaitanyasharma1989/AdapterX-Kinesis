package com.csharma.kinesis.recordprocessor;

import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * Interface for record handling following Single Responsibility Principle.
 * Responsible for processing individual records.
 */
public interface RecordHandler {
    
    /**
     * Handle a record
     * 
     * @param data the record data as string
     * @param record the original Kinesis record
     * @throws Exception if processing fails
     */
    void handleRecord(String data, KinesisClientRecord record) throws Exception;
} 