package com.runtimeheal.backend.telemetry.service;

import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryStoreServiceTest {

    private TelemetryStoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new TelemetryStoreService();
    }

    @Test
    @DisplayName("recordIncident should store and retrieve telemetry event by ID")
    void testRecordAndRetrieveIncident() {
        AgentTelemetryEvent event = new AgentTelemetryEvent(
                "INC-TEST-001",
                Instant.now(),
                "com.runtimeheal.backend.sample.service.OrderProcessingService",
                "calculateDiscountRatio",
                "(II)I",
                "java.lang.ArithmeticException",
                "/ by zero",
                "java.lang.ArithmeticException: / by zero\n\tat ...",
                "test-thread",
                new String[]{"int", "int"}
        );

        AgentTelemetryEvent saved = storeService.recordIncident(event);
        assertNotNull(saved);
        assertEquals(1, storeService.getTotalIncidentCount());

        Optional<AgentTelemetryEvent> found = storeService.getIncidentById("INC-TEST-001");
        assertTrue(found.isPresent());
        assertEquals("java.lang.ArithmeticException", found.get().getExceptionType());
        assertEquals("calculateDiscountRatio", found.get().getMethodName());
    }

    @Test
    @DisplayName("getExceptionDistribution should aggregate counts by exception class")
    void testExceptionDistribution() {
        storeService.recordIncident(new AgentTelemetryEvent("INC-1", Instant.now(), "ClassA", "m1", "()V",
                "java.lang.NullPointerException", "npe", "trace", "t1", new String[0]));
        storeService.recordIncident(new AgentTelemetryEvent("INC-2", Instant.now(), "ClassB", "m2", "()V",
                "java.lang.NullPointerException", "npe", "trace", "t1", new String[0]));
        storeService.recordIncident(new AgentTelemetryEvent("INC-3", Instant.now(), "ClassC", "m3", "()V",
                "java.lang.ArithmeticException", "div0", "trace", "t1", new String[0]));

        Map<String, Long> distribution = storeService.getExceptionDistribution();
        assertEquals(2L, distribution.get("java.lang.NullPointerException"));
        assertEquals(1L, distribution.get("java.lang.ArithmeticException"));
    }

    @Test
    @DisplayName("recordIncident should reject null event safely")
    void testRecordNullIncident() {
        assertNull(storeService.recordIncident(null));
        assertEquals(0, storeService.getTotalIncidentCount());
    }
}
