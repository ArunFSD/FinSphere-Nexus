package com.finsphere.common.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for audited JPA entities.
 * <p>
 * Spring Data JPA auditing fills who created / last changed the row and when (the actor is the signed-in user
 * id, {@code null} for system actions). {@link Version} adds optimistic locking, so two administrators editing
 * the same record cannot silently overwrite each other (the loser gets HTTP 409).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private @Nullable Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private @Nullable Long createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private @Nullable Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private @Nullable Long updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private @Nullable Long version;

    public @Nullable Instant getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable Long getCreatedBy() {
        return this.createdBy;
    }

    public @Nullable Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public @Nullable Long getUpdatedBy() {
        return this.updatedBy;
    }

    public @Nullable Long getVersion() {
        return this.version;
    }
}
