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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecordProcessor functionality with updated KCL 3.x checkpointing.
 */
@ExtendWith(MockitoExtension.class)
class RecordProcessorUnitTest {

    @Mock
    private RecordProcessorCheckpointer mockCheckpointer;

    private RecordProcessor recordProcessor;
    private TestRecordHandler testRecordHandler;
    private AtomicInteger processedCount;
    private TestBean testBean;
    private Method testMethod;

    @BeforeEach
    void setUp() throws Exception {
        // Setup test bean and method
        testBean = new TestBean();
        testMethod = TestBean.class.getMethod("handleRecord", String.class);

        // Setup test record handler
        processedCount = new AtomicInteger(0);
        testRecordHandler = new TestRecordHandler(processedCount);

        // Create record processor with test configuration
        RecordProcessorConfig config = new RecordProcessorConfig(
            testBean, testMethod, 2, null, false, null, null, null, null, false
        );

        recordProcessor = new RecordProcessor(config);
    }

    @Test
    void testRecordProcessorInitialization() {
        assertNotNull(recordProcessor);
        assertFalse(recordProcessor.isShutdown());
        assertEquals(0, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
        assertEquals(0, recordProcessor.getValidationFailedRecords());
    }

    @Test
    void testProcessRecordsWithValidData() throws Exception {
        // Create test records
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("test-data-1", "partition-key-1"),
            createTestRecord("test-data-2", "partition-key-2")
        );

        // Create ProcessRecordsInput
        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // Process records
        recordProcessor.processRecords(input);

        // Wait for processing to complete
        Thread.sleep(1000);

        // Verify processing
        assertEquals(2, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
        assertEquals(0, recordProcessor.getValidationFailedRecords());
    }

    @Test
    void testProcessRecordsWithEmptyList() {
        // Create empty records list
        List<KinesisClientRecord> records = Arrays.asList();

        // Create ProcessRecordsInput
        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // Process records
        recordProcessor.processRecords(input);

        // Verify no processing occurred
        assertEquals(0, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
    }

    @Test
    void testMultiThreadedProcessing() throws Exception {
        // Create multiple test records
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("data-1", "key-1"),
            createTestRecord("data-2", "key-2"),
            createTestRecord("data-3", "key-3"),
            createTestRecord("data-4", "key-4"),
            createTestRecord("data-5", "key-5")
        );

        // Create ProcessRecordsInput
        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // Process records
        recordProcessor.processRecords(input);

        // Wait for processing to complete
        Thread.sleep(2000);

        // Verify all records were processed
        assertEquals(5, recordProcessor.getProcessedRecords());
        assertEquals(0, recordProcessor.getFailedRecords());
        assertEquals(0, recordProcessor.getActiveTasks());
    }

    @Test
    void testCheckpointingWithKCL3x() throws Exception {
        // Create test records
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("test-data", "partition-key")
        );

        // Create ProcessRecordsInput
        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // Process records
        recordProcessor.processRecords(input);

        // Wait for processing to complete
        Thread.sleep(1000);

        // Verify checkpoint was called using KCL 3.x checkpointer
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
    }

    @Test
    void testCheckpointingWithNullCheckpointer() throws Exception {
        // Create test records
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("test-data", "partition-key")
        );

        // Create ProcessRecordsInput with null checkpointer
        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(null)
            .build();

        // Process records - should not throw exception
        assertDoesNotThrow(() -> recordProcessor.processRecords(input));

        // Wait for processing to complete
        Thread.sleep(1000);

        // Verify processing completed
        assertEquals(1, recordProcessor.getProcessedRecords());
    }

    @Test
    void testShutdown() throws Exception {
        // Start processing some records
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("test-data", "partition-key")
        );

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // Process records
        recordProcessor.processRecords(input);

        // Shutdown the processor
        recordProcessor.shutdownRequested(ShutdownRequestedInput.builder().build());

        // Verify shutdown
        assertTrue(recordProcessor.isShutdown());
        assertEquals(0, recordProcessor.getActiveTasks());
    }

    @Test
    void testShardEndedCheckpointing() throws Exception {
        // Create test records
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("test-data", "partition-key")
        );

        // Process some records first
        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        recordProcessor.processRecords(input);
        Thread.sleep(1000);

        // Create ShardEndedInput
        ShardEndedInput shardEndedInput = ShardEndedInput.builder()
            .checkpointer(mockCheckpointer)
            .build();

        // Call shardEnded
        recordProcessor.shardEnded(shardEndedInput);

        // Verify checkpoint was called
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
    }

    @Test
    void testLeaseLost() throws Exception {
        // Create LeaseLostInput
        LeaseLostInput leaseLostInput = LeaseLostInput.builder().build();

        // Call leaseLost
        recordProcessor.leaseLost(leaseLostInput);

        // Verify shutdown occurred
        assertTrue(recordProcessor.isShutdown());
    }

    @Test
    void testThreadPoolConfiguration() {
        // Verify thread pool configuration
        assertEquals(2, recordProcessor.getExecutorService().getCorePoolSize());
        assertEquals(2, recordProcessor.getExecutorService().getMaximumPoolSize());
    }

    // Helper methods
    private KinesisClientRecord createTestRecord(String data, String partitionKey) {
        return KinesisClientRecord.builder()
            .data(ByteBuffer.wrap(data.getBytes()))
            .partitionKey(partitionKey)
            .sequenceNumber("test-sequence-" + System.currentTimeMillis() + "-" + Math.random())
            .build();
    }

    // Test bean implementation
    public static class TestBean {
        public void handleRecord(String data) {
            // Simple test implementation
            System.out.println("Processing record: " + data);
        }
    }

    // Test record handler implementation
    private static class TestRecordHandler implements RecordHandler {
        private final AtomicInteger processedCount;

        public TestRecordHandler(AtomicInteger processedCount) {
            this.processedCount = processedCount;
        }

        @Override
        public void handleRecord(String data, KinesisClientRecord record) throws Exception {
            // Simulate processing
            Thread.sleep(10);
            processedCount.incrementAndGet();
        }
    }
} 