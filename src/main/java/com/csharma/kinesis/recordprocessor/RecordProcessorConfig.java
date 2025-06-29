package com.csharma.kinesis.recordprocessor;

import com.csharma.kinesis.service.SchemaRegistryService;

import java.lang.reflect.Method;


public class RecordProcessorConfig {
    private Object bean;
    private Method method;
    private int threadPoolSize;
    private SchemaRegistryService schemaRegistryService;
    private boolean validateSchema;
    private String schemaRegistryName;
    private String schemaName;
    private String schemaVersion;
    private String dataFormat;
    private boolean failOnValidationError;

    public RecordProcessorConfig(Object bean, Method method, int threadPoolSize,
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

    // Getter methods
    public Object getBean() {
        return bean;
    }

    public Method getMethod() {
        return method;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public SchemaRegistryService getSchemaRegistryService() {
        return schemaRegistryService;
    }

    public boolean isValidateSchema() {
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

    public boolean isFailOnValidationError() {
        return failOnValidationError;
    }

    // Setter methods
    public void setBean(Object bean) {
        this.bean = bean;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public void setSchemaRegistryService(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    public void setValidateSchema(boolean validateSchema) {
        this.validateSchema = validateSchema;
    }

    public void setSchemaRegistryName(String schemaRegistryName) {
        this.schemaRegistryName = schemaRegistryName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public void setFailOnValidationError(boolean failOnValidationError) {
        this.failOnValidationError = failOnValidationError;
    }
} 