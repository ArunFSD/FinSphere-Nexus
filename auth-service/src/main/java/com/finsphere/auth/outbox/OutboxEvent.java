package com.finsphere.auth.outbox;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/**
 * Transactional outbox row: a Kafka message written in the same database transaction as the business change and
 * relayed to the broker afterwards by {@link OutboxRelay}. Table {@code fsn_identity.outbox_events}.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    public static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @Column(name = "payload", nullable = false, length = 4000)
    private String payload;

    @Column(name = "correlation_id", length = 64)
    private @Nullable String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private @Nullable Instant publishedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private @Nullable String lastError;

    protected OutboxEvent() {
        // for JPA
    }

    public OutboxEvent(String topic, String messageKey, String payload, @Nullable String correlationId,
            Instant createdAt) {
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant when) {
        this.publishedAt = when;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = (error.length() > MAX_ERROR_LENGTH) ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }

    public Long getId() {
        return this.id;
    }

    public String getTopic() {
        return this.topic;
    }

    public String getMessageKey() {
        return this.messageKey;
    }

    public String getPayload() {
        return this.payload;
    }

    public @Nullable String getCorrelationId() {
        return this.correlationId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable Instant getPublishedAt() {
        return this.publishedAt;
    }

    public int getAttempts() {
        return this.attempts;
    }

    public @Nullable String getLastError() {
        return this.lastError;
    }
}
