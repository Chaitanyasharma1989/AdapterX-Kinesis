//package com.csharma.kinesis;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
///**
// * Example showing how to use KinesisTemplate methods without method signature clashes.
// */
//public class KinesisTemplateUsageExample {
//
//    /**
//     * Example of using sendBulk with List<Object>
//     */
//    public void exampleSendBulkWithObjects() {
//        // Create KinesisTemplate (in real usage, this would be injected)
//        // KinesisTemplate template = new KinesisTemplate();
//
//        // Create a list of simple objects
//        List<Object> simpleRecords = Arrays.asList(
//            "record1",
//            "record2",
//            "record3"
//        );
//
//        // Use sendBulk method for simple objects
//        // CompletableFuture<BulkSendResult> result = template.sendBulk("my-stream", simpleRecords);
//
//        System.out.println("Using sendBulk with List<Object> - no method signature clash!");
//    }
//
//    /**
//     * Example of using sendBulkRecords with List<BulkRecord>
//     */
//    public void exampleSendBulkWithBulkRecords() {
//        // Create KinesisTemplate (in real usage, this would be injected)
//        // KinesisTemplate template = new KinesisTemplate();
//
//        // Create a list of BulkRecord objects with custom configuration
//        // List<BulkRecord> bulkRecords = Arrays.asList(
//        //     new BulkRecord("record1", "partition-key-1"),
//        //     new BulkRecord("record2", "partition-key-2"),
//        //     new BulkRecord("record3", "partition-key-3")
//        // );
//
//        // Use sendBulkRecords method for BulkRecord objects
//        // CompletableFuture<BulkSendResult> result = template.sendBulkRecords("my-stream", bulkRecords);
//
//        System.out.println("Using sendBulkRecords with List<BulkRecord> - no method signature clash!");
//    }
//
//    /**
//     * Example showing the difference between the two methods
//     */
//    public void exampleMethodDifferences() {
//        System.out.println("=== KinesisTemplate Method Usage Examples ===");
//
//        // Method 1: sendBulk(String, List<Object>)
//        // - For simple objects that don't need custom configuration
//        // - Uses default serialization and partition key strategy
//        System.out.println("1. sendBulk(String streamName, List<Object> records)");
//        System.out.println("   - Use for simple objects");
//        System.out.println("   - Default serialization and partition key strategy");
//        System.out.println("   - Example: template.sendBulk(\"stream\", Arrays.asList(\"data1\", \"data2\"))");
//
//        System.out.println();
//
//        // Method 2: sendBulkRecords(String, List<BulkRecord>)
//        // - For objects that need custom configuration
//        // - Allows custom partition keys, serialization, etc.
//        System.out.println("2. sendBulkRecords(String streamName, List<BulkRecord> records)");
//        System.out.println("   - Use for objects with custom configuration");
//        System.out.println("   - Custom partition keys, serialization, etc.");
//        System.out.println("   - Example: template.sendBulkRecords(\"stream\", bulkRecordList)");
//
//        System.out.println();
//        System.out.println("✅ No method signature clashes - both methods can coexist!");
//    }
//
//    /**
//     * Example of method resolution
//     */
//    public void exampleMethodResolution() {
//        System.out.println("=== Method Resolution Examples ===");
//
//        // These would resolve to different methods:
//
//        // 1. List<Object> -> sendBulk method
//        List<Object> objectList = Arrays.asList("data1", "data2");
//        System.out.println("List<Object> resolves to: sendBulk(String, List<Object>)");
//
//        // 2. List<BulkRecord> -> sendBulkRecords method
//        // List<BulkRecord> bulkRecordList = Arrays.asList(
//        //     new BulkRecord("data1", "key1"),
//        //     new BulkRecord("data2", "key2")
//        // );
//        System.out.println("List<BulkRecord> resolves to: sendBulkRecords(String, List<BulkRecord>)");
//
//        System.out.println("✅ No ambiguity - each type resolves to the correct method!");
//    }
//
//    public static void main(String[] args) {
//        KinesisTemplateUsageExample example = new KinesisTemplateUsageExample();
//        example.exampleMethodDifferences();
//        System.out.println();
//        example.exampleMethodResolution();
//    }
//}