package com.csharma.kinesis.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default metrics collector that logs events.
 */
public class LoggingMetricsCollector implements MetricsCollector {
    private static final Logger log = LoggerFactory.getLogger(LoggingMetricsCollector.class);

    @Override
    public void recordSendSuccess(String streamName, Object record) {
        log.info("Metrics: Send success for stream {}: {}", streamName, record);
    }

    @Override
    public void recordSendFailure(String streamName, Object record, Throwable error) {
        log.warn("Metrics: Send failure for stream {}: {}", streamName, record, error);
    }

    @Override
    public void recordRetry(String streamName, Object record, int attempt, Throwable error) {
        log.info("Metrics: Retry {} for stream {}: {}", attempt, streamName, record, error);
    }

    @Override
    public void recordDLQ(String streamName, Object record, Throwable error) {
        log.error("Metrics: DLQ for stream {}: {}", streamName, record, error);
    }

    @Override
    public void recordProcessingTime(String streamName, long durationMs) {
        log.info("Metrics: Processing time for stream {}: {} ms", streamName, durationMs);
    }
} 