package com.csharma.kinesis.strategy;

import com.csharma.kinesis.common.BulkRecord;

import java.util.Objects;

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