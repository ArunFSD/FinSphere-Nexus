package com.finsphere.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Async} (login audit persistence) and {@code @Scheduled} (outbox relay).
 * <p>
 * With {@code spring.threads.virtual.enabled=true} Spring Boot runs both on virtual threads, and the shared MDC
 * task decorator keeps correlation ids on asynchronous log lines.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
public class ExecutionConfig {

}
