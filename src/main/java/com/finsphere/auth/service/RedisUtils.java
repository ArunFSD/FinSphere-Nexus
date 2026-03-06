package com.finsphere.auth.service;

import com.finsphere.auth.entity.User;
import com.finsphere.auth.model.UserSession;
import com.finsphere.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final UserSessionRepository sessionRepository; // Redis Repository

    public List<UserSession> getSessionDetails(User user){
        return sessionRepository.findByPhoneNumber(user.getPhoneNumber());
    }

    public void saveSessionToRedis(String token, User user, String ipAddress, String userAgent) {
        UserSession session = UserSession.builder()
                .sessionId(token)
                .phoneNumber(user.getPhoneNumber())
                .loginIp(ipAddress)
                .userAgent(userAgent)
                .role(user.getRole().name())
                .build();
        sessionRepository.save(session);
    }

    public void delSessionToRedis(String token) {
        sessionRepository.deleteById(token);
    }

}
