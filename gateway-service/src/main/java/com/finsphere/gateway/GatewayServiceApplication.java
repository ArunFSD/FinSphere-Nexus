package com.finsphere.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for browsers and API clients. Routes (see {@code config-repo/gateway-service.yml}):
 * {@code /auth/**} to auth-service, {@code /chits/**} and {@code /finance/**} to chit-finance-service and
 * everything else to frontend-service, all resolved through Eureka.
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
