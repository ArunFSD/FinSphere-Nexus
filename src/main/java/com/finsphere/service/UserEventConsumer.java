package com.finsphere.service;

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

    private final UserMirrorRepository userMirrorRepository;

    @KafkaListener(topics = "user-updates-topic", groupId = "chit-service-group")
    public void consumer(UserUpdateEvent event) {
        log.info("Received user update event for ID: {}", event.getUserId());

        UserMirror mirror = UserMirror.builder()
                .userId(event.getUserId())
                .fullName(event.getFullName())
                .phoneNumber(event.getPhoneNumber())
                .careOf(event.getCareOf())
                .isActive(event.getIsActive())
                .build();

        userMirrorRepository.save(mirror);
        log.info("User Mirror updated successfully for: {}", event.getFullName());
    }
}
