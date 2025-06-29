package com.csharma.kinesis.validation;

/**
 * Interface for record validation following Single Responsibility Principle.
 * Responsible for validating records before processing.
 */
public interface RecordValidator {
    
    /**
     * Validate a record
     * 
     * @param data the record data as string
     * @param partitionKey the partition key
     * @throws Exception if validation fails
     */
    void validate(String data, String partitionKey) throws Exception;
} 