package com.csharma.kinesis.errorhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoggingDeadLetterQueueHandler implements DeadLetterQueueHandler {
    private static final Logger log = LoggerFactory.getLogger(LoggingDeadLetterQueueHandler.class);

    @Override
    public void handle(Object record, Throwable error) {
        log.error("DLQ: Record permanently failed: {}", record, error);
    }
} 