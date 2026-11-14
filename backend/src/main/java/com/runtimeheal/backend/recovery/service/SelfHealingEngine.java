package com.runtimeheal.backend.recovery.service;

import com.runtimeheal.agent.recovery.RecoveryDecision;
import com.runtimeheal.agent.recovery.RecoveryHandler;
import com.runtimeheal.backend.llm.model.LlmDiagnosticReport;
import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import com.runtimeheal.backend.llm.service.LlmDiagnosticService;
import com.runtimeheal.backend.recovery.config.HealingPolicyProperties;
import com.runtimeheal.backend.recovery.model.HealingActionRecord;
import com.runtimeheal.backend.recovery.model.HealingStatus;
import com.runtimeheal.backend.recovery.model.MethodCircuitBreaker;
import com.runtimeheal.backend.recovery.provider.TypeSafeFallbackProvider;
import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import com.runtimeheal.backend.telemetry.service.TelemetryStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.runtimeheal.backend.recovery.cache.VerifiedStrategyCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Core self-healing execution engine that receives intercepted method failures,
 * evaluates guardrails and safety policies, queries diagnostic recommendations,
 * and executes bounded recovery strategies (RETURN_FALLBACK, RETRY, SKIP_OPERATION).
 */
@Service
public class SelfHealingEngine implements RecoveryHandler {

    private static final Logger log = LoggerFactory.getLogger(SelfHealingEngine.class);

    private final HealingPolicyProperties policyProperties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TypeSafeFallbackProvider fallbackProvider;
    private final HealingAuditService auditService;
    private final LlmDiagnosticService llmDiagnosticService;
    private final TelemetryStoreService telemetryStore;
    private final VerifiedStrategyCache strategyCache;

    public SelfHealingEngine(HealingPolicyProperties policyProperties,
                             CircuitBreakerRegistry circuitBreakerRegistry,
                             TypeSafeFallbackProvider fallbackProvider,
                             HealingAuditService auditService,
                             LlmDiagnosticService llmDiagnosticService,
                             TelemetryStoreService telemetryStore) {
        this(policyProperties, circuitBreakerRegistry, fallbackProvider, auditService,
                llmDiagnosticService, telemetryStore, new VerifiedStrategyCache());
    }

    @Autowired
    public SelfHealingEngine(HealingPolicyProperties policyProperties,
                             CircuitBreakerRegistry circuitBreakerRegistry,
                             TypeSafeFallbackProvider fallbackProvider,
                             HealingAuditService auditService,
                             LlmDiagnosticService llmDiagnosticService,
                             TelemetryStoreService telemetryStore,
                             @Autowired(required = false) VerifiedStrategyCache strategyCache) {
        this.policyProperties = policyProperties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.fallbackProvider = fallbackProvider;
        this.auditService = auditService;
        this.llmDiagnosticService = llmDiagnosticService;
        this.telemetryStore = telemetryStore;
        this.strategyCache = strategyCache != null ? strategyCache : new VerifiedStrategyCache();
    }

    public VerifiedStrategyCache getStrategyCache() {
        return strategyCache;
    }

    @Override
    public RecoveryDecision handleRecovery(
            String className,
            String methodName,
            String methodSignature,
            String returnTypeName,
            Object target,
            Method method,
            Object[] args,
            Throwable thrown,
            String incidentId) {

        long startTime = System.currentTimeMillis();
        String actionId = "HEAL-" + UUID.randomUUID().toString().substring(0, 8);
        String excType = thrown != null ? thrown.getClass().getName() : "UnknownException";

        // 1. Check Global Engine Enablement
        if (!policyProperties.isEnabled()) {
            log.info("[SelfHealingEngine] Self-healing is disabled globally. Propagating exception for {}.{}()",
                    className, methodName);
            auditService.recordAction(new HealingActionRecord(
                    actionId, incidentId, className, methodName, methodSignature, excType,
                    RecoveryStrategy.NO_ACTION, HealingStatus.DISABLED, 0.0,
                    "Self-healing engine is globally disabled in configuration",
                    System.currentTimeMillis() - startTime, null
            ));
            return RecoveryDecision.noAction(actionId, "Self-healing disabled");
        }

        // 2. Policy Guardrail: Denylisted / Transactional Methods
        if (policyProperties.isMethodDenylisted(methodName, className)) {
            log.warn("[SelfHealingEngine] Method {}.{}() is denylisted. Bypassing automated healing.",
                    className, methodName);
            auditService.recordAction(new HealingActionRecord(
                    actionId, incidentId, className, methodName, methodSignature, excType,
                    RecoveryStrategy.NO_ACTION, HealingStatus.BLOCKED_POLICY, 0.0,
                    "Method execution is prohibited by transactional safety denylist",
                    System.currentTimeMillis() - startTime, null
            ));
            return RecoveryDecision.noAction(actionId, "Blocked by safety policy denylist");
        }

        // 3. Safety Guardrail: Sliding-Window Circuit Breaker
        MethodCircuitBreaker cb = circuitBreakerRegistry.getCircuitBreaker(className, methodName);
        if (!cb.allowExecution()) {
            log.warn("[SelfHealingEngine] Circuit breaker is OPEN for {}.{}() ({} failures in window). Halting healing.",
                    className, methodName, cb.getRecentFailureCount());
            auditService.recordAction(new HealingActionRecord(
                    actionId, incidentId, className, methodName, methodSignature, excType,
                    RecoveryStrategy.NO_ACTION, HealingStatus.BLOCKED_CIRCUIT_BREAKER, 0.0,
                    "Circuit breaker tripped to OPEN state after excessive failures",
                    System.currentTimeMillis() - startTime, null
            ));
            return RecoveryDecision.noAction(actionId, "Blocked by circuit breaker");
        }

        // Record this failure against the circuit breaker sliding window
        cb.recordFailure();

        // 4. Retrieve or Generate Diagnostic Recommendation (Verified Strategy Cache Check)
        RecoveryStrategy strategy;
        double confidence;
        String reason;
        long diagnosisLatency = 0;
        boolean isCacheHit = false;

        var cachedOpt = strategyCache.get(className, methodName, excType);
        if (cachedOpt.isPresent()) {
            var cached = cachedOpt.get();
            strategy = cached.getStrategy();
            confidence = cached.getConfidenceScore();
            reason = cached.getPolicyReason() + " [Verified Strategy Cache Hit]";
            diagnosisLatency = 0;
            isCacheHit = true;
            log.info("[SelfHealingEngine] Sub-5ms cache HIT for {}.{}() -> Strategy: {}, Confidence: {}",
                    className, methodName, strategy, confidence);

            // Asynchronous non-blocking background refresh
            final String fIncId = incidentId;
            final String fClass = className;
            final String fMethod = methodName;
            final String fSig = methodSignature;
            final String fExc = excType;
            final Throwable fThrown = thrown;
            final Object[] fArgs = args;
            CompletableFuture.runAsync(() -> {
                try {
                    AgentTelemetryEvent bgEvent = new AgentTelemetryEvent(
                            fIncId != null ? fIncId : "INC-" + UUID.randomUUID().toString().substring(0, 8),
                            Instant.now(), fClass, fMethod, fSig, fExc,
                            fThrown != null ? fThrown.getMessage() : "Unknown exception",
                            extractStackTrace(fThrown), Thread.currentThread().getName(),
                            extractArgumentTypes(fArgs)
                    );
                    LlmDiagnosticReport refreshed = llmDiagnosticService.analyzeEvent(bgEvent);
                    if (refreshed != null && refreshed.getSuggestedRecoveryStrategy() != null) {
                        strategyCache.put(fClass, fMethod, fExc,
                                refreshed.getSuggestedRecoveryStrategy(),
                                refreshed.getConfidenceScore(),
                                refreshed.getReasonForSuggestion());
                    }
                } catch (Exception bgEx) {
                    log.debug("[SelfHealingEngine] Background diagnostic refresh ignored: {}", bgEx.getMessage());
                }
            });
        } else {
            long diagStart = System.currentTimeMillis();
            LlmDiagnosticReport diagnostic = null;
            if (incidentId != null) {
                diagnostic = telemetryStore.getIncidentById(incidentId)
                        .map(llmDiagnosticService::analyzeEvent)
                        .orElse(null);
            }

            if (diagnostic == null) {
                AgentTelemetryEvent event = new AgentTelemetryEvent(
                        incidentId != null ? incidentId : "INC-" + UUID.randomUUID().toString().substring(0, 8),
                        Instant.now(),
                        className,
                        methodName,
                        methodSignature,
                        excType,
                        thrown != null ? thrown.getMessage() : "Unknown exception",
                        extractStackTrace(thrown),
                        Thread.currentThread().getName(),
                        extractArgumentTypes(args)
                );
                diagnostic = llmDiagnosticService.analyzeEvent(event);
            }
            diagnosisLatency = System.currentTimeMillis() - diagStart;
            confidence = diagnostic.getConfidenceScore();
            strategy = diagnostic.getSuggestedRecoveryStrategy();
            reason = diagnostic.getReasonForSuggestion();
        }

        // 5. Policy Guardrail: Confidence Score Threshold Check
        if (confidence < policyProperties.getMinConfidence()) {
            log.warn("[SelfHealingEngine] Diagnostic confidence ({}) is below minimum threshold ({}). Aborting auto-healing.",
                    confidence, policyProperties.getMinConfidence());
            auditService.recordAction(new HealingActionRecord(
                    actionId, incidentId, className, methodName, methodSignature, excType,
                    strategy, HealingStatus.SKIPPED_LOW_CONFIDENCE, confidence,
                    "Confidence score " + confidence + " is below minimum required " + policyProperties.getMinConfidence(),
                    System.currentTimeMillis() - startTime, diagnosisLatency, System.currentTimeMillis() - startTime - diagnosisLatency, null
            ));
            return RecoveryDecision.noAction(actionId, "Confidence score below threshold");
        }

        // Cache strategy for subsequent fast recovery
        if (!isCacheHit) {
            strategyCache.put(className, methodName, excType, strategy, confidence, reason);
        }

        // 6. Execute Bounded Strategy
        log.info("[SelfHealingEngine] Executing strategy {} for {}.{}() with confidence {} (CacheHit: {})",
                strategy, className, methodName, confidence, isCacheHit);

        long healStart = System.currentTimeMillis();

        switch (strategy) {
            case RETURN_FALLBACK: {
                Object fallback = fallbackProvider.getFallbackValue(returnTypeName);
                long healLatency = System.currentTimeMillis() - healStart;
                long totalLatency = diagnosisLatency + healLatency;
                auditService.recordAction(new HealingActionRecord(
                        actionId, incidentId, className, methodName, methodSignature, excType,
                        strategy, HealingStatus.SUCCESS, confidence,
                        "Injected safe fallback default value: " + fallback + (isCacheHit ? " (Verified Cache Hit)" : ""),
                        totalLatency, diagnosisLatency, healLatency, fallback
                ));
                return RecoveryDecision.heal(fallback, actionId, strategy.name(), "Safe fallback injected");
            }

            case SKIP_OPERATION: {
                boolean isVoid = "void".equalsIgnoreCase(returnTypeName) || "V".equals(returnTypeName);
                Object fallback = isVoid ? null : fallbackProvider.getFallbackValue(returnTypeName);
                long healLatency = System.currentTimeMillis() - healStart;
                long totalLatency = diagnosisLatency + healLatency;
                auditService.recordAction(new HealingActionRecord(
                        actionId, incidentId, className, methodName, methodSignature, excType,
                        strategy, HealingStatus.SUCCESS, confidence,
                        "Bypassed failing operation safely" + (isCacheHit ? " (Verified Cache Hit)" : ""),
                        totalLatency, diagnosisLatency, healLatency, fallback
                ));
                return RecoveryDecision.heal(fallback, actionId, strategy.name(), "Operation bypassed safely");
            }

            case RETRY: {
                if (method != null && (target != null || Modifier.isStatic(method.getModifiers()))) {
                    int maxRetries = policyProperties.getMaxRetries();
                    long backoff = policyProperties.getRetryBackoffMs();
                    Object retryResult = null;
                    boolean retrySuccess = false;
                    Throwable lastEx = thrown;

                    for (int attempt = 1; attempt <= maxRetries; attempt++) {
                        try {
                            if (backoff > 0) {
                                Thread.sleep(backoff * attempt);
                            }
                            method.setAccessible(true);
                            retryResult = method.invoke(target, args);
                            retrySuccess = true;
                            log.info("[SelfHealingEngine] Retry attempt {}/{} succeeded for {}.{}()",
                                    attempt, maxRetries, className, methodName);
                            break;
                        } catch (Throwable t) {
                            lastEx = t instanceof java.lang.reflect.InvocationTargetException ? t.getCause() : t;
                            log.warn("[SelfHealingEngine] Retry attempt {}/{} failed for {}.{}(): {}",
                                    attempt, maxRetries, className, methodName, lastEx.getMessage());
                        }
                    }

                    long healLatency = System.currentTimeMillis() - healStart;
                    long totalLatency = diagnosisLatency + healLatency;
                    if (retrySuccess) {
                        cb.recordSuccess();
                        auditService.recordAction(new HealingActionRecord(
                                actionId, incidentId, className, methodName, methodSignature, excType,
                                strategy, HealingStatus.SUCCESS, confidence,
                                "Operation succeeded on retry attempt", totalLatency, diagnosisLatency, healLatency, retryResult
                        ));
                        return RecoveryDecision.heal(retryResult, actionId, strategy.name(), "Operation succeeded on retry");
                    } else {
                        cb.recordFailure();
                        Object fallback = fallbackProvider.getFallbackValue(returnTypeName);
                        auditService.recordAction(new HealingActionRecord(
                                actionId, incidentId, className, methodName, methodSignature, excType,
                                strategy, HealingStatus.SUCCESS, confidence,
                                "All " + maxRetries + " retry attempts failed (" + (lastEx != null ? lastEx.getMessage() : "") + "); gracefully fell back to default value: " + fallback,
                                totalLatency, diagnosisLatency, healLatency, fallback
                        ));
                        return RecoveryDecision.heal(fallback, actionId, strategy.name(), "Retries exhausted; graceful fallback injected");
                    }
                } else {
                    // Method reference not available; fallback to default value
                    Object fallback = fallbackProvider.getFallbackValue(returnTypeName);
                    long healLatency = System.currentTimeMillis() - healStart;
                    long totalLatency = diagnosisLatency + healLatency;
                    auditService.recordAction(new HealingActionRecord(
                            actionId, incidentId, className, methodName, methodSignature, excType,
                            strategy, HealingStatus.SUCCESS, confidence,
                            "Retry unavailable; injected safe fallback value", totalLatency, diagnosisLatency, healLatency, fallback
                    ));
                    return RecoveryDecision.heal(fallback, actionId, strategy.name(), "Injected fallback for retry");
                }
            }

            case NO_ACTION:
            default: {
                long latency = System.currentTimeMillis() - startTime;
                auditService.recordAction(new HealingActionRecord(
                        actionId, incidentId, className, methodName, methodSignature, excType,
                        strategy, HealingStatus.SKIPPED_LOW_CONFIDENCE, confidence,
                        "No automated recovery action recommended by strategy", latency, null
                ));
                return RecoveryDecision.noAction(actionId, "Strategy is NO_ACTION");
            }
        }
    }

    private String extractStackTrace(Throwable thrown) {
        if (thrown == null) {
            return "";
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        thrown.printStackTrace(pw);
        return sw.toString();
    }

    private String[] extractArgumentTypes(Object[] args) {
        if (args == null || args.length == 0) {
            return new String[0];
        }
        return java.util.Arrays.stream(args)
                .map(arg -> arg != null ? arg.getClass().getName() : "null")
                .toArray(String[]::new);
    }
}
