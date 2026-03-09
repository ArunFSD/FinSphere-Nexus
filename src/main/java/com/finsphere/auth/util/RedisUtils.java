package com.finsphere.auth.util;

import com.finsphere.auth.entity.User;
import com.finsphere.auth.model.UserSession;
import com.finsphere.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final UserSessionRepository sessionRepository; // Redis Repository

    public Optional<UserSession> getSessionDetails(String token) {
        return sessionRepository.findById(token);
    }

    public List<UserSession> getSessionDetails(User user){
        return sessionRepository.findByPhoneNumber(user.getPhoneNumber());
    }

    public void saveSessionToRedis(String token, User user, String ipAddress, String userAgent) {
        UserSession session = UserSession.builder()
                .token(token)
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .loginIp(ipAddress)
                .userAgent(userAgent)
                .build();
        sessionRepository.save(session);
    }

    public void delSessionToRedis(String token) {
        sessionRepository.deleteById(token);
    }
    
    

}
