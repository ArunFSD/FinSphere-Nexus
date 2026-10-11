package com.finsphere.common.autoconfigure;

import jakarta.servlet.DispatcherType;

import com.finsphere.common.exception.DataAccessExceptionHandler;
import com.finsphere.common.exception.FinsphereErrorAttributes;
import com.finsphere.common.exception.GlobalExceptionHandler;
import com.finsphere.common.web.CorrelationIdFilter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Servlet web support: correlation ids, the global REST exception handlers and consistent {@code /error} JSON.
 * Ordered before Boot's error MVC auto-configuration so our {@link ErrorAttributes} replaces the default.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FinsphereWebAutoConfiguration {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(
                new CorrelationIdFilter());
        registration.setName("correlationIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(ErrorAttributes.class)
    public FinsphereErrorAttributes finsphereErrorAttributes() {
        return new FinsphereErrorAttributes();
    }

    /** Only services with a database have spring-tx (and therefore the DAO exception hierarchy). */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.dao.DataAccessException")
    static class DataAccessExceptionHandlingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        DataAccessExceptionHandler dataAccessExceptionHandler() {
            return new DataAccessExceptionHandler();
        }
    }
}
