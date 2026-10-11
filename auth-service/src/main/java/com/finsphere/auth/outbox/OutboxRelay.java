package com.finsphere.auth.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.common.web.CorrelationId;

import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Relays outbox rows to Kafka in insertion order.
 * <p>
 * Delivery is at-least-once: a message is marked published only after the broker acknowledged it, so a crash
 * may resend it and consumers must be idempotent (chit-finance upserts by user id). When the broker is down the
 * current run stops at the first failure and the message is retried on the next run; after
 * {@code max-attempts} failures it is parked (left unpublished, with the last error) for manual inspection.
 * The backlog is exported as the {@code finsphere.outbox.pending} gauge.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final TransactionTemplate transactionTemplate;

    private final OutboxProperties properties;

    private final Clock clock;

    public OutboxRelay(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate,
            PlatformTransactionManager transactionManager, OutboxProperties properties, Clock clock,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // A slow but healthy broker may take up to send-timeout per message. The default for requests
        // (spring.transaction.default-timeout) is far shorter and would roll such a batch back on every run.
        this.transactionTemplate.setTimeout(batchTimeoutSeconds(properties));
        this.properties = properties;
        this.clock = clock;
        Gauge.builder("finsphere.outbox.pending", repository, OutboxEventRepository::countByPublishedAtIsNull)
            .description("Outbox messages not yet acknowledged by Kafka")
            .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${finsphere.outbox.poll-interval:2s}",
            initialDelayString = "${finsphere.outbox.initial-delay:5s}")
    public void relay() {
        int published;
        do {
            Integer result = this.transactionTemplate.execute((status) -> relayBatch());
            published = (result != null) ? result : 0;
        }
        while (published == this.properties.batchSize());
    }

    /** @return number of messages published in this batch */
    int relayBatch() {
        List<OutboxEvent> batch = this.repository.findByPublishedAtIsNullAndAttemptsLessThanOrderByIdAsc(
                this.properties.maxAttempts(), Limit.of(this.properties.batchSize()));
        int published = 0;
        for (OutboxEvent event : batch) {
            ProducerRecord<String, String> record = new ProducerRecord<>(event.getTopic(), event.getMessageKey(),
                    event.getPayload());
            if (event.getCorrelationId() != null) {
                record.headers().add(CorrelationId.HEADER, event.getCorrelationId().getBytes(StandardCharsets.UTF_8));
            }
            try {
                this.kafkaTemplate.send(record).get(this.properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
                event.markPublished(this.clock.instant());
                published++;
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                event.markFailed("Interrupted while waiting for the broker");
                break;
            }
            catch (ExecutionException | TimeoutException | RuntimeException ex) {
                Throwable cause = (ex instanceof ExecutionException && ex.getCause() != null) ? ex.getCause() : ex;
                event.markFailed(cause.getClass().getSimpleName() + ": " + cause.getMessage());
                log.warn("Outbox message {} to {} not delivered (attempt {}): {}", event.getId(), event.getTopic(),
                        event.getAttempts(), cause.toString());
                break;
            }
        }
        if (published > 0) {
            log.debug("Relayed {} outbox message(s)", published);
        }
        return published;
    }

    /** Long enough for a full batch that waits the whole send timeout for every message, plus the database work. */
    static int batchTimeoutSeconds(OutboxProperties properties) {
        long seconds = properties.sendTimeout().toSeconds() * Math.max(properties.batchSize(), 1) + 30;
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }

    @Scheduled(cron = "${finsphere.outbox.cleanup-cron:0 30 3 * * *}")
    public void purgePublished() {
        Integer removed = this.transactionTemplate
            .execute((status) -> this.repository
                .deletePublishedBefore(this.clock.instant().minus(this.properties.retention())));
        if (removed != null && removed > 0) {
            log.info("Purged {} published outbox message(s)", removed);
        }
    }
}
