package com.finsphere.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.finsphere.auth.repository")
public class RedisConfig {
    // Basic settings are picked up from application.yml/properties
}