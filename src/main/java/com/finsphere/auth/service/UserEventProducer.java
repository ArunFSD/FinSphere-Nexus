package com.finsphere.auth.service;

import com.finsphere.common.dto.events.UserUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private static final String TOPIC = "user-updates-topic";
    private final KafkaTemplate<String, UserUpdateEvent> kafkaTemplate;

    public void sendUserUpdate(UserUpdateEvent event) {
        log.info("Sending user update event to Kafka for ID: {}", event.getUserId());
        kafkaTemplate.send(TOPIC, String.valueOf(event.getUserId()), event);
    }

}
