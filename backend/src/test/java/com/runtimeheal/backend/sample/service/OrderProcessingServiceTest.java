package com.runtimeheal.backend.sample.service;

import com.runtimeheal.backend.sample.dto.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderProcessingServiceTest {

    private OrderProcessingService orderService;

    @BeforeEach
    void setUp() {
        com.runtimeheal.agent.recovery.RecoveryCoordinator.setEnabled(false);
        orderService = new OrderProcessingService();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        com.runtimeheal.agent.recovery.RecoveryCoordinator.setEnabled(true);
    }

    @Test
    @DisplayName("calculateDiscountRatio should return correct ratio when divisor is non-zero")
    void testCalculateDiscountRatio_Success() {
        int result = orderService.calculateDiscountRatio(100, 5);
        assertEquals(20, result);
    }

    @Test
    @DisplayName("calculateDiscountRatio should throw ArithmeticException when divisor is 0")
    void testCalculateDiscountRatio_DivisionByZero() {
        assertThrows(ArithmeticException.class, () -> orderService.calculateDiscountRatio(100, 0));
    }

    @Test
    @DisplayName("retrieveCustomerPreference should return currency for valid account with preferences")
    void testRetrieveCustomerPreference_Success() {
        String currency = orderService.retrieveCustomerPreference("ACC-100");
        assertEquals("USD", currency);
    }

    @Test
    @DisplayName("retrieveCustomerPreference should throw NullPointerException for unknown account")
    void testRetrieveCustomerPreference_UnknownAccount_NPE() {
        assertThrows(NullPointerException.class, () -> orderService.retrieveCustomerPreference("UNKNOWN-999"));
    }

    @Test
    @DisplayName("retrieveCustomerPreference should throw NullPointerException when account preferences map is null")
    void testRetrieveCustomerPreference_NullPreferencesMap_NPE() {
        assertThrows(NullPointerException.class, () -> orderService.retrieveCustomerPreference("ACC-200"));
    }

    @Test
    @DisplayName("validateAndProcessTransfer should succeed for valid payment request")
    void testValidateAndProcessTransfer_Success() {
        PaymentRequest request = new PaymentRequest("ACC-100", new BigDecimal("150.50"), "USD");
        Map<String, Object> response = orderService.validateAndProcessTransfer(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.get("status"));
        assertEquals("ACC-100", response.get("accountId"));
        assertEquals(new BigDecimal("150.50"), response.get("amount"));
        assertEquals("USD", response.get("currency"));
        assertTrue(response.containsKey("transactionId"));
    }

    @Test
    @DisplayName("validateAndProcessTransfer should throw IllegalArgumentException for null request")
    void testValidateAndProcessTransfer_NullRequest() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.validateAndProcessTransfer(null));
        assertTrue(ex.getMessage().contains("cannot be null"));
    }

    @Test
    @DisplayName("validateAndProcessTransfer should throw IllegalArgumentException for blank accountId")
    void testValidateAndProcessTransfer_BlankAccountId() {
        PaymentRequest request = new PaymentRequest("", new BigDecimal("100"), "USD");
        assertThrows(IllegalArgumentException.class, () -> orderService.validateAndProcessTransfer(request));
    }

    @Test
    @DisplayName("validateAndProcessTransfer should throw IllegalArgumentException for non-positive amount")
    void testValidateAndProcessTransfer_NegativeAmount() {
        PaymentRequest request = new PaymentRequest("ACC-100", new BigDecimal("-50.00"), "USD");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.validateAndProcessTransfer(request));
        assertTrue(ex.getMessage().contains("strictly greater than 0"));
    }

    @Test
    @DisplayName("validateAndProcessTransfer should throw IllegalArgumentException for invalid currency code")
    void testValidateAndProcessTransfer_InvalidCurrency() {
        PaymentRequest request = new PaymentRequest("ACC-100", new BigDecimal("100"), "US");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.validateAndProcessTransfer(request));
        assertTrue(ex.getMessage().contains("3-letter ISO code"));
    }
}
