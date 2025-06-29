package com.csharma.kinesis.prpoerties;

import com.csharma.kinesis.producer.ProducerConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kinesis")
public class KinesisProperties implements ProducerConfiguration {

    private String region = "us-east-1";
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    private String endpoint;
    private CrossAccount crossAccount = new CrossAccount();
    private SchemaRegistry schemaRegistry = new SchemaRegistry();

    public static class Producer {
        private int maxConnections = 24;
        private long requestTimeout = 6000;
        private long recordMaxBufferedTime = 100;
        private int recordTtl = 30000;
        private String aggregationEnabled = "true";
        private int maxUserRecordsPerRequest = 500;
        private long maxUserRecordSizeBytes = 1048576;
        private long bulkOperationTimeout = 30000; // 30 seconds
        private int maxBatchSize = 100; // Maximum records per batch for bulk operations
        
        // Getter methods
        public int getMaxConnections() { return maxConnections; }
        public long getRequestTimeout() { return requestTimeout; }
        public long getRecordMaxBufferedTime() { return recordMaxBufferedTime; }
        public int getRecordTtl() { return recordTtl; }
        public String getAggregationEnabled() { return aggregationEnabled; }
        public int getMaxUserRecordsPerRequest() { return maxUserRecordsPerRequest; }
        public long getMaxUserRecordSizeBytes() { return maxUserRecordSizeBytes; }
        public long getBulkOperationTimeout() { return bulkOperationTimeout; }
        public int getMaxBatchSize() { return maxBatchSize; }
        
        // Setter methods
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        public void setRequestTimeout(long requestTimeout) { this.requestTimeout = requestTimeout; }
        public void setRecordMaxBufferedTime(long recordMaxBufferedTime) { this.recordMaxBufferedTime = recordMaxBufferedTime; }
        public void setRecordTtl(int recordTtl) { this.recordTtl = recordTtl; }
        public void setAggregationEnabled(String aggregationEnabled) { this.aggregationEnabled = aggregationEnabled; }
        public void setMaxUserRecordsPerRequest(int maxUserRecordsPerRequest) { this.maxUserRecordsPerRequest = maxUserRecordsPerRequest; }
        public void setMaxUserRecordSizeBytes(long maxUserRecordSizeBytes) { this.maxUserRecordSizeBytes = maxUserRecordSizeBytes; }
        public void setBulkOperationTimeout(long bulkOperationTimeout) { this.bulkOperationTimeout = bulkOperationTimeout; }
        public void setMaxBatchSize(int maxBatchSize) { this.maxBatchSize = maxBatchSize; }
    }

    public static class Consumer {
        private long failoverTimeMillis = 10000L;
        private long idleTimeBetweenReadsInMillis = 1000L;
        private int maxRecords = 10000;
        private long shardSyncIntervalMillis = 60000L;
        private boolean callProcessRecordsEvenForEmptyRecordList = false;
        private String initialPositionInStream = "LATEST";
        private EnhancedFanOut enhancedFanOut = new EnhancedFanOut();
        private ThreadPool threadPool = new ThreadPool();
        
        // Getter methods
        public long getFailoverTimeMillis() { return failoverTimeMillis; }
        public long getIdleTimeBetweenReadsInMillis() { return idleTimeBetweenReadsInMillis; }
        public int getMaxRecords() { return maxRecords; }
        public long getShardSyncIntervalMillis() { return shardSyncIntervalMillis; }
        public boolean isCallProcessRecordsEvenForEmptyRecordList() { return callProcessRecordsEvenForEmptyRecordList; }
        public String getInitialPositionInStream() { return initialPositionInStream; }
        public EnhancedFanOut getEnhancedFanOut() { return enhancedFanOut; }
        public ThreadPool getThreadPool() { return threadPool; }
        
        // Setter methods
        public void setFailoverTimeMillis(long failoverTimeMillis) { this.failoverTimeMillis = failoverTimeMillis; }
        public void setIdleTimeBetweenReadsInMillis(long idleTimeBetweenReadsInMillis) { this.idleTimeBetweenReadsInMillis = idleTimeBetweenReadsInMillis; }
        public void setMaxRecords(int maxRecords) { this.maxRecords = maxRecords; }
        public void setShardSyncIntervalMillis(long shardSyncIntervalMillis) { this.shardSyncIntervalMillis = shardSyncIntervalMillis; }
        public void setCallProcessRecordsEvenForEmptyRecordList(boolean callProcessRecordsEvenForEmptyRecordList) { this.callProcessRecordsEvenForEmptyRecordList = callProcessRecordsEvenForEmptyRecordList; }
        public void setInitialPositionInStream(String initialPositionInStream) { this.initialPositionInStream = initialPositionInStream; }
        public void setEnhancedFanOut(EnhancedFanOut enhancedFanOut) { this.enhancedFanOut = enhancedFanOut; }
        public void setThreadPool(ThreadPool threadPool) { this.threadPool = threadPool; }
    }

    public static class ThreadPool {
        private int size = Runtime.getRuntime().availableProcessors();
        private int queueCapacity = 1000;
        private long shutdownTimeoutMs = 30000L;
        private long recordProcessingTimeoutMs = 30000L;
        private long activeTaskWaitTimeoutMs = 30000L;
        
        // Getter methods
        public int getSize() { return size; }
        public int getQueueCapacity() { return queueCapacity; }
        public long getShutdownTimeoutMs() { return shutdownTimeoutMs; }
        public long getRecordProcessingTimeoutMs() { return recordProcessingTimeoutMs; }
        public long getActiveTaskWaitTimeoutMs() { return activeTaskWaitTimeoutMs; }
        
        // Setter methods
        public void setSize(int size) { this.size = size; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        public void setShutdownTimeoutMs(long shutdownTimeoutMs) { this.shutdownTimeoutMs = shutdownTimeoutMs; }
        public void setRecordProcessingTimeoutMs(long recordProcessingTimeoutMs) { this.recordProcessingTimeoutMs = recordProcessingTimeoutMs; }
        public void setActiveTaskWaitTimeoutMs(long activeTaskWaitTimeoutMs) { this.activeTaskWaitTimeoutMs = activeTaskWaitTimeoutMs; }
    }

    public static class EnhancedFanOut {
        private boolean enabled = false;
        private boolean autoCreateConsumer = true;
        private long subscribeToShardTimeout = 30000L;
        private int maxSubscriptionRetries = 3;
        private long subscriptionRetryBackoffMillis = 1000L;
        
        // Getter methods
        public boolean isEnabled() { return enabled; }
        public boolean isAutoCreateConsumer() { return autoCreateConsumer; }
        public long getSubscribeToShardTimeout() { return subscribeToShardTimeout; }
        public int getMaxSubscriptionRetries() { return maxSubscriptionRetries; }
        public long getSubscriptionRetryBackoffMillis() { return subscriptionRetryBackoffMillis; }
        
        // Setter methods
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setAutoCreateConsumer(boolean autoCreateConsumer) { this.autoCreateConsumer = autoCreateConsumer; }
        public void setSubscribeToShardTimeout(long subscribeToShardTimeout) { this.subscribeToShardTimeout = subscribeToShardTimeout; }
        public void setMaxSubscriptionRetries(int maxSubscriptionRetries) { this.maxSubscriptionRetries = maxSubscriptionRetries; }
        public void setSubscriptionRetryBackoffMillis(long subscriptionRetryBackoffMillis) { this.subscriptionRetryBackoffMillis = subscriptionRetryBackoffMillis; }
    }

    public static class CrossAccount {
        private boolean enabled = false;
        private String defaultTargetAccountId = "";
        private String defaultRoleArn = "";
        private String defaultExternalId = "";
        private String defaultSessionName = "kinesis-cross-account-session";
        private int sessionDurationSeconds = 3600; // 1 hour
        private boolean useDefaultCredentials = true;
        private String profileName = "";
        
        // Getter methods
        public boolean isEnabled() { return enabled; }
        public String getDefaultTargetAccountId() { return defaultTargetAccountId; }
        public String getDefaultRoleArn() { return defaultRoleArn; }
        public String getDefaultExternalId() { return defaultExternalId; }
        public String getDefaultSessionName() { return defaultSessionName; }
        public int getSessionDurationSeconds() { return sessionDurationSeconds; }
        public boolean isUseDefaultCredentials() { return useDefaultCredentials; }
        public String getProfileName() { return profileName; }
        
        // Setter methods
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setDefaultTargetAccountId(String defaultTargetAccountId) { this.defaultTargetAccountId = defaultTargetAccountId; }
        public void setDefaultRoleArn(String defaultRoleArn) { this.defaultRoleArn = defaultRoleArn; }
        public void setDefaultExternalId(String defaultExternalId) { this.defaultExternalId = defaultExternalId; }
        public void setDefaultSessionName(String defaultSessionName) { this.defaultSessionName = defaultSessionName; }
        public void setSessionDurationSeconds(int sessionDurationSeconds) { this.sessionDurationSeconds = sessionDurationSeconds; }
        public void setUseDefaultCredentials(boolean useDefaultCredentials) { this.useDefaultCredentials = useDefaultCredentials; }
        public void setProfileName(String profileName) { this.profileName = profileName; }
    }

    public static class SchemaRegistry {
        private boolean enabled = false;
        private String registryName = "kinesis-schema-registry";
        private String schemaName = "kinesis-schema";
        private String schemaVersion = "1.0";
        private String dataFormat = "JSON"; // JSON, AVRO, PROTOBUF
        private boolean validateOnConsume = true;
        private boolean validateOnProduce = false;
        private boolean cacheSchemas = true;
        private int cacheSize = 1000;
        private long cacheExpirationMinutes = 60;
        private boolean failOnValidationError = true;
        private boolean logValidationErrors = true;
        private String compatibilityMode = "BACKWARD"; // BACKWARD, FORWARD, FULL, NONE
        private String description = "Kinesis stream schema for data validation";
        
        // Getter methods
        public boolean isEnabled() { return enabled; }
        public String getRegistryName() { return registryName; }
        public String getSchemaName() { return schemaName; }
        public String getSchemaVersion() { return schemaVersion; }
        public String getDataFormat() { return dataFormat; }
        public boolean isValidateOnConsume() { return validateOnConsume; }
        public boolean isValidateOnProduce() { return validateOnProduce; }
        public boolean isCacheSchemas() { return cacheSchemas; }
        public int getCacheSize() { return cacheSize; }
        public long getCacheExpirationMinutes() { return cacheExpirationMinutes; }
        public boolean isFailOnValidationError() { return failOnValidationError; }
        public boolean isLogValidationErrors() { return logValidationErrors; }
        public String getCompatibilityMode() { return compatibilityMode; }
        public String getDescription() { return description; }
        
        // Setter methods
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setRegistryName(String registryName) { this.registryName = registryName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
        public void setDataFormat(String dataFormat) { this.dataFormat = dataFormat; }
        public void setValidateOnConsume(boolean validateOnConsume) { this.validateOnConsume = validateOnConsume; }
        public void setValidateOnProduce(boolean validateOnProduce) { this.validateOnProduce = validateOnProduce; }
        public void setCacheSchemas(boolean cacheSchemas) { this.cacheSchemas = cacheSchemas; }
        public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }
        public void setCacheExpirationMinutes(long cacheExpirationMinutes) { this.cacheExpirationMinutes = cacheExpirationMinutes; }
        public void setFailOnValidationError(boolean failOnValidationError) { this.failOnValidationError = failOnValidationError; }
        public void setLogValidationErrors(boolean logValidationErrors) { this.logValidationErrors = logValidationErrors; }
        public void setCompatibilityMode(String compatibilityMode) { this.compatibilityMode = compatibilityMode; }
        public void setDescription(String description) { this.description = description; }
    }

    // ProducerConfiguration interface implementation
    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public int getMaxConnections() {
        return producer.getMaxConnections();
    }

    @Override
    public long getRequestTimeout() {
        return producer.getRequestTimeout();
    }

    @Override
    public long getRecordMaxBufferedTime() {
        return producer.getRecordMaxBufferedTime();
    }

    @Override
    public long getRecordTtl() {
        return producer.getRecordTtl();
    }

    @Override
    public boolean isAggregationEnabled() {
        return Boolean.parseBoolean(producer.getAggregationEnabled());
    }

    @Override
    public int getMaxUserRecordsPerRequest() {
        return producer.getMaxUserRecordsPerRequest();
    }

    @Override
    public int getMaxUserRecordSizeBytes() {
        return (int) producer.getMaxUserRecordSizeBytes();
    }

    @Override
    public long getBulkOperationTimeout() {
        return producer.getBulkOperationTimeout();
    }

    @Override
    public int getMaxBatchSize() {
        return producer.getMaxBatchSize();
    }
    
    // Additional getter methods for other classes
    public Consumer getConsumer() {
        return consumer;
    }
    
    public CrossAccount getCrossAccount() {
        return crossAccount;
    }
    
    public SchemaRegistry getSchemaRegistry() {
        return schemaRegistry;
    }
    
    // Setter methods for main class
    public void setRegion(String region) {
        this.region = region;
    }
    
    public void setProducer(Producer producer) {
        this.producer = producer;
    }
    
    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public void setCrossAccount(CrossAccount crossAccount) {
        this.crossAccount = crossAccount;
    }
    
    public void setSchemaRegistry(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }
} 