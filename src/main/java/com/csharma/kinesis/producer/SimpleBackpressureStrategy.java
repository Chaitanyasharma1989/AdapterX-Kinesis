package com.csharma.kinesis.producer;

/**
 * Simple backpressure strategy: applies backpressure when queue is full.
 * This is the default implementation that rejects new records when the queue reaches its maximum capacity.
 */
public class SimpleBackpressureStrategy implements BackpressureStrategy {
    
    @Override
    public boolean shouldAccept(int currentQueueSize, int maxQueueSize) {
        // Accept records only if the current queue size is less than the maximum
        return currentQueueSize < maxQueueSize;
    }
} 