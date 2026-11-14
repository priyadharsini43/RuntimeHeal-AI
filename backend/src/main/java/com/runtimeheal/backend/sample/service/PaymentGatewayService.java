package com.runtimeheal.backend.sample.service;

import com.runtimeheal.backend.sample.exception.SimulatedTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service simulating communication with an external payment gateway.
 * Demonstrates transient network latency and timeout scenarios.
 */
@Service
public class PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayService.class);
    private static final long DEFAULT_SLA_TIMEOUT_MILLIS = 1000L;

    /**
     * Executes a gateway query or authorization.
     * Throws SimulatedTimeoutException if simulateSlow is true.
     */
    public Map<String, Object> executeGatewayCall(boolean simulateSlow, long delayMillis) {
        log.info("Executing external payment gateway call. simulateSlow: {}, requestedDelay: {} ms",
                simulateSlow, delayMillis);

        if (simulateSlow) {
            long effectiveDelay = delayMillis > 0 ? delayMillis : 3000L;
            log.warn("Simulated network latency active. Target provider failed SLA of {} ms (waited {} ms)",
                    DEFAULT_SLA_TIMEOUT_MILLIS, effectiveDelay);

            throw new SimulatedTimeoutException(
                    "Connection to external payment processor timed out after " + effectiveDelay + " ms. Service unavailable.",
                    "External-Payment-Gateway",
                    effectiveDelay
            );
        }

        log.info("Payment gateway responded promptly within SLA limit.");
        return Map.of(
                "gatewayStatus", "CONNECTED",
                "provider", "Stripe-Mock-Gateway",
                "responseTimeMillis", 85,
                "message", "Payment gateway operational."
        );
    }
}
