package com.csharma.kinesis.recordprocessor;

import com.csharma.kinesis.service.SchemaRegistryService;
import com.csharma.kinesis.validation.NoOpRecordValidator;
import com.csharma.kinesis.validation.RecordValidator;
import com.csharma.kinesis.validation.SchemaRecordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


public class RecordProcessor implements ShardRecordProcessor {

    private static final Logger log = LoggerFactory.getLogger(RecordProcessor.class);

    private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;
    private static final long DEFAULT_SHUTDOWN_TIMEOUT_MS = 30000; // 30 seconds

    private final RecordProcessorConfig config;
    private final RecordHandler recordHandler;
    private final RecordValidator recordValidator;
    private final ExecutorService executorService;
    private final ReentrantLock checkpointLock = new ReentrantLock();
    private final ProcessingMetrics metrics;

    private volatile boolean isShutdown = false;
    private volatile RecordProcessorCheckpointer checkpointer;

    public RecordProcessor(Object bean, Method method) {
        this(new RecordProcessorConfig(bean, method, DEFAULT_THREAD_POOL_SIZE, null, false, null, null, null, null, false));
    }

    public RecordProcessor(Object bean, Method method, int threadPoolSize) {
        this(new RecordProcessorConfig(bean, method, threadPoolSize, null, false, null, null, null, null, false));
    }

    public RecordProcessor(Object bean, Method method, int threadPoolSize,
                           SchemaRegistryService schemaRegistryService, boolean validateSchema,
                           String schemaRegistryName, String schemaName, String schemaVersion,
                           String dataFormat, boolean failOnValidationError) {
        this(new RecordProcessorConfig(bean, method, threadPoolSize, schemaRegistryService,
                validateSchema, schemaRegistryName, schemaName, schemaVersion,
                dataFormat, failOnValidationError));
    }

    public RecordProcessor(RecordProcessorConfig config) {
        this.config = config;
        this.recordHandler = new MethodInvocationRecordHandler(config.getBean(), config.getMethod());
        this.recordValidator = createRecordValidator();
        this.executorService = createThreadPool(config.getThreadPoolSize());
        this.metrics = new ProcessingMetrics();
    }

    private RecordValidator createRecordValidator() {
        if (config.isValidateSchema() && config.getSchemaRegistryService() != null) {
            return new SchemaRecordValidator(
                    config.getSchemaRegistryService(),
                    config.getSchemaRegistryName(),
                    config.getSchemaName(),
                    config.getSchemaVersion(),
                    config.getDataFormat(),
                    config.isFailOnValidationError()
            );
        }
        return new NoOpRecordValidator();
    }

    private ExecutorService createThreadPool(int threadPoolSize) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "kinesis-record-processor-" + threadNumber.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        };

        return new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy() // Prevents rejection by making caller thread execute the task
        );
    }

    @Override
    public void initialize(InitializationInput initializationInput) {
        log.info("Initializing record processor for shard: {} with thread pool size: {} (Schema validation: {})",
                initializationInput.shardId(),
                ((ThreadPoolExecutor) executorService).getCorePoolSize(),
                config.isValidateSchema());
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        if (isShutdown) {
            log.warn("Record processor is shutting down, skipping record processing");
            return;
        }

        List<KinesisClientRecord> records = processRecordsInput.records();
        this.checkpointer = processRecordsInput.checkpointer();

        if (records.isEmpty()) {
            log.debug("No records to process");
            return;
        }

        // Process records in parallel with better error handling
        CountDownLatch latch = new CountDownLatch(records.size());
        List<Exception> processingErrors = new CopyOnWriteArrayList<>();

        for (KinesisClientRecord record : records) {
            metrics.incrementActiveTasks();

            try {
                executorService.submit(() -> {
                    try {
                        processRecord(record);
                        metrics.incrementProcessedRecords();
                    } catch (Exception e) {
                        if (e instanceof SchemaRegistryService.SchemaValidationException) {
                            metrics.incrementValidationFailedRecords();
                            log.error("Schema validation failed for record: {}", record, e);
                            if (config.isFailOnValidationError()) {
                                processingErrors.add(e);
                            }
                        } else {
                            metrics.incrementFailedRecords();
                            log.error("Error processing record: {}", record, e);
                            processingErrors.add(e);
                        }
                    } finally {
                        metrics.decrementActiveTasks();
                        latch.countDown();
                    }
                });
            } catch (RejectedExecutionException e) {
                // Executor service is full, process synchronously
                log.warn("Executor service is full, processing record synchronously: {}", record);
                metrics.decrementActiveTasks();
                latch.countDown();

                try {
                    processRecord(record);
                    metrics.incrementProcessedRecords();
                } catch (Exception processingException) {
                    if (processingException instanceof SchemaRegistryService.SchemaValidationException) {
                        metrics.incrementValidationFailedRecords();
                        log.error("Schema validation failed for record (sync): {}", record, processingException);
                        if (config.isFailOnValidationError()) {
                            processingErrors.add(processingException);
                        }
                    } else {
                        metrics.incrementFailedRecords();
                        log.error("Error processing record (sync): {}", record, processingException);
                        processingErrors.add(processingException);
                    }
                }
            }
        }

        // Wait for all records to be processed with timeout
        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Timeout waiting for record processing to complete. Remaining tasks: {}",
                        latch.getCount());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for record processing", e);
        }

        // Handle any processing errors
        if (!processingErrors.isEmpty()) {
            log.error("{} records failed processing", processingErrors.size());
            if (config.isFailOnValidationError()) {
                // Find the first validation error and re-throw it
                for (Exception error : processingErrors) {
                    if (error instanceof SchemaRegistryService.SchemaValidationException) {
                        throw new RuntimeException("Schema validation failed", error);
                    }
                }
            }
        }

        // Checkpoint after all records are processed
        checkpoint();
    }

    private void processRecord(KinesisClientRecord record) throws Exception {
        String data = new String(record.data().array());

        // Perform schema validation if enabled
        recordValidator.validate(data, record.partitionKey());

        // Process the record
        recordHandler.handleRecord(data, record);
    }

    private void checkpoint() {
        if (checkpointer == null) {
            return;
        }

        checkpointLock.lock();
        try {
            // Wait for all active tasks to complete before checkpointing
            while (metrics.getActiveTasks() > 0) {
                try {
                    Thread.sleep(10); // Small delay to avoid busy waiting
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting for active tasks", e);
                    return;
                }
            }

            try {
                checkpointer.checkpoint();
                log.debug("Checkpoint completed. Processed: {}, Failed: {}, Validation Failed: {}",
                        metrics.getProcessedRecords(), metrics.getFailedRecords(), metrics.getValidationFailedRecords());
            } catch (Exception e) {
                log.error("Failed to checkpoint", e);
            }
        } finally {
            checkpointLock.unlock();
        }
    }

    @Override
    public void leaseLost(LeaseLostInput leaseLostInput) {
        log.info("Lease lost for shard: {}", leaseLostInput.toString());
        shutdown();
    }

    @Override
    public void shardEnded(ShardEndedInput shardEndedInput) {
        try {
            // Wait for all active tasks to complete
            waitForActiveTasks();

            // Use the checkpoint method from AWS KCL
            shardEndedInput.checkpointer().checkpoint();
            log.info("Shard ended, check pointed: {}", shardEndedInput.toString());
        } catch (Exception e) {
            log.error("Failed to checkpoint at shard end", e);
        } finally {
            shutdown();
        }
    }

    @Override
    public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
        log.info("Shutdown requested for shard: {}", shutdownRequestedInput.toString());
        shutdown();
    }

    private void shutdown() {
        if (isShutdown) {
            return;
        }

        isShutdown = true;
        log.info("Shutting down record processor. Final stats - Processed: {}, Failed: {}, Validation Failed: {}",
                metrics.getProcessedRecords(), metrics.getFailedRecords(), metrics.getValidationFailedRecords());

        // Wait for active tasks to complete
        waitForActiveTasks();

        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(DEFAULT_SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                log.warn("Executor service did not terminate within timeout, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for executor service shutdown", e);
            executorService.shutdownNow();
        }
    }

    private void waitForActiveTasks() {
        int maxWaitTime = 30; // seconds
        int waited = 0;

        while (metrics.getActiveTasks() > 0 && waited < maxWaitTime) {
            try {
                Thread.sleep(1000);
                waited++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for active tasks", e);
                break;
            }
        }

        if (metrics.getActiveTasks() > 0) {
            log.warn("Some tasks are still active after waiting {} seconds", maxWaitTime);
        }
    }

    // Getter methods for monitoring
    public long getProcessedRecords() {
        return metrics.getProcessedRecords();
    }

    public long getFailedRecords() {
        return metrics.getFailedRecords();
    }

    public long getValidationFailedRecords() {
        return metrics.getValidationFailedRecords();
    }

    public int getActiveTasks() {
        return metrics.getActiveTasks();
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public ThreadPoolExecutor getExecutorService() {
        return (ThreadPoolExecutor) executorService;
    }

    public boolean isSchemaValidationEnabled() {
        return config.isValidateSchema();
    }
}

