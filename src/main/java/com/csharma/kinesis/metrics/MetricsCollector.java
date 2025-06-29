package com.csharma.kinesis.metrics;

/**
 * Interface for collecting producer/consumer metrics.
 */
public interface MetricsCollector {
    void recordSendSuccess(String streamName, Object record);
    void recordSendFailure(String streamName, Object record, Throwable error);
    void recordRetry(String streamName, Object record, int attempt, Throwable error);
    void recordDLQ(String streamName, Object record, Throwable error);
    void recordProcessingTime(String streamName, long durationMs);
} 