package com.finsphere.gateway.error;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import com.finsphere.gateway.filter.CorrelationIdGlobalFilter;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

/**
 * Turns routing failures (service down, no instance registered, timeout) into the same error envelope the
 * services use, or into a small HTML page when a browser navigated to the failing URL.
 * Ordered before Spring Boot's default handler ({@code -1}).
 */
@Component
@Order(-2)
public class GatewayErrorHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    private final JsonMapper jsonMapper;

    public GatewayErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatusCode status = resolveStatus(ex);
        String message = messageFor(status);
        String path = exchange.getRequest().getURI().getRawPath();
        Object correlationId = exchange.getAttributes().get(CorrelationIdGlobalFilter.ATTRIBUTE);
        if (status.is5xxServerError()) {
            log.warn("[cid={}] {} {} failed with {}: {}", correlationId, exchange.getRequest().getMethod(), path,
                    status.value(), ex.toString());
        }

        byte[] body;
        response.setStatusCode(status);
        if (wantsHtml(exchange)) {
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            body = htmlPage(status, message, String.valueOf(correlationId)).getBytes(StandardCharsets.UTF_8);
        }
        else {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("success", false);
            envelope.put("status", status.value());
            envelope.put("message", message);
            envelope.put("path", path);
            if (correlationId != null) {
                envelope.put("correlationId", correlationId);
            }
            envelope.put("timestamp", Instant.now().toString());
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            body = this.jsonMapper.writeValueAsBytes(envelope);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private static HttpStatusCode resolveStatus(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ResponseStatusException rse) {
                return rse.getStatusCode();
            }
            if (current instanceof ConnectException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            if (current instanceof TimeoutException) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
            current = current.getCause();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 502, 503 -> "The service is temporarily unavailable. Please try again in a moment";
            case 504 -> "The service took too long to respond. Please try again";
            case 404 -> "Resource not found";
            default -> status.is4xxClientError() ? "The request could not be processed"
                    : "An unexpected gateway error occurred";
        };
    }

    private static boolean wantsHtml(ServerWebExchange exchange) {
        try {
            return exchange.getRequest()
                .getHeaders()
                .getAccept()
                .stream()
                .anyMatch((type) -> type.isCompatibleWith(MediaType.TEXT_HTML) && !type.isWildcardType());
        }
        catch (InvalidMediaTypeException ex) {
            return false;
        }
    }

    private static String htmlPage(HttpStatusCode status, String message, String correlationId) {
        // All values are server generated (status, fixed message, validated correlation id), no user input
        return """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>FinSphere Nexus - %d</title>
                <style>
                  body{margin:0;min-height:100vh;display:grid;place-items:center;font-family:system-ui,sans-serif;
                       background:#f4f6fb;color:#111827;padding:16px}
                  .card{max-width:440px;background:#fff;border:1px solid #e5e7eb;border-radius:16px;padding:32px;
                        box-shadow:0 10px 30px rgba(17,24,39,.08);text-align:center}
                  h1{font-size:20px;margin:0 0 8px} p{color:#4b5563;line-height:1.5}
                  a{display:inline-block;margin-top:16px;padding:10px 18px;border-radius:10px;background:#1d4ed8;
                    color:#fff;text-decoration:none;font-weight:600} small{color:#9ca3af}
                </style></head>
                <body><div class="card"><h1>FinSphere Nexus</h1><p>%s</p>
                <a href="/">Try again</a><p><small>Reference: %s</small></p></div></body></html>
                """.formatted(status.value(), message, correlationId);
    }
}
