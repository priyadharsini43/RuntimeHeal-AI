package com.runtimeheal.backend.llm.prompt;

import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticPromptBuilderTest {

    private final DiagnosticPromptBuilder promptBuilder = new DiagnosticPromptBuilder();

    @Test
    @DisplayName("Prompt should contain structured FailureContext information and JSON format schema")
    void testBuildPromptWithEvent() {
        AgentTelemetryEvent event = new AgentTelemetryEvent(
                "INC-TEST-001",
                Instant.now(),
                "com.runtimeheal.backend.sample.service.OrderProcessingService",
                "calculateDiscountRatio",
                "(int,int)",
                "java.lang.ArithmeticException",
                "/ by zero",
                "java.lang.ArithmeticException: / by zero\n\tat OrderProcessingService.java:40\n\tat Controller.java:38",
                "http-thread-1",
                new String[]{"java.lang.Integer", "java.lang.Integer"}
        );

        String prompt = promptBuilder.buildPrompt(event);

        assertNotNull(prompt);
        assertTrue(prompt.contains("INC-TEST-001"));
        assertTrue(prompt.contains("java.lang.ArithmeticException"));
        assertTrue(prompt.contains("/ by zero"));
        assertTrue(prompt.contains("OrderProcessingService"));
        assertTrue(prompt.contains("calculateDiscountRatio"));
        assertTrue(prompt.contains("java.lang.Integer"));
        assertTrue(prompt.contains("rootCause"));
        assertTrue(prompt.contains("suggestedRecoveryStrategy"));
        assertTrue(prompt.contains("confidenceScore"));
    }

    @Test
    @DisplayName("Prompt builder should safely handle null event")
    void testBuildPromptWithNull() {
        String prompt = promptBuilder.buildPrompt(null);
        assertEquals("No failure context available.", prompt);
    }

    @Test
    @DisplayName("truncateStackTrace should filter internal reflection noise and limit frame count")
    void testTruncateStackTrace() {
        String rawStack = "java.lang.NullPointerException: null\n" +
                "\tat com.runtimeheal.Service.call(Service.java:10)\n" +
                "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat com.runtimeheal.Controller.handle(Controller.java:20)\n" +
                "\tat org.apache.catalina.core.Valve.invoke(Valve.java:50)";

        String truncated = promptBuilder.truncateStackTrace(rawStack, 2);
        assertFalse(truncated.contains("jdk.internal"));
        assertTrue(truncated.contains("Service.call"));
    }
}
