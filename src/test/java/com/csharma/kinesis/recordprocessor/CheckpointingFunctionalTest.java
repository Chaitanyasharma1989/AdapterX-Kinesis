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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests specifically for KCL 3.x checkpointing functionality.
 */
@ExtendWith(MockitoExtension.class)
class CheckpointingFunctionalTest {

    @Mock
    private RecordProcessorCheckpointer mockCheckpointer;

    private RecordProcessor recordProcessor;
    private TestBean testBean;
    private Method testMethod;

    @BeforeEach
    void setUp() throws Exception {
        // Setup test bean and method
        testBean = new TestBean();
        testMethod = TestBean.class.getMethod("handleRecord", String.class);

        // Create record processor with test configuration
        RecordProcessorConfig config = new RecordProcessorConfig(
            testBean, testMethod, 2, null, false, null, null, null, null, false
        );

        recordProcessor = new RecordProcessor(config);
    }

    @Test
    void testBasicCheckpointing() throws Exception {
        // Given - Create test records
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("test-data", "partition-key")
        );

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process records
        recordProcessor.processRecords(input);

        // Then - Wait for processing and verify checkpoint
        Thread.sleep(1000);
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
    }

    @Test
    void testCheckpointingWithMultipleRecords() throws Exception {
        // Given - Create multiple test records
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("data-1", "key-1"),
            createTestRecord("data-2", "key-2"),
            createTestRecord("data-3", "key-3")
        );

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process records
        recordProcessor.processRecords(input);

        // Then - Wait for processing and verify checkpoint
        Thread.sleep(2000);
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
        assertEquals(3, recordProcessor.getProcessedRecords());
    }

    @Test
    void testCheckpointingWithNullCheckpointer() throws Exception {
        // Given - Create test records with null checkpointer
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("test-data", "partition-key")
        );

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(null)
            .build();

        // When/Then - Process records should not throw exception
        assertDoesNotThrow(() -> recordProcessor.processRecords(input));
        Thread.sleep(1000);
        assertEquals(1, recordProcessor.getProcessedRecords());
    }

    @Test
    void testCheckpointingAfterProcessingErrors() throws Exception {
        // Given - Create records with some that will cause errors
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("valid-data", "key-1"),
            createTestRecord("error-data", "key-2"),
            createTestRecord("valid-data-2", "key-3")
        );

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process records
        recordProcessor.processRecords(input);

        // Then - Wait for processing and verify checkpoint still occurs
        Thread.sleep(2000);
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
        assertEquals(2, recordProcessor.getProcessedRecords());
        assertEquals(1, recordProcessor.getFailedRecords());
    }

    @Test
    void testShardEndedCheckpointing() throws Exception {
        // Given - Process some records first
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("test-data", "partition-key")
        );

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        recordProcessor.processRecords(input);
        Thread.sleep(1000);

        // When - Shard ends
        ShardEndedInput shardEndedInput = ShardEndedInput.builder()
            .checkpointer(mockCheckpointer)
            .build();

        recordProcessor.shardEnded(shardEndedInput);

        // Then - Verify checkpoint was called
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
        assertTrue(recordProcessor.isShutdown());
    }

    @Test
    void testCheckpointingDuringShutdown() throws Exception {
        // Given - Start processing records
        List<KinesisClientRecord> records = Arrays.asList(
            createTestRecord("test-data", "partition-key")
        );

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        recordProcessor.processRecords(input);

        // When - Shutdown requested
        recordProcessor.shutdownRequested(ShutdownRequestedInput.builder().build());

        // Then - Verify checkpoint occurred and shutdown completed
        Thread.sleep(1000);
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
        assertTrue(recordProcessor.isShutdown());
    }

    @Test
    void testCheckpointingWithConcurrentProcessing() throws Exception {
        // Given - Create multiple batches
        List<KinesisClientRecord> batch1 = Arrays.asList(
            createTestRecord("batch1-data1", "key1"),
            createTestRecord("batch1-data2", "key2")
        );

        List<KinesisClientRecord> batch2 = Arrays.asList(
            createTestRecord("batch2-data1", "key3"),
            createTestRecord("batch2-data2", "key4")
        );

        // When - Process batches concurrently
        ProcessRecordsInput input1 = ProcessRecordsInput.builder()
            .records(batch1)
            .checkpointer(mockCheckpointer)
            .build();

        ProcessRecordsInput input2 = ProcessRecordsInput.builder()
            .records(batch2)
            .checkpointer(mockCheckpointer)
            .build();

        recordProcessor.processRecords(input1);
        Thread.sleep(100);
        recordProcessor.processRecords(input2);

        // Then - Wait for processing and verify checkpoint
        Thread.sleep(3000);
        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
        assertEquals(4, recordProcessor.getProcessedRecords());
    }

    @Test
    void testCheckpointingWithLargeBatch() throws Exception {
        // Given - Create a large batch of records
        List<KinesisClientRecord> records = createLargeBatch(50);

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process records
        long startTime = System.currentTimeMillis();
        recordProcessor.processRecords(input);

        // Then - Wait for processing and verify checkpoint
        Thread.sleep(5000);
        long endTime = System.currentTimeMillis();

        verify(mockCheckpointer, timeout(5000).atLeastOnce()).checkpoint();
        assertEquals(50, recordProcessor.getProcessedRecords());
        
        // Verify processing completed in reasonable time
        long processingTime = endTime - startTime;
        assertTrue(processingTime < 10000, "Processing took too long: " + processingTime + "ms");
    }

    @Test
    void testCheckpointingWithEmptyBatch() throws Exception {
        // Given - Create empty batch
        List<KinesisClientRecord> records = Arrays.asList();

        ProcessRecordsInput input = ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(mockCheckpointer)
            .build();

        // When - Process records
        recordProcessor.processRecords(input);

        // Then - Verify no checkpoint occurs for empty batch
        verify(mockCheckpointer, never()).checkpoint();
        assertEquals(0, recordProcessor.getProcessedRecords());
    }

    // Helper methods
    private KinesisClientRecord createTestRecord(String data, String partitionKey) {
        return KinesisClientRecord.builder()
            .data(ByteBuffer.wrap(data.getBytes()))
            .partitionKey(partitionKey)
            .sequenceNumber("test-sequence-" + System.currentTimeMillis() + "-" + Math.random())
            .build();
    }

    private List<KinesisClientRecord> createLargeBatch(int count) {
        List<KinesisClientRecord> records = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(createTestRecord("large-batch-data-" + i, "partition-key-" + i));
        }
        return records;
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
} 