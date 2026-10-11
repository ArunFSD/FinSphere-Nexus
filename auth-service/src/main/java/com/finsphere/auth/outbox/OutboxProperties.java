package com.finsphere.auth.outbox;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Outbox relay settings ({@code finsphere.outbox.*}).
 *
 * @param pollInterval delay between relay runs
 * @param batchSize    messages claimed per run
 * @param maxAttempts  failed sends before a message is parked for manual inspection
 * @param sendTimeout  how long to wait for the broker to acknowledge one message
 * @param retention    how long published rows are kept
 */
@ConfigurationProperties("finsphere.outbox")
public record OutboxProperties(
        @DefaultValue("2s") Duration pollInterval,
        @DefaultValue("50") int batchSize,
        @DefaultValue("20") int maxAttempts,
        @DefaultValue("10s") Duration sendTimeout,
        @DefaultValue("7d") Duration retention) {
}
