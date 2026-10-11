package com.finsphere.common.security;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

import org.jspecify.annotations.Nullable;

/**
 * The signed-in user, rebuilt from the JWT once per request and stored as the Spring Security principal.
 * Services read it through {@link SecurityUtils} instead of re-parsing the token.
 *
 * @param userId     user id
 * @param identifier login phone number (JWT subject)
 * @param role       {@code CUSTOMER} or {@code ADMIN}
 * @param fullName   display name
 * @param email      optional e-mail
 */
public record AuthenticatedUser(Long userId, String identifier, String role, String fullName,
        @Nullable String email) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String ROLE_ADMIN = "ADMIN";

    public static final String ROLE_CUSTOMER = "CUSTOMER";

    @Override
    public String getName() {
        return this.identifier;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(this.role);
    }

    /** Spring Security authority, e.g. {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + this.role;
    }
}
