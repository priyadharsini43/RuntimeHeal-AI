package com.runtimeheal.backend.llm.controller;

import com.runtimeheal.backend.llm.client.OllamaClient;
import com.runtimeheal.backend.llm.model.LlmDiagnosticReport;
import com.runtimeheal.backend.llm.service.LlmDiagnosticService;
import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing endpoints for triggering and inspecting LLM-driven incident diagnostics.
 */
@RestController
@RequestMapping("/api/diagnostics")
@CrossOrigin(origins = "*")
public class DiagnosticController {

    private final LlmDiagnosticService diagnosticService;
    private final OllamaClient ollamaClient;

    public DiagnosticController(LlmDiagnosticService diagnosticService, OllamaClient ollamaClient) {
        this.diagnosticService = diagnosticService;
        this.ollamaClient = ollamaClient;
    }

    /**
     * Triggers LLM diagnosis for a recorded incident ID.
     */
    @PostMapping("/analyze/{incidentId}")
    public ResponseEntity<LlmDiagnosticReport> analyzeIncident(@PathVariable String incidentId) {
        return diagnosticService.analyzeIncident(incidentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Directly analyzes an ad-hoc FailureContext payload.
     */
    @PostMapping("/analyze")
    public ResponseEntity<LlmDiagnosticReport> analyzeCustomEvent(@RequestBody AgentTelemetryEvent event) {
        if (event == null) {
            return ResponseEntity.badRequest().build();
        }
        LlmDiagnosticReport report = diagnosticService.analyzeEvent(event);
        return ResponseEntity.ok(report);
    }

    /**
     * Retrieves an existing cached diagnosis report.
     */
    @GetMapping("/{incidentId}")
    public ResponseEntity<LlmDiagnosticReport> getDiagnosis(@PathVariable String incidentId) {
        return diagnosticService.getCachedDiagnosis(incidentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves the history of all generated diagnostic reports.
     */
    @GetMapping("/history")
    public ResponseEntity<List<LlmDiagnosticReport>> getHistory() {
        return ResponseEntity.ok(diagnosticService.getAllDiagnoses());
    }

    /**
     * Health check endpoint reporting Ollama reachability and configured model.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getOllamaHealth() {
        boolean available = ollamaClient.isAvailable();
        return ResponseEntity.ok(Map.of(
                "status", available ? "CONNECTED" : "UNREACHABLE",
                "service", "Local Ollama LLM",
                "model", ollamaClient.getProperties().getModel(),
                "baseUrl", ollamaClient.getProperties().getBaseUrl(),
                "enabled", ollamaClient.getProperties().isEnabled()
        ));
    }
}
