package com.csharma.kinesis.configuration;

public interface ProducerConfiguration {

    String getRegion();

    String getEndpoint();

    int getMaxConnections();

    long getRequestTimeout();

    long getRecordMaxBufferedTime();

    long getRecordTtl();

    boolean isAggregationEnabled();

    int getMaxUserRecordsPerRequest();

    int getMaxUserRecordSizeBytes();

    long getBulkOperationTimeout();

    int getMaxBatchSize();
} 