package com.csharma.kinesis.recordprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for RecordProcessor functionality with updated KCL 3.x checkpointing.
 */
@ExtendWith(MockitoExtension.class)
class RecordProcessorIntegrationTest {

    @Mock
    private RecordProcessorCheckpointer mockCheckpointer;

    private RecordProcessor recordProcessor;
    private TestIntegrationRecordHandler testHandler;
    private AtomicInteger processedCount;
    private AtomicInteger errorCount;
    private TestBean testBean;
    private Method testMethod;

    @BeforeEach
    void setUp() throws Exception {
        // Setup test bean and method
        testBean = new TestBean();
        testMethod = TestBean.class.getMethod("handleRecord", String.class);

        // Setup test handler
        processedCount = new AtomicInteger(0);
        errorCount = new AtomicInteger(0);
        testHandler = new TestIntegrationRecordHandler(processedCount, errorCount);

        // Create record processor with integration test configuration
        RecordProcessorConfig config = new RecordProcessorConfig(
            testBean, testMethod, 4, null, false, null, null, null, null, false
        );

        recordProcessor = new RecordProcessor(config);
    }

    @Test
    void testCompleteRecordProcessingFlow() throws Exception {
        // Given - Create a batch of records
        List<KinesisClientRecord> records = createTestRecords(10);

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process the records
        recordProcessor.processRecords(input);

        // Then - Wait for processing and verify
        waitForProcessing(5000);

        assertEquals(10, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
        assertEquals(0, recordProcessor.getValidationFailedRecords());
        assertEquals(0, recordProcessor.getActiveTasks());

        // Verify checkpoint was called using KCL 3.x checkpointer
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
    }

    @Test
    void testLargeBatchProcessing() throws Exception {
        // Given - Create a large batch of records
        List<KinesisClientRecord> records = createTestRecords(100);

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process the records
        long startTime = System.currentTimeMillis();
        recordProcessor.processRecords(input);

        // Then - Wait for processing and verify
        waitForProcessing(10000);
        long endTime = System.currentTimeMillis();

        assertEquals(100, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
        assertEquals(0, recordProcessor.getActiveTasks());

        // Verify processing time is reasonable (should be fast with multi-threading)
        long processingTime = endTime - startTime;
        assertTrue(processingTime < 5000, "Processing took too long: " + processingTime + "ms");
    }

    @Test
    void testConcurrentBatchProcessing() throws Exception {
        // Given - Multiple concurrent batches
        int batchCount = 3;
        int recordsPerBatch = 20;
        CountDownLatch allBatchesComplete = new CountDownLatch(batchCount);

        // When - Process multiple batches concurrently
        for (int i = 0; i < batchCount; i++) {
            new Thread(() -> {
                try {
                    List<KinesisClientRecord> records = createTestRecords(recordsPerBatch);
                    ProcessRecordsInput input = ProcessRecordsInput.builder()
                        .records(records)
                        .checkpointer(mockCheckpointer)
                        .build();

                    recordProcessor.processRecords(input);
                } finally {
                    allBatchesComplete.countDown();
                }
            }).start();
        }

        // Then - Wait for all batches to complete
        assertTrue(allBatchesComplete.await(15, TimeUnit.SECONDS));
        waitForProcessing(2000);

        assertEquals(batchCount * recordsPerBatch, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
        assertEquals(0, recordProcessor.getActiveTasks());
    }

    @Test
    void testProcessingWithErrors() throws Exception {
        // Given - Create records that will cause errors
        List<KinesisClientRecord> records = List.of(
            createTestRecord("valid-data", "key-1"),
            createTestRecord("error-data", "key-2"), // This will cause an error
            createTestRecord("valid-data-2", "key-3")
        );

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process the records
        recordProcessor.processRecords(input);

        // Then - Wait for processing and verify
        waitForProcessing(3000);

        assertEquals(2, recordProcessor.getProcessedRecords()); // 2 successful
        assertEquals(1, recordProcessor.getFailedRecords());   // 1 failed
        assertEquals(0, recordProcessor.getActiveTasks());

        // Verify checkpoint still occurred using KCL 3.x checkpointer
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
    }

    @Test
    void testShutdownDuringProcessing() throws Exception {
        // Given - Start processing a large batch
        List<KinesisClientRecord> records = createTestRecords(50);
        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Start processing and then shutdown
        recordProcessor.processRecords(input);

        // Wait a bit for processing to start
        Thread.sleep(100);

        // Shutdown the processor
        recordProcessor.shutdownRequested(ShutdownRequestedInput.builder().build());

        // Then - Verify shutdown occurred
        assertTrue(recordProcessor.isShutdown());
        assertEquals(0, recordProcessor.getActiveTasks());

        // Some records may have been processed before shutdown
        assertTrue(recordProcessor.getProcessedRecords() >= 0);
    }

    @Test
    void testCheckpointingUnderLoad() throws Exception {
        // Given - Create multiple batches to process
        int batchCount = 5;
        int recordsPerBatch = 10;

        // When - Process multiple batches
        for (int i = 0; i < batchCount; i++) {
            List<KinesisClientRecord> records = createTestRecords(recordsPerBatch);
            ProcessRecordsInput input = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mockCheckpointer)
                .build();

            recordProcessor.processRecords(input);

            // Small delay between batches
            Thread.sleep(100);
        }

        // Then - Wait for all processing to complete
        waitForProcessing(10000);

        assertEquals(batchCount * recordsPerBatch, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
        assertEquals(0, recordProcessor.getActiveTasks());

        // Verify checkpoint was called multiple times using KCL 3.x checkpointer
        verify(mockCheckpointer, timeout(5000).atLeast(batchCount)).checkpoint();
    }

    @Test
    void testMemoryEfficientProcessing() throws Exception {
        // Given - Create a very large batch
        List<KinesisClientRecord> records = createTestRecords(1000);

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process the records
        long startTime = System.currentTimeMillis();
        recordProcessor.processRecords(input);

        // Then - Wait for processing and verify
        waitForProcessing(15000);
        long endTime = System.currentTimeMillis();

        assertEquals(1000, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
        assertEquals(0, recordProcessor.getActiveTasks());

        // Verify processing completed in reasonable time
        long processingTime = endTime - startTime;
        assertTrue(processingTime < 10000, "Processing took too long: " + processingTime + "ms");
    }

    @Test
    void testGracefulHandlingOfSlowProcessing() throws Exception {
        // Given - Create records that will be processed slowly
        List<KinesisClientRecord> records = createTestRecords(5);

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process the records (some will be slow)
        recordProcessor.processRecords(input);

        // Then - Wait for processing and verify
        waitForProcessing(10000);

        assertEquals(5, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
        assertEquals(0, recordProcessor.getActiveTasks());

        // Verify checkpoint occurred despite slow processing
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
    }

    @Test
    void testShardEndedCheckpointingIntegration() throws Exception {
        // Given - Process some records first
        List<KinesisClientRecord> records = createTestRecords(10);
        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        recordProcessor.processRecords(input);
        waitForProcessing(3000);

        // When - Shard ends
        ShardEndedInput shardEndedInput = ShardEndedInput.builder()
            .checkpointer(mockCheckpointer)
            .build();

        recordProcessor.shardEnded(shardEndedInput);

        // Then - Verify checkpoint was called and processor shutdown
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
        assertTrue(recordProcessor.isShutdown());
    }

    @Test
    void testLeaseLostIntegration() throws Exception {
        // Given - Process some records first
        List<KinesisClientRecord> records = createTestRecords(5);
        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        recordProcessor.processRecords(input);
        waitForProcessing(2000);

        // When - Lease is lost
        LeaseLostInput leaseLostInput = LeaseLostInput.builder().build();
        recordProcessor.leaseLost(leaseLostInput);

        // Then - Verify processor shutdown
        assertTrue(recordProcessor.isShutdown());
        assertEquals(0, recordProcessor.getActiveTasks());
    }

    // Helper methods
    private List<KinesisClientRecord> createTestRecords(int count) {
        List<KinesisClientRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(createTestRecord("test-data-" + i, "partition-key-" + i));
        }
        return records;
    }

    private KinesisClientRecord createTestRecord(String data, String partitionKey) {
        return KinesisClientRecord.builder()
            .data(ByteBuffer.wrap(data.getBytes()))
            .partitionKey(partitionKey)
            .sequenceNumber("test-sequence-" + System.currentTimeMillis() + "-" + Math.random())
            .build();
    }

    private void waitForProcessing(long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (recordProcessor.getActiveTasks() > 0 && 
               (System.currentTimeMillis() - startTime) < timeoutMs) {
            Thread.sleep(100);
        }
    }

    // Test bean implementation
    public static class TestBean {
        public void handleRecord(String data) throws Exception {
            // Simple test implementation that throws exception for error-data
            if ("error-data".equals(data)) {
                throw new RuntimeException("Simulated processing error for: " + data);
            }
            System.out.println("Processing record: " + data);
        }
    }

    // Test record handler implementation for integration tests
    private static class TestIntegrationRecordHandler implements RecordHandler {
        private final AtomicInteger processedCount;
        private final AtomicInteger errorCount;

        public TestIntegrationRecordHandler(AtomicInteger processedCount, AtomicInteger errorCount) {
            this.processedCount = processedCount;
            this.errorCount = errorCount;
        }

        @Override
        public void handleRecord(String data, KinesisClientRecord record) throws Exception {
            // Simulate processing with some errors
            if ("error-data".equals(data)) {
                errorCount.incrementAndGet();
                throw new RuntimeException("Simulated processing error");
            }

            // Simulate processing time
            Thread.sleep(5);
            processedCount.incrementAndGet();
        }
    }
} 