package com.runtimeheal.backend.sample.exception;

import com.runtimeheal.backend.sample.dto.SampleErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

/**
 * Centralized exception handler providing structured error payloads for runtime failures.
 * Captures incident correlation telemetry for the self-healing observability pipeline.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<SampleErrorResponse> handleArithmeticException(ArithmeticException ex, HttpServletRequest request) {
        String incidentId = generateIncidentId("ARITH");
        log.error("[{}] ArithmeticException intercepted at {}: {}", incidentId, request.getRequestURI(), ex.getMessage(), ex);

        SampleErrorResponse response = new SampleErrorResponse(
                incidentId,
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Arithmetic Failure",
                ex.getClass().getName(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<SampleErrorResponse> handleNullPointerException(NullPointerException ex, HttpServletRequest request) {
        String incidentId = generateIncidentId("NPE");
        log.error("[{}] NullPointerException intercepted at {}: {}", incidentId, request.getRequestURI(), ex.getMessage(), ex);

        SampleErrorResponse response = new SampleErrorResponse(
                incidentId,
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Null Pointer Dereference",
                ex.getClass().getName(),
                ex.getMessage() != null ? ex.getMessage() : "Attempted to dereference a null object pointer",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SampleErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        String incidentId = generateIncidentId("ARG");
        log.warn("[{}] IllegalArgumentException intercepted at {}: {}", incidentId, request.getRequestURI(), ex.getMessage());

        SampleErrorResponse response = new SampleErrorResponse(
                incidentId,
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Business Argument",
                ex.getClass().getName(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(SimulatedTimeoutException.class)
    public ResponseEntity<SampleErrorResponse> handleSimulatedTimeoutException(SimulatedTimeoutException ex, HttpServletRequest request) {
        String incidentId = generateIncidentId("TIMEOUT");
        log.error("[{}] SimulatedTimeoutException intercepted for service '{}': {}",
                incidentId, ex.getTargetService(), ex.getMessage());

        SampleErrorResponse response = new SampleErrorResponse(
                incidentId,
                Instant.now(),
                HttpStatus.GATEWAY_TIMEOUT.value(),
                "Service Gateway Timeout",
                ex.getClass().getName(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SampleErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String incidentId = generateIncidentId("GEN");
        log.error("[{}] Unhandled exception intercepted at {}: {}", incidentId, request.getRequestURI(), ex.getMessage(), ex);

        SampleErrorResponse response = new SampleErrorResponse(
                incidentId,
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getClass().getName(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String generateIncidentId(String prefix) {
        return "INC-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
