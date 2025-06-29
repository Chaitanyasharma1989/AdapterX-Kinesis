# Schema Validation with AWS Glue Schema Registry

## Overview

The Kinesis consumer now supports schema validation using AWS Glue Schema Registry, ensuring data integrity and consistency across your streaming applications. This feature validates incoming records against predefined schemas before processing, preventing data quality issues downstream.

## Key Features

### 1. Multi-Format Schema Support
- **JSON Schema**: Validate JSON data against JSON Schema specifications
- **Avro Schema**: Validate Avro serialized data
- **Protobuf Schema**: Validate Protocol Buffer data (placeholder implementation)
- **Extensible**: Easy to add support for additional formats

### 2. Schema Registry Integration
- **AWS Glue Schema Registry**: Native integration with AWS Glue Schema Registry
- **Schema Caching**: Efficient caching to minimize API calls
- **Version Management**: Support for schema versioning and compatibility
- **Cross-Account**: Schema validation across multiple AWS accounts

### 3. Flexible Configuration
- **Per-Listener Configuration**: Configure schema validation per `@KinesisListener`
- **Global Defaults**: Set default schema settings in configuration
- **Error Handling**: Configurable error handling for validation failures
- **Performance**: Optimized validation with caching and parallel processing

### 4. Integration with Existing Features
- **Multi-Threading**: Schema validation works with parallel record processing
- **Cross-Account**: Schema validation across multiple AWS accounts
- **Enhanced Fan-Out**: Schema validation with Enhanced Fan-Out consumers
- **Monitoring**: Rich metrics for validation success/failure rates

## Configuration

### Global Schema Registry Configuration

```yaml
kinesis:
  schemaRegistry:
    enabled: true
    registryName: "kinesis-schema-registry"
    schemaName: "kinesis-schema"
    schemaVersion: "1.0"
    dataFormat: "JSON"  # JSON, AVRO, PROTOBUF
    validateOnConsume: true
    validateOnProduce: false
    cacheSchemas: true
    cacheSize: 1000
    cacheExpirationMinutes: 60
    failOnValidationError: true
    logValidationErrors: true
    compatibilityMode: "BACKWARD"  # BACKWARD, FORWARD, FULL, NONE
    description: "Kinesis stream schema for data validation"
```

### Configuration Options

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `enabled` | Enable schema validation globally | `false` | No |
| `registryName` | Default schema registry name | `"kinesis-schema-registry"` | No |
| `schemaName` | Default schema name | `"kinesis-schema"` | No |
| `schemaVersion` | Default schema version | `"1.0"` | No |
| `dataFormat` | Default data format | `"JSON"` | No |
| `validateOnConsume` | Validate records on consumption | `true` | No |
| `validateOnProduce` | Validate records on production | `false` | No |
| `cacheSchemas` | Cache schemas for performance | `true` | No |
| `cacheSize` | Maximum number of cached schemas | `1000` | No |
| `cacheExpirationMinutes` | Schema cache expiration time | `60` | No |
| `failOnValidationError` | Fail processing on validation errors | `true` | No |
| `logValidationErrors` | Log validation error details | `true` | No |
| `compatibilityMode` | Schema compatibility mode | `"BACKWARD"` | No |
| `description` | Schema description | `"Kinesis stream schema for data validation"` | No |

## Usage Examples

### 1. Basic Schema Validation

```java
@KinesisListener(
    streamName = "user-events-stream",
    validateSchema = true
)
public void processUserEvent(String data) {
    // Process validated user event data
    System.out.println("Processing validated: " + data);
}
```

### 2. Custom Schema Configuration

```java
@KinesisListener(
    streamName = "order-events-stream",
    validateSchema = true,
    schemaRegistryName = "order-schema-registry",
    schemaName = "order-schema",
    schemaVersion = "2.0",
    dataFormat = "JSON",
    failOnValidationError = true
)
public void processOrderEvent(String data) {
    // Process order event with custom schema validation
}
```

### 3. Cross-Account Schema Validation

```java
@KinesisListener(
    streamName = "cross-account-events-stream",
    targetAccountId = "123456789012",
    validateSchema = true,
    schemaRegistryName = "cross-account-schema-registry",
    schemaName = "event-schema"
)
public void processCrossAccountEvent(String data) {
    // Process cross-account event with schema validation
}
```

### 4. Avro Schema Validation

```java
@KinesisListener(
    streamName = "avro-events-stream",
    validateSchema = true,
    schemaName = "avro-user-event",
    dataFormat = "AVRO",
    failOnValidationError = false
)
public void processAvroEvent(String data) {
    // Process Avro event with schema validation
}
```

### 5. Schema Validation with Enhanced Fan-Out

```java
@KinesisListener(
    streamName = "high-throughput-validated-stream",
    enhancedFanOut = true,
    consumerName = "schema-validated-efo-consumer",
    validateSchema = true,
    schemaName = "high-throughput-schema"
)
public void processHighThroughputValidatedEvent(String data) {
    // Process event with both EFO and schema validation
}
```

## Schema Definition Examples

### JSON Schema Example

```json
{
  "type": "object",
  "properties": {
    "userId": {
      "type": "string",
      "pattern": "^[a-zA-Z0-9]{8,}$"
    },
    "eventType": {
      "type": "string",
      "enum": ["user_created", "user_updated", "user_deleted"]
    },
    "timestamp": {
      "type": "string",
      "format": "date-time"
    },
    "data": {
      "type": "object",
      "properties": {
        "email": {
          "type": "string",
          "format": "email"
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "maxLength": 100
        }
      },
      "required": ["email"]
    }
  },
  "required": ["userId", "eventType", "timestamp"],
  "additionalProperties": false
}
```

### Avro Schema Example

```json
{
  "type": "record",
  "name": "UserEvent",
  "namespace": "com.example.events",
  "fields": [
    {
      "name": "userId",
      "type": "string"
    },
    {
      "name": "eventType",
      "type": {
        "type": "enum",
        "name": "EventType",
        "symbols": ["user_created", "user_updated", "user_deleted"]
      }
    },
    {
      "name": "timestamp",
      "type": "long"
    },
    {
      "name": "data",
      "type": [
        "null",
        {
          "type": "record",
          "name": "UserData",
          "fields": [
            {"name": "email", "type": "string"},
            {"name": "name", "type": ["null", "string"]}
          ]
        }
      ],
      "default": null
    }
  ]
}
```

## AWS Glue Schema Registry Setup

### 1. Create Schema Registry

```bash
# Create schema registry
aws schemas create-registry \
  --registry-name "kinesis-schema-registry" \
  --description "Schema registry for Kinesis streams"
```

### 2. Create Schema

```bash
# Create JSON schema
aws schemas create-schema \
  --registry-name "kinesis-schema-registry" \
  --schema-name "user-event-schema" \
  --data-format "JSON" \
  --compatibility "BACKWARD" \
  --content '{"type":"object","properties":{"userId":{"type":"string"},"eventType":{"type":"string"},"timestamp":{"type":"string"}},"required":["userId","eventType","timestamp"]}'
```

### 3. IAM Permissions

The application needs the following IAM permissions for schema registry access:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "schemas:DescribeRegistry",
                "schemas:DescribeSchema",
                "schemas:ListSchemas",
                "schemas:ListSchemaVersions"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "schemas:CreateSchema",
                "schemas:UpdateSchema"
            ],
            "Resource": "arn:aws:schemas:*:*:registry/*"
        }
    ]
}
```

## Monitoring and Metrics

### Validation Metrics

```java
@Autowired
private RecordProcessor processor;

// Get validation metrics
long processedRecords = processor.getProcessedRecords();
long failedRecords = processor.getFailedRecords();
long validationFailedRecords = processor.getValidationFailedRecords();
boolean isSchemaValidationEnabled = processor.isSchemaValidationEnabled();
```

### Logging Configuration

```yaml
logging:
  level:
    com.csharma.kinesis.service.SchemaRegistryService: DEBUG
    com.csharma.kinesis.recordprocessor: DEBUG
```

### CloudWatch Metrics

The schema validation service automatically publishes metrics to CloudWatch:
- `SchemaValidationSuccess` - Number of successful validations
- `SchemaValidationFailure` - Number of validation failures
- `SchemaCacheHit` - Number of schema cache hits
- `SchemaCacheMiss` - Number of schema cache misses

## Performance Considerations

### Schema Caching
- **Cache Size**: Adjust `cacheSize` based on number of schemas
- **Cache Expiration**: Set appropriate `cacheExpirationMinutes`
- **Memory Usage**: Monitor memory usage with large schemas

### Validation Performance
- **JSON Validation**: Fastest validation method
- **Avro Validation**: Moderate performance impact
- **Protobuf Validation**: Highest performance impact (when implemented)

### Thread Pool Sizing
- **CPU-Bound**: Use `Runtime.getRuntime().availableProcessors()`
- **I/O-Bound**: Increase thread pool size for schema registry calls
- **Mixed Workloads**: Start with CPU count and adjust based on monitoring

## Error Handling

### Validation Error Strategies

#### 1. Fail Fast (Default)
```java
@KinesisListener(
    streamName = "strict-validation-stream",
    validateSchema = true,
    failOnValidationError = true
)
public void processStrictValidation(String data) {
    // Processing stops on validation failure
}
```

#### 2. Graceful Degradation
```java
@KinesisListener(
    streamName = "graceful-validation-stream",
    validateSchema = true,
    failOnValidationError = false
)
public void processGracefulValidation(String data) {
    // Processing continues, validation errors are logged
}
```

#### 3. Custom Error Handling
```java
@KinesisListener(
    streamName = "custom-error-handling-stream",
    validateSchema = true,
    failOnValidationError = false
)
public void processWithCustomErrorHandling(String data) {
    try {
        // Process validated data
    } catch (SchemaRegistryService.SchemaValidationException e) {
        // Handle validation errors
        log.error("Schema validation failed: {}", e.getMessage());
        // Send to dead letter queue or retry
    }
}
```

## Migration Guide

### From No Schema Validation to Schema Validation

1. **Create Schemas**: Define schemas in AWS Glue Schema Registry
2. **Enable Validation**: Set `kinesis.schemaRegistry.enabled=true`
3. **Update Listeners**: Add `validateSchema=true` to existing listeners
4. **Test Gradually**: Start with non-critical streams
5. **Monitor**: Watch validation metrics and adjust configuration

### Schema Evolution

1. **Backward Compatibility**: Use `BACKWARD` compatibility mode
2. **Version Management**: Increment schema versions carefully
3. **Testing**: Test schema changes in development environment
4. **Rollback Plan**: Keep previous schema versions available

## Troubleshooting

### Common Issues

#### Schema Not Found
```
Error: Schema not found in registry
```
**Solutions:**
- Verify schema registry name and schema name
- Check IAM permissions for schema registry access
- Ensure schema exists in the specified registry

#### Validation Failures
```
Error: Schema validation failed
```
**Solutions:**
- Check data format matches schema definition
- Verify required fields are present
- Review schema compatibility settings

#### Performance Issues
```
Error: Slow validation performance
```
**Solutions:**
- Increase schema cache size
- Optimize schema definitions
- Consider using simpler data formats

### Debugging

Enable debug logging for detailed validation information:

```yaml
logging:
  level:
    com.csharma.kinesis.service.SchemaRegistryService: DEBUG
```

## Best Practices

### 1. Schema Design
- **Keep Schemas Simple**: Avoid overly complex nested structures
- **Use Appropriate Types**: Choose the right data types for your use case
- **Document Schemas**: Add descriptions and examples to schemas
- **Version Strategically**: Plan schema versioning strategy

### 2. Performance Optimization
- **Cache Schemas**: Enable schema caching for better performance
- **Optimize Validation**: Use efficient validation strategies
- **Monitor Metrics**: Track validation performance metrics
- **Scale Appropriately**: Adjust thread pool size based on workload

### 3. Error Handling
- **Graceful Degradation**: Handle validation errors gracefully
- **Dead Letter Queues**: Send invalid records to DLQ for analysis
- **Retry Logic**: Implement retry mechanisms for transient failures
- **Monitoring**: Set up alerts for validation failure rates

### 4. Security
- **IAM Permissions**: Use least privilege principle for schema registry access
- **Cross-Account**: Secure cross-account schema access
- **Audit Logging**: Enable CloudTrail for schema registry operations
- **Schema Validation**: Validate schemas before deployment

## Example Complete Setup

### 1. Application Configuration

```yaml
# application.yml
kinesis:
  region: us-east-1
  schemaRegistry:
    enabled: true
    registryName: "my-schema-registry"
    schemaName: "user-event-schema"
    dataFormat: "JSON"
    failOnValidationError: true
  consumer:
    threadPool:
      size: 8
```

### 2. Java Code

```java
@Component
public class SchemaValidatedProcessor {
    
    @KinesisListener(
        streamName = "user-events",
        validateSchema = true,
        schemaName = "user-event-schema"
    )
    public void processUserEvent(String data) {
        // Process validated user event
        System.out.println("Validated event: " + data);
    }
}
```

### 3. AWS Setup

```bash
# Create schema registry
aws schemas create-registry --registry-name "my-schema-registry"

# Create schema
aws schemas create-schema \
  --registry-name "my-schema-registry" \
  --schema-name "user-event-schema" \
  --data-format "JSON" \
  --content '{"type":"object","properties":{"userId":{"type":"string"},"eventType":{"type":"string"}},"required":["userId","eventType"]}'
```

This schema validation functionality provides robust data quality assurance for your Kinesis streams while maintaining high performance and flexibility. 