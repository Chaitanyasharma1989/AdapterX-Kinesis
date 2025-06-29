package com.csharma.kinesis.recordprocessor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProcessingMetrics {
    
    private final AtomicLong processedRecords = new AtomicLong(0);
    private final AtomicLong failedRecords = new AtomicLong(0);
    private final AtomicLong validationFailedRecords = new AtomicLong(0);
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    
    public void incrementProcessedRecords() {
        processedRecords.incrementAndGet();
    }
    
    public void incrementFailedRecords() {
        failedRecords.incrementAndGet();
    }
    
    public void incrementValidationFailedRecords() {
        validationFailedRecords.incrementAndGet();
    }
    
    public void incrementActiveTasks() {
        activeTasks.incrementAndGet();
    }
    
    public void decrementActiveTasks() {
        activeTasks.decrementAndGet();
    }
    
    public long getProcessedRecords() {
        return processedRecords.get();
    }
    
    public long getFailedRecords() {
        return failedRecords.get();
    }
    
    public long getValidationFailedRecords() {
        return validationFailedRecords.get();
    }
    
    public int getActiveTasks() {
        return activeTasks.get();
    }
    
    public void reset() {
        processedRecords.set(0);
        failedRecords.set(0);
        validationFailedRecords.set(0);
        activeTasks.set(0);
    }
} 