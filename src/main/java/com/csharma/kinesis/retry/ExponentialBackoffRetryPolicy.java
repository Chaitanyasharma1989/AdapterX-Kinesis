package com.csharma.kinesis.retry;


public class ExponentialBackoffRetryPolicy implements RetryPolicy {
    private final int maxAttempts;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double multiplier;

    public ExponentialBackoffRetryPolicy() {
        this(3, 100, 5000, 2.0);
    }

    public ExponentialBackoffRetryPolicy(int maxAttempts, long initialDelayMs, long maxDelayMs, double multiplier) {
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
    }

    @Override
    public boolean shouldRetry(int attempt, Throwable lastException) {
        return attempt < maxAttempts;
    }

    @Override
    public long getBackoffDelay(int attempt, Throwable lastException) {
        long delay = (long) (initialDelayMs * Math.pow(multiplier, attempt - 1));
        return Math.min(delay, maxDelayMs);
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }
} 