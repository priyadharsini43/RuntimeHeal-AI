package com.runtimeheal.backend.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import com.runtimeheal.backend.recovery.model.HealingActionRecord;
import com.runtimeheal.backend.recovery.model.HealingStatus;
import com.runtimeheal.backend.recovery.service.HealingAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealingAuditService auditService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/healing/policy should return safety configuration and circuit breaker states")
    void testGetPolicy() throws Exception {
        mockMvc.perform(get("/api/healing/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.minConfidence", is(0.70)))
                .andExpect(jsonPath("$.denylist", hasItem("validateAndProcessTransfer")))
                .andExpect(jsonPath("$.circuitBreakerConfig.failureThreshold", is(5)));
    }

    @Test
    @DisplayName("POST /api/healing/policy should update runtime guardrail parameters")
    void testUpdatePolicy() throws Exception {
        Map<String, Object> updates = Map.of(
                "enabled", false,
                "minConfidence", 0.85
        );

        mockMvc.perform(post("/api/healing/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(false)))
                .andExpect(jsonPath("$.minConfidence", is(0.85)));

        // Restore default settings
        Map<String, Object> restore = Map.of(
                "enabled", true,
                "minConfidence", 0.70
        );
        mockMvc.perform(post("/api/healing/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restore)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/healing/actions and /summary should return recorded self-healing metrics")
    void testGetActionsAndSummary() throws Exception {
        auditService.recordAction(new HealingActionRecord(
                "ACTION-TEST-1", "INC-TEST-1",
                "com.runtimeheal.SampleService", "testMethod", "()I",
                "java.lang.ArithmeticException", RecoveryStrategy.RETURN_FALLBACK,
                HealingStatus.SUCCESS, 0.85, "Fallback 0 applied", 12L, 0
        ));

        mockMvc.perform(get("/api/healing/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].actionId", notNullValue()));

        mockMvc.perform(get("/api/healing/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHealingAttempts", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.successfulHeals", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("POST /api/healing/circuit-breaker/reset should reset circuit breakers")
    void testResetCircuitBreaker() throws Exception {
        mockMvc.perform(post("/api/healing/circuit-breaker/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ALL_RESET")));
    }
}
