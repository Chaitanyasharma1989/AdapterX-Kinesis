package com.csharma.kinesis.strategy;

public interface PartitionKeyStrategy {
    String getPartitionKey(Object record);
} 