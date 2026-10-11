package com.finsphere.frontend.client;

import java.util.Map;

import com.finsphere.common.api.ApiResponse;

/** Unwraps the {@code ApiResponse} envelope of a successful call. */
public final class BackendResults {

    private BackendResults() {
    }

    public static <T> T data(ApiResponse<T> response) {
        T data = (response != null) ? response.data() : null;
        if (data == null) {
            throw new BackendException(502, "The service sent an empty answer. Try again.", Map.of());
        }
        return data;
    }
}
