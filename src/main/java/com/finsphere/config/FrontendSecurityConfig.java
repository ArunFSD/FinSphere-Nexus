package com.finsphere.config;

import com.finsphere.common.security.JwtAuthenticationFilter;
import com.finsphere.common.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class FrontendSecurityConfig {

    private final JwtUtils jwtUtils;

    @Bean
    @Primary
    public SecurityFilterChain frontendSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Explicitly permit these without any filter interference
                    .requestMatchers("/favicon.ico", "/static/**", "/css/**", "/js/**", "/images/**", "/error")
                    .permitAll()
                    .requestMatchers("/login", "/register").permitAll()
                    // Everything else allowed here, because DashboardController handles the role check
                    .anyRequest().permitAll()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
