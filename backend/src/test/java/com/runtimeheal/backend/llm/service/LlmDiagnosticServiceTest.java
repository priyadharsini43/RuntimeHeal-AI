package com.runtimeheal.backend.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.llm.model.DiagnosisSeverity;
import com.runtimeheal.backend.llm.model.LlmDiagnosticReport;
import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import com.runtimeheal.backend.llm.prompt.DiagnosticPromptBuilder;
import com.runtimeheal.backend.llm.provider.DiagnosticLlmProvider;
import com.runtimeheal.backend.llm.provider.LlmProviderResponse;
import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import com.runtimeheal.backend.telemetry.service.TelemetryStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class LlmDiagnosticServiceTest {

    private DiagnosticLlmProvider llmProvider;
    private DiagnosticPromptBuilder promptBuilder;
    private TelemetryStoreService telemetryStore;
    private ObjectMapper objectMapper;
    private LlmDiagnosticService diagnosticService;

    @BeforeEach
    void setUp() {
        llmProvider = Mockito.mock(DiagnosticLlmProvider.class);
        when(llmProvider.getProviderId()).thenReturn("ollama");
        when(llmProvider.getProviderName()).thenReturn("Local Ollama");
        when(llmProvider.getModel()).thenReturn("qwen2.5:3b");
        promptBuilder = new DiagnosticPromptBuilder();
        telemetryStore = new TelemetryStoreService();
        objectMapper = new ObjectMapper();
        diagnosticService = new LlmDiagnosticService(llmProvider, promptBuilder, telemetryStore, objectMapper);
    }

    @Test
    @DisplayName("Should successfully parse structured JSON from mocked Ollama response")
    void testSuccessfulLlmDiagnosis() throws Exception {
        AgentTelemetryEvent event = new AgentTelemetryEvent(
                "INC-TEST-ARITH",
                Instant.now(),
                "OrderProcessingService",
                "calculateDiscountRatio",
                "(int,int)",
                "java.lang.ArithmeticException",
                "/ by zero",
                "ArithmeticException: / by zero",
                "main",
                new String[]{"int", "int"}
        );

        when(llmProvider.isAvailable()).thenReturn(true);

        String mockResponse = "{\n" +
                "  \"rootCause\": \"Division by zero in discount computation\",\n" +
                "  \"explanation\": \"Divisor parameter was zero causing ArithmeticException\",\n" +
                "  \"severity\": \"MEDIUM\",\n" +
                "  \"confidenceScore\": 0.95,\n" +
                "  \"suggestedRecoveryStrategy\": \"RETURN_FALLBACK\",\n" +
                "  \"reasonForSuggestion\": \"Safe fallback of zero prevents crash\"\n" +
                "}";

        when(llmProvider.generate(anyString(), anyString()))
                .thenReturn(new LlmProviderResponse("qwen2.5:3b", mockResponse));

        LlmDiagnosticReport report = diagnosticService.analyzeEvent(event);

        assertNotNull(report);
        assertEquals("INC-TEST-ARITH", report.getIncidentId());
        assertEquals("Division by zero in discount computation", report.getRootCause());
        assertEquals(DiagnosisSeverity.MEDIUM, report.getSeverity());
        assertEquals(0.95, report.getConfidenceScore(), 0.001);
        assertEquals(RecoveryStrategy.RETURN_FALLBACK, report.getSuggestedRecoveryStrategy());
        assertTrue(report.getSource().contains("OLLAMA"));
    }

    @Test
    @DisplayName("Should safely fallback to heuristics when Ollama is offline")
    void testHeuristicFallbackWhenOllamaOffline() {
        AgentTelemetryEvent event = new AgentTelemetryEvent(
                "INC-TEST-NPE",
                Instant.now(),
                "OrderProcessingService",
                "retrieveCustomerPreference",
                "(String)",
                "java.lang.NullPointerException",
                "Cannot invoke getTierName() because profile is null",
                "NullPointerException at OrderProcessingService.java:55",
                "main",
                new String[]{"String"}
        );

        when(llmProvider.isAvailable()).thenReturn(false);

        LlmDiagnosticReport report = diagnosticService.analyzeEvent(event);

        assertNotNull(report);
        assertEquals("INC-TEST-NPE", report.getIncidentId());
        assertTrue(report.getRootCause().contains("Null reference"));
        assertEquals(DiagnosisSeverity.HIGH, report.getSeverity());
        assertEquals(RecoveryStrategy.RETURN_FALLBACK, report.getSuggestedRecoveryStrategy());
        assertTrue(report.getSource().contains("HEURISTIC_FALLBACK"));
    }

    @Test
    @DisplayName("Should activate heuristic fallback when Ollama throws exception or times out")
    void testFallbackOnOllamaError() throws Exception {
        AgentTelemetryEvent event = new AgentTelemetryEvent(
                "INC-TEST-TIMEOUT",
                Instant.now(),
                "PaymentGatewayService",
                "executeGatewayCall",
                "(boolean,long)",
                "com.runtimeheal.backend.sample.exception.SimulatedTimeoutException",
                "SLA breached",
                "SimulatedTimeoutException: SLA breached",
                "main",
                new String[]{"boolean", "long"}
        );

        when(llmProvider.isAvailable()).thenReturn(true);
        when(llmProvider.generate(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection timed out after 15000ms"));

        LlmDiagnosticReport report = diagnosticService.analyzeEvent(event);

        assertNotNull(report);
        assertEquals("INC-TEST-TIMEOUT", report.getIncidentId());
        assertEquals(RecoveryStrategy.RETRY, report.getSuggestedRecoveryStrategy());
        assertEquals(DiagnosisSeverity.HIGH, report.getSeverity());
        assertTrue(report.getSource().contains("HEURISTIC_FALLBACK"));
    }

    @Test
    @DisplayName("Should handle unknown strategy by defaulting safely to NO_ACTION")
    void testUnknownStrategySafeCoercion() throws Exception {
        AgentTelemetryEvent event = new AgentTelemetryEvent(
                "INC-TEST-UNKNOWN",
                Instant.now(),
                "OrderService",
                "process",
                "()",
                "java.lang.Exception",
                "Error",
                "Exception",
                "main",
                new String[0]
        );

        when(llmProvider.isAvailable()).thenReturn(true);

        String mockResponse = "{\n" +
                "  \"rootCause\": \"Unknown error\",\n" +
                "  \"explanation\": \"Something failed\",\n" +
                "  \"severity\": \"LOW\",\n" +
                "  \"confidenceScore\": 0.50,\n" +
                "  \"suggestedRecoveryStrategy\": \"EXECUTE_CUSTOM_SCRIPT\",\n" +
                "  \"reasonForSuggestion\": \"Unsafe arbitrary instruction\"\n" +
                "}";

        when(llmProvider.generate(anyString(), anyString()))
                .thenReturn(new LlmProviderResponse("qwen2.5:3b", mockResponse));

        LlmDiagnosticReport report = diagnosticService.analyzeEvent(event);

        // Security rule: Unrecognized strategy MUST default to NO_ACTION
        assertEquals(RecoveryStrategy.NO_ACTION, report.getSuggestedRecoveryStrategy());
    }

    @Test
    @DisplayName("Should label malformed provider output as heuristic rather than report it as AI diagnosis")
    void testMalformedProviderOutputIsExplicitlyLabeled() throws Exception {
        AgentTelemetryEvent event = new AgentTelemetryEvent(
                "INC-TEST-MALFORMED",
                Instant.now(),
                "OrderService",
                "process",
                "()",
                "java.lang.Exception",
                "Error",
                "Exception",
                "main",
                new String[0]
        );
        when(llmProvider.isAvailable()).thenReturn(true);
        when(llmProvider.generate(anyString(), anyString()))
                .thenReturn(new LlmProviderResponse("qwen2.5:3b", "{\"rootCause\":\"partial\"}"));

        LlmDiagnosticReport report = diagnosticService.analyzeEvent(event);

        assertTrue(report.getSource().contains("HEURISTIC_FALLBACK"));
        assertTrue(report.getRawModelResponse().contains("missing required field"));
    }

    @Test
    @DisplayName("analyzeIncident should return empty when incident does not exist in store")
    void testAnalyzeIncidentNotFound() {
        Optional<LlmDiagnosticReport> result = diagnosticService.analyzeIncident("NON_EXISTENT_ID");
        assertTrue(result.isEmpty());
    }
}
