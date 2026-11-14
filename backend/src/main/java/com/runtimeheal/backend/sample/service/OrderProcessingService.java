package com.runtimeheal.backend.sample.service;

import com.runtimeheal.backend.sample.dto.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample business service providing order and financial processing logic.
 * Contains realistic methods where arithmetic, null-pointer, and argument errors occur.
 */
@Service
public class OrderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    // Mock in-memory database of registered customer preferences
    private final Map<String, CustomerProfile> customerDatabase = new HashMap<>();

    public OrderProcessingService() {
        // Seed valid demo accounts
        customerDatabase.put("ACC-100", new CustomerProfile("ACC-100", "Gold Tier", Map.of("currency", "USD", "autoHeal", "true")));
        customerDatabase.put("ACC-200", new CustomerProfile("ACC-200", "Silver Tier", null)); // null preferences map triggers NPE on access
    }

    /**
     * Calculates the unit discount allocation across items.
     * Throws ArithmeticException if divisor is 0.
     */
    public int calculateDiscountRatio(int totalAmount, int divisor) {
        log.info("Calculating discount ratio for total: {}, divisor: {}", totalAmount, divisor);
        if (divisor == 0) {
            log.warn("Attempted division by zero: divisor=0");
        }
        // Deliberate integer division to trigger ArithmeticException when divisor is 0
        return totalAmount / divisor;
    }

    /**
     * Retrieves the preferred currency for a customer account.
     * Throws NullPointerException when accessing uninitialized profile or nested preference map.
     */
    public String retrieveCustomerPreference(String accountId) {
        log.info("Retrieving customer preferences for account: {}", accountId);

        CustomerProfile profile = customerDatabase.get(accountId);

        // Scenario 1: Unknown account returns null profile
        if (profile == null) {
            log.warn("Account {} not found in customer database; dereferencing null profile", accountId);
            return profile.getTierName().toLowerCase(); // Intentionally dereference null -> NullPointerException
        }

        // Scenario 2: Account exists but preferences map is null (ACC-200)
        log.info("Account {} found. Accessing nested preferences map", accountId);
        return profile.getPreferences().get("currency"); // Intentionally dereference null preferences -> NullPointerException
    }

    /**
     * Validates and processes a payment transfer request.
     * Throws IllegalArgumentException if arguments violate business constraints.
     */
    public Map<String, Object> validateAndProcessTransfer(PaymentRequest request) {
        log.info("Validating payment transfer request for account: {}", request != null ? request.getAccountId() : "null");

        if (request == null) {
            throw new IllegalArgumentException("Payment request payload cannot be null");
        }

        if (request.getAccountId() == null || request.getAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("Account identifier cannot be blank or missing");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly greater than 0. Received: " + request.getAmount());
        }

        if (request.getCurrency() == null || request.getCurrency().trim().length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-letter ISO code (e.g. USD, EUR, INR). Received: " + request.getCurrency());
        }

        log.info("Payment successfully processed for account {} with amount {} {}",
                request.getAccountId(), request.getAmount(), request.getCurrency());

        return Map.of(
                "status", "SUCCESS",
                "accountId", request.getAccountId(),
                "amount", request.getAmount(),
                "currency", request.getCurrency(),
                "transactionId", "TXN-" + System.currentTimeMillis()
        );
    }

    /**
     * Internal customer representation for demo data.
     */
    public static class CustomerProfile {
        private final String accountId;
        private final String tierName;
        private final Map<String, String> preferences;

        public CustomerProfile(String accountId, String tierName, Map<String, String> preferences) {
            this.accountId = accountId;
            this.tierName = tierName;
            this.preferences = preferences;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getTierName() {
            return tierName;
        }

        public Map<String, String> getPreferences() {
            return preferences;
        }
    }
}
