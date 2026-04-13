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
        log.info(">>>> [KAFKA_PRODUCE_START] Dispatching UserUpdateEvent for ID: {} to Topic: {}",
                event.getUserId(), TOPIC);

        kafkaTemplate.send(TOPIC, String.valueOf(event.getUserId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("<<<< [KAFKA_PRODUCE_SUCCESS] Event delivered for ID: {} | Offset: {}",
                                event.getUserId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("!!!! [KAFKA_PRODUCE_ERROR] Failed to send ID: {} | Reason: {}",
                                event.getUserId(), ex.getMessage());
                    }
                });
    }
}