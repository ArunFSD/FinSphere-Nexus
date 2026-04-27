package com.finsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"com.finsphere"})
@EnableJpaRepositories(basePackages = "com.finsphere.repository.jpa")
@EnableMongoRepositories(basePackages = "com.finsphere.repository.mongo")
public class ChitFinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitFinanceServiceApplication.class, args);
    }

}
