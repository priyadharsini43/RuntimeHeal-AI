package com.runtimeheal.backend.recovery.controller;

import com.runtimeheal.backend.recovery.cache.VerifiedStrategyCache;
import com.runtimeheal.backend.recovery.config.HealingPolicyProperties;
import com.runtimeheal.backend.recovery.model.HealingActionRecord;
import com.runtimeheal.backend.recovery.service.CircuitBreakerRegistry;
import com.runtimeheal.backend.recovery.service.HealingAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing self-healing telemetry, audit trails, and policy management.
 */
@RestController
@RequestMapping("/api/healing")
public class HealingController {

    private final HealingAuditService auditService;
    private final HealingPolicyProperties policyProperties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private VerifiedStrategyCache strategyCache;

    public HealingController(HealingAuditService auditService,
                             HealingPolicyProperties policyProperties,
                             CircuitBreakerRegistry circuitBreakerRegistry) {
        this.auditService = auditService;
        this.policyProperties = policyProperties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Exposes Verified Strategy Cache statistics.
     */
    @GetMapping("/cache")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        if (strategyCache == null) {
            return ResponseEntity.ok(Map.of("status", "DISABLED", "entries", List.of()));
        }
        return ResponseEntity.ok(Map.of(
                "size", strategyCache.size(),
                "totalHits", strategyCache.getTotalHits(),
                "totalMisses", strategyCache.getTotalMisses(),
                "hitRatePercentage", Math.round(strategyCache.getHitRate() * 10.0) / 10.0,
                "entries", strategyCache.getAllEntries()
        ));
    }

    /**
     * Lists all recorded healing execution records.
     */
    @GetMapping("/actions")
    public ResponseEntity<List<HealingActionRecord>> getAllActions() {
        return ResponseEntity.ok(auditService.getAllActions());
    }

    /**
     * Fetches details of a specific healing action by its action ID.
     */
    @GetMapping("/actions/{actionId}")
    public ResponseEntity<HealingActionRecord> getActionById(@PathVariable String actionId) {
        return auditService.getActionById(actionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns an aggregate summary of self-healing successes, blocks, and rates.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(auditService.getAuditSummary());
    }

    /**
     * Retrieves the current healing policy configuration and circuit breaker states.
     */
    @GetMapping("/policy")
    public ResponseEntity<Map<String, Object>> getPolicy() {
        return ResponseEntity.ok(Map.of(
                "enabled", policyProperties.isEnabled(),
                "minConfidence", policyProperties.getMinConfidence(),
                "maxRetries", policyProperties.getMaxRetries(),
                "retryBackoffMs", policyProperties.getRetryBackoffMs(),
                "denylist", policyProperties.getDenylist(),
                "circuitBreakerConfig", Map.of(
                        "failureThreshold", policyProperties.getCircuitBreaker().getFailureThreshold(),
                        "windowSeconds", policyProperties.getCircuitBreaker().getWindowSeconds()
                ),
                "circuitBreakers", circuitBreakerRegistry.getAllStates()
        ));
    }

    /**
     * Updates runtime healing policy properties dynamically.
     */
    @PostMapping("/policy")
    public ResponseEntity<Map<String, Object>> updatePolicy(@RequestBody Map<String, Object> updates) {
        if (updates.containsKey("enabled")) {
            policyProperties.setEnabled(Boolean.parseBoolean(String.valueOf(updates.get("enabled"))));
        }
        if (updates.containsKey("minConfidence")) {
            policyProperties.setMinConfidence(Double.parseDouble(String.valueOf(updates.get("minConfidence"))));
        }
        if (updates.containsKey("maxRetries")) {
            policyProperties.setMaxRetries(Integer.parseInt(String.valueOf(updates.get("maxRetries"))));
        }
        if (updates.containsKey("retryBackoffMs")) {
            policyProperties.setRetryBackoffMs(Long.parseLong(String.valueOf(updates.get("retryBackoffMs"))));
        }
        if (updates.containsKey("denylist") && updates.get("denylist") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) updates.get("denylist");
            policyProperties.setDenylist(list);
        }

        return getPolicy();
    }

    /**
     * Resets circuit breakers across all methods or for a specific method key.
     */
    @PostMapping("/circuit-breaker/reset")
    public ResponseEntity<Map<String, Object>> resetCircuitBreaker(@RequestParam(required = false) String methodKey) {
        if (methodKey != null && !methodKey.isBlank()) {
            boolean reset = circuitBreakerRegistry.reset(methodKey);
            return ResponseEntity.ok(Map.of(
                    "status", reset ? "RESET" : "NOT_FOUND",
                    "methodKey", methodKey
            ));
        } else {
            circuitBreakerRegistry.resetAll();
            return ResponseEntity.ok(Map.of(
                    "status", "ALL_RESET",
                    "message", "All circuit breakers reset to CLOSED state"
            ));
        }
    }
}
