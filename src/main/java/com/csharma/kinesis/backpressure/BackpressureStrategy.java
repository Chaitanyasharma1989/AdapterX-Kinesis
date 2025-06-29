package com.csharma.kinesis.backpressure;

public interface BackpressureStrategy {
    boolean shouldAccept(int currentQueueSize, int maxQueueSize);
} 