package com.csharma.kinesis.producer;

/**
 * Interface for handling dead letter queue (DLQ) operations.
 */
public interface DeadLetterQueueHandler {
    /**
     * Handle a permanently failed record (after all retries).
     * @param record The failed record.
     * @param error The error that caused the failure.
     */
    void handle(Object record, Throwable error);
} 