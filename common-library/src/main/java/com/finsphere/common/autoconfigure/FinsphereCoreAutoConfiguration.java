package com.finsphere.common.autoconfigure;

import java.time.Clock;
import java.util.Optional;

import tools.jackson.databind.json.JsonMapper;

import com.finsphere.common.audit.AuditTrail;
import com.finsphere.common.audit.AuditTrailAspect;
import com.finsphere.common.audit.SecurityAuditorAware;
import com.finsphere.common.logging.MethodLoggingAspect;
import com.finsphere.common.logging.MethodLoggingProperties;
import com.finsphere.common.web.MdcTaskDecorator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Clock, audit trail, auditing hooks for Spring Data and the method timing aspect.
 * <p>
 * JPA auditing ({@code @CreatedBy}, {@code @LastModifiedDate}...) is switched on automatically when Spring Data
 * JPA is on the classpath. Services must therefore <em>not</em> add {@code @EnableJpaAuditing} themselves.
 * MongoDB services can reuse the {@code auditorProvider} and {@code auditingDateTimeProvider} beans with
 * {@code @EnableMongoAuditing}.
 */
@AutoConfiguration
@EnableConfigurationProperties(MethodLoggingProperties.class)
public class FinsphereCoreAutoConfiguration {

    public static final String AUDITOR_PROVIDER = "auditorProvider";

    public static final String DATE_TIME_PROVIDER = "auditingDateTimeProvider";

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Applied by Spring Boot to its application task executor, so {@code @Async} work keeps the MDC. */
    @Bean
    @ConditionalOnMissingBean
    public MdcTaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditTrail auditTrail(JsonMapper jsonMapper, ApplicationEventPublisher publisher, Environment environment,
            Clock clock) {
        return new AuditTrail(jsonMapper, publisher, environment.getProperty("spring.application.name", "finsphere"),
                clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditTrailAspect auditTrailAspect(AuditTrail auditTrail) {
        return new AuditTrailAspect(auditTrail);
    }

    @Bean
    @ConditionalOnBooleanProperty(name = "finsphere.logging.method.enabled", matchIfMissing = true)
    public MethodLoggingAspect methodLoggingAspect(MethodLoggingProperties properties) {
        return new MethodLoggingAspect(properties);
    }

    @Bean(name = AUDITOR_PROVIDER)
    @ConditionalOnMissingBean(name = AUDITOR_PROVIDER)
    public AuditorAware<Long> auditorProvider() {
        return new SecurityAuditorAware();
    }

    @Bean(name = DATE_TIME_PROVIDER)
    @ConditionalOnMissingBean(name = DATE_TIME_PROVIDER)
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.jpa.domain.support.AuditingEntityListener")
    @ConditionalOnBooleanProperty(name = "finsphere.audit.jpa.enabled", matchIfMissing = true)
    @EnableJpaAuditing(auditorAwareRef = AUDITOR_PROVIDER, dateTimeProviderRef = DATE_TIME_PROVIDER)
    static class JpaAuditingConfiguration {

    }
}
