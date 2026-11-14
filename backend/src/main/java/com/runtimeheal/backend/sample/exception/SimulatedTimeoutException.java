package com.runtimeheal.backend.sample.exception;

/**
 * Custom runtime exception representing a simulated network or external service timeout.
 * Used to model transient external failures in enterprise systems.
 */
public class SimulatedTimeoutException extends RuntimeException {

    private final String targetService;
    private final long timeoutDurationMillis;

    public SimulatedTimeoutException(String message, String targetService, long timeoutDurationMillis) {
        super(message);
        this.targetService = targetService;
        this.timeoutDurationMillis = timeoutDurationMillis;
    }

    public String getTargetService() {
        return targetService;
    }

    public long getTimeoutDurationMillis() {
        return timeoutDurationMillis;
    }
}
