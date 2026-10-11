package com.finsphere.chit.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.finsphere.common.audit.AuditableEntity;

/**
 * A member's slot in a chit scheme. Table {@code fsn_finance.chit_enrollments}; an active slot is unique per plan
 * (enforced by a partial unique index, so concurrent enrolments cannot take the same slot).
 */
@Entity
@Table(name = "chit_enrollments")
public class ChitEnrollment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private ChitPlan plan;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "joined_date", nullable = false)
    private Instant joinedAt;

    protected ChitEnrollment() {
        // for JPA
    }

    public ChitEnrollment(ChitPlan plan, Long userId, Integer slotNumber, Instant joinedAt) {
        this.plan = plan;
        this.userId = userId;
        this.slotNumber = slotNumber;
        this.joinedAt = joinedAt;
        this.active = true;
    }

    public Long getId() {
        return this.id;
    }

    public ChitPlan getPlan() {
        return this.plan;
    }

    public Long getUserId() {
        return this.userId;
    }

    public Integer getSlotNumber() {
        return this.slotNumber;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getJoinedAt() {
        return this.joinedAt;
    }
}
