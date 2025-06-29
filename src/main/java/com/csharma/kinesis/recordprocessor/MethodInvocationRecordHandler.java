package com.csharma.kinesis.recordprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.lang.reflect.Method;

/**
 * Record handler that invokes a method on a bean.
 * This is the default implementation that calls the annotated method.
 */
public class MethodInvocationRecordHandler implements RecordHandler {
    
    private static final Logger log = LoggerFactory.getLogger(MethodInvocationRecordHandler.class);
    
    private final Object bean;
    private final Method method;

    public MethodInvocationRecordHandler(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
    }

    @Override
    public void handleRecord(String data, KinesisClientRecord record) throws Exception {
        try {
            if (method.getParameterTypes().length == 1) {
                if (method.getParameterTypes()[0] == String.class) {
                    method.invoke(bean, data);
                } else if (method.getParameterTypes()[0] == KinesisClientRecord.class) {
                    method.invoke(bean, record);
                }
            } else if (method.getParameterTypes().length == 0) {
                method.invoke(bean);
            } else {
                log.warn("Method {} has unsupported parameter count: {}", 
                        method.getName(), method.getParameterTypes().length);
            }
        } catch (Exception e) {
            log.error("Error invoking method {} on bean: {}", method.getName(), e.getMessage(), e);
            throw e;
        }
    }
} 