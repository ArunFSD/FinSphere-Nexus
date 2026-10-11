package com.finsphere.frontend;

import com.finsphere.frontend.client.AuthClient;
import com.finsphere.frontend.client.ChitClient;
import com.finsphere.frontend.client.FinanceClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Server-rendered web front end. Pages are rendered with Thymeleaf from data fetched from the back-end services
 * (found through Eureka, one HTTP service group per service), and every change is a plain HTML form post, so the
 * site works on any phone, tablet or desktop browser, with or without JavaScript.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@ImportHttpServices(group = "auth-service", types = AuthClient.class)
@ImportHttpServices(group = "chit-finance-service", types = { ChitClient.class, FinanceClient.class })
public class FrontendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendServiceApplication.class, args);
    }
}
