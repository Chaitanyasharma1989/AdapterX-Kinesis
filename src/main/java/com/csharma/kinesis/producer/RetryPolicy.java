package com.csharma.kinesis.producer;

/**
 * Interface for retry policy abstraction.
 */
public interface RetryPolicy {
    /**
     * Returns true if another retry should be attempted for the given attempt number and exception.
     */
    boolean shouldRetry(int attempt, Throwable lastException);

    /**
     * Returns the delay (in milliseconds) before the next retry attempt.
     */
    long getBackoffDelay(int attempt, Throwable lastException);

    /**
     * Returns the maximum number of retry attempts.
     */
    int getMaxAttempts();
} 