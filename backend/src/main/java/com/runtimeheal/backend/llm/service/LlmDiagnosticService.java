package com.runtimeheal.backend.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.llm.model.DiagnosisSeverity;
import com.runtimeheal.backend.llm.model.LlmDiagnosticReport;
import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import com.runtimeheal.backend.llm.prompt.DiagnosticPromptBuilder;
import com.runtimeheal.backend.llm.provider.DiagnosticLlmProvider;
import com.runtimeheal.backend.llm.provider.LlmProviderResponse;
import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import com.runtimeheal.backend.telemetry.service.TelemetryStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.runtimeheal.backend.persistence.entity.DiagnosticReportEntity;
import com.runtimeheal.backend.persistence.repository.DiagnosticReportRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core diagnostic service that orchestrates provider-backed root-cause analysis
 * and labels heuristic reports explicitly when the provider is unavailable.
 */
@Service
public class LlmDiagnosticService {

    private static final Logger log = LoggerFactory.getLogger(LlmDiagnosticService.class);
    private static final int MAX_DIAGNOSTIC_REPORTS = 1000;

    private final DiagnosticLlmProvider llmProvider;
    private final DiagnosticPromptBuilder promptBuilder;
    private final TelemetryStoreService telemetryStore;
    private final ObjectMapper objectMapper;
    private final DiagnosticReportRepository diagnosticReportRepository;

    private final Map<String, LlmDiagnosticReport> diagnosticHistory = new ConcurrentHashMap<>();

    public LlmDiagnosticService(DiagnosticLlmProvider llmProvider,
                                DiagnosticPromptBuilder promptBuilder,
                                TelemetryStoreService telemetryStore,
                                ObjectMapper objectMapper) {
        this(llmProvider, promptBuilder, telemetryStore, objectMapper, null);
    }

    @Autowired
    public LlmDiagnosticService(DiagnosticLlmProvider llmProvider,
                                DiagnosticPromptBuilder promptBuilder,
                                TelemetryStoreService telemetryStore,
                                ObjectMapper objectMapper,
                                @Autowired(required = false) DiagnosticReportRepository diagnosticReportRepository) {
        this.llmProvider = llmProvider;
        this.promptBuilder = promptBuilder;
        this.telemetryStore = telemetryStore;
        this.objectMapper = objectMapper;
        this.diagnosticReportRepository = diagnosticReportRepository;
    }

    @PostConstruct
    public void init() {
        if (diagnosticReportRepository != null) {
            try {
                List<DiagnosticReportEntity> saved = diagnosticReportRepository
                        .findAllByOrderByTimestampDesc(PageRequest.of(0, MAX_DIAGNOSTIC_REPORTS));
                for (DiagnosticReportEntity entity : saved) {
                    LlmDiagnosticReport report = entity.toModel();
                    diagnosticHistory.put(report.getIncidentId(), report);
                }
                log.info("[LLM Diagnostics] Hydrated {} diagnostic reports from database", saved.size());
            } catch (Exception e) {
                log.warn("[LLM Diagnostics] Could not hydrate diagnostic reports from database: {}", e.getMessage());
            }
        }
    }

    private synchronized void recordReport(LlmDiagnosticReport report) {
        if (report == null || report.getIncidentId() == null) return;
        if (!diagnosticHistory.containsKey(report.getIncidentId())
                && diagnosticHistory.size() >= MAX_DIAGNOSTIC_REPORTS) {
            diagnosticHistory.entrySet().stream()
                    .min(Comparator.comparing(entry -> Optional.ofNullable(entry.getValue().getTimestamp())
                            .orElse(java.time.Instant.MIN)))
                    .map(Map.Entry::getKey)
                    .ifPresent(diagnosticHistory::remove);
        }
        diagnosticHistory.put(report.getIncidentId(), report);
        if (diagnosticReportRepository != null) {
            try {
                diagnosticReportRepository.save(DiagnosticReportEntity.fromModel(report));
            } catch (Exception e) {
                log.warn("[LLM Diagnostics] Failed to persist diagnostic report {}: {}", report.getIncidentId(), e.getMessage());
            }
        }
    }

    /**
     * Diagnoses a captured incident by its ID.
     */
    public Optional<LlmDiagnosticReport> analyzeIncident(String incidentId) {
        Optional<AgentTelemetryEvent> eventOpt = telemetryStore.getIncidentById(incidentId);
        if (eventOpt.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(analyzeEvent(eventOpt.get()));
    }

    /**
     * Analyzes a FailureContext event with the configured provider and labels heuristic fallback explicitly.
     */
    public LlmDiagnosticReport analyzeEvent(AgentTelemetryEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Cannot diagnose null telemetry event");
        }

        long startTime = System.currentTimeMillis();
        String incidentId = event.getIncidentId();

        if (!llmProvider.isAvailable()) {
            String unavailableReason = llmProvider.getProviderName() + " is not configured or reachable";
            log.warn("{}; recording an explicitly labeled heuristic report for {}", unavailableReason, incidentId);
            LlmDiagnosticReport fallback = generateHeuristicFallback(event, unavailableReason);
            recordReport(fallback);
            return fallback;
        }

        try {
            String prompt = promptBuilder.buildPrompt(event);
            LlmProviderResponse response = llmProvider.generate(prompt, DiagnosticPromptBuilder.SYSTEM_PROMPT);

            long latency = System.currentTimeMillis() - startTime;
            String rawJson = response != null ? response.content() : null;

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new IllegalStateException(llmProvider.getProviderName() + " returned an empty response payload.");
            }

            // Clean up any potential markdown wrapper if model output includes ```json ... ```
            String cleanJson = extractJson(rawJson);
            JsonNode root = objectMapper.readTree(cleanJson);

            String rootCause = requiredText(root, "rootCause");
            String explanation = requiredText(root, "explanation");
            String severityText = requiredText(root, "severity");
            DiagnosisSeverity severity = DiagnosisSeverity.valueOf(severityText.toUpperCase(Locale.ROOT));
            JsonNode confidenceNode = root.get("confidenceScore");
            if (confidenceNode == null || !confidenceNode.isNumber()
                    || confidenceNode.asDouble() < 0.0 || confidenceNode.asDouble() > 1.0) {
                throw new IllegalArgumentException("AI response must contain a numeric confidenceScore between 0 and 1.");
            }
            double confidence = confidenceNode.asDouble();

            RecoveryStrategy strategy = RecoveryStrategy.fromString(requiredText(root, "suggestedRecoveryStrategy"));
            String reason = requiredText(root, "reasonForSuggestion");

            LlmDiagnosticReport report = new LlmDiagnosticReport(
                    incidentId,
                    rootCause,
                    explanation,
                    severity,
                    confidence,
                    strategy,
                    reason,
                    providerSource(response.model()),
                    latency,
                    rawJson
            );

            log.info("[LLM Diagnostics] Successfully analyzed incident {} in {} ms -> Strategy: {}, Confidence: {}",
                    incidentId, latency, strategy, confidence);

            recordReport(report);
            return report;

        } catch (Exception e) {
            log.warn("[LLM Diagnostics] {} analysis failed for incident {} ({}). Recording an explicitly labeled heuristic report.",
                    llmProvider.getProviderName(),
                    incidentId, e.getMessage());

            long latency = System.currentTimeMillis() - startTime;
            LlmDiagnosticReport fallback = generateHeuristicFallback(event, "LLM query error: " + e.getMessage());
            fallback.setLatencyMillis(latency);
            recordReport(fallback);
            return fallback;
        }
    }

    private String providerSource(String model) {
        String source = llmProvider.getProviderId().toUpperCase(Locale.ROOT)
                + "_" + (model == null || model.isBlank() ? llmProvider.getModel() : model).toUpperCase(Locale.ROOT);
        return source.length() <= 64 ? source : source.substring(0, 64);
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("AI response is missing required field: " + field);
        }
        return value.asText();
    }

    /**
     * Deterministic rule-based heuristic fallback when LLM is unavailable or errors out.
     */
    public LlmDiagnosticReport generateHeuristicFallback(AgentTelemetryEvent event, String reason) {
        String excType = event.getExceptionType() != null ? event.getExceptionType() : "";
        String incidentId = event.getIncidentId();

        String rootCause;
        String explanation;
        DiagnosisSeverity severity;
        double confidence;
        RecoveryStrategy strategy;
        String strategyReason;

        if (excType.contains("ArithmeticException")) {
            rootCause = "Arithmetic evaluation failure (division by zero or invalid operand)";
            explanation = "A mathematical operation attempted an invalid computation (such as integer division by zero) in method "
                    + event.getMethodName() + ".";
            severity = DiagnosisSeverity.MEDIUM;
            confidence = 0.80;
            strategy = RecoveryStrategy.RETURN_FALLBACK;
            strategyReason = "Returning a safe default ratio or numeric fallback prevents cascading computational failures.";

        } else if (excType.contains("NullPointerException")) {
            rootCause = "Null reference dereference";
            explanation = "The application attempted to invoke a method or access a field on a null object reference in "
                    + event.getClassName() + "." + event.getMethodName() + "().";
            severity = DiagnosisSeverity.HIGH;
            confidence = 0.75;
            strategy = RecoveryStrategy.RETURN_FALLBACK;
            strategyReason = "Providing a non-null default profile or empty fallback object restores request processing flow.";

        } else if (excType.contains("IllegalArgumentException")) {
            rootCause = "Illegal or out-of-bounds argument validation failure";
            explanation = "Input argument failed business constraint validation: " + event.getExceptionMessage();
            severity = DiagnosisSeverity.MEDIUM;
            confidence = 0.70;
            strategy = RecoveryStrategy.SKIP_OPERATION;
            strategyReason = "Bypassing or aborting the invalid operation protects transactional database integrity.";

        } else if (excType.contains("TimeoutException") || excType.contains("Timeout")) {
            rootCause = "Downstream service latency SLA breach / timeout";
            explanation = "An external service or network call exceeded the designated execution threshold.";
            severity = DiagnosisSeverity.HIGH;
            confidence = 0.85;
            strategy = RecoveryStrategy.RETRY;
            strategyReason = "Transient network timeouts often recover successfully upon immediate or backoff retry.";

        } else {
            rootCause = "Unhandled runtime exception in " + event.getMethodName();
            explanation = "An unexpected " + excType + " occurred: " + event.getExceptionMessage();
            severity = DiagnosisSeverity.MEDIUM;
            confidence = 0.50;
            strategy = RecoveryStrategy.NO_ACTION;
            strategyReason = "Unrecognized failure pattern requires manual engineering review before safe automated recovery.";
        }

        return new LlmDiagnosticReport(
                incidentId,
                rootCause,
                explanation,
                severity,
                confidence,
                strategy,
                strategyReason,
                "HEURISTIC_FALLBACK (" + reason + ")",
                0L,
                "{ \"fallback\": true, \"reason\": \"" + reason + "\" }"
        );
    }

    /**
     * Cleans up any markdown wrapper characters (e.g. ```json ... ```) that LLMs sometimes generate.
     */
    private String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    public Optional<LlmDiagnosticReport> getCachedDiagnosis(String incidentId) {
        return Optional.ofNullable(diagnosticHistory.get(incidentId));
    }

    public List<LlmDiagnosticReport> getAllDiagnoses() {
        return new ArrayList<>(diagnosticHistory.values());
    }

    public void clearHistory() {
        diagnosticHistory.clear();
        if (diagnosticReportRepository != null) {
            try {
                diagnosticReportRepository.deleteAll();
            } catch (Exception e) {
                log.warn("[LLM Diagnostics] Failed to clear persisted diagnostic reports: {}", e.getMessage());
            }
        }
    }
}
