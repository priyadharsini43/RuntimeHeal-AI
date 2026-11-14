package com.runtimeheal.backend.llm.prompt;

import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Constructs structured, schema-constrained diagnostic prompts from captured FailureContext telemetry.
 */
@Component
public class DiagnosticPromptBuilder {

    public static final String SYSTEM_PROMPT =
            "You are RuntimeHeal AI, an expert Java runtime diagnostics engine.\n" +
            "Analyze the provided Java runtime exception telemetry (FailureContext).\n" +
            "Diagnose the root cause, determine technical explanation, assign a severity (LOW, MEDIUM, HIGH, CRITICAL),\n" +
            "provide a confidence score between 0.0 and 1.0, and recommend a bounded recovery strategy from ONLY\n" +
            "the following allowed list: [NO_ACTION, RETURN_FALLBACK, RETRY, SKIP_OPERATION].\n" +
            "Strategy guidelines:\n" +
            "- RETURN_FALLBACK: Recommended for deterministic computational or reference exceptions (such as ArithmeticException, NullPointerException) where returning a safe default value restores normal execution.\n" +
            "- RETRY: Use ONLY for transient network, IO, or concurrency exceptions (such as TimeoutException, SocketException) that may succeed upon re-execution.\n" +
            "- SKIP_OPERATION: Use for non-critical void methods that can be safely bypassed.\n" +
            "- NO_ACTION: Use when no safe automated recovery is possible without risking data integrity.\n" +
            "Output strictly valid JSON matching the exact schema specified, with no markdown code blocks or surrounding text.";

    /**
     * Builds the user prompt containing formatted incident telemetry and output schema constraints.
     */
    public String buildPrompt(AgentTelemetryEvent event) {
        if (event == null) {
            return "No failure context available.";
        }

        String truncatedStack = truncateStackTrace(event.getStackTrace(), 8);
        String argTypes = (event.getArgumentTypes() != null && event.getArgumentTypes().length > 0)
                ? String.join(", ", event.getArgumentTypes())
                : "None";

        StringBuilder sb = new StringBuilder();
        sb.append("=== RUNTIME FAILURE CONTEXT ===\n");
        sb.append("Incident ID: ").append(event.getIncidentId()).append("\n");
        sb.append("Exception Type: ").append(event.getExceptionType()).append("\n");
        sb.append("Exception Message: ").append(event.getExceptionMessage()).append("\n");
        sb.append("Target Class: ").append(event.getClassName()).append("\n");
        sb.append("Target Method: ").append(event.getMethodName()).append("\n");
        sb.append("Method Signature: ").append(event.getMethodSignature()).append("\n");
        sb.append("Argument Types: ").append(argTypes).append("\n");
        sb.append("Relevant Stack Trace Frames:\n").append(truncatedStack).append("\n\n");

        sb.append("=== REQUIRED OUTPUT FORMAT (STRICT JSON ONLY) ===\n");
        sb.append("{\n");
        sb.append("  \"rootCause\": \"<concise one-sentence root cause>\",\n");
        sb.append("  \"explanation\": \"<detailed technical explanation of what caused the exception>\",\n");
        sb.append("  \"severity\": \"<LOW | MEDIUM | HIGH | CRITICAL>\",\n");
        sb.append("  \"confidenceScore\": <number between 0.0 and 1.0>,\n");
        sb.append("  \"suggestedRecoveryStrategy\": \"<NO_ACTION | RETURN_FALLBACK | RETRY | SKIP_OPERATION>\",\n");
        sb.append("  \"reasonForSuggestion\": \"<explanation of why this recovery strategy is safe and optimal>\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Truncates raw stack trace to top N meaningful frames, filtering noise.
     */
    public String truncateStackTrace(String fullStackTrace, int maxFrames) {
        if (fullStackTrace == null || fullStackTrace.trim().isEmpty()) {
            return "No stack trace recorded.";
        }

        String[] lines = fullStackTrace.split("\r?\n");
        return Arrays.stream(lines)
                .filter(line -> !line.contains("jdk.internal") && !line.contains("java.base/jdk"))
                .limit(maxFrames + 1L)
                .collect(Collectors.joining("\n"));
    }
}
