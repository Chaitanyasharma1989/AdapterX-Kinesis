# RecordProcessor Test Suite

This directory contains comprehensive tests for the RecordProcessor functionality, including checkpointing, multi-threading, error handling, and performance testing.

## Test Files Overview

### 1. `RecordProcessorTest.java`
**Unit tests** for core RecordProcessor functionality:
- Basic record processing
- Multithreaded processing
- Error handling and validation
- Checkpointing behavior
- Shutdown and lifecycle events
- Metrics collection
- Thread pool configuration

### 2. `CheckpointStrategyTest.java`
**Unit tests** for checkpoint strategy implementations:
- KCLv2CheckpointStrategy functionality
- NoOpCheckpointStrategy functionality
- Error handling in checkpoint strategies
- Null checkpointer handling
- Multiple checkpoint calls

### 3. `RecordProcessorIntegrationTest.java`
**Integration tests** for end-to-end scenarios:
- Complete record processing flow
- Large batch processing
- Concurrent batch processing
- Processing with errors
- Shutdown during processing
- Checkpointing under load
- Memory efficient processing
- Graceful handling of slow processing

### 4. `TestConfiguration.java`
**Test utilities and configuration**:
- Common test setup methods
- Test record creation utilities
- Wait and verification helpers
- Test record handler implementations

### 5. `TestRunner.java`
**Programmatic test runner**:
- Main method to run all tests
- Performance benchmarking
- Automated test execution
- Test result reporting

## Running the Tests

### Prerequisites
- Java 8 or higher
- Maven or Gradle build system
- JUnit 5 and Mockito dependencies

### Using Maven
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=RecordProcessorTest

# Run specific test method
mvn test -Dtest=RecordProcessorTest#testMultiThreadedProcessing

# Run with verbose output
mvn test -Dtest=RecordProcessorTest -Dsurefire.useFile=false
```

### Using Gradle
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests RecordProcessorTest

# Run specific test method
./gradlew test --tests RecordProcessorTest.testMultiThreadedProcessing
```

### Programmatic Execution
```bash
# Run the test runner directly
java -cp target/test-classes:target/classes com.csharma.kinesis.recordprocessor.TestRunner
```

## Test Categories

### 1. Unit Tests
- **Purpose**: Test individual components in isolation
- **Coverage**: Methods, classes, and small units of functionality
- **Dependencies**: Mocked external dependencies
- **Speed**: Fast execution

### 2. Integration Tests
- **Purpose**: Test complete workflows and component interactions
- **Coverage**: End-to-end scenarios and real-world usage patterns
- **Dependencies**: Some real dependencies, some mocked
- **Speed**: Slower than unit tests

### 3. Performance Tests
- **Purpose**: Verify performance characteristics and scalability
- **Coverage**: Throughput, latency, memory usage
- **Dependencies**: Minimal mocking for realistic performance
- **Speed**: Slowest test category

## Test Scenarios Covered

### Basic Functionality
- ✅ Record processing with valid data
- ✅ Empty record batch handling
- ✅ Single record processing
- ✅ Multiple record batch processing

### Multi-threading
- ✅ Configurable thread pool sizes
- ✅ Concurrent record processing
- ✅ Thread pool executor configuration
- ✅ Task distribution across threads
- ✅ Thread safety verification

### Error Handling
- ✅ Processing errors during record handling
- ✅ Validation errors with schema validation
- ✅ Graceful error recovery
- ✅ Error metrics collection
- ✅ Checkpointing despite errors

### Checkpointing
- ✅ KCL v2.x checkpoint strategy
- ✅ NoOp checkpoint strategy fallback
- ✅ Checkpoint timing and frequency
- ✅ Checkpoint error handling
- ✅ Null checkpointer handling

### Lifecycle Management
- ✅ Processor initialization
- ✅ Graceful shutdown
- ✅ Lease lost handling
- ✅ Shard ended processing
- ✅ Resource cleanup

### Performance and Scalability
- ✅ Large batch processing (1000+ records)
- ✅ Memory efficient processing
- ✅ Processing time measurement
- ✅ Throughput calculation
- ✅ Resource utilization monitoring

### Schema Validation
- ✅ Schema validation integration
- ✅ Validation error handling
- ✅ Fail-on-validation-error configuration
- ✅ Validation metrics collection

## Test Data and Utilities

### Test Record Creation
```java
// Create single test record
KinesisClientRecord record = createTestRecord("test-data", "partition-key");

// Create multiple test records
List<KinesisClientRecord> records = createTestRecords(10);

// Create records with specific pattern
List<KinesisClientRecord> records = createTestRecordsWithPattern("user-event", 5);
```

### Test Configuration
```java
// Basic configuration
RecordProcessorConfig config = createBasicConfig();

// Configuration with schema validation
RecordProcessorConfig config = createSchemaValidationConfig();
```

### Test Record Handlers
```java
// Simple handler for counting
SimpleTestRecordHandler handler = new SimpleTestRecordHandler();

// Handler with error simulation
TestRecordHandler handler = new TestRecordHandler(processedCount, errorCount);
```

## Expected Test Results

### Unit Tests
- **Execution Time**: < 5 seconds
- **Success Rate**: 100%
- **Coverage**: > 90%

### Integration Tests
- **Execution Time**: < 30 seconds
- **Success Rate**: 100%
- **Coverage**: End-to-end scenarios

### Performance Tests
- **Throughput**: > 10 records/second
- **Memory Usage**: < 100MB for 1000 records
- **Processing Time**: < 10 seconds for 200 records

## Troubleshooting

### Common Issues

1. **Test Timeout**
   - Increase timeout values in test methods
   - Check for deadlocks in multi-threaded tests
   - Verify thread pool configuration

2. **Mock Verification Failures**
   - Ensure proper mock setup
   - Check timing of mock interactions
   - Verify method call parameters

3. **Performance Test Failures**
   - Check system resources (CPU, memory)
   - Adjust performance thresholds
   - Verify thread pool sizing

4. **Checkpoint Strategy Issues**
   - Verify AWS KCL dependency version
   - Check checkpoint strategy implementation
   - Ensure proper exception handling

### Debug Mode
```bash
# Run tests with debug logging
mvn test -Dtest=RecordProcessorTest -Dlogging.level.com.csharma.kinesis=DEBUG

# Run with JVM debug options
mvn test -Dtest=RecordProcessorTest -DargLine="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

## Contributing

When adding new tests:

1. **Follow naming conventions**: `test[Feature][Scenario]`
2. **Use descriptive test names**: Clear about what is being tested
3. **Add appropriate assertions**: Verify expected behavior
4. **Include error scenarios**: Test both success and failure cases
5. **Document complex tests**: Add comments for non-obvious test logic
6. **Update this README**: Document new test scenarios

## Test Maintenance

- **Regular execution**: Run tests before each commit
- **Performance monitoring**: Track test execution times
- **Coverage analysis**: Maintain high test coverage
- **Dependency updates**: Update test dependencies with main code
- **Documentation updates**: Keep test documentation current 