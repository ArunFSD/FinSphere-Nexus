package com.finsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.finsphere"})
public class ChitFinanceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChitFinanceServiceApplication.class, args);
	}

}
