package com.runtimeheal.backend.llm.model;

/**
 * Severity level assigned to a runtime incident by the LLM diagnostic engine.
 */
public enum DiagnosisSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static DiagnosisSeverity fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MEDIUM;
        }
        try {
            return DiagnosisSeverity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
