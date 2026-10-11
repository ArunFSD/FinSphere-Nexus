package com.finsphere.common.exception;

import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import com.finsphere.common.api.ApiResponse;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates every exception thrown by a {@code @RestController} into the standard {@link ApiResponse} error
 * envelope with the right HTTP status, and logs it once at the right level.
 * <p>
 * Internal exception messages are never sent to the client for 5xx errors; the response carries the
 * correlation id instead so the failure can be found in the logs. Persistence exceptions are handled by
 * {@link DataAccessExceptionHandler} in services that have a database.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final String VALIDATION_FAILED = "Some details need changing. Check the highlighted fields.";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(DomainException ex, HttpServletRequest request) {
        return ErrorResponder.respond(ex.getStatus(), ex.getMessage(), ex.getErrors(), request, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleBodyValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach((error) -> errors.putIfAbsent(error.getField(), text(error)));
        ex.getBindingResult()
            .getGlobalErrors()
            .forEach((error) -> errors.putIfAbsent(error.getObjectName(), text(error)));
        return ErrorResponder.respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, errors, request, ex);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String name = Objects.requireNonNullElse(result.getMethodParameter().getParameterName(), "parameter");
            result.getResolvableErrors().forEach((error) -> errors.putIfAbsent(name, text(error)));
        }
        return ErrorResponder.respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, errors, request, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations()
            .forEach((violation) -> errors.putIfAbsent(leafName(violation.getPropertyPath()), violation.getMessage()));
        return ErrorResponder.respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, errors, request, ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (ex.getMostSpecificCause() instanceof MismatchedInputException mismatch) {
            String field = fieldPath(mismatch);
            if (!field.isEmpty()) {
                errors.put(field, describe(mismatch));
            }
        }
        String message = errors.isEmpty() ? "Malformed or unreadable request body" : VALIDATION_FAILED;
        return ErrorResponder.respond(HttpStatus.BAD_REQUEST, message, errors, request, ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, Map.of(ex.getName(), "Invalid value"),
                request, ex);
    }

    /** {@code ?sort=unknownField} on a paginated endpoint. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadSort(PropertyReferenceException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED,
                Map.of("sort", "Cannot sort by '" + ex.getPropertyName() + "'"), request, ex);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED,
                Map.of(ex.getParameterName(), "Is required"), request, ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method " + ex.getMethod() + " is not supported here", Map.of(), request, ex);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type is not supported, send application/json", Map.of(), request, ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.NOT_FOUND, "Resource not found", Map.of(), request, ex);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.UNAUTHORIZED, "Authentication required", Map.of(), request, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.FORBIDDEN, "You do not have permission to perform this action",
                Map.of(), request, ex);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex,
            HttpServletRequest request) {
        String reason = (ex.getReason() != null) ? ex.getReason() : "Request failed";
        return ErrorResponder.respond(ex.getStatusCode(), reason, Map.of(), request, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ErrorResponder.respond(status, "An unexpected system error occurred", Map.of(), request, ex);
    }

    private static String text(MessageSourceResolvable error) {
        return Objects.requireNonNullElse(error.getDefaultMessage(), "Is invalid");
    }

    private static String leafName(Path path) {
        String name = "";
        for (Path.Node node : path) {
            if (node.getName() != null) {
                name = node.getName();
            }
        }
        return name.isEmpty() ? "request" : name;
    }

    private static String fieldPath(JacksonException ex) {
        return ex.getPath()
            .stream()
            .map(JacksonException.Reference::getPropertyName)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("."));
    }

    private static String describe(MismatchedInputException ex) {
        Class<?> target = ex.getTargetType();
        if (target == null) {
            return "Invalid value";
        }
        if (target.isEnum()) {
            String allowed = Arrays.stream(target.getEnumConstants())
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
            return "Invalid value. Allowed values: " + allowed;
        }
        if (Number.class.isAssignableFrom(target) || target.isPrimitive()) {
            return "Must be a number";
        }
        if (Temporal.class.isAssignableFrom(target)) {
            return "Must be a date in yyyy-MM-dd format";
        }
        return "Invalid value";
    }
}
