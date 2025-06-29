package com.csharma.kinesis.service;

import com.csharma.kinesis.prpoerties.KinesisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class CrossAccountCredentialsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CrossAccountCredentialsService.class);
    
    @Autowired
    private KinesisProperties kinesisProperties;
    
    private final ConcurrentHashMap<String, CachedCredentials> credentialsCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private StsClient stsClient;
    
    public CrossAccountCredentialsService() {
        // Initialize STS client
        this.stsClient = StsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        // Schedule cache cleanup
        scheduler.scheduleAtFixedRate(this::cleanupExpiredCredentials, 5, 5, TimeUnit.MINUTES);
    }
    
    public AwsCredentialsProvider getCredentialsForAccount(String targetAccountId, String roleArn, 
                                                         String externalId, String sessionName) {
        if (!kinesisProperties.getCrossAccount().isEnabled()) {
            logger.debug("Cross-account access is disabled");
            return DefaultCredentialsProvider.create();
        }
        
        String cacheKey = generateCacheKey(targetAccountId, roleArn, externalId, sessionName);
        CachedCredentials cached = credentialsCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            logger.debug("Using cached credentials for account: {}", targetAccountId);
            return cached.getCredentialsProvider();
        }
        
        logger.info("Assuming role for cross-account access: {} -> {}", 
                   kinesisProperties.getCrossAccount().getDefaultTargetAccountId(), targetAccountId);
        
        try {
            AwsCredentialsProvider credentialsProvider = assumeRole(targetAccountId, roleArn, externalId, sessionName);
            
            // Cache the credentials
            long expirationTime = System.currentTimeMillis() + 
                    (kinesisProperties.getCrossAccount().getSessionDurationSeconds() * 1000L);
            CachedCredentials newCached = new CachedCredentials(credentialsProvider, expirationTime);
            credentialsCache.put(cacheKey, newCached);
            
            return credentialsProvider;
            
        } catch (Exception e) {
            logger.error("Failed to assume role for account: {}", targetAccountId, e);
            throw new RuntimeException("Failed to assume cross-account role", e);
        }
    }
    
    private AwsCredentialsProvider assumeRole(String targetAccountId, String roleArn, 
                                            String externalId, String sessionName) {
        try {
            AssumeRoleRequest.Builder requestBuilder = AssumeRoleRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName(sessionName)
                    .durationSeconds(kinesisProperties.getCrossAccount().getSessionDurationSeconds());
            
            if (externalId != null && !externalId.isEmpty()) {
                requestBuilder.externalId(externalId);
            }
            
            AssumeRoleRequest request = requestBuilder.build();
            AssumeRoleResponse response = stsClient.assumeRole(request);
            Credentials credentials = response.credentials();
            
            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(request)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Failed to assume role: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assume role", e);
        }
    }
    
    public AwsCredentialsProvider getDefaultCredentials() {
        if (kinesisProperties.getCrossAccount().isUseDefaultCredentials()) {
            return DefaultCredentialsProvider.create();
        } else if (!kinesisProperties.getCrossAccount().getProfileName().isEmpty()) {
            return ProfileCredentialsProvider.create(kinesisProperties.getCrossAccount().getProfileName());
        } else {
            return DefaultCredentialsProvider.create();
        }
    }
    
    private String generateCacheKey(String targetAccountId, String roleArn, String externalId, String sessionName) {
        return String.format("%s:%s:%s:%s", targetAccountId, roleArn, externalId, sessionName);
    }
    
    private void cleanupExpiredCredentials() {
        long currentTime = System.currentTimeMillis();
        credentialsCache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
        logger.debug("Cleaned up expired credentials from cache");
    }
    
    public void shutdown() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
        credentialsCache.clear();
    }
    
    private static class CachedCredentials {
        private final AwsCredentialsProvider credentialsProvider;
        private final long expirationTime;
        
        public CachedCredentials(AwsCredentialsProvider credentialsProvider, long expirationTime) {
            this.credentialsProvider = credentialsProvider;
            this.expirationTime = expirationTime;
        }

        public AwsCredentialsProvider getCredentialsProvider() {
            return credentialsProvider;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        public boolean isExpired(long currentTime) {
            return currentTime >= expirationTime;
        }
    }
} 