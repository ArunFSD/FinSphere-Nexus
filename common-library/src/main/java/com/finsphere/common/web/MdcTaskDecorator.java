package com.finsphere.common.web;

import java.util.Map;

import org.slf4j.MDC;

import org.springframework.core.task.TaskDecorator;

/**
 * Copies the caller's MDC (correlation id, user id) onto worker threads so asynchronous work stays traceable.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) {
                MDC.setContextMap(context);
            }
            else {
                MDC.clear();
            }
            try {
                runnable.run();
            }
            finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                }
                else {
                    MDC.clear();
                }
            }
        };
    }
}
