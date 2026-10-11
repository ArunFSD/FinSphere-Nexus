package com.finsphere.configserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The config server hands out database passwords and signing secrets, so every request except health and info
 * needs HTTP basic credentials ({@code CONFIG_SERVER_USERNAME} / {@code CONFIG_SERVER_PASSWORD}).
 */
@Configuration(proxyBeanMethods = false)
public class ConfigServerSecurityConfig {

    @Bean
    SecurityFilterChain configServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests((auth) -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus", "/error")
                .permitAll()
                .anyRequest()
                .authenticated())
            .httpBasic(Customizer.withDefaults())
            .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
