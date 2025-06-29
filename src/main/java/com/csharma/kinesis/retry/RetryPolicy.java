package com.csharma.kinesis.retry;


public interface RetryPolicy {
    boolean shouldRetry(int attempt, Throwable lastException);
    long getBackoffDelay(int attempt, Throwable lastException);
    int getMaxAttempts();
} 