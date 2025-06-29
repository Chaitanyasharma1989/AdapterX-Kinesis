# Multi-Threading and Parallelism in Kinesis RecordProcessor

## Overview

The `RecordProcessor` has been refactored to support multi-threading and parallelism, significantly improving throughput and performance for high-volume Kinesis streams.

## Key Improvements

### 1. Parallel Record Processing
- **Thread Pool Executor**: Records are now processed in parallel using a configurable thread pool
- **Configurable Thread Count**: Default is `Runtime.getRuntime().availableProcessors()`, but can be customized
- **Queue Management**: Configurable queue capacity to handle backpressure

### 2. Thread-Safe Checkpointing
- **ReentrantLock**: Ensures thread-safe checkpointing operations
- **Active Task Tracking**: Waits for all active tasks to complete before checkpointing
- **Atomic Counters**: Thread-safe tracking of processed and failed records

### 3. Graceful Shutdown
- **Timeout-Based Shutdown**: Configurable shutdown timeouts
- **Active Task Completion**: Waits for active tasks to complete before shutting down
- **Resource Cleanup**: Proper cleanup of thread pool resources

### 4. Enhanced Monitoring
- **Performance Metrics**: Track processed records, failed records, and active tasks
- **Thread Pool Statistics**: Access to thread pool metrics for monitoring
- **Detailed Logging**: Enhanced logging with thread information

## Configuration

### Thread Pool Configuration

```yaml
kinesis:
  consumer:
    threadPool:
      # Number of threads for parallel processing
      size: 8
      
      # Maximum queue capacity for pending records
      queueCapacity: 2000
      
      # Shutdown timeout in milliseconds
      shutdownTimeoutMs: 60000
      
      # Record processing timeout in milliseconds
      recordProcessingTimeoutMs: 45000
      
      # Active task wait timeout in milliseconds
      activeTaskWaitTimeoutMs: 45000
```

### Default Values
- `size`: `Runtime.getRuntime().availableProcessors()`
- `queueCapacity`: `1000`
- `shutdownTimeoutMs`: `30000` (30 seconds)
- `recordProcessingTimeoutMs`: `30000` (30 seconds)
- `activeTaskWaitTimeoutMs`: `30000` (30 seconds)

## Usage Examples

### Basic Usage (Uses Default Thread Pool Size)
```java
@KinesisListener(streamName = "my-stream")
public void processRecord(String data) {
    // Process record data
    System.out.println("Processing: " + data);
}
```

### Custom Thread Pool Size
```java
// The thread pool size is configured via application.yml
// No code changes needed - it's automatically applied
```

## Performance Considerations

### Thread Pool Sizing
- **CPU-Bound Tasks**: Use `Runtime.getRuntime().availableProcessors()`
- **I/O-Bound Tasks**: Use `Runtime.getRuntime().availableProcessors() * 2`
- **Mixed Workloads**: Start with CPU count and adjust based on monitoring

### Queue Capacity
- **High Throughput**: Increase queue capacity to handle bursts
- **Memory Usage**: Larger queues use more memory
- **Backpressure**: Queue capacity acts as backpressure mechanism

### Timeout Configuration
- **Processing Timeout**: Should be longer than expected record processing time
- **Shutdown Timeout**: Should allow for graceful completion of active tasks
- **Active Task Wait**: Should be sufficient for longest-running tasks

## Monitoring and Metrics

### Available Metrics
```java
RecordProcessor processor = // get processor instance

// Performance metrics
long processedRecords = processor.getProcessedRecords();
long failedRecords = processor.getFailedRecords();
int activeTasks = processor.getActiveTasks();
boolean isShutdown = processor.isShutdown();

// Thread pool metrics
ThreadPoolExecutor executor = processor.getExecutorService();
int poolSize = executor.getPoolSize();
int activeThreads = executor.getActiveCount();
int queueSize = executor.getQueue().size();
```

### Logging
Enable debug logging for detailed thread information:
```yaml
logging:
  level:
    com.csharma.kinesis.recordprocessor: DEBUG
```

## Thread Safety

### Thread-Safe Components
- **Record Processing**: Each record is processed in its own thread
- **Checkpointing**: Protected by `ReentrantLock`
- **Counters**: Use atomic operations (`AtomicLong`, `AtomicInteger`)
- **Shutdown**: Thread-safe shutdown mechanism

### Best Practices
1. **Stateless Methods**: Keep record processing methods stateless
2. **Thread-Safe Dependencies**: Ensure injected beans are thread-safe
3. **Exception Handling**: Handle exceptions within record processing methods
4. **Resource Management**: Use proper resource cleanup in processing methods

## Migration Guide

### From Single-Threaded to Multi-Threaded
1. **No Code Changes Required**: Existing `@KinesisListener` methods work unchanged
2. **Configuration**: Add thread pool configuration to `application.yml`
3. **Testing**: Test with your specific workload to optimize thread pool size
4. **Monitoring**: Monitor performance metrics and adjust configuration as needed

### Backward Compatibility
- All existing functionality is preserved
- Default behavior uses reasonable defaults
- No breaking changes to existing APIs

## Troubleshooting

### Common Issues

#### High Memory Usage
- **Cause**: Large queue capacity or too many threads
- **Solution**: Reduce `queueCapacity` or `threadPool.size`

#### Slow Processing
- **Cause**: Insufficient threads or long-running tasks
- **Solution**: Increase `threadPool.size` or optimize processing logic

#### Checkpoint Failures
- **Cause**: Tasks not completing within timeout
- **Solution**: Increase `recordProcessingTimeoutMs` or optimize task duration

#### Shutdown Issues
- **Cause**: Tasks not completing during shutdown
- **Solution**: Increase `shutdownTimeoutMs` or `activeTaskWaitTimeoutMs`

### Performance Tuning

1. **Monitor Metrics**: Track processed records, failed records, and active tasks
2. **Adjust Thread Count**: Start with CPU count and adjust based on workload
3. **Optimize Processing**: Ensure record processing methods are efficient
4. **Queue Management**: Monitor queue size and adjust capacity as needed

## Example Configuration

See `application-multithreading-example.yml` for a complete example configuration.

## Thread Pool Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Kinesis       │    │   Thread Pool    │    │   Record        │
│   Records       │───▶│   Executor       │───▶│   Processing    │
│                 │    │                  │    │   Threads       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │   Checkpointing  │
                       │   (Thread-Safe)  │
                       └──────────────────┘
```

This architecture ensures:
- **Parallel Processing**: Multiple records processed simultaneously
- **Thread Safety**: Safe checkpointing and resource management
- **Scalability**: Configurable thread pool size based on workload
- **Reliability**: Proper error handling and graceful shutdown 