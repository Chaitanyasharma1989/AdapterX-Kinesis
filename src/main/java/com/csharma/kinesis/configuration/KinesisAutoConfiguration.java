package com.csharma.kinesis.configuration;

import com.csharma.kinesis.manager.KinesisConsumerManager;
import com.csharma.kinesis.kinesistemplate.KinesisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KinesisProperties.class)
@ConditionalOnProperty(prefix = "kinesis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KinesisAutoConfiguration {

    @Bean
    public KinesisTemplate kinesisTemplate() {
        return new KinesisTemplate();
    }

    @Bean
    public KinesisConsumerManager kinesisConsumerManager() {
        return new KinesisConsumerManager();
    }
}