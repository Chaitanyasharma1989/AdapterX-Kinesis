package com.csharma.kinesis.validation;

/**
 * No-operation record validator that does nothing.
 * Used when schema validation is disabled.
 */
public class NoOpRecordValidator implements RecordValidator {
    
    @Override
    public void validate(String data, String partitionKey) throws Exception {
        // Do nothing - validation is disabled
    }
} 