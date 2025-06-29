package com.csharma.kinesis.serialization;

import java.nio.ByteBuffer;

/**
 * Interface for data serialization following Interface Segregation Principle.
 * Responsible for converting objects to ByteBuffer for Kinesis transmission.
 */
public interface DataSerializer {
    
    /**
     * Serialize an object to ByteBuffer
     * 
     * @param data the object to serialize
     * @return ByteBuffer containing serialized data
     * @throws SerializationException if serialization fails
     */
    ByteBuffer serialize(Object data) throws SerializationException;
    
    /**
     * Deserialize ByteBuffer to object
     * 
     * @param data the ByteBuffer to deserialize
     * @param targetClass the target class type
     * @param <T> the target type
     * @return deserialized object
     * @throws SerializationException if deserialization fails
     */
    <T> T deserialize(ByteBuffer data, Class<T> targetClass) throws SerializationException;
    
    /**
     * Get the content type of this serializer
     * 
     * @return content type string
     */
    String getContentType();
} 