package com.finsphere.auth.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import com.finsphere.common.audit.AuditableEntity;

/**
 * Login account. Table {@code fsn_identity.users}.
 */
@Entity
@Table(name = "users")
public class UserAccount extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "email", unique = true, length = 100)
    private @Nullable String email;

    /** Delegating password hash, e.g. {@code {bcrypt}$2a$10$...} (legacy hashes without prefix are BCrypt). */
    @Column(name = "password", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private @Nullable CustomerProfile profile;

    protected UserAccount() {
        // for JPA
    }

    public UserAccount(String phoneNumber, @Nullable String email, String passwordHash, UserRole role) {
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
    }

    /** Links the profile to this account (shared primary key). */
    public void attachProfile(CustomerProfile customerProfile) {
        this.profile = customerProfile;
        customerProfile.setUser(this);
    }

    /** Display name, falling back to a neutral label when no profile exists. */
    public String displayName() {
        return (this.profile != null && this.profile.getFullName() != null) ? this.profile.getFullName() : "User";
    }

    public Long getId() {
        return this.id;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public @Nullable String getEmail() {
        return this.email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return this.role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public @Nullable CustomerProfile getProfile() {
        return this.profile;
    }
}
