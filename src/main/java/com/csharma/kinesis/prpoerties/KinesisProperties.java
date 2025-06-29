package com.csharma.prpoerties;

import jakarta.annotation.sql.DataSourceDefinition;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Data
@Component
@ConfigurationProperties(prefix = "kinesis")
public class KinesisProperties {

    private String region = "us-east-1";
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    private String endpoint;

    @Data
    public static class Producer {
        private int maxConnections = 24;
        private long requestTimeout = 6000;
        private long recordMaxBufferedTime = 100;
        private int recordTtl = 30000;
        private String aggregationEnabled = "true";
        private int maxUserRecordsPerRequest = 500;
        private long maxUserRecordSizeBytes = 1048576;
    }

    @Data
    public static class Consumer {
        private long failoverTimeMillis = 10000L;
        private long idleTimeBetweenReadsInMillis = 1000L;
        private int maxRecords = 10000;
        private long shardSyncIntervalMillis = 60000L;
        private boolean callProcessRecordsEvenForEmptyRecordList = false;
        private String initialPositionInStream = "LATEST";
    }
}