package com.runtimeheal.backend.sample.service;

import com.runtimeheal.backend.sample.exception.SimulatedTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaymentGatewayServiceTest {

    private PaymentGatewayService gatewayService;

    @BeforeEach
    void setUp() {
        gatewayService = new PaymentGatewayService();
    }

    @Test
    @DisplayName("executeGatewayCall should return gateway response when simulateSlow is false")
    void testExecuteGatewayCall_Success() {
        Map<String, Object> response = gatewayService.executeGatewayCall(false, 0);

        assertNotNull(response);
        assertEquals("CONNECTED", response.get("gatewayStatus"));
        assertEquals("Stripe-Mock-Gateway", response.get("provider"));
        assertEquals(85, response.get("responseTimeMillis"));
    }

    @Test
    @DisplayName("executeGatewayCall should throw SimulatedTimeoutException when simulateSlow is true")
    void testExecuteGatewayCall_ThrowsTimeoutException() {
        SimulatedTimeoutException ex = assertThrows(SimulatedTimeoutException.class,
                () -> gatewayService.executeGatewayCall(true, 4500));

        assertEquals("External-Payment-Gateway", ex.getTargetService());
        assertEquals(4500, ex.getTimeoutDurationMillis());
        assertTrue(ex.getMessage().contains("timed out after 4500 ms"));
    }

    @Test
    @DisplayName("executeGatewayCall should use default 3000ms delay when requested delay <= 0")
    void testExecuteGatewayCall_DefaultDelay() {
        SimulatedTimeoutException ex = assertThrows(SimulatedTimeoutException.class,
                () -> gatewayService.executeGatewayCall(true, 0));

        assertEquals(3000, ex.getTimeoutDurationMillis());
    }
}
