package com.finsphere.chit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async} (audit trail persistence) on Spring Boot's virtual-thread task executor.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class ExecutionConfig {

}
