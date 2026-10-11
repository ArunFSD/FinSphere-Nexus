package com.finsphere.common.logging;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the service / controller timing aspect.
 *
 * @param enabled       turn the aspect on or off
 * @param slowThreshold calls slower than this are logged at WARN, the cheapest way to spot bottlenecks
 */
@ConfigurationProperties("finsphere.logging.method")
public record MethodLoggingProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("500ms") Duration slowThreshold) {
}
