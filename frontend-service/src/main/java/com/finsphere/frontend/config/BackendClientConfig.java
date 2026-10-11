package com.finsphere.frontend.config;

import java.net.http.HttpClient;

import tools.jackson.databind.json.JsonMapper;

import com.finsphere.frontend.client.BackendCallContextResolver;
import com.finsphere.frontend.client.BackendErrorDecoder;
import com.finsphere.frontend.client.BackendRequestInterceptor;

import org.springframework.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

/**
 * HTTP service clients for the back-end services. Each {@code @ImportHttpServices} group is named after the Eureka
 * service id, so Spring Cloud LoadBalancer resolves {@code http://<group>} to a live instance. Timeouts come from
 * {@code spring.http.clients.*}.
 */
@Configuration(proxyBeanMethods = false)
public class BackendClientConfig {

    /**
     * Keeps Spring Boot's JDK client (virtual-thread executor, timeouts from {@code spring.http.clients.*}) but pins
     * it to HTTP/1.1: the services speak plain HTTP/1.1, so an h2c upgrade attempt on new connections only adds a
     * round trip. {@code spring.http.clients.imperative.factory=jdk} selects this client.
     */
    @Bean
    ClientHttpRequestFactoryBuilderCustomizer<JdkClientHttpRequestFactoryBuilder> http11ClientCustomizer() {
        return (builder) -> builder
            .withHttpClientCustomizer((client) -> client.version(HttpClient.Version.HTTP_1_1));
    }

    /**
     * Runs after Spring Boot's own group configurers and before the load balancer's, so the interceptor wraps the
     * load-balanced call and can report "no instance available" as an unavailable service.
     */
    @Bean
    RestClientHttpServiceGroupConfigurer backendCallConfigurer(BackendCallContextResolver contextResolver,
            JsonMapper jsonMapper) {
        return new RestClientHttpServiceGroupConfigurer() {

            @Override
            public void configureGroups(Groups<RestClient.Builder> groups) {
                groups.forEachClient((group, builder) -> builder
                    .requestInterceptor(new BackendRequestInterceptor(group.name(), contextResolver))
                    .defaultStatusHandler(HttpStatusCode::isError, new BackendErrorDecoder(jsonMapper, group.name())));
            }

            @Override
            public int getOrder() {
                return 5;
            }
        };
    }
}
