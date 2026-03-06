package com.finsphere.auth.exception;

import com.finsphere.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle Validation Errors (e.g., @NotBlank, @Pattern)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Failed", errors);
    }

    // 2. Handle Business Logic Errors (e.g., Manual Checks in Service Layer)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        Map<String, String> errors = new HashMap<>();

        /* Industrial Trick: We put the exception message inside the errors map
           under the key "error" so the frontend always finds a consistent structure.
        */
        errors.put("error", ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Failed", errors);
    }

    // 3. Handle Generic Internal Errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        // For generic exceptions, we keep the error map empty but non-null
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                new HashMap<>()
        );
    }

    // 4. Handle Specific FinSphere Business & Security Errors
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        // We pass ex.getErrors() directly to your helper method
        return buildErrorResponse(ex.getStatus(), ex.getTopLevelMessage(), ex.getErrors());
    }

    /**
     * Helper method to maintain a strictly consistent response format across all layers.
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            Map<String, String> errors
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .errors(errors) // This will now always be a non-null JSON object {}
                .build();
        return new ResponseEntity<>(response, status);
    }
}