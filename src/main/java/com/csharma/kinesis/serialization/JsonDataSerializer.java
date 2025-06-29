package com.csharma.kinesis.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * JSON implementation of DataSerializer using Jackson ObjectMapper.
 */
public class JsonDataSerializer implements DataSerializer {
    
    private static final Logger log = LoggerFactory.getLogger(JsonDataSerializer.class);
    
    private final ObjectMapper objectMapper;
    
    public JsonDataSerializer() {
        this.objectMapper = new ObjectMapper();
    }
    
    public JsonDataSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ByteBuffer serialize(Object data) throws SerializationException {
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            return ByteBuffer.wrap(jsonString.getBytes());
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", data, e);
            throw new SerializationException("Failed to serialize object to JSON", e);
        }
    }
    
    @Override
    public <T> T deserialize(ByteBuffer data, Class<T> targetClass) throws SerializationException {
        try {
            String jsonString = new String(data.array());
            return objectMapper.readValue(jsonString, targetClass);
        } catch (Exception e) {
            log.error("Failed to deserialize JSON to object of type: {}", targetClass, e);
            throw new SerializationException("Failed to deserialize JSON to object", e);
        }
    }
    
    @Override
    public String getContentType() {
        return "application/json";
    }
} 