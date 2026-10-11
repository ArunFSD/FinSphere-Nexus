package com.finsphere.chit.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.finsphere.common.event.KafkaTopicProperties;
import com.finsphere.common.event.KafkaTopics;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Failure handling for Kafka consumers.
 * <ul>
 * <li>Values are read through {@code ErrorHandlingDeserializer} (see config-repo), so a malformed message
 * ("poison pill") no longer makes the consumer fail on the same offset forever.</li>
 * <li>A message that keeps failing is retried 3 times, 1 second apart, and then parked on
 * {@code user-updates-topic-dlt} with the failure in its headers, instead of being silently dropped.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaConsumerConfig {

    /** Declared here too, so this service can start (and subscribe) before auth-service has created it. */
    @Bean
    NewTopic userUpdatesTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(KafkaTopics.USER_UPDATES)
            .partitions(topics.partitions())
            .replicas(topics.replicationFactor())
            .build();
    }

    @Bean
    NewTopic userUpdatesDeadLetterTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(KafkaTopics.USER_UPDATES_DLT)
            .partitions(1)
            .replicas(topics.replicationFactor())
            .build();
    }

    /**
     * Template for the dead-letter topic: raw bytes for records that could not be deserialized, JSON otherwise.
     */
    @Bean
    KafkaTemplate<String, Object> deadLetterKafkaTemplate(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());
        delegates.put(Object.class, new JacksonJsonSerializer<>());
        DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(config,
                new StringSerializer(), new DelegatingByTypeSerializer(delegates, true));
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> deadLetterKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate,
                (record, exception) -> new TopicPartition(KafkaTopics.USER_UPDATES_DLT, -1));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
