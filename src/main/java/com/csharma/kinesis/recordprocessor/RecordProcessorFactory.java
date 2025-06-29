package com.csharma.kinesis.recordprocessor;

import com.csharma.kinesis.service.SchemaRegistryService;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

import java.lang.reflect.Method;

public class RecordProcessorFactory implements ShardRecordProcessorFactory {

    private final Object bean;
    private final Method method;
    private final int threadPoolSize;
    private final SchemaRegistryService schemaRegistryService;
    private final boolean validateSchema;
    private final String schemaRegistryName;
    private final String schemaName;
    private final String schemaVersion;
    private final String dataFormat;
    private final boolean failOnValidationError;

    public RecordProcessorFactory(Object bean, Method method) {
        this(bean, method, Runtime.getRuntime().availableProcessors(), null, false, "", "", "", "JSON", true);
    }

    public RecordProcessorFactory(Object bean, Method method, int threadPoolSize) {
        this(bean, method, threadPoolSize, null, false, "", "", "", "JSON", true);
    }

    public RecordProcessorFactory(Object bean, Method method, int threadPoolSize, 
                                SchemaRegistryService schemaRegistryService, boolean validateSchema,
                                String schemaRegistryName, String schemaName, String schemaVersion, 
                                String dataFormat, boolean failOnValidationError) {
        this.bean = bean;
        this.method = method;
        this.threadPoolSize = threadPoolSize;
        this.schemaRegistryService = schemaRegistryService;
        this.validateSchema = validateSchema;
        this.schemaRegistryName = schemaRegistryName;
        this.schemaName = schemaName;
        this.schemaVersion = schemaVersion;
        this.dataFormat = dataFormat;
        this.failOnValidationError = failOnValidationError;
    }

    @Override
    public ShardRecordProcessor shardRecordProcessor() {
        return new RecordProcessor(bean, method, threadPoolSize, schemaRegistryService, validateSchema,
                                 schemaRegistryName, schemaName, schemaVersion, dataFormat, failOnValidationError);
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public boolean isSchemaValidationEnabled() {
        return validateSchema;
    }

    public String getSchemaRegistryName() {
        return schemaRegistryName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getDataFormat() {
        return dataFormat;
    }
}