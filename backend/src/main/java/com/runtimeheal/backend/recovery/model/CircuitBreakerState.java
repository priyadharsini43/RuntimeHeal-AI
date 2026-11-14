package com.runtimeheal.backend.recovery.model;

/**
 * Lifecycle states of a method-level circuit breaker.
 */
public enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
