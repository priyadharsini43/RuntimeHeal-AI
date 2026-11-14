package com.runtimeheal.backend.recovery.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sliding-window circuit breaker protecting a specific method against repeated failure cascades.
 */
public class MethodCircuitBreaker {

    private final String methodKey;
    private final int failureThreshold;
    private final long windowMillis;

    private final Deque<Long> failureTimestamps = new ArrayDeque<>();
    private CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private long openedAtTimestamp = 0;
    private static final long COOLDOWN_MILLIS = 30000; // 30s cooldown before trial HALF_OPEN

    public MethodCircuitBreaker(String methodKey, int failureThreshold, int windowSeconds) {
        this.methodKey = methodKey;
        this.failureThreshold = failureThreshold;
        this.windowMillis = windowSeconds * 1000L;
    }

    public synchronized boolean allowHealing() {
        long now = System.currentTimeMillis();
        evictStaleFailures(now);

        if (state == CircuitBreakerState.OPEN) {
            if (now - openedAtTimestamp >= COOLDOWN_MILLIS) {
                state = CircuitBreakerState.HALF_OPEN;
                return true;
            }
            return false;
        }

        return true;
    }

    public synchronized boolean allowExecution() {
        return allowHealing();
    }

    public synchronized void recordFailure() {
        long now = System.currentTimeMillis();
        evictStaleFailures(now);
        failureTimestamps.addLast(now);

        if (state == CircuitBreakerState.HALF_OPEN || failureTimestamps.size() >= failureThreshold) {
            state = CircuitBreakerState.OPEN;
            openedAtTimestamp = now;
        }
    }

    public synchronized void recordSuccess() {
        failureTimestamps.clear();
        if (state == CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.CLOSED;
        }
    }

    public synchronized void reset() {
        state = CircuitBreakerState.CLOSED;
        failureTimestamps.clear();
        openedAtTimestamp = 0;
    }

    private void evictStaleFailures(long now) {
        while (!failureTimestamps.isEmpty() && (now - failureTimestamps.peekFirst() > windowMillis)) {
            failureTimestamps.pollFirst();
        }
    }

    public synchronized CircuitBreakerState getState() {
        long now = System.currentTimeMillis();
        if (state == CircuitBreakerState.OPEN && (now - openedAtTimestamp >= COOLDOWN_MILLIS)) {
            return CircuitBreakerState.HALF_OPEN;
        }
        return state;
    }

    public synchronized int getRecentFailureCount() {
        evictStaleFailures(System.currentTimeMillis());
        return failureTimestamps.size();
    }

    public String getMethodKey() {
        return methodKey;
    }
}
