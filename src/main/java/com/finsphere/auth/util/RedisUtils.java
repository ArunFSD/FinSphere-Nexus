package com.finsphere.auth.util;

import com.finsphere.auth.entity.User;
import com.finsphere.auth.model.UserSession;
import com.finsphere.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisUtils {

    private final UserSessionRepository sessionRepository;

    public Optional<UserSession> getSessionDetails(String token) {
        log.debug(">>>> [REDIS_FETCH_SESSION] Looking up session by token hash");
        return sessionRepository.findById(token);
    }

    public List<UserSession> getSessionDetails(User user) {
        log.debug(">>>> [REDIS_CHECK_CONCURRENT] Checking active sessions for Phone: {}", user.getPhoneNumber());
        return sessionRepository.findByPhoneNumber(user.getPhoneNumber());
    }

    public void saveSessionToRedis(String token, User user, String ipAddress, String userAgent) {
        log.info(">>>> [REDIS_SAVE_SESSION] Creating session for UserID: {} | IP: {}", user.getId(), ipAddress);

        UserSession session = UserSession.builder()
                .token(token)
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .loginIp(ipAddress)
                .userAgent(userAgent)
                .build();

        sessionRepository.save(session);
        log.info("<<<< [REDIS_SAVE_SUCCESS] Session persisted in Redis for UserID: {}", user.getId());
    }

    public void delSessionToRedis(String token) {
        log.info(">>>> [REDIS_DELETE_SESSION] Removing token from cache");
        sessionRepository.deleteById(token);
    }
}