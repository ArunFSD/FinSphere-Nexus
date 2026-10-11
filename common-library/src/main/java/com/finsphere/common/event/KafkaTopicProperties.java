package com.finsphere.common.event;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Layout of the FinSphere topics. Every service that touches a topic declares it with these values, so the topic
 * exists with the right shape whichever service starts first.
 *
 * @param partitions        partitions of the user updates topic (events are keyed by user id)
 * @param replicationFactor 1 for a single local broker, 3 in production
 */
@ConfigurationProperties("finsphere.kafka.topic")
public record KafkaTopicProperties(
        @DefaultValue("3") int partitions,
        @DefaultValue("1") short replicationFactor) {
}
