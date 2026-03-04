package com.finsphere.auth.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@Data
@Builder
@RedisHash(value = "UserSession", timeToLive = 86400) // 24 Hours TTL
public class UserSession implements Serializable {
    @Id
    private String sessionId; // The JWT or a unique UUID

    @Indexed
    private String phoneNumber;

    private String loginIp;
    private String userAgent;
    private String role;
}
