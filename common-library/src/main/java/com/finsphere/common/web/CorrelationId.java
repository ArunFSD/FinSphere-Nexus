package com.finsphere.common.web;

import java.util.UUID;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

/**
 * Correlation id helpers shared by HTTP filters, outbound clients and Kafka producers/consumers.
 * <p>
 * A correlation id travels with a request through gateway, frontend and backend services (as the
 * {@value #HEADER} header) and is written into every log line through the {@value #MDC_KEY} MDC key.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";

    public static final String MDC_KEY = "correlationId";

    /** Only accept well-formed inbound ids so a caller cannot inject arbitrary text into our logs. */
    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    private CorrelationId() {
    }

    public static @Nullable String current() {
        return MDC.get(MDC_KEY);
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static String sanitizeOrCreate(@Nullable String candidate) {
        return (candidate != null && SAFE_VALUE.matcher(candidate).matches()) ? candidate : newId();
    }

    public static boolean isValid(@Nullable String candidate) {
        return candidate != null && SAFE_VALUE.matcher(candidate).matches();
    }
}
