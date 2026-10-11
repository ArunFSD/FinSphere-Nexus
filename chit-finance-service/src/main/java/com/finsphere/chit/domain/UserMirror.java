package com.finsphere.chit.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/**
 * Read-only copy of auth-service users, kept current by Kafka {@code user-updates-topic} events.
 * Lets this service validate members without a synchronous call to auth-service. Table
 * {@code fsn_finance.user_mirror}.
 */
@Entity
@Table(name = "user_mirror")
public class UserMirror {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "care_of")
    private @Nullable String careOf;

    @Column(name = "role")
    private String role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public @Nullable String getCareOf() {
        return this.careOf;
    }

    public void setCareOf(@Nullable String careOf) {
        this.careOf = careOf;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastUpdatedAt() {
        return this.lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
