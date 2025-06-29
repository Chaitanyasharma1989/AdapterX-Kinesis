package com.csharma.kinesis.common;


public class FailedRecord {
    private final String partitionKey;
    private final Object data;
    private final Throwable error;
    private final long timestamp;

    public FailedRecord(String partitionKey, Object data, Throwable error) {
        this.partitionKey = partitionKey;
        this.data = data;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }

    public FailedRecord(Object data, Throwable error) {
        this.partitionKey = null;
        this.data = data;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Throwable getError() {
        return error;
    }

    public Object getData() {
        return data;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    @Override
    public String toString() {
        return "FailedRecord{" +
                "partitionKey='" + partitionKey + '\'' +
                ", data=" + data +
                ", error=" + error.getMessage() +
                ", timestamp=" + timestamp +
                '}';
    }
} 