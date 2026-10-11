package com.finsphere.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Serves the shared and per-service configuration in {@code config-repo/}.
 * <p>
 * Backends: {@code native} (default, reads the folder from disk) or {@code git} (set
 * {@code CONFIG_SERVER_BACKEND=git} and {@code CONFIG_GIT_URI}).
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
