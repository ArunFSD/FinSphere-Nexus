package com.finsphere.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private int status; // Keep the HTTP status code here for convenience
    private String message;
    private LocalDateTime timestamp;
    private T data;             // Used for Success (List of Plans, User Details, etc.)
    private Object errors;      // Used for Failures (Validation maps, error strings)

    public static ApiResponse<Object> error(int status, String message, Object errors) {
        return ApiResponse.builder()
                .success(false)
                .status(status)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
