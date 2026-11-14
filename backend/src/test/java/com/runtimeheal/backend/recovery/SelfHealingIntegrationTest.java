package com.runtimeheal.backend.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.agent.AgentBootstrapService;
import com.runtimeheal.backend.recovery.config.HealingPolicyProperties;
import com.runtimeheal.backend.recovery.model.HealingActionRecord;
import com.runtimeheal.backend.recovery.model.HealingStatus;
import com.runtimeheal.backend.recovery.service.CircuitBreakerRegistry;
import com.runtimeheal.backend.recovery.service.HealingAuditService;
import com.runtimeheal.backend.sample.dto.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SelfHealingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentBootstrapService bootstrapService;

    @Autowired
    private HealingPolicyProperties policyProperties;

    @Autowired
    private HealingAuditService auditService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        bootstrapService.initializeAgent();
        com.runtimeheal.agent.recovery.RecoveryCoordinator.setEnabled(true);
        auditService.clear();
        circuitBreakerRegistry.resetAll();
        policyProperties.setEnabled(true);
        policyProperties.setMinConfidence(0.70);
    }

    @Test
    @DisplayName("End-to-End: Arithmetic division by zero should be healed automatically and return HTTP 200 with fallback value 0")
    void testEndToEndArithmeticSelfHealing() throws Exception {
        mockMvc.perform(get("/api/sample/arithmetic")
                        .param("total", "100")
                        .param("divisor", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountRatio", is(0)))
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        List<HealingActionRecord> actions = auditService.getAllActions();
        assertFalse(actions.isEmpty(), "Healing action should be recorded in audit repository");

        HealingActionRecord action = actions.get(0);
        assertEquals(HealingStatus.SUCCESS, action.getStatus());
        assertEquals("RETURN_FALLBACK", action.getStrategy().name());
        assertEquals(0, action.getFallbackValue());
    }

    @Test
    @DisplayName("End-to-End: Denylisted transactional method validateAndProcessTransfer should be blocked by policy and preserve 400 Bad Request")
    void testEndToEndDenylistEnforcement() throws Exception {
        PaymentRequest invalidRequest = new PaymentRequest();
        invalidRequest.setAccountId("ACC-100");
        invalidRequest.setAmount(new BigDecimal("-50.00")); // Negative amount triggers IllegalArgumentException
        invalidRequest.setCurrency("USD");

        mockMvc.perform(post("/api/sample/illegal-argument")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        List<HealingActionRecord> actions = auditService.getAllActions();
        assertFalse(actions.isEmpty(), "Policy block should be audited");

        HealingActionRecord action = actions.get(0);
        assertEquals(HealingStatus.BLOCKED_POLICY, action.getStatus());
        assertTrue(action.getMethodName().contains("validateAndProcessTransfer"));
    }

    @Test
    @DisplayName("End-to-End: Circuit breaker trips after threshold failures and reset restores healing capability")
    void testEndToEndCircuitBreakerTrippingAndReset() throws Exception {
        policyProperties.getCircuitBreaker().setFailureThreshold(3);

        // First 3 calls heal successfully (hitting threshold 3)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/sample/arithmetic")
                            .param("total", "100")
                            .param("divisor", "0"))
                    .andExpect(status().isOk());
        }

        // 4th call: Circuit breaker is now OPEN! Healing blocked, exception propagates as HTTP 500
        mockMvc.perform(get("/api/sample/arithmetic")
                        .param("total", "100")
                        .param("divisor", "0"))
                .andExpect(status().isInternalServerError());

        // Verify audit log has BLOCKED_CIRCUIT_BREAKER
        List<HealingActionRecord> actions = auditService.getAllActions();
        assertEquals(HealingStatus.BLOCKED_CIRCUIT_BREAKER, actions.get(0).getStatus());

        // Reset circuit breaker via API
        mockMvc.perform(post("/api/healing/circuit-breaker/reset"))
                .andExpect(status().isOk());

        // 5th call: Healing restored! Returns HTTP 200 again
        mockMvc.perform(get("/api/sample/arithmetic")
                        .param("total", "100")
                        .param("divisor", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountRatio", is(0)));
    }
}
