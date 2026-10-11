package com.finsphere.common.security;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Session cookie settings.
 *
 * @param name     cookie name
 * @param secure   send only over HTTPS (browsers also accept secure cookies on http://localhost)
 * @param sameSite {@code Lax} blocks the cookie on cross-site POSTs, which is the first CSRF defence
 * @param path     cookie path
 * @param domain   optional cookie domain
 */
@ConfigurationProperties("finsphere.security.cookie")
public record CookieProperties(
        @DefaultValue("fsn_auth_token") String name,
        @DefaultValue("true") boolean secure,
        @DefaultValue("Lax") String sameSite,
        @DefaultValue("/") String path,
        @Nullable String domain) {
}
