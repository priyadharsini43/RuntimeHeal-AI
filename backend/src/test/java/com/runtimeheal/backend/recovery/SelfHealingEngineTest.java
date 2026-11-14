package com.runtimeheal.backend.recovery;

import com.runtimeheal.agent.recovery.RecoveryDecision;
import com.runtimeheal.backend.llm.model.DiagnosisSeverity;
import com.runtimeheal.backend.llm.model.LlmDiagnosticReport;
import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import com.runtimeheal.backend.llm.service.LlmDiagnosticService;
import com.runtimeheal.backend.recovery.config.HealingPolicyProperties;
import com.runtimeheal.backend.recovery.model.HealingActionRecord;
import com.runtimeheal.backend.recovery.model.HealingStatus;
import com.runtimeheal.backend.recovery.provider.TypeSafeFallbackProvider;
import com.runtimeheal.backend.recovery.service.CircuitBreakerRegistry;
import com.runtimeheal.backend.recovery.service.HealingAuditService;
import com.runtimeheal.backend.recovery.service.SelfHealingEngine;
import com.runtimeheal.backend.telemetry.service.TelemetryStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SelfHealingEngineTest {

    private HealingPolicyProperties properties;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private TypeSafeFallbackProvider fallbackProvider;
    private HealingAuditService auditService;
    private LlmDiagnosticService llmDiagnosticService;
    private TelemetryStoreService telemetryStore;
    private SelfHealingEngine engine;

    @BeforeEach
    void setUp() {
        properties = new HealingPolicyProperties();
        properties.setEnabled(true);
        properties.setMinConfidence(0.70);
        properties.setDenylist(List.of("validateAndProcessTransfer", "executeGatewayCall"));

        circuitBreakerRegistry = new CircuitBreakerRegistry(properties);
        fallbackProvider = new TypeSafeFallbackProvider();
        auditService = new HealingAuditService();
        llmDiagnosticService = Mockito.mock(LlmDiagnosticService.class);
        telemetryStore = Mockito.mock(TelemetryStoreService.class);

        engine = new SelfHealingEngine(
                properties,
                circuitBreakerRegistry,
                fallbackProvider,
                auditService,
                llmDiagnosticService,
                telemetryStore
        );
    }

    @Test
    @DisplayName("Should successfully heal arithmetic division by returning safe integer fallback (0)")
    void testReturnFallbackStrategy() {
        LlmDiagnosticReport report = new LlmDiagnosticReport(
                "INC-1", "Division by zero", "Arithmetic division error",
                DiagnosisSeverity.MEDIUM, 0.85, RecoveryStrategy.RETURN_FALLBACK,
                "Numeric fallback safe", "MOCK_LLM", 10L, "{}"
        );
        when(llmDiagnosticService.analyzeEvent(any())).thenReturn(report);

        RecoveryDecision decision = engine.handleRecovery(
                "com.runtimeheal.backend.sample.service.OrderProcessingService",
                "calculateDiscountRatio",
                "(II)I",
                "int",
                null,
                null,
                new Object[]{100, 0},
                new ArithmeticException("/ by zero"),
                "INC-1"
        );

        assertNotNull(decision);
        assertTrue(decision.isHealed());
        assertEquals(0, decision.getFallbackValue());
        assertEquals("RETURN_FALLBACK", decision.getStrategy());

        List<HealingActionRecord> records = auditService.getAllActions();
        assertEquals(1, records.size());
        assertEquals(HealingStatus.SUCCESS, records.get(0).getStatus());
    }

    @Test
    @DisplayName("Should block healing for denylisted transactional methods")
    void testDenylistedMethodBlocked() {
        RecoveryDecision decision = engine.handleRecovery(
                "com.runtimeheal.backend.sample.service.OrderProcessingService",
                "validateAndProcessTransfer",
                "()V",
                "void",
                null,
                null,
                new Object[]{},
                new IllegalArgumentException("Invalid amount"),
                "INC-2"
        );

        assertNotNull(decision);
        assertFalse(decision.isHealed());
        assertEquals("NO_ACTION", decision.getStrategy());

        List<HealingActionRecord> records = auditService.getAllActions();
        assertEquals(1, records.size());
        assertEquals(HealingStatus.BLOCKED_POLICY, records.get(0).getStatus());
    }

    @Test
    @DisplayName("Should skip healing when diagnostic confidence is below minimum threshold")
    void testLowConfidenceSkipped() {
        LlmDiagnosticReport lowConfidenceReport = new LlmDiagnosticReport(
                "INC-3", "Uncertain error", "Ambiguous exception context",
                DiagnosisSeverity.LOW, 0.45, RecoveryStrategy.RETURN_FALLBACK,
                "Tentative recommendation", "MOCK_LLM", 5L, "{}"
        );
        when(llmDiagnosticService.analyzeEvent(any())).thenReturn(lowConfidenceReport);

        RecoveryDecision decision = engine.handleRecovery(
                "com.runtimeheal.backend.sample.service.OrderProcessingService",
                "someUntestedMethod",
                "()I",
                "int",
                null,
                null,
                new Object[]{},
                new RuntimeException("Something odd"),
                "INC-3"
        );

        assertNotNull(decision);
        assertFalse(decision.isHealed());

        List<HealingActionRecord> records = auditService.getAllActions();
        assertEquals(1, records.size());
        assertEquals(HealingStatus.SKIPPED_LOW_CONFIDENCE, records.get(0).getStatus());
    }

    @Test
    @DisplayName("Should trip circuit breaker and block healing after threshold is exceeded")
    void testCircuitBreakerTripped() {
        properties.getCircuitBreaker().setFailureThreshold(3);

        LlmDiagnosticReport report = new LlmDiagnosticReport(
                "INC-CB", "Error", "Error", DiagnosisSeverity.MEDIUM, 0.90,
                RecoveryStrategy.RETURN_FALLBACK, "Fallback", "MOCK", 5L, "{}"
        );
        when(llmDiagnosticService.analyzeEvent(any())).thenReturn(report);

        // Fail 3 times to hit threshold
        for (int i = 0; i < 3; i++) {
            RecoveryDecision d = engine.handleRecovery(
                    "com.runtimeheal.TestClass", "failingMethod", "()I", "int",
                    null, null, new Object[]{}, new RuntimeException("fail"), "INC-" + i
            );
            assertTrue(d.isHealed());
        }

        // 4th call should be blocked by circuit breaker
        RecoveryDecision blocked = engine.handleRecovery(
                "com.runtimeheal.TestClass", "failingMethod", "()I", "int",
                null, null, new Object[]{}, new RuntimeException("fail"), "INC-BLOCKED"
        );

        assertFalse(blocked.isHealed());

        List<HealingActionRecord> records = auditService.getAllActions();
        assertEquals(4, records.size());
        // First record in reversed list is the most recent
        assertEquals(HealingStatus.BLOCKED_CIRCUIT_BREAKER, records.get(0).getStatus());
    }

    @Test
    @DisplayName("Should propagate exception when self-healing engine is disabled globally")
    void testEngineDisabled() {
        properties.setEnabled(false);

        RecoveryDecision decision = engine.handleRecovery(
                "com.runtimeheal.TestClass", "anyMethod", "()I", "int",
                null, null, new Object[]{}, new RuntimeException("fail"), "INC-DISABLED"
        );

        assertNotNull(decision);
        assertFalse(decision.isHealed());

        List<HealingActionRecord> records = auditService.getAllActions();
        assertEquals(1, records.size());
        assertEquals(HealingStatus.DISABLED, records.get(0).getStatus());
    }
}
