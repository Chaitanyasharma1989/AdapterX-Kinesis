package com.csharma.kinesis.common;


import java.util.List;

public class BulkSendResult {
    private final String streamName;
    private final int totalRecords;
    private final int successfulRecords;
    private final int failedRecords;
    private final List<SendResult> successfulResults;
    private final List<FailedRecord> failedResults;
    private final long timestamp;
    private final long durationMs;

    public BulkSendResult(String streamName, int totalRecords, int successfulRecords, int failedRecords,
                         List<SendResult> successfulResults, List<FailedRecord> failedResults, long durationMs) {
        this.streamName = streamName;
        this.totalRecords = totalRecords;
        this.successfulRecords = successfulRecords;
        this.failedRecords = failedRecords;
        this.successfulResults = successfulResults;
        this.failedResults = failedResults;
        this.timestamp = System.currentTimeMillis();
        this.durationMs = durationMs;
    }

    public boolean isAllSuccessful() {
        return failedRecords == 0;
    }

    public boolean isAllFailed() {
        return successfulRecords == 0;
    }

    public double getSuccessRate() {
        return totalRecords > 0 ? (double) successfulRecords / totalRecords : 0.0;
    }

    @Override
    public String toString() {
        return "BulkSendResult{" +
                "streamName='" + streamName + '\'' +
                ", totalRecords=" + totalRecords +
                ", successfulRecords=" + successfulRecords +
                ", failedRecords=" + failedRecords +
                ", successRate=" + String.format("%.2f%%", getSuccessRate() * 100) +
                ", durationMs=" + durationMs +
                ", timestamp=" + timestamp +
                '}';
    }
} 