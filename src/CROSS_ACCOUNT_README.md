# Cross-Account Kinesis Consumer Support

## Overview

The Kinesis consumer now supports cross-account functionality, allowing you to consume data from Kinesis streams in different AWS accounts. This feature uses AWS STS (Security Token Service) for role assumption and provides secure, temporary credentials for cross-account access.

## Key Features

### 1. Cross-Account Role Assumption
- **STS Integration**: Uses AWS STS to assume roles in target accounts
- **Temporary Credentials**: Generates secure, temporary credentials with configurable session duration
- **Credential Caching**: Caches credentials to minimize STS API calls
- **Automatic Refresh**: Handles credential expiration and renewal

### 2. Flexible Configuration
- **Per-Listener Configuration**: Configure cross-account settings per `@KinesisListener`
- **Global Defaults**: Set default cross-account settings in configuration
- **Fallback Support**: Graceful fallback to same-account access when cross-account is not configured

### 3. Security Features
- **External ID Support**: Optional external ID for additional security
- **Session Naming**: Configurable session names for audit trails
- **Credential Isolation**: Separate credentials per cross-account configuration

## Configuration

### Global Cross-Account Configuration

```yaml
kinesis:
  crossAccount:
    enabled: true
    defaultTargetAccountId: "123456789012"
    defaultRoleArn: "arn:aws:iam::123456789012:role/KinesisCrossAccountRole"
    defaultExternalId: "unique-external-id-for-security"
    defaultSessionName: "kinesis-cross-account-session"
    sessionDurationSeconds: 3600  # 1 hour
    useDefaultCredentials: true
    profileName: ""  # Optional AWS profile name
```

### Configuration Options

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `enabled` | Enable cross-account functionality | `false` | No |
| `defaultTargetAccountId` | Default target AWS account ID | `""` | No |
| `defaultRoleArn` | Default role ARN to assume | `""` | No |
| `defaultExternalId` | Default external ID for security | `""` | No |
| `defaultSessionName` | Default session name | `"kinesis-cross-account-session"` | No |
| `sessionDurationSeconds` | Session duration in seconds | `3600` | No |
| `useDefaultCredentials` | Use default AWS credentials | `true` | No |
| `profileName` | AWS profile name | `""` | No |

## Usage Examples

### 1. Same-Account Consumer (Default)

```java
@KinesisListener(streamName = "my-stream")
public void processRecord(String data) {
    // Process record from same account
    System.out.println("Processing: " + data);
}
```

### 2. Cross-Account Consumer with Global Defaults

```java
@KinesisListener(
    streamName = "cross-account-stream",
    targetAccountId = "123456789012"  // Uses global defaults for role ARN, external ID, etc.
)
public void processCrossAccountRecord(String data) {
    // Process record from cross-account stream
    System.out.println("Processing cross-account: " + data);
}
```

### 3. Cross-Account Consumer with Custom Configuration

```java
@KinesisListener(
    streamName = "custom-cross-account-stream",
    targetAccountId = "987654321098",
    roleArn = "arn:aws:iam::987654321098:role/CustomKinesisRole",
    externalId = "my-custom-external-id",
    sessionName = "custom-session"
)
public void processCustomCrossAccountRecord(String data) {
    // Process record with custom cross-account configuration
    System.out.println("Processing custom cross-account: " + data);
}
```

### 4. Mixed Same-Account and Cross-Account Consumers

```java
@Component
public class MixedAccountProcessor {
    
    // Same-account consumer
    @KinesisListener(streamName = "local-stream")
    public void processLocalRecord(String data) {
        System.out.println("Local: " + data);
    }
    
    // Cross-account consumer
    @KinesisListener(
        streamName = "remote-stream",
        targetAccountId = "123456789012"
    )
    public void processRemoteRecord(String data) {
        System.out.println("Remote: " + data);
    }
}
```

## AWS IAM Setup

### 1. Source Account (Consumer Account)

Create an IAM role or user with permissions to assume the target account role:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "sts:AssumeRole",
            "Resource": "arn:aws:iam::TARGET_ACCOUNT_ID:role/KinesisCrossAccountRole"
        }
    ]
}
```

### 2. Target Account (Stream Account)

Create an IAM role that the source account can assume:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::SOURCE_ACCOUNT_ID:root"
            },
            "Action": "sts:AssumeRole",
            "Condition": {
                "StringEquals": {
                    "sts:ExternalId": "unique-external-id-for-security"
                }
            }
        }
    ]
}
```

### 3. Target Account Role Permissions

The assumed role needs permissions to access Kinesis streams:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "kinesis:DescribeStream",
                "kinesis:GetRecords",
                "kinesis:GetShardIterator",
                "kinesis:ListShards",
                "kinesis:SubscribeToShard",
                "kinesis:DescribeStreamConsumer",
                "kinesis:RegisterStreamConsumer",
                "kinesis:DeregisterStreamConsumer"
            ],
            "Resource": [
                "arn:aws:kinesis:REGION:TARGET_ACCOUNT_ID:stream/STREAM_NAME",
                "arn:aws:kinesis:REGION:TARGET_ACCOUNT_ID:stream/STREAM_NAME/consumer/CONSUMER_NAME"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:*"
            ],
            "Resource": "arn:aws:dynamodb:REGION:TARGET_ACCOUNT_ID:table/APPLICATION_NAME"
        },
        {
            "Effect": "Allow",
            "Action": [
                "cloudwatch:PutMetricData"
            ],
            "Resource": "*"
        }
    ]
}
```

## Security Best Practices

### 1. External ID Usage
Always use an external ID when assuming cross-account roles:

```java
@KinesisListener(
    streamName = "secure-stream",
    targetAccountId = "123456789012",
    externalId = "unique-external-id-for-security"
)
public void processSecureRecord(String data) {
    // Process record with external ID security
}
```

### 2. Minimal Permissions
Follow the principle of least privilege:
- Grant only necessary Kinesis permissions
- Use specific resource ARNs instead of wildcards
- Regularly review and audit permissions

### 3. Session Duration
Choose appropriate session durations:
- **Short sessions** (1-4 hours): Higher security, more frequent renewals
- **Long sessions** (8-12 hours): Better performance, less frequent renewals

### 4. Credential Management
- Use IAM roles instead of access keys when possible
- Rotate credentials regularly
- Monitor credential usage with CloudTrail

## Monitoring and Troubleshooting

### 1. Logging

Enable debug logging for cross-account operations:

```yaml
logging:
  level:
    com.csharma.kinesis.service.CrossAccountCredentialsService: DEBUG
    com.csharma.kinesis.manager: INFO
```

### 2. Common Issues

#### Role Assumption Failures
```
Error: Failed to assume cross-account role
```
**Solutions:**
- Verify role ARN is correct
- Check external ID matches
- Ensure source account has assume role permissions
- Verify target account role trust policy

#### Credential Expiration
```
Error: Credentials expired
```
**Solutions:**
- Increase session duration
- Check credential cache cleanup
- Monitor STS API limits

#### Stream Access Denied
```
Error: Access denied to Kinesis stream
```
**Solutions:**
- Verify assumed role has Kinesis permissions
- Check stream ARN in permissions
- Ensure DynamoDB table permissions for checkpointing

### 3. Performance Monitoring

Monitor cross-account performance:

```java
// Access credential service for monitoring
@Autowired
private CrossAccountCredentialsService credentialsService;

// Monitor credential cache
// (Internal metrics available in the service)
```

## Migration Guide

### From Same-Account to Cross-Account

1. **Enable Cross-Account**: Set `kinesis.crossAccount.enabled=true`
2. **Configure Defaults**: Set default target account and role ARN
3. **Update Listeners**: Add `targetAccountId` to existing listeners
4. **Test Gradually**: Start with one stream, then expand
5. **Monitor**: Watch logs and metrics for issues

### Backward Compatibility

- Existing same-account consumers continue to work unchanged
- Cross-account is opt-in per listener
- Graceful fallback to same-account when cross-account is not configured

## Example Complete Setup

### 1. Application Configuration

```yaml
# application.yml
kinesis:
  region: us-east-1
  crossAccount:
    enabled: true
    defaultTargetAccountId: "123456789012"
    defaultRoleArn: "arn:aws:iam::123456789012:role/KinesisCrossAccountRole"
    defaultExternalId: "my-app-external-id"
    sessionDurationSeconds: 3600
  consumer:
    threadPool:
      size: 8
```

### 2. Java Code

```java
@Component
public class CrossAccountProcessor {
    
    @KinesisListener(
        streamName = "production-stream",
        targetAccountId = "123456789012",
        enhancedFanOut = true
    )
    public void processProductionData(String data) {
        // Process production data from cross-account
        System.out.println("Production: " + data);
    }
    
    @KinesisListener(
        streamName = "development-stream",
        targetAccountId = "987654321098",
        roleArn = "arn:aws:iam::987654321098:role/DevKinesisRole",
        externalId = "dev-external-id"
    )
    public void processDevelopmentData(String data) {
        // Process development data with custom configuration
        System.out.println("Development: " + data);
    }
}
```

### 3. AWS Setup

```bash
# Create role in target account
aws iam create-role \
  --role-name KinesisCrossAccountRole \
  --assume-role-policy-document trust-policy.json

# Attach permissions
aws iam attach-role-policy \
  --role-name KinesisCrossAccountRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonKinesisFullAccess
```

This cross-account functionality provides secure, scalable access to Kinesis streams across multiple AWS accounts while maintaining the same simple programming model. 