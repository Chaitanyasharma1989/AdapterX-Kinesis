package com.csharma.kinesis.producer;

/**
 * Interface for backpressure handling in producer.
 */
public interface BackpressureStrategy {
    /**
     * Returns true if the producer should accept more records, false to apply backpressure.
     */
    boolean shouldAccept(int currentQueueSize, int maxQueueSize);
} 