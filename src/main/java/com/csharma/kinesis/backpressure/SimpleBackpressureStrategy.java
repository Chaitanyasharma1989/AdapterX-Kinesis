package com.csharma.kinesis.backpressure;

public class SimpleBackpressureStrategy implements BackpressureStrategy {
    @Override
    public boolean shouldAccept(int currentQueueSize, int maxQueueSize) {
        // Accept records only if the current queue size is less than the maximum
        return currentQueueSize < maxQueueSize;
    }
} 