package com.finsphere.common.exception;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.web.CorrelationId;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

/**
 * Shapes Spring Boot's {@code /error} responses (unknown URLs, errors raised in filters) like the
 * {@code ApiResponse} envelope, so clients always see {@code success}, {@code status}, {@code message},
 * {@code path} and {@code correlationId}. Never exposes exception messages or stack traces.
 */
public class FinsphereErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, @Nullable Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, @Nullable Object> attributes = super.getErrorAttributes(webRequest, options);
        int status = (attributes.get("status") instanceof Integer code) ? code : 500;
        HttpStatus resolved = HttpStatus.resolve(status);
        Object message = attributes.get("message");
        if (!(message instanceof String text) || text.isBlank() || "No message available".equals(text)) {
            attributes.put("message", (resolved != null) ? resolved.getReasonPhrase() : "Request failed");
        }
        attributes.put("success", Boolean.FALSE);
        attributes.put("correlationId", CorrelationId.current());
        attributes.remove("trace");
        attributes.remove("exception");
        return attributes;
    }
}
