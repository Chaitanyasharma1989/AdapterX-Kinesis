package com.csharma.kinesis.manager;

import com.csharma.kinesis.listener.KinesisListener;
import com.csharma.kinesis.configuration.KinesisProperties;
import com.csharma.kinesis.recordprocessor.RecordProcessorFactory;
import com.csharma.kinesis.service.CrossAccountCredentialsService;
import com.csharma.kinesis.service.SchemaRegistryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.KinesisClientUtil;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.retrieval.fanout.FanOutConfig;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class KinesisConsumerManager {
    private static final Logger log = LoggerFactory.getLogger(KinesisConsumerManager.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private KinesisProperties kinesisProperties;

    @Autowired
    private CrossAccountCredentialsService crossAccountCredentialsService;

    @Autowired
    private SchemaRegistryService schemaRegistryService;

    private final Map<String, Scheduler> schedulers = new ConcurrentHashMap<>();
    private final Map<String, String> consumerArns = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private KinesisAsyncClient kinesisClient;

    @PostConstruct
    public void init() {
        Region region = Region.of(kinesisProperties.getRegion());
        this.kinesisClient = KinesisClientUtil.createKinesisAsyncClient(
                KinesisAsyncClient.builder().region(region));
        scanForKinesisListeners();
    }

    private void scanForKinesisListeners() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Method[] methods = bean.getClass().getMethods();
            for (Method method : methods) {
                KinesisListener annotation = method.getAnnotation(KinesisListener.class);
                if (annotation != null) {
                    createConsumer(bean, method, annotation);
                }
            }
        }
    }

    private void createConsumer(Object bean, Method method, KinesisListener annotation) {
        String streamName = annotation.streamName();
        String applicationName = annotation.applicationName().isEmpty() ? "kinesis-consumer-" + streamName : annotation.applicationName();
        boolean useEFO = annotation.enhancedFanOut() || kinesisProperties.getConsumer().getEnhancedFanOut().isEnabled();
        boolean isCrossAccount = isCrossAccountConfiguration(annotation);
        boolean isSchemaValidation = isSchemaValidationConfiguration(annotation);
        try {
            Region region = Region.of(kinesisProperties.getRegion());
            AwsCredentialsProvider credentialsProvider = getCredentialsProvider(annotation);
            if (isSchemaValidation) {
                schemaRegistryService.initialize(credentialsProvider);
            }


            DynamoDbAsyncClient dynamoClient = DynamoDbAsyncClient.builder()
                    .region(region)
                    .credentialsProvider(credentialsProvider)
                    .build();

            CloudWatchAsyncClient cloudWatchClient = CloudWatchAsyncClient.builder()
                    .region(region)
                    .credentialsProvider(credentialsProvider)
                    .build();

            KinesisAsyncClient consumerKinesisClient = KinesisAsyncClient.builder()
                    .region(region)
                    .credentialsProvider(credentialsProvider)
                    .build();

            RecordProcessorFactory recordProcessorFactory = createRecordProcessorFactory(
                    bean, method, annotation, isSchemaValidation
            );

            ConfigsBuilder configsBuilder = new ConfigsBuilder(streamName,
                    applicationName,
                    consumerKinesisClient,
                    dynamoClient,
                    cloudWatchClient,
                    applicationName,
                    recordProcessorFactory);

            Scheduler scheduler;

            if (useEFO) {
                String consumerName = annotation.consumerName().isEmpty() ? applicationName + "-efo-consumer" : annotation.consumerName();
                String consumerArn = createOrGetEnhancedFanOutConsumer(streamName, consumerName, consumerKinesisClient);
                consumerArns.put(streamName + "-" + consumerName, consumerArn);

                FanOutConfig fanOutConfig = new FanOutConfig(consumerKinesisClient)
                        .streamName(streamName)
                        .applicationName(applicationName)
                        .consumerArn(consumerArn);

                scheduler = new Scheduler(
                        configsBuilder.checkpointConfig(),
                        configsBuilder.coordinatorConfig(),
                        configsBuilder.leaseManagementConfig(),
                        configsBuilder.lifecycleConfig(),
                        configsBuilder.metricsConfig(),
                        configsBuilder.processorConfig(),
                        configsBuilder.retrievalConfig().retrievalSpecificConfig(fanOutConfig)
                );

                log.info("Created Enhanced Fan-Out consumer for stream: {} with consumer ARN: {} (Cross-account: {}, Schema validation: {})",
                        streamName, consumerArn, isCrossAccount, isSchemaValidation);
            } else {
                PollingConfig pollingConfig = new PollingConfig(streamName, consumerKinesisClient);
                pollingConfig.maxRecords(annotation.maxRecords());
                pollingConfig.idleTimeBetweenReadsInMillis(annotation.idleTimeBetweenReadsInMillis());

                scheduler = new Scheduler(
                        configsBuilder.checkpointConfig(),
                        configsBuilder.coordinatorConfig(),
                        configsBuilder.leaseManagementConfig(),
                        configsBuilder.lifecycleConfig(),
                        configsBuilder.metricsConfig(),
                        configsBuilder.processorConfig(),
                        configsBuilder.retrievalConfig().retrievalSpecificConfig(pollingConfig)
                );

                log.info("Created standard polling consumer for stream: {} (Cross-account: {}, Schema validation: {})",
                        streamName, isCrossAccount, isSchemaValidation);
            }

            schedulers.put(streamName, scheduler);
            executorService.submit(() -> {
                try {
                    log.info("Starting Kinesis consumer for stream: {} (EFO: {}, Cross-account: {}, Schema validation: {})",
                            streamName, useEFO, isCrossAccount, isSchemaValidation);
                    scheduler.run();
                } catch (Exception e) {
                    log.error("Error running Kinesis consumer for stream: {}", streamName, e);
                }
            });

        } catch (Exception e) {
            log.error("Failed to create consumer for stream: {}", streamName, e);
        }
    }

    private RecordProcessorFactory createRecordProcessorFactory(Object bean, Method method,
                                                                KinesisListener annotation, boolean isSchemaValidation) {
        if (isSchemaValidation) {
            String schemaRegistryName = annotation.schemaRegistryName().isEmpty() ?
                    kinesisProperties.getSchemaRegistry().getRegistryName() : annotation.schemaRegistryName();
            String schemaName = annotation.schemaName().isEmpty() ?
                    kinesisProperties.getSchemaRegistry().getSchemaName() : annotation.schemaName();
            String schemaVersion = annotation.schemaVersion().isEmpty() ?
                    kinesisProperties.getSchemaRegistry().getSchemaVersion() : annotation.schemaVersion();
            String dataFormat = annotation.dataFormat();
            boolean failOnValidationError = annotation.failOnValidationError();

            return new RecordProcessorFactory(
                    bean, method, kinesisProperties.getConsumer().getThreadPool().getSize(),
                    schemaRegistryService, true, schemaRegistryName, schemaName, schemaVersion,
                    dataFormat, failOnValidationError
            );
        } else {
            return new RecordProcessorFactory(bean, method, kinesisProperties.getConsumer().getThreadPool().getSize());
        }
    }

    private boolean isCrossAccountConfiguration(KinesisListener annotation) {
        return annotation.targetAccountId() != null && !annotation.targetAccountId().trim().isEmpty();
    }

    private boolean isSchemaValidationConfiguration(KinesisListener annotation) {
        return annotation.validateSchema() || kinesisProperties.getSchemaRegistry().isEnabled();
    }

    private AwsCredentialsProvider getCredentialsProvider(KinesisListener annotation) {
        if (isCrossAccountConfiguration(annotation)) {
            return crossAccountCredentialsService.getCredentialsForAccount(
                    annotation.targetAccountId(),
                    annotation.roleArn(),
                    annotation.externalId(),
                    annotation.sessionName()
            );
        } else {
            // Use default credentials for same-account access
            return software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create();
        }
    }

    private String createOrGetEnhancedFanOutConsumer(String streamName, String consumerName, KinesisAsyncClient kinesisClient) {
        try {
            DescribeStreamConsumerRequest describeRequest = DescribeStreamConsumerRequest.builder()
                    .streamARN(getStreamArn(streamName, kinesisClient))
                    .consumerName(consumerName)
                    .build();

            try {
                DescribeStreamConsumerResponse describeResponse = kinesisClient.describeStreamConsumer(describeRequest).get();
                ConsumerDescription consumer = describeResponse.consumerDescription();

                if (consumer.consumerStatus() == ConsumerStatus.ACTIVE) {
                    log.info("Found existing active EFO consumer: {} for stream: {}", consumerName, streamName);
                    return consumer.consumerARN();
                } else {
                    log.info("Found existing EFO consumer in {} state, waiting for ACTIVE...", consumer.consumerStatus());
                    return waitForConsumerActive(consumer.consumerARN(), kinesisClient);
                }
            } catch (Exception e) {
                if (kinesisProperties.getConsumer().getEnhancedFanOut().isAutoCreateConsumer()) {
                    log.info("Creating new EFO consumer: {} for stream: {}", consumerName, streamName);
                    return createEnhancedFanOutConsumer(streamName, consumerName, kinesisClient);
                } else {
                    throw new RuntimeException("EFO consumer does not exist and auto-creation is disabled", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to create or get EFO consumer: {} for stream: {}", consumerName, streamName, e);
            throw new RuntimeException("Failed to setup Enhanced Fan-Out consumer", e);
        }
    }

    private String createEnhancedFanOutConsumer(String streamName, String consumerName, KinesisAsyncClient kinesisClient) {
        try {
            RegisterStreamConsumerRequest registerRequest = RegisterStreamConsumerRequest.builder()
                    .streamARN(getStreamArn(streamName, kinesisClient))
                    .consumerName(consumerName)
                    .build();

            RegisterStreamConsumerResponse registerResponse = kinesisClient.registerStreamConsumer(registerRequest).get();
            String consumerArn = registerResponse.consumer().consumerARN();

            log.info("Created EFO consumer: {} with ARN: {}", consumerName, consumerArn);
            return waitForConsumerActive(consumerArn, kinesisClient);

        } catch (Exception e) {
            log.error("Failed to create EFO consumer: {}", consumerName, e);
            throw new RuntimeException("Failed to create Enhanced Fan-Out consumer", e);
        }
    }

    private String waitForConsumerActive(String consumerArn, KinesisAsyncClient kinesisClient) throws Exception {
        int maxRetries = kinesisProperties.getConsumer().getEnhancedFanOut().getMaxSubscriptionRetries();
        long backoffMillis = kinesisProperties.getConsumer().getEnhancedFanOut().getSubscriptionRetryBackoffMillis();

        for (int i = 0; i < maxRetries; i++) {
            DescribeStreamConsumerRequest describeRequest = DescribeStreamConsumerRequest.builder()
                    .consumerARN(consumerArn)
                    .build();

            DescribeStreamConsumerResponse response = kinesisClient.describeStreamConsumer(describeRequest).get();
            ConsumerStatus status = response.consumerDescription().consumerStatus();

            if (status == ConsumerStatus.ACTIVE) {
                log.info("EFO consumer is now ACTIVE: {}", consumerArn);
                return consumerArn;
            }

            log.info("EFO consumer status: {}, waiting... (attempt {}/{})", status, i + 1, maxRetries);
            Thread.sleep(backoffMillis);
        }

        throw new RuntimeException("EFO consumer did not become ACTIVE within timeout period");
    }

    private String getStreamArn(String streamName, KinesisAsyncClient kinesisClient) {
        try {
            DescribeStreamRequest request = DescribeStreamRequest.builder()
                    .streamName(streamName)
                    .build();
            DescribeStreamResponse response = kinesisClient.describeStream(request).get();
            return response.streamDescription().streamARN();
        } catch (Exception e) {
            log.error("Failed to get stream ARN for: {}", streamName, e);
            throw new RuntimeException("Failed to get stream ARN", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Kinesis consumers...");
        crossAccountCredentialsService.shutdown();
        schemaRegistryService.shutdown();
        if (kinesisProperties.getConsumer().getEnhancedFanOut().isAutoCreateConsumer()) {
            for (Map.Entry<String, String> entry : consumerArns.entrySet()) {
                try {
                    DeregisterStreamConsumerRequest deregisterRequest = DeregisterStreamConsumerRequest.builder()
                            .consumerARN(entry.getValue())
                            .build();
                    kinesisClient.deregisterStreamConsumer(deregisterRequest).get();
                    log.info("Deregistered EFO consumer: {}", entry.getKey());
                } catch (Exception e) {
                    log.warn("Failed to deregister EFO consumer: {}", entry.getKey(), e);
                }
            }
        }

        if (kinesisClient != null) {
            kinesisClient.close();
        }

        for (Map.Entry<String, Scheduler> entry : schedulers.entrySet()) {
            try {
                Future<Boolean> gracefulShutdownFuture = entry.getValue().startGracefulShutdown();
                gracefulShutdownFuture.get(); // Wait for shutdown
                log.info("Gracefully shut down consumer for stream: {}", entry.getKey());
            } catch (Exception e) {
                log.error("Error shutting down consumer for stream: {}", entry.getKey(), e);
            }
        }
        executorService.shutdown();
    }

}
