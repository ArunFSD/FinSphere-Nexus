package com.finsphere.chit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Chit schemes, monthly auctions and daily-collection loans (PostgreSQL), and the payment log book and audit trail
 * (MongoDB). The two repository packages are scanned separately so each store only sees its own repositories.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaRepositories(basePackages = "com.finsphere.chit.repository.jpa")
@EnableMongoRepositories(basePackages = "com.finsphere.chit.repository.mongo")
public class ChitFinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitFinanceServiceApplication.class, args);
    }
}
