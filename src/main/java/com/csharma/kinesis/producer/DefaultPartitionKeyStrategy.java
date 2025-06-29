package com.csharma.kinesis.producer;

import java.util.Objects;

/**
 * Default partition key strategy: uses provided key or falls back to hash.
 */
public class DefaultPartitionKeyStrategy implements PartitionKeyStrategy {
    @Override
    public String getPartitionKey(Object record) {
        if (record instanceof BulkRecord) {
            String key = ((BulkRecord) record).getPartitionKey();
            if (key != null) return key;
        }
        return String.valueOf(Objects.hashCode(record));
    }
} 