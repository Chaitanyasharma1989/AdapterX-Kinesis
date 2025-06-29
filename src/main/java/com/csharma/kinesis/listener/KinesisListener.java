package com.csharma.kinesis.listener;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KinesisListener {
    String streamName();
    String applicationName() default "";
    String consumerGroup() default "";
    int maxRecords() default 1000;
    long idleTimeBetweenReadsInMillis() default 1000L;
    boolean enhancedFanOut() default false;
    String consumerName() default "";
    
    // Cross-account configuration
    String targetAccountId() default "";
    String roleArn() default "";
    String externalId() default "";
    String sessionName() default "kinesis-cross-account-session";
    
    // Schema validation configuration
    boolean validateSchema() default false;
    String schemaRegistryName() default "";
    String schemaName() default "";
    String schemaVersion() default "";
    String dataFormat() default "JSON";
    boolean failOnValidationError() default true;
}