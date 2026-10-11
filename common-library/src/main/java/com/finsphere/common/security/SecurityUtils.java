package com.finsphere.common.security;

import java.util.Objects;
import java.util.Optional;

import com.finsphere.common.exception.DomainException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Static access to the signed-in user. Cheap: no token parsing, just a look at the security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AuthenticatedUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /** The signed-in user, or a 401 {@link DomainException} when the request is anonymous. */
    public static AuthenticatedUser requireCurrentUser() {
        return currentUser().orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "Authentication required",
                "session", "Please sign in again"));
    }

    public static Optional<Long> currentUserId() {
        return currentUser().map(AuthenticatedUser::userId);
    }

    public static boolean isAdmin() {
        return currentUser().map(AuthenticatedUser::isAdmin).orElse(false);
    }

    /** {@code true} when the signed-in user is {@code userId} or an administrator. */
    public static boolean canAccessUser(Long userId) {
        return currentUser().map((user) -> user.isAdmin() || Objects.equals(user.userId(), userId)).orElse(false);
    }
}
