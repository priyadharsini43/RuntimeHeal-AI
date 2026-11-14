package com.runtimeheal.backend.recovery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties governing the safety policies of the self-healing recovery engine.
 */
@Component
@ConfigurationProperties(prefix = "runtimeheal.healing")
public class HealingPolicyProperties {

    private boolean enabled = true;
    private double minConfidence = 0.70;
    private int maxRetries = 3;
    private long retryBackoffMs = 100;
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    private List<String> denylist = new ArrayList<>(List.of("validateAndProcessTransfer", "executeGatewayCall"));

    public static class CircuitBreakerConfig {
        private int failureThreshold = 5;
        private int windowSeconds = 60;

        public int getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public List<String> getDenylist() {
        return denylist;
    }

    public void setDenylist(List<String> denylist) {
        this.denylist = denylist;
    }

    public boolean isMethodDenylisted(String methodName, String className) {
        if (denylist == null || denylist.isEmpty()) {
            return false;
        }
        for (String pattern : denylist) {
            if (pattern.equalsIgnoreCase(methodName) || (className != null && (className + "." + methodName).contains(pattern))) {
                return true;
            }
        }
        return false;
    }
}
