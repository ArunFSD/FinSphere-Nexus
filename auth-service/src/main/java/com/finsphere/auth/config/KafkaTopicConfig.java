package com.finsphere.auth.config;

import org.apache.kafka.clients.admin.NewTopic;

import com.finsphere.common.event.KafkaTopicProperties;
import com.finsphere.common.event.KafkaTopics;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the topics this service produces to, instead of relying on broker auto-creation.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {

    @Bean
    NewTopic userUpdatesTopic(KafkaTopicProperties properties) {
        return TopicBuilder.name(KafkaTopics.USER_UPDATES)
            .partitions(properties.partitions())
            .replicas(properties.replicationFactor())
            .build();
    }
}
