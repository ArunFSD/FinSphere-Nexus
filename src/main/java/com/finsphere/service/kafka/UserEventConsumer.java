package com.finsphere.service.kafka;

import com.finsphere.common.dto.events.UserUpdateEvent;
import com.finsphere.entity.UserMirror;
import com.finsphere.repository.UserMirrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventConsumer {
    private final UserMirrorRepository repository;

    @KafkaListener(topics = "user-updates-topic", groupId = "chit-service-group")
    public void handleUserUpdate(UserUpdateEvent event) {
        log.info(">>>> [KAFKA_SYNC_START] Syncing UserID: {} | Role: {}", event.getUserId(), event.getRole());

        try {
            UserMirror mirror = UserMirror.builder()
                    .userId(event.getUserId())
                    .fullName(event.getFullName())
                    .phoneNumber(event.getPhoneNumber())
                    .careOf(event.getCareOf())
                    .role(event.getRole())
                    .isActive(event.getIsActive())
                    .build();

            repository.save(mirror);
            log.info("<<<< [KAFKA_SYNC_SUCCESS] User {} mirrored in Chit Database", event.getUserId());
        } catch (Exception e) {
            log.error("!!!! [KAFKA_SYNC_ERROR] Failed to mirror UserID: {} | Reason: {}",
                    event.getUserId(), e.getMessage());
        }
    }
}
