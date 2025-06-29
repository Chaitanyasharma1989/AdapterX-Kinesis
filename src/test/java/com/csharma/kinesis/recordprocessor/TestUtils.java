package com.csharma.kinesis.recordprocessor;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Test utilities for RecordProcessor tests.
 */
public class TestUtils {

    @Mock
    protected RecordProcessorCheckpointer mockCheckpointer;

    private TestBean testBean;
    private Method testMethod;

    /**
     * Initialize mocks for test setup.
     */
    public void initMocks() {
        MockitoAnnotations.openMocks(this);
        setupTestBean();
    }

    /**
     * Setup test bean and method.
     */
    public void setupTestBean() {
        testBean = new TestBean();
        try {
            testMethod = TestBean.class.getMethod("handleRecord", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Test method not found", e);
        }
    }

    /**
     * Create a basic RecordProcessorConfig for testing.
     */
    public RecordProcessorConfig createBasicConfig() {
        return new RecordProcessorConfig(
            testBean, testMethod, 2, null, false, null, null, null, null, false
        );
    }

    /**
     * Create a test record with given data and partition key.
     */
    public KinesisClientRecord createTestRecord(String data, String partitionKey) {
        return KinesisClientRecord.builder()
            .data(ByteBuffer.wrap(data.getBytes()))
            .partitionKey(partitionKey)
            .sequenceNumber("test-sequence-" + System.currentTimeMillis() + "-" + Math.random())
            .build();
    }

    /**
     * Create a list of test records.
     */
    public List<KinesisClientRecord> createTestRecords(int count) {
        List<KinesisClientRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(createTestRecord("test-data-" + i, "partition-key-" + i));
        }
        return records;
    }

    /**
     * Create ProcessRecordsInput with given records and checkpointer.
     */
    public ProcessRecordsInput createProcessRecordsInput(List<KinesisClientRecord> records, RecordProcessorCheckpointer checkpointer) {
        return ProcessRecordsInput.builder()
            .records(records)
            .checkpointer(checkpointer)
            .build();
    }

    /**
     * Wait for processing to complete.
     */
    public void waitForProcessing(RecordProcessor processor, long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (processor.getActiveTasks() > 0 && 
               (System.currentTimeMillis() - startTime) < timeoutMs) {
            Thread.sleep(100);
        }
    }

    /**
     * Verify basic metrics for a processor.
     */
    public void verifyBasicMetrics(RecordProcessor processor, long expectedProcessed, long expectedFailed) {
        Assertions.assertEquals(expectedProcessed, processor.getProcessedRecords());
        Assertions.assertEquals(expectedFailed, processor.getFailedRecords());
        Assertions.assertEquals(0, processor.getActiveTasks());
    }

    // Test bean implementation
    public static class TestBean {
        public void handleRecord(String data) {
            // Simple test implementation
            System.out.println("Processing record: " + data);
        }
    }
} 