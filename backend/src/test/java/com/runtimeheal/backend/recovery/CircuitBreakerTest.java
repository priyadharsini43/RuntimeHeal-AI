package com.runtimeheal.backend.recovery;

import com.runtimeheal.backend.recovery.model.CircuitBreakerState;
import com.runtimeheal.backend.recovery.model.MethodCircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    private MethodCircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        // Threshold: 3 failures, Window: 10 seconds
        circuitBreaker = new MethodCircuitBreaker("testMethod", 3, 10);
    }

    @Test
    @DisplayName("Should initialize in CLOSED state and allow execution")
    void testInitialState() {
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowExecution());
        assertEquals(0, circuitBreaker.getRecentFailureCount());
    }

    @Test
    @DisplayName("Should stay CLOSED when failure count is below threshold")
    void testBelowThreshold() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertEquals(2, circuitBreaker.getRecentFailureCount());
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowExecution());
    }

    @Test
    @DisplayName("Should trip to OPEN state when failures meet or exceed threshold")
    void testTripToOpen() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        assertFalse(circuitBreaker.allowExecution());
    }

    @Test
    @DisplayName("Should return to CLOSED state when explicitly reset")
    void testReset() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());

        circuitBreaker.reset();
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowExecution());
        assertEquals(0, circuitBreaker.getRecentFailureCount());
    }

    @Test
    @DisplayName("Recording success should clear failures in CLOSED state")
    void testRecordSuccess() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        assertEquals(2, circuitBreaker.getRecentFailureCount());

        circuitBreaker.recordSuccess();
        assertEquals(0, circuitBreaker.getRecentFailureCount());
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }
}
