package com.finsphere.common.autoconfigure;

import java.time.Clock;

import tools.jackson.databind.json.JsonMapper;

import com.finsphere.common.security.AuthCookieService;
import com.finsphere.common.security.CookieProperties;
import com.finsphere.common.security.EncryptionProperties;
import com.finsphere.common.security.EncryptionService;
import com.finsphere.common.security.JwtAuthenticationFilter;
import com.finsphere.common.security.JwtProperties;
import com.finsphere.common.security.JwtService;
import com.finsphere.common.security.RestAccessDeniedHandler;
import com.finsphere.common.security.RestAuthenticationEntryPoint;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;

/**
 * JWT cookie authentication building blocks. Each service still declares its own {@link SecurityFilterChain}
 * (its URL rules differ) and plugs in the {@link JwtAuthenticationFilter}, entry point and access-denied handler
 * provided here.
 */
@AutoConfiguration(after = FinsphereCoreAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties({ JwtProperties.class, CookieProperties.class, EncryptionProperties.class })
public class FinsphereSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(JwtProperties properties, Clock clock) {
        return new JwtService(properties, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthCookieService authCookieService(CookieProperties cookieProperties, JwtProperties jwtProperties) {
        return new AuthCookieService(cookieProperties, jwtProperties.expiration());
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, AuthCookieService cookieService) {
        return new JwtAuthenticationFilter(jwtService, cookieService);
    }

    /**
     * The JWT filter belongs inside the Spring Security chain only. Without this, Spring Boot would also register
     * it as a plain servlet filter and every request would be authenticated twice.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint(JsonMapper jsonMapper) {
        return new RestAuthenticationEntryPoint(jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAccessDeniedHandler restAccessDeniedHandler(JsonMapper jsonMapper) {
        return new RestAccessDeniedHandler(jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "finsphere.security.encryption", name = "key")
    public EncryptionService encryptionService(EncryptionProperties properties) {
        return new EncryptionService(properties);
    }
}
