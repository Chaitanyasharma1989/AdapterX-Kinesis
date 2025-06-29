//package com.csharma.kinesis;
//
//import org.junit.jupiter.api.Test;
//import java.lang.reflect.Method;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Test to verify KinesisTemplate method signatures are correct.
// */
//public class KinesisTemplateMethodTest {
//
//    @Test
//    void testMethodSignatures() {
//        // Get all methods from KinesisTemplate
//        Method[] methods = KinesisTemplate.class.getDeclaredMethods();
//
//        // Check for sendBulk methods
//        boolean hasSendBulk = false;
//        boolean hasSendBulkRecords = false;
//
//        for (Method method : methods) {
//            if (method.getName().equals("sendBulk")) {
//                hasSendBulk = true;
//                // Verify it takes List<Object>
//                assertEquals(2, method.getParameterCount());
//                assertEquals(String.class, method.getParameterTypes()[0]);
//                assertEquals(List.class, method.getParameterTypes()[1]);
//                assertEquals(CompletableFuture.class, method.getReturnType());
//            }
//
//            if (method.getName().equals("sendBulkRecords")) {
//                hasSendBulkRecords = true;
//                // Verify it takes List<BulkRecord>
//                assertEquals(2, method.getParameterCount());
//                assertEquals(String.class, method.getParameterTypes()[0]);
//                assertEquals(List.class, method.getParameterTypes()[1]);
//                assertEquals(CompletableFuture.class, method.getReturnType());
//            }
//        }
//
//        // Verify both methods exist
//        assertTrue(hasSendBulk, "sendBulk method should exist");
//        assertTrue(hasSendBulkRecords, "sendBulkRecords method should exist");
//    }
//
//    @Test
//    void testNoMethodClashes() {
//        // This test verifies that there are no method signature clashes
//        Method[] methods = KinesisTemplate.class.getDeclaredMethods();
//
//        // Check for any duplicate method signatures
//        for (int i = 0; i < methods.length; i++) {
//            for (int j = i + 1; j < methods.length; j++) {
//                Method method1 = methods[i];
//                Method method2 = methods[j];
//
//                // Check if methods have the same name and parameter count
//                if (method1.getName().equals(method2.getName()) &&
//                    method1.getParameterCount() == method2.getParameterCount()) {
//
//                    // Check parameter types
//                    boolean sameSignature = true;
//                    for (int k = 0; k < method1.getParameterCount(); k++) {
//                        if (!method1.getParameterTypes()[k].equals(method2.getParameterTypes()[k])) {
//                            sameSignature = false;
//                            break;
//                        }
//                    }
//
//                    // If same signature, it should be the same method
//                    if (sameSignature) {
//                        assertEquals(method1, method2,
//                            "Duplicate method signature found: " + method1.getName());
//                    }
//                }
//            }
//        }
//    }
//
//    @Test
//    void testMethodNamesAreDifferent() {
//        // Verify that sendBulk and sendBulkRecords are different method names
//        Method[] methods = KinesisTemplate.class.getDeclaredMethods();
//
//        String sendBulkMethod = null;
//        String sendBulkRecordsMethod = null;
//
//        for (Method method : methods) {
//            if (method.getName().equals("sendBulk")) {
//                sendBulkMethod = method.getName();
//            }
//            if (method.getName().equals("sendBulkRecords")) {
//                sendBulkRecordsMethod = method.getName();
//            }
//        }
//
//        assertNotNull(sendBulkMethod, "sendBulk method should exist");
//        assertNotNull(sendBulkRecordsMethod, "sendBulkRecords method should exist");
//        assertNotEquals(sendBulkMethod, sendBulkRecordsMethod,
//            "Method names should be different to avoid erasure conflicts");
//    }
//}