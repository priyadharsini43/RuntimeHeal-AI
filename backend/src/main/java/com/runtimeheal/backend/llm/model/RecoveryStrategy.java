package com.runtimeheal.backend.llm.model;

/**
 * Bounded set of safe self-healing recovery actions that the LLM diagnostic engine can recommend.
 * Enforces strict safety constraints to prevent arbitrary code execution or unsafe operations.
 */
public enum RecoveryStrategy {
    NO_ACTION("Log incident and continue; no safe automated recovery action available"),
    RETURN_FALLBACK("Return safe fallback default value, empty collection, or cached response"),
    RETRY("Retry the failed transient operation with exponential backoff"),
    SKIP_OPERATION("Bypass the failing non-critical step safely and proceed");

    private final String description;

    RecoveryStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static RecoveryStrategy fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NO_ACTION;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        try {
            return RecoveryStrategy.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Check substrings for resilient mapping
            if (normalized.contains("FALLBACK") || normalized.contains("DEFAULT")) {
                return RETURN_FALLBACK;
            } else if (normalized.contains("RETRY")) {
                return RETRY;
            } else if (normalized.contains("SKIP") || normalized.contains("IGNORE") || normalized.contains("BYPASS")) {
                return SKIP_OPERATION;
            }
            return NO_ACTION;
        }
    }
}
