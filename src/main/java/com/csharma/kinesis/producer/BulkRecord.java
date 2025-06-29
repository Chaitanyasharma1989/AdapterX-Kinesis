package com.csharma.kinesis.producer;

public class BulkRecord {
    private String partitionKey;
    private Object data;

    public BulkRecord(String partitionKey, Object data) {
        this.partitionKey = partitionKey;
        this.data = data;
    }

    public BulkRecord(Object data) {
        this.partitionKey = null;
        this.data = data;
    }

    // Getter methods
    public String getPartitionKey() {
        return partitionKey;
    }
    
    public Object getData() {
        return data;
    }

    // Setter methods
    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }
    
    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "BulkRecord{" +
                "partitionKey='" + partitionKey + '\'' +
                ", data=" + data +
                '}';
    }
} 