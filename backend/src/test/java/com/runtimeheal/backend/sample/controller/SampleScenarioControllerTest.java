package com.runtimeheal.backend.sample.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.sample.dto.PaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SampleScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        com.runtimeheal.agent.recovery.RecoveryCoordinator.setEnabled(false);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        com.runtimeheal.agent.recovery.RecoveryCoordinator.setEnabled(true);
    }

    @Test
    @DisplayName("GET /api/sample/arithmetic with valid divisor returns HTTP 200 OK")
    void testArithmetic_Success() throws Exception {
        mockMvc.perform(get("/api/sample/arithmetic")
                        .param("total", "100")
                        .param("divisor", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.discountRatio", is(20)));
    }

    @Test
    @DisplayName("GET /api/sample/arithmetic with divisor=0 returns HTTP 500 and structured error response")
    void testArithmetic_DivisionByZero_ErrorResponse() throws Exception {
        mockMvc.perform(get("/api/sample/arithmetic")
                        .param("total", "100")
                        .param("divisor", "0"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.incidentId", startsWith("INC-ARITH-")))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Arithmetic Failure")))
                .andExpect(jsonPath("$.exceptionType", is("java.lang.ArithmeticException")))
                .andExpect(jsonPath("$.message", containsString("/ by zero")))
                .andExpect(jsonPath("$.path", is("/api/sample/arithmetic")));
    }

    @Test
    @DisplayName("GET /api/sample/npe with valid account returns HTTP 200 OK")
    void testNpe_Success() throws Exception {
        mockMvc.perform(get("/api/sample/npe")
                        .param("accountId", "ACC-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.preferredCurrency", is("USD")));
    }

    @Test
    @DisplayName("GET /api/sample/npe with unknown account returns HTTP 500 and structured error response")
    void testNpe_UnknownAccount_ErrorResponse() throws Exception {
        mockMvc.perform(get("/api/sample/npe")
                        .param("accountId", "UNKNOWN-999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.incidentId", startsWith("INC-NPE-")))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Null Pointer Dereference")))
                .andExpect(jsonPath("$.exceptionType", is("java.lang.NullPointerException")))
                .andExpect(jsonPath("$.path", is("/api/sample/npe")));
    }

    @Test
    @DisplayName("POST /api/sample/illegal-argument with valid payload returns HTTP 200 OK")
    void testIllegalArgument_Success() throws Exception {
        PaymentRequest request = new PaymentRequest("ACC-100", new BigDecimal("75.00"), "USD");

        mockMvc.perform(post("/api/sample/illegal-argument")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.accountId", is("ACC-100")))
                .andExpect(jsonPath("$.amount", is(75.00)));
    }

    @Test
    @DisplayName("POST /api/sample/illegal-argument with negative amount returns HTTP 400 and structured error response")
    void testIllegalArgument_NegativeAmount_ErrorResponse() throws Exception {
        PaymentRequest request = new PaymentRequest("ACC-100", new BigDecimal("-25.00"), "USD");

        mockMvc.perform(post("/api/sample/illegal-argument")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.incidentId", startsWith("INC-ARG-")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Invalid Business Argument")))
                .andExpect(jsonPath("$.exceptionType", is("java.lang.IllegalArgumentException")))
                .andExpect(jsonPath("$.message", containsString("strictly greater than 0")))
                .andExpect(jsonPath("$.path", is("/api/sample/illegal-argument")));
    }

    @Test
    @DisplayName("GET /api/sample/timeout with simulateSlow=false returns HTTP 200 OK")
    void testTimeout_Success() throws Exception {
        mockMvc.perform(get("/api/sample/timeout")
                        .param("simulateSlow", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayStatus", is("CONNECTED")))
                .andExpect(jsonPath("$.provider", is("Stripe-Mock-Gateway")));
    }

    @Test
    @DisplayName("GET /api/sample/timeout with simulateSlow=true returns HTTP 504 Gateway Timeout")
    void testTimeout_SimulateSlow_ErrorResponse() throws Exception {
        mockMvc.perform(get("/api/sample/timeout")
                        .param("simulateSlow", "true")
                        .param("delayMillis", "3500"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.incidentId", startsWith("INC-TIMEOUT-")))
                .andExpect(jsonPath("$.status", is(504)))
                .andExpect(jsonPath("$.error", is("Service Gateway Timeout")))
                .andExpect(jsonPath("$.exceptionType", is("com.runtimeheal.backend.sample.exception.SimulatedTimeoutException")))
                .andExpect(jsonPath("$.message", containsString("timed out after 3500 ms")))
                .andExpect(jsonPath("$.path", is("/api/sample/timeout")));
    }

    @Test
    @DisplayName("GET /api/sample/scenarios returns list of available test scenarios")
    void testListScenarios() throws Exception {
        mockMvc.perform(get("/api/sample/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].id", hasItems("SCENARIO-ARITHMETIC", "SCENARIO-NPE", "SCENARIO-ILLEGAL-ARG", "SCENARIO-TIMEOUT")));
    }
}
