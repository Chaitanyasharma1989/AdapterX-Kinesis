package com.csharma.listener;

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
}