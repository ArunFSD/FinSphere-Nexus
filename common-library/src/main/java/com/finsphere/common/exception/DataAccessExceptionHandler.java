package com.finsphere.common.exception;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.finsphere.common.api.ApiResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Persistence failures. Registered only when {@code spring-tx} is on the classpath (services with a database),
 * and ordered before {@link GlobalExceptionHandler} so its catch-all does not win.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class DataAccessExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.CONFLICT,
                "This record was changed by someone else. Refresh and try again", Map.of(), request, ex);
    }

    /** Another request holds the row (lock wait timeout or deadlock), so retrying shortly usually works. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handlePessimisticLock(PessimisticLockingFailureException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.CONFLICT,
                "This record is being changed by another request right now. Try again", Map.of(), request, ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.CONFLICT, "The request conflicts with existing data", Map.of(),
                request, ex);
    }

    /** Database, Redis or MongoDB unreachable, or the connection pool is exhausted. */
    @ExceptionHandler({ DataAccessResourceFailureException.class, CannotCreateTransactionException.class,
            TransientDataAccessResourceException.class })
    public ResponseEntity<ApiResponse<Void>> handleBackendUnavailable(Exception ex, HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.SERVICE_UNAVAILABLE,
                "A backing service is temporarily unavailable. Please try again shortly", Map.of(), request, ex);
    }

    /**
     * A statement or the whole transaction ran past its limit ({@code spring.transaction.default-timeout}, the Redis
     * command timeout or the MongoDB socket timeout) and was cancelled. A database transaction is rolled back, but a
     * MongoDB write may still have been applied, so the caller is asked to check.
     */
    @ExceptionHandler({ QueryTimeoutException.class, TransactionTimedOutException.class })
    public ResponseEntity<ApiResponse<Void>> handleTimeout(Exception ex, HttpServletRequest request) {
        return ErrorResponder.respond(HttpStatus.SERVICE_UNAVAILABLE,
                "The request took too long and was cancelled. Check whether the change was saved before trying again",
                Map.of(), request, ex);
    }
}
