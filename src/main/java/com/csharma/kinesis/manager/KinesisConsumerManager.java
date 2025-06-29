package com.csharma;



import com.csharma.listener.KinesisListener;
import com.csharma.prpoerties.KinesisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.coordinator.SchedulerConfig;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import software.amazon.kinesis.retrieval.polling.PollingConfig;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.KinesisClientUtil;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.List;
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

    private final Map<String, Scheduler> schedulers = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @PostConstruct
    public void init() {
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
        String applicationName = annotation.applicationName().isEmpty() ?
                "kinesis-consumer-" + streamName : annotation.applicationName();

        try {
            Region region = Region.of(kinesisProperties.getRegion());
            KinesisAsyncClient kinesisClient = KinesisClientUtil.createKinesisAsyncClient(
                    KinesisAsyncClient.builder().region(region));
            DynamoDbAsyncClient dynamoClient = DynamoDbAsyncClient.builder().region(region).build();
            CloudWatchAsyncClient cloudWatchClient = CloudWatchAsyncClient.builder().region(region).build();

            ConfigsBuilder configsBuilder = new ConfigsBuilder(streamName, applicationName, kinesisClient,
                    dynamoClient, cloudWatchClient, applicationName, new RecordProcessorFactory(bean, method));

            PollingConfig pollingConfig = new PollingConfig(streamName, kinesisClient);
            pollingConfig.maxRecords(annotation.maxRecords());
            pollingConfig.idleTimeBetweenReadsInMillis(annotation.idleTimeBetweenReadsInMillis());

            SchedulerConfig schedulerConfig = configsBuilder.schedulerConfig()
                    .schedulerConfig()
                    .build();

            Scheduler scheduler = new Scheduler(
                    configsBuilder.checkpointConfig(),
                    configsBuilder.coordinatorConfig(),
                    configsBuilder.leaseManagementConfig(),
                    configsBuilder.lifecycleConfig(),
                    configsBuilder.metricsConfig(),
                    configsBuilder.processorConfig(),
                    configsBuilder.retrievalConfig().retrievalSpecificConfig(pollingConfig)
            );

            schedulers.put(streamName, scheduler);

            // Start the scheduler in background
            executorService.submit(() -> {
                try {
                    log.info("Starting Kinesis consumer for stream: {}", streamName);
                    scheduler.run();
                } catch (Exception e) {
                    log.error("Error running Kinesis consumer for stream: {}", streamName, e);
                }
            });

        } catch (Exception e) {
            log.error("Failed to create consumer for stream: {}", streamName, e);
        }
    }

    private static class RecordProcessorFactory implements ShardRecordProcessorFactory {
        private final Object bean;
        private final Method method;

        public RecordProcessorFactory(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
        }

        @Override
        public ShardRecordProcessor shardRecordProcessor() {
            return new RecordProcessor(bean, method);
        }
    }

    private static class RecordProcessor implements ShardRecordProcessor {
        private static final Logger log = LoggerFactory.getLogger(RecordProcessor.class);
        private final Object bean;
        private final Method method;

        public RecordProcessor(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
        }

        @Override
        public void initialize(InitializationInput initializationInput) {
            log.info("Initializing record processor for shard: {}", initializationInput.shardId());
        }

        @Override
        public void processRecords(ProcessRecordsInput processRecordsInput) {
            List<KinesisClientRecord> records = processRecordsInput.records();

            for (KinesisClientRecord record : records) {
                try {
                    // Convert record data to string (assuming JSON)
                    String data = new String(record.data().array());

                    // Invoke the listener method
                    if (method.getParameterTypes().length == 1) {
                        if (method.getParameterTypes()[0] == String.class) {
                            method.invoke(bean, data);
                        } else if (method.getParameterTypes()[0] == KinesisClientRecord.class) {
                            method.invoke(bean, record);
                        }
                    } else if (method.getParameterTypes().length == 0) {
                        method.invoke(bean);
                    }

                } catch (Exception e) {
                    log.error("Error processing record: {}", record, e);
                }
            }

            try {
                processRecordsInput.checkpointer().checkpoint();
            } catch (Exception e) {
                log.error("Failed to checkpoint", e);
            }
        }

        @Override
        public void leaseLost(LeaseLostInput leaseLostInput) {
            log.info("Lease lost for shard: {}", leaseLostInput.toString());
        }

        @Override
        public void shardEnded(ShardEndedInput shardEndedInput) {
            try {
                shardEndedInput.checkpointer().checkpoint();
                log.info("Shard ended, checkpointed: {}", shardEndedInput.toString());
            } catch (Exception e) {
                log.error("Failed to checkpoint at shard end", e);
            }
        }

        @Override
        public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {

        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Kinesis consumers...");
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
