package com.csharma.kinesis.producer;

/**
 * Interface for producer configuration following Interface Segregation Principle.
 * Responsible for providing configuration parameters for Kinesis producer.
 */
public interface ProducerConfiguration {
    
    /**
     * Get the AWS region
     * 
     * @return region string
     */
    String getRegion();
    
    /**
     * Get the Kinesis endpoint (optional, for local testing)
     * 
     * @return endpoint string or null
     */
    String getEndpoint();
    
    /**
     * Get maximum number of connections
     * 
     * @return max connections
     */
    int getMaxConnections();
    
    /**
     * Get request timeout in milliseconds
     * 
     * @return timeout in ms
     */
    long getRequestTimeout();
    
    /**
     * Get record max buffered time in milliseconds
     * 
     * @return buffered time in ms
     */
    long getRecordMaxBufferedTime();
    
    /**
     * Get record TTL in milliseconds
     * 
     * @return TTL in ms
     */
    long getRecordTtl();
    
    /**
     * Check if aggregation is enabled
     * 
     * @return true if enabled
     */
    boolean isAggregationEnabled();
    
    /**
     * Get maximum user records per request
     * 
     * @return max records per request
     */
    int getMaxUserRecordsPerRequest();
    
    /**
     * Get maximum user record size in bytes
     * 
     * @return max record size in bytes
     */
    int getMaxUserRecordSizeBytes();
    
    /**
     * Get bulk operation timeout in milliseconds
     * 
     * @return timeout in ms
     */
    long getBulkOperationTimeout();
    
    /**
     * Get maximum batch size for bulk operations
     * 
     * @return max batch size
     */
    int getMaxBatchSize();
} 