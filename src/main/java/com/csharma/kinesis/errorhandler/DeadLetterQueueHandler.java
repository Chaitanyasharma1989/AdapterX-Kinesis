package com.csharma.kinesis.errorhandler;


public interface DeadLetterQueueHandler {
    void handle(Object record, Throwable error);
} 