package com.csharma.kinesis.service;

import com.csharma.kinesis.configuration.KinesisProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.*;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;

@Service
public class SchemaRegistryService {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryService.class);
    
    @Autowired
    private KinesisProperties kinesisProperties;
    
    @Autowired
    private CrossAccountCredentialsService crossAccountCredentialsService;
    
    private final ConcurrentHashMap<String, CachedSchema> schemaCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private GlueClient glueClient;
    
    public SchemaRegistryService() {
        // Start cleanup task for expired schemas
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSchemas, 1, 1, TimeUnit.HOURS);
    }
    
    public void initialize(AwsCredentialsProvider credentialsProvider) {
        if (glueClient == null) {
            glueClient = GlueClient.builder()
                    .credentialsProvider(credentialsProvider)
                    .region(software.amazon.awssdk.regions.Region.of(kinesisProperties.getRegion()))
                    .build();
        }
    }
    
    public boolean validateRecord(String data, String streamName, String schemaRegistryName, 
                                String schemaName, String schemaVersion, String dataFormat) {
        if (!kinesisProperties.getSchemaRegistry().isEnabled()) {
            log.debug("Schema validation is disabled");
            return true;
        }
        
        try {
            String cacheKey = generateCacheKey(schemaRegistryName, schemaName, schemaVersion);
            CachedSchema cachedSchema = schemaCache.get(cacheKey);
            
            if (cachedSchema == null || cachedSchema.isExpired()) {
                log.debug("Fetching schema from registry: {}", cacheKey);
                String schemaDefinition = getSchemaDefinition(schemaRegistryName, schemaName, schemaVersion);
                cachedSchema = new CachedSchema(schemaDefinition, System.currentTimeMillis() + 
                        (kinesisProperties.getSchemaRegistry().getCacheExpirationMinutes() * 60 * 1000L));
                schemaCache.put(cacheKey, cachedSchema);
            }
            
            return validateData(data, cachedSchema.getSchemaDefinition(), dataFormat);
            
        } catch (Exception e) {
            log.error("Schema validation failed for stream: {}", streamName, e);
            if (kinesisProperties.getSchemaRegistry().isFailOnValidationError()) {
                throw new SchemaValidationException("Schema validation failed", e);
            }
            return false;
        }
    }
    
    private String getSchemaDefinition(String registryName, String schemaName, String schemaVersion) {
        try {
            GetSchemaVersionRequest request = GetSchemaVersionRequest.builder()
                    .schemaId(SchemaId.builder()
                            .registryName(registryName)
                            .schemaName(schemaName)
                            .build())
                    .schemaVersionId(schemaVersion)
                    .build();
            
            GetSchemaVersionResponse response = glueClient.getSchemaVersion(request);
            return response.schemaDefinition();
            
        } catch (Exception e) {
            log.error("Failed to get schema definition for registry: {}, schema: {}", registryName, schemaName, e);
            throw new RuntimeException("Failed to retrieve schema definition", e);
        }
    }
    
    private boolean validateData(String data, String schemaDefinition, String dataFormat) {
        try {
            switch (dataFormat.toUpperCase()) {
                case "JSON":
                    return validateJsonData(data, schemaDefinition);
                case "AVRO":
                    return validateAvroData(data, schemaDefinition);
                case "PROTOBUF":
                    return validateProtobufData(data, schemaDefinition);
                default:
                    log.warn("Unsupported data format: {}", dataFormat);
                    return true;
            }
        } catch (Exception e) {
            log.error("Data validation failed for format: {}", dataFormat, e);
            if (kinesisProperties.getSchemaRegistry().isLogValidationErrors()) {
                log.error("Validation error details - Data: {}, Schema: {}", data, schemaDefinition);
            }
            return false;
        }
    }
    
    private boolean validateJsonData(String data, String schemaDefinition) {
        try {
            JsonNode dataNode = objectMapper.readTree(data);
            JsonNode schemaNode = objectMapper.readTree(schemaDefinition);
            
            // Basic JSON schema validation
            return validateJsonSchema(dataNode, schemaNode);
            
        } catch (Exception e) {
            log.error("JSON validation failed", e);
            return false;
        }
    }
    
    private boolean validateJsonSchema(JsonNode data, JsonNode schema) {
        // Basic JSON schema validation implementation
        // This is a simplified version - in production, you might want to use a full JSON Schema validator
        
        if (schema.has("type")) {
            String expectedType = schema.get("type").asText();
            if (!validateJsonType(data, expectedType)) {
                log.error("Type mismatch. Expected: {}, Got: {}", expectedType, data.getNodeType());
                return false;
            }
        }
        
        if (schema.has("required") && data.isObject()) {
            JsonNode required = schema.get("required");
            for (JsonNode field : required) {
                if (!data.has(field.asText())) {
                    log.error("Required field missing: {}", field.asText());
                    return false;
                }
            }
        }
        
        if (schema.has("properties") && data.isObject()) {
            JsonNode properties = schema.get("properties");
            Iterator<String> fieldNames = data.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (properties.has(fieldName)) {
                    if (!validateJsonSchema(data.get(fieldName), properties.get(fieldName))) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    private boolean validateJsonType(JsonNode data, String expectedType) {
        switch (expectedType.toLowerCase()) {
            case "string":
                return data.isTextual();
            case "number":
                return data.isNumber();
            case "integer":
                return data.isInt();
            case "boolean":
                return data.isBoolean();
            case "object":
                return data.isObject();
            case "array":
                return data.isArray();
            case "null":
                return data.isNull();
            default:
                return true;
        }
    }
    
    private boolean validateAvroData(String data, String schemaDefinition) {
        try {
            Schema schema = new Schema.Parser().parse(schemaDefinition);
            DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
            
            // For Avro, we assume the data is base64 encoded
            byte[] avroData = java.util.Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(avroData);
            
            Decoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
            reader.read(null, decoder);
            
            return true;
            
        } catch (Exception e) {
            log.error("Avro validation failed", e);
            return false;
        }
    }
    
    private boolean validateProtobufData(String data, String schemaDefinition) {
        try {
            // Protobuf validation would require the protobuf library
            // This is a placeholder implementation
            log.warn("Protobuf validation not implemented yet");
            return true;
            
        } catch (Exception e) {
            log.error("Protobuf validation failed", e);
            return false;
        }
    }
    
    public void createSchema(String registryName, String schemaName, String schemaDefinition, 
                           String dataFormat, String compatibilityMode) {
        try {
            CreateSchemaRequest request = CreateSchemaRequest.builder()
                    .registryId(RegistryId.builder().registryName(registryName).build())
                    .schemaName(schemaName)
                    .dataFormat(dataFormat)
                    .compatibility(compatibilityMode)
                    .schemaDefinition(schemaDefinition)
                    .description(kinesisProperties.getSchemaRegistry().getDescription())
                    .build();
            
            CreateSchemaResponse response = glueClient.createSchema(request);
            log.info("Created schema: {} in registry: {}", schemaName, registryName);
            
        } catch (Exception e) {
            log.error("Failed to create schema: {} in registry: {}", schemaName, registryName, e);
            throw new RuntimeException("Failed to create schema", e);
        }
    }
    
    public void updateSchema(String registryName, String schemaName, String schemaDefinition, 
                           String dataFormat, String compatibilityMode) {
        try {
            UpdateSchemaRequest request = UpdateSchemaRequest.builder()
                    .schemaId(SchemaId.builder()
                            .registryName(registryName)
                            .schemaName(schemaName)
                            .build())
                    .compatibility(compatibilityMode)
                    .description(kinesisProperties.getSchemaRegistry().getDescription())
                    .build();
            
            UpdateSchemaResponse response = glueClient.updateSchema(request);
            log.info("Schema updated successfully: {}", response.schemaArn());
            
            // Clear cache for this schema
            String cacheKey = generateCacheKey(registryName, schemaName, "");
            schemaCache.remove(cacheKey);
            
        } catch (Exception e) {
            log.error("Failed to update schema: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update schema", e);
        }
    }
    
    private String generateCacheKey(String registryName, String schemaName, String schemaVersion) {
        return String.format("%s:%s:%s", registryName, schemaName, schemaVersion);
    }
    
    private void cleanupExpiredSchemas() {
        long currentTime = System.currentTimeMillis();
        schemaCache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
        log.debug("Cleaned up expired schemas from cache");
    }
    
    private static class CachedSchema {
        private final String schemaDefinition;
        private final long expirationTime;
        
        public CachedSchema(String schemaDefinition, long expirationTime) {
            this.schemaDefinition = schemaDefinition;
            this.expirationTime = expirationTime;
        }
        
        public String getSchemaDefinition() {
            return schemaDefinition;
        }
        
        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        public boolean isExpired(long currentTime) {
            return currentTime >= expirationTime;
        }
    }
    
    public static class SchemaValidationException extends RuntimeException {
        public SchemaValidationException(String message) {
            super(message);
        }
        
        public SchemaValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
        
        if (glueClient != null) {
            glueClient.close();
        }
        
        schemaCache.clear();
    }
} 