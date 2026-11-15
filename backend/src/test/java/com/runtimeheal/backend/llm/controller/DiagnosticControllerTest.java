package com.runtimeheal.backend.llm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import com.runtimeheal.backend.telemetry.service.TelemetryStoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiagnosticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryStoreService telemetryStore;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/diagnostics/health should return active provider status and model configuration")
    void testGetHealth() throws Exception {
        mockMvc.perform(get("/api/diagnostics/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider", is("ollama")))
                .andExpect(jsonPath("$.service", is("Local Ollama")))
                .andExpect(jsonPath("$.model", is("qwen2.5:3b")))
                .andExpect(jsonPath("$.status", anyOf(is("CONNECTED"), is("UNREACHABLE"), is("NOT_CONFIGURED"))));
    }

    @Test
    @DisplayName("POST /api/diagnostics/analyze/{incidentId} should return 404 for unknown incident")
    void testAnalyzeUnknownIncident() throws Exception {
        mockMvc.perform(post("/api/diagnostics/analyze/NON_EXISTENT_INCIDENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/diagnostics/analyze/{incidentId} should diagnose recorded incident")
    void testAnalyzeRecordedIncident() throws Exception {
        AgentTelemetryEvent event = new AgentTelemetryEvent(
                "INC-MOCK-TEST-100",
                Instant.now(),
                "com.runtimeheal.backend.sample.service.OrderProcessingService",
                "calculateDiscountRatio",
                "(int,int)",
                "java.lang.ArithmeticException",
                "/ by zero",
                "java.lang.ArithmeticException: / by zero",
                "main",
                new String[]{"int", "int"}
        );

        telemetryStore.recordIncident(event);

        mockMvc.perform(post("/api/diagnostics/analyze/INC-MOCK-TEST-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId", is("INC-MOCK-TEST-100")))
                .andExpect(jsonPath("$.rootCause", not(emptyOrNullString())))
                .andExpect(jsonPath("$.suggestedRecoveryStrategy", not(emptyOrNullString())))
                .andExpect(jsonPath("$.confidenceScore", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.severity", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("POST /api/diagnostics/analyze with direct payload should return diagnostic report")
    void testAnalyzeDirectEvent() throws Exception {
        AgentTelemetryEvent event = new AgentTelemetryEvent(
                "INC-DIRECT-200",
                Instant.now(),
                "com.runtimeheal.backend.sample.service.OrderProcessingService",
                "retrieveCustomerPreference",
                "(String)",
                "java.lang.NullPointerException",
                "Cannot invoke profile",
                "NullPointerException",
                "main",
                new String[]{"String"}
        );

        mockMvc.perform(post("/api/diagnostics/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId", is("INC-DIRECT-200")))
                .andExpect(jsonPath("$.rootCause", not(emptyOrNullString())))
                .andExpect(jsonPath("$.suggestedRecoveryStrategy", not(emptyOrNullString())));
    }
}
