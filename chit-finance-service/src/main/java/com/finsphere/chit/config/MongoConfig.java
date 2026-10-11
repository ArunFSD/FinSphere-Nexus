package com.finsphere.chit.config;

import com.finsphere.common.autoconfigure.FinsphereCoreAutoConfiguration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Fills {@code @CreatedBy} / {@code @CreatedDate} on MongoDB documents with the same auditor and clock as JPA
 * (JPA auditing itself is enabled by the common library).
 */
@Configuration(proxyBeanMethods = false)
@EnableMongoAuditing(auditorAwareRef = FinsphereCoreAutoConfiguration.AUDITOR_PROVIDER,
        dateTimeProviderRef = FinsphereCoreAutoConfiguration.DATE_TIME_PROVIDER)
public class MongoConfig {

}
