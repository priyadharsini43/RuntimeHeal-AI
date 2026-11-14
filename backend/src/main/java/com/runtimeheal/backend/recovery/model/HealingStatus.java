package com.runtimeheal.backend.recovery.model;

/**
 * Execution outcome status of a self-healing action.
 */
public enum HealingStatus {
    SUCCESS,
    BLOCKED_POLICY,
    BLOCKED_CIRCUIT_BREAKER,
    SKIPPED_LOW_CONFIDENCE,
    DISABLED,
    FAILED
}
