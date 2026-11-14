package com.runtimeheal.agent;

import com.runtimeheal.agent.telemetry.CapturedFailureEvent;
import com.runtimeheal.agent.telemetry.TelemetryDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeHealAgentTest {

    @BeforeAll
    static void init() {
        // Disable external HTTP dispatch during unit test execution
        TelemetryDispatcher.setHttpDispatchEnabled(false);
    }

    @AfterEach
    void clean() {
        TelemetryDispatcher.clearHistory();
    }

    @Test
    @DisplayName("TelemetryDispatcher should store captured failure event with exact details")
    void testTelemetryDispatcherCapture() {
        ArithmeticException ex = new ArithmeticException("/ by zero in test calculation");

        TelemetryDispatcher.captureAndDispatch(
                "com.runtimeheal.backend.sample.service.OrderProcessingService",
                "calculateDiscountRatio",
                "(II)I",
                new Object[]{100, 0},
                ex
        );

        List<CapturedFailureEvent> history = TelemetryDispatcher.getEventHistory();
        assertEquals(1, history.size());

        CapturedFailureEvent event = history.get(0);
        assertTrue(event.getIncidentId().startsWith("INC-AGENT-"));
        assertEquals("com.runtimeheal.backend.sample.service.OrderProcessingService", event.getClassName());
        assertEquals("calculateDiscountRatio", event.getMethodName());
        assertEquals("(II)I", event.getMethodSignature());
        assertEquals("java.lang.ArithmeticException", event.getExceptionType());
        assertEquals("/ by zero in test calculation", event.getExceptionMessage());
        assertTrue(event.getStackTrace().contains("ArithmeticException"));
        assertEquals(2, event.getArgumentTypes().length);
        assertEquals("java.lang.Integer", event.getArgumentTypes()[0]);
    }

    @Test
    @DisplayName("CapturedFailureEvent handles null thrown safely without crashing")
    void testCapturedFailureEventWithNull() {
        CapturedFailureEvent event = new CapturedFailureEvent(
                "TestClass",
                "testMethod",
                "()V",
                null,
                null
        );

        assertEquals("Unknown", event.getExceptionType());
        assertEquals("", event.getExceptionMessage());
        assertEquals("", event.getStackTrace());
        assertEquals(0, event.getArgumentTypes().length);
    }
}
