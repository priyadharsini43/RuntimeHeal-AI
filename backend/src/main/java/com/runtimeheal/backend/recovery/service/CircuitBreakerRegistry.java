package com.runtimeheal.backend.recovery.service;

import com.runtimeheal.backend.recovery.config.HealingPolicyProperties;
import com.runtimeheal.backend.recovery.model.MethodCircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry managing method-level circuit breakers across the application.
 */
@Service
public class CircuitBreakerRegistry {

    private final HealingPolicyProperties policyProperties;
    private final Map<String, MethodCircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public CircuitBreakerRegistry(HealingPolicyProperties policyProperties) {
        this.policyProperties = policyProperties;
    }

    public MethodCircuitBreaker getCircuitBreaker(String className, String methodName) {
        String key = (className != null ? className : "") + "." + methodName;
        return circuitBreakers.computeIfAbsent(key, k -> new MethodCircuitBreaker(
                k,
                policyProperties.getCircuitBreaker().getFailureThreshold(),
                policyProperties.getCircuitBreaker().getWindowSeconds()
        ));
    }

    public void resetAll() {
        circuitBreakers.values().forEach(MethodCircuitBreaker::reset);
    }

    public boolean reset(String methodKey) {
        MethodCircuitBreaker cb = circuitBreakers.get(methodKey);
        if (cb != null) {
            cb.reset();
            return true;
        }
        return false;
    }

    public Map<String, Object> getAllStates() {
        Map<String, Object> states = new ConcurrentHashMap<>();
        circuitBreakers.forEach((k, cb) -> states.put(k, Map.of(
                "state", cb.getState().name(),
                "recentFailures", cb.getRecentFailureCount()
        )));
        return states;
    }
}
