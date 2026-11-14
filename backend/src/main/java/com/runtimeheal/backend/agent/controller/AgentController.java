package com.runtimeheal.backend.agent.controller;

import com.runtimeheal.backend.agent.AgentBootstrapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller providing live ByteBuddy agent status and telemetry statistics.
 */
@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentBootstrapService bootstrapService;

    public AgentController(AgentBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(bootstrapService.getAgentStatus());
    }

    @PostMapping("/attach/{pid}")
    public ResponseEntity<Map<String, Object>> attachToProcess(@PathVariable String pid) {
        Map<String, Object> result = bootstrapService.attachToExternalPid(pid);
        return ResponseEntity.ok(result);
    }
}
