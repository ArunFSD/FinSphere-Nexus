package com.finsphere.gateway.filter;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * First filter of every routed request.
 * <ul>
 * <li>assigns a correlation id (reusing a well-formed inbound {@code X-Correlation-Id}), forwards it downstream and
 * echoes it to the client</li>
 * <li>writes one access-log line per request with status and latency; requests slower than
 * {@code finsphere.gateway.slow-request-threshold} are logged at WARN</li>
 * </ul>
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    public static final String ATTRIBUTE = CorrelationIdGlobalFilter.class.getName() + ".ID";

    private static final String MDC_KEY = "correlationId";

    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    private static final Pattern STATIC_ASSET = Pattern
        .compile(".*\\.(css|js|map|png|jpe?g|gif|svg|ico|webp|woff2?|ttf)$");

    private static final Logger log = LoggerFactory.getLogger("com.finsphere.gateway.ACCESS");

    private final long slowThresholdMillis;

    public CorrelationIdGlobalFilter(
            @Value("${finsphere.gateway.slow-request-threshold:2s}") Duration slowRequestThreshold) {
        this.slowThresholdMillis = slowRequestThreshold.toMillis();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String inbound = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = (inbound != null && SAFE_VALUE.matcher(inbound).matches()) ? inbound
                : UUID.randomUUID().toString();
        ServerHttpRequest request = exchange.getRequest()
            .mutate()
            .headers((headers) -> headers.set(HEADER, correlationId))
            .build();
        ServerWebExchange mutated = exchange.mutate().request(request).build();
        mutated.getAttributes().put(ATTRIBUTE, correlationId);
        mutated.getResponse().getHeaders().set(HEADER, correlationId);
        long start = System.nanoTime();
        return chain.filter(mutated).doFinally((signal) -> logAccess(mutated, correlationId, start));
    }

    private void logAccess(ServerWebExchange exchange, String correlationId, long start) {
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        int code = (status != null) ? status.value() : 0;
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();
        MDC.put(MDC_KEY, correlationId);
        try {
            if (elapsedMillis >= this.slowThresholdMillis) {
                log.warn("SLOW {} {} -> {} in {} ms", method, path, code, elapsedMillis);
            }
            else if (STATIC_ASSET.matcher(path).matches()) {
                log.debug("{} {} -> {} in {} ms", method, path, code, elapsedMillis);
            }
            else {
                log.info("{} {} -> {} in {} ms", method, path, code, elapsedMillis);
            }
        }
        finally {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
