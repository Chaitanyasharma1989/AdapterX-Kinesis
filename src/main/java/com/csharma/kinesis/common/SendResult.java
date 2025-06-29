package com.csharma.kinesis.common;


public class SendResult {
    private final String streamName;
    private final String partitionKey;
    private final String shardId;
    private final String sequenceNumber;
    private final long timestamp;

    public SendResult(String streamName, String partitionKey, String shardId, String sequenceNumber) {
        this.streamName = streamName;
        this.partitionKey = partitionKey;
        this.shardId = shardId;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = System.currentTimeMillis();
    }

    public SendResult(String streamName, String partitionKey, String shardId, String sequenceNumber, long timestamp) {
        this.streamName = streamName;
        this.partitionKey = partitionKey;
        this.shardId = shardId;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SendResult{" +
                "streamName='" + streamName + '\'' +
                ", partitionKey='" + partitionKey + '\'' +
                ", shardId='" + shardId + '\'' +
                ", sequenceNumber='" + sequenceNumber + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 