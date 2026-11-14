package com.runtimeheal.backend.telemetry.controller;

import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import com.runtimeheal.backend.telemetry.service.TelemetryStoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for receiving and querying agent telemetry and intercepted incidents.
 */
@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "*")
public class TelemetryController {

    private final TelemetryStoreService telemetryStore;

    public TelemetryController(TelemetryStoreService telemetryStore) {
        this.telemetryStore = telemetryStore;
    }

    /**
     * Endpoint for external agents to submit failure telemetry over HTTP.
     */
    @PostMapping("/incidents")
    public ResponseEntity<Map<String, Object>> receiveTelemetry(@RequestBody AgentTelemetryEvent event) {
        AgentTelemetryEvent stored = telemetryStore.recordIncident(event);
        if (stored == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "REJECTED", "reason", "Invalid event payload"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "RECORDED",
                "incidentId", stored.getIncidentId()
        ));
    }

    /**
     * Retrieves all captured failure incidents.
     */
    @GetMapping("/incidents")
    public ResponseEntity<List<AgentTelemetryEvent>> getAllIncidents() {
        return ResponseEntity.ok(telemetryStore.getAllIncidents());
    }

    /**
     * Retrieves a single incident by its correlation ID.
     */
    @GetMapping("/incidents/{incidentId}")
    public ResponseEntity<AgentTelemetryEvent> getIncident(@PathVariable String incidentId) {
        return telemetryStore.getIncidentById(incidentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns a summary of telemetry statistics for observability dashboard cards.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(Map.of(
                "totalIncidents", telemetryStore.getTotalIncidentCount(),
                "distribution", telemetryStore.getExceptionDistribution()
        ));
    }
}
