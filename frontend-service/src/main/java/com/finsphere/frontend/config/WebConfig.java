package com.finsphere.frontend.config;

import java.time.Clock;

import com.finsphere.frontend.web.support.Formats;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration(proxyBeanMethods = false)
public class WebConfig {

    @Bean
    Formats formats(FrontendProperties properties, Clock clock) {
        return new Formats(properties.zone(), clock);
    }

    /**
     * Registered after the forwarded-header and correlation-id filters and before Spring Security, so the security
     * rules and the controllers only ever see the canonical address.
     */
    @Bean
    FilterRegistrationBean<UrlHandlerFilter> trailingSlashRedirect() {
        FilterRegistrationBean<UrlHandlerFilter> registration = new FilterRegistrationBean<>(trailingSlashFilter());
        registration.setName("trailingSlashRedirect");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * {@code /schemes/} and {@code /schemes} are the same page: an address with a trailing slash is redirected to
     * the address without it, query kept. 308 rather than 301, so a form sent to such an address keeps its method
     * and fields.
     */
    public static UrlHandlerFilter trailingSlashFilter() {
        return UrlHandlerFilter.trailingSlashHandler("/**").redirect(HttpStatus.PERMANENT_REDIRECT).build();
    }
}
