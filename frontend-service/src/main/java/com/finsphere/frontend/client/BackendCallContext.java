package com.finsphere.frontend.client;

import org.jspecify.annotations.Nullable;

/**
 * What the back-end services need to know about the browser request a call is made for: the signed-in user's
 * session cookie, the correlation id, and the client's address and user agent (for their audit trail).
 * <p>
 * Calls made on the request thread read it from the current request. Calls made on other threads (pages that
 * load several sections in parallel) get it through {@link #CURRENT}, bound for the duration of the call.
 *
 * @param cookieHeader  {@code Cookie} header carrying only the session cookie, or {@code null} when signed out
 * @param correlationId correlation id of the browser request
 * @param forwardedFor  client address chain for {@code X-Forwarded-For}
 * @param userAgent     browser user agent
 */
public record BackendCallContext(@Nullable String cookieHeader, @Nullable String correlationId,
        @Nullable String forwardedFor, @Nullable String userAgent) {

    public static final ScopedValue<BackendCallContext> CURRENT = ScopedValue.newInstance();

    public static final BackendCallContext NONE = new BackendCallContext(null, null, null, null);
}
