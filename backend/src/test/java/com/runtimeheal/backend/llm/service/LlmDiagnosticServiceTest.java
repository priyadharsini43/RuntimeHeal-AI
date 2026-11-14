package com.runtimeheal.backend.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.llm.client.OllamaClient;
import com.runtimeheal.backend.llm.model.DiagnosisSeverity;
import com.runtimeheal.backend.llm.model.LlmDiagnosticReport;
import com.runtimeheal.backend.llm.model.OllamaGenerateResponse;
import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import com.runtimeheal.backend.llm.prompt.DiagnosticPromptBuilder;
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

    private OllamaClient ollamaClient;
    private DiagnosticPromptBuilder promptBuilder;
    private TelemetryStoreService telemetryStore;
    private ObjectMapper objectMapper;
    private LlmDiagnosticService diagnosticService;

    @BeforeEach
    void setUp() {
        ollamaClient = Mockito.mock(OllamaClient.class);
        promptBuilder = new DiagnosticPromptBuilder();
        telemetryStore = new TelemetryStoreService();
        objectMapper = new ObjectMapper();
        diagnosticService = new LlmDiagnosticService(ollamaClient, promptBuilder, telemetryStore, objectMapper);
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

        when(ollamaClient.isAvailable()).thenReturn(true);

        OllamaGenerateResponse mockResponse = new OllamaGenerateResponse();
        mockResponse.setModel("qwen2.5:3b");
        mockResponse.setDone(true);
        mockResponse.setResponse("{\n" +
                "  \"rootCause\": \"Division by zero in discount computation\",\n" +
                "  \"explanation\": \"Divisor parameter was zero causing ArithmeticException\",\n" +
                "  \"severity\": \"MEDIUM\",\n" +
                "  \"confidenceScore\": 0.95,\n" +
                "  \"suggestedRecoveryStrategy\": \"RETURN_FALLBACK\",\n" +
                "  \"reasonForSuggestion\": \"Safe fallback of zero prevents crash\"\n" +
                "}");

        when(ollamaClient.generate(anyString(), anyString())).thenReturn(mockResponse);

        LlmDiagnosticReport report = diagnosticService.analyzeEvent(event);

        assertNotNull(report);
        assertEquals("INC-TEST-ARITH", report.getIncidentId());
        assertEquals("Division by zero in discount computation", report.getRootCause());
        assertEquals(DiagnosisSeverity.MEDIUM, report.getSeverity());
        assertEquals(0.95, report.getConfidenceScore(), 0.001);
        assertEquals(RecoveryStrategy.RETURN_FALLBACK, report.getSuggestedRecoveryStrategy());
        assertTrue(report.getSource().contains("LOCAL_OLLAMA"));
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

        when(ollamaClient.isAvailable()).thenReturn(false);

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

        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.generate(anyString(), anyString())).thenThrow(new RuntimeException("Connection timed out after 15000ms"));

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

        when(ollamaClient.isAvailable()).thenReturn(true);

        OllamaGenerateResponse mockResponse = new OllamaGenerateResponse();
        mockResponse.setResponse("{\n" +
                "  \"rootCause\": \"Unknown error\",\n" +
                "  \"explanation\": \"Something failed\",\n" +
                "  \"severity\": \"LOW\",\n" +
                "  \"confidenceScore\": 0.50,\n" +
                "  \"suggestedRecoveryStrategy\": \"EXECUTE_CUSTOM_SCRIPT\",\n" +
                "  \"reasonForSuggestion\": \"Unsafe arbitrary instruction\"\n" +
                "}");

        when(ollamaClient.generate(anyString(), anyString())).thenReturn(mockResponse);

        LlmDiagnosticReport report = diagnosticService.analyzeEvent(event);

        // Security rule: Unrecognized strategy MUST default to NO_ACTION
        assertEquals(RecoveryStrategy.NO_ACTION, report.getSuggestedRecoveryStrategy());
    }

    @Test
    @DisplayName("analyzeIncident should return empty when incident does not exist in store")
    void testAnalyzeIncidentNotFound() {
        Optional<LlmDiagnosticReport> result = diagnosticService.analyzeIncident("NON_EXISTENT_ID");
        assertTrue(result.isEmpty());
    }
}
