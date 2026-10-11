package com.finsphere.chit.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.finsphere.chit.service.UserDirectoryService;
import com.finsphere.common.event.KafkaTopics;
import com.finsphere.common.event.UserUpdateEvent;
import com.finsphere.common.web.CorrelationId;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the local user directory in sync with auth-service.
 * <p>
 * Exceptions are deliberately <em>not</em> swallowed: the container's error handler retries and then parks the
 * record on the dead-letter topic (see {@code KafkaConsumerConfig}).
 */
@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final UserDirectoryService directory;

    public UserEventListener(UserDirectoryService directory) {
        this.directory = directory;
    }

    @KafkaListener(topics = KafkaTopics.USER_UPDATES, groupId = "${spring.kafka.consumer.group-id:chit-service-group}")
    public void onUserUpdate(ConsumerRecord<String, UserUpdateEvent> record) {
        Header correlation = record.headers().lastHeader(CorrelationId.HEADER);
        if (correlation != null) {
            MDC.put(CorrelationId.MDC_KEY, new String(correlation.value(), StandardCharsets.UTF_8));
        }
        try {
            UserUpdateEvent event = record.value();
            if (event == null || event.userId() == null) {
                log.warn("Skipping empty user update at {}-{}@{}", record.topic(), record.partition(), record.offset());
                return;
            }
            this.directory.apply(event);
        }
        finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
