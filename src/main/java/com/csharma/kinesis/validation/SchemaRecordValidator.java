package com.csharma.kinesis.validation;

import com.csharma.kinesis.service.SchemaRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schema-based record validator using AWS Glue Schema Registry.
 */
public class SchemaRecordValidator implements RecordValidator {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaRecordValidator.class);
    
    private final SchemaRegistryService schemaRegistryService;
    private final String schemaRegistryName;
    private final String schemaName;
    private final String schemaVersion;
    private final String dataFormat;
    private final boolean failOnValidationError;

    public SchemaRecordValidator(SchemaRegistryService schemaRegistryService,
                               String schemaRegistryName, String schemaName, 
                               String schemaVersion, String dataFormat, 
                               boolean failOnValidationError) {
        this.schemaRegistryService = schemaRegistryService;
        this.schemaRegistryName = schemaRegistryName;
        this.schemaName = schemaName;
        this.schemaVersion = schemaVersion;
        this.dataFormat = dataFormat;
        this.failOnValidationError = failOnValidationError;
    }

    @Override
    public void validate(String data, String partitionKey) throws Exception {
        if (schemaRegistryService == null) {
            log.warn("Schema registry service is null, skipping validation");
            return;
        }

        boolean isValid = schemaRegistryService.validateRecord(
            data, partitionKey, schemaRegistryName, schemaName, schemaVersion, dataFormat
        );

        if (!isValid) {
            log.warn("Schema validation failed for record with partition key: {}", partitionKey);
            if (failOnValidationError) {
                throw new SchemaRegistryService.SchemaValidationException(
                    "Schema validation failed for record: " + partitionKey
                );
            }
        }
    }
} 