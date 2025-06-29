package com.csharma.kinesis.producer;

/**
 * Interface for pluggable partition key assignment.
 */
public interface PartitionKeyStrategy {
    /**
     * Returns the partition key for the given record.
     */
    String getPartitionKey(Object record);
} 