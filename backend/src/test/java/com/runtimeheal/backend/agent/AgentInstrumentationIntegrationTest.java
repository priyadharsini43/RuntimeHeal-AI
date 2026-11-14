package com.runtimeheal.backend.agent;

import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import com.runtimeheal.backend.telemetry.service.TelemetryStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentInstrumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentBootstrapService bootstrapService;

    @Autowired
    private TelemetryStoreService telemetryStore;

    @Autowired
    private com.runtimeheal.backend.recovery.config.HealingPolicyProperties policyProperties;

    @BeforeEach
    void setUp() {
        bootstrapService.initializeAgent();
        telemetryStore.clear();
        policyProperties.setEnabled(false);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        policyProperties.setEnabled(true);
    }

    @Test
    @DisplayName("GET /api/agent/status should report ACTIVE agent")
    void testAgentStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/agent/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.agentName", containsString("RuntimeHeal")))
                .andExpect(jsonPath("$.instrumentationEngine", containsString("ByteBuddy")))
                .andExpect(jsonPath("$.targetPackage", is("com.runtimeheal.backend.sample")));
    }

    @Test
    @DisplayName("Triggering arithmetic failure should be intercepted by ByteBuddy agent and stored in telemetry")
    void testAgentInterceptsArithmeticException() throws Exception {
        // Trigger the fault via REST controller
        mockMvc.perform(get("/api/sample/arithmetic")
                        .param("total", "100")
                        .param("divisor", "0"))
                .andExpect(status().isInternalServerError());

        // Verify that the ByteBuddy agent intercepted the method and recorded telemetry
        List<AgentTelemetryEvent> incidents = telemetryStore.getAllIncidents();
        assertFalse(incidents.isEmpty(), "Agent should have captured at least one incident");

        AgentTelemetryEvent event = incidents.stream()
                .filter(i -> "java.lang.ArithmeticException".equals(i.getExceptionType())
                        && i.getClassName().contains("OrderProcessingService"))
                .findFirst()
                .orElse(null);

        org.junit.jupiter.api.Assertions.assertNotNull(event, "ArithmeticException event should be captured");
        assertTrue(event.getClassName().contains("OrderProcessingService"));
        org.junit.jupiter.api.Assertions.assertEquals("calculateDiscountRatio", event.getMethodName());
        assertTrue(event.getIncidentId().startsWith("INC-AGENT-"));
        assertTrue(event.getStackTrace().contains("ArithmeticException"));
    }

    @Test
    @DisplayName("GET /api/telemetry/incidents and /summary should return captured events")
    void testTelemetryEndpoints() throws Exception {
        // Trigger NPE
        mockMvc.perform(get("/api/sample/npe")
                        .param("accountId", "UNKNOWN-999"))
                .andExpect(status().isInternalServerError());

        // Check GET /api/telemetry/incidents
        mockMvc.perform(get("/api/telemetry/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].exceptionType", is("java.lang.NullPointerException")));

        // Check GET /api/telemetry/summary
        mockMvc.perform(get("/api/telemetry/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidents", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.distribution['java.lang.NullPointerException']", greaterThanOrEqualTo(1)));
    }
}
