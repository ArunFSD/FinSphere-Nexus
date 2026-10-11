package com.finsphere.frontend.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

/**
 * Status handler for back-end calls: reads the {@code ApiResponse} error envelope of any 4xx/5xx answer and throws a
 * {@link BackendException} with its message and field errors. Bodies that are not the envelope (a proxy's HTML page,
 * an empty body) still produce a sensible exception.
 */
public class BackendErrorDecoder implements RestClient.ResponseSpec.ErrorHandler {

    private static final int MAX_BODY_BYTES = 64 * 1024;

    private final JsonMapper jsonMapper;

    private final String service;

    /**
     * @param service the service id the client calls ({@code chit-finance-service}), so error pages can name it
     */
    public BackendErrorDecoder(JsonMapper jsonMapper, String service) {
        this.jsonMapper = jsonMapper;
        this.service = service;
    }

    @Override
    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String message = null;
        Map<String, String> errors = new LinkedHashMap<>();
        byte[] body;
        try (InputStream in = response.getBody()) {
            body = in.readNBytes(MAX_BODY_BYTES);
        }
        catch (IOException ex) {
            // The status is what matters: a body cut off by a timeout or a dropped connection must not hide it
            body = new byte[0];
        }
        if (body.length > 0) {
            try {
                JsonNode root = this.jsonMapper.readTree(body);
                JsonNode messageNode = root.path("message");
                if (messageNode.isString()) {
                    message = messageNode.stringValue();
                }
                JsonNode errorsNode = root.path("errors");
                if (errorsNode.isObject()) {
                    for (Map.Entry<String, JsonNode> entry : errorsNode.properties()) {
                        if (entry.getValue().isString()) {
                            errors.put(entry.getKey(), entry.getValue().stringValue());
                        }
                    }
                }
            }
            catch (JacksonException ex) {
                // not the ApiResponse envelope, keep the default message for the status
            }
        }
        throw new BackendException(status, message, errors, this.service);
    }
}
