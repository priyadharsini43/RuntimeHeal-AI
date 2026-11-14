package com.runtimeheal.backend.sample.controller;

import com.runtimeheal.backend.sample.dto.PaymentRequest;
import com.runtimeheal.backend.sample.service.OrderProcessingService;
import com.runtimeheal.backend.sample.service.PaymentGatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing controlled runtime exception trigger scenarios.
 * Used for testing, demonstration, and future ByteBuddy instrumentation.
 */
@RestController
@RequestMapping("/api/sample")
@CrossOrigin(origins = "*")
public class SampleScenarioController {

    private final OrderProcessingService orderService;
    private final PaymentGatewayService paymentService;

    public SampleScenarioController(OrderProcessingService orderService, PaymentGatewayService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    /**
     * Scenario 1: ArithmeticException (Division by zero)
     * GET /api/sample/arithmetic?total=100&divisor=0
     */
    @GetMapping("/arithmetic")
    public ResponseEntity<Map<String, Object>> triggerArithmetic(
            @RequestParam(name = "total", defaultValue = "100") int total,
            @RequestParam(name = "divisor", defaultValue = "0") int divisor) {

        int result = orderService.calculateDiscountRatio(total, divisor);
        return ResponseEntity.ok(Map.of(
                "total", total,
                "divisor", divisor,
                "discountRatio", result,
                "status", "SUCCESS"
        ));
    }

    /**
     * Scenario 2: NullPointerException (Missing account profile or null preferences)
     * GET /api/sample/npe?accountId=UNKNOWN-999
     */
    @GetMapping("/npe")
    public ResponseEntity<Map<String, Object>> triggerNullPointer(
            @RequestParam(name = "accountId", defaultValue = "UNKNOWN-999") String accountId) {

        String preferredCurrency = orderService.retrieveCustomerPreference(accountId);
        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "preferredCurrency", preferredCurrency,
                "status", "SUCCESS"
        ));
    }

    /**
     * Scenario 3: IllegalArgumentException (Invalid business constraint / negative amount)
     * POST /api/sample/illegal-argument
     */
    @PostMapping("/illegal-argument")
    public ResponseEntity<Map<String, Object>> triggerIllegalArgument(
            @RequestBody PaymentRequest request) {

        Map<String, Object> result = orderService.validateAndProcessTransfer(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Scenario 4: Simulated Timeout / Transient failure
     * GET /api/sample/timeout?simulateSlow=true&delayMillis=3000
     */
    @GetMapping("/timeout")
    public ResponseEntity<Map<String, Object>> triggerTimeout(
            @RequestParam(name = "simulateSlow", defaultValue = "true") boolean simulateSlow,
            @RequestParam(name = "delayMillis", defaultValue = "3000") long delayMillis) {

        Map<String, Object> response = paymentService.executeGatewayCall(simulateSlow, delayMillis);
        return ResponseEntity.ok(response);
    }

    /**
     * Scenario Catalog: Lists all available fault injection scenarios for the dashboard Test Lab.
     * GET /api/sample/scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<List<Map<String, Object>>> listScenarios() {
        List<Map<String, Object>> scenarios = List.of(
                Map.of(
                        "id", "SCENARIO-ARITHMETIC",
                        "title", "Arithmetic Exception (Division by Zero)",
                        "exceptionClass", "java.lang.ArithmeticException",
                        "method", "GET",
                        "endpoint", "/api/sample/arithmetic?total=100&divisor=0",
                        "description", "Triggers integer division by zero in the discount ratio calculation.",
                        "supportedRecovery", "RETURN_FALLBACK"
                ),
                Map.of(
                        "id", "SCENARIO-NPE",
                        "title", "NullPointerException (Missing Profile)",
                        "exceptionClass", "java.lang.NullPointerException",
                        "method", "GET",
                        "endpoint", "/api/sample/npe?accountId=UNKNOWN-999",
                        "description", "Triggers null pointer dereference on uninitialized customer profile preferences.",
                        "supportedRecovery", "RETURN_FALLBACK"
                ),
                Map.of(
                        "id", "SCENARIO-ILLEGAL-ARG",
                        "title", "IllegalArgumentException (Invalid Negative Amount)",
                        "exceptionClass", "java.lang.IllegalArgumentException",
                        "method", "POST",
                        "endpoint", "/api/sample/illegal-argument",
                        "description", "Triggers argument validation constraint failure when transfer amount <= 0.",
                        "supportedRecovery", "NO_ACTION"
                ),
                Map.of(
                        "id", "SCENARIO-TIMEOUT",
                        "title", "Simulated Gateway Timeout",
                        "exceptionClass", "com.runtimeheal.backend.sample.exception.SimulatedTimeoutException",
                        "method", "GET",
                        "endpoint", "/api/sample/timeout?simulateSlow=true&delayMillis=3000",
                        "description", "Simulates transient third-party payment gateway network timeout exceeding SLA.",
                        "supportedRecovery", "RETRY"
                )
        );
        return ResponseEntity.ok(scenarios);
    }
}
