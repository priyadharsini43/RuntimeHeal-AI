package com.runtimeheal.backend.health.controller;

import com.runtimeheal.backend.health.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;

/**
 * Health check REST controller providing runtime health status of RuntimeHeal AI Backend.
 */
@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private static final String SERVICE_NAME = "RuntimeHeal AI Backend";
    private static final String APP_VERSION = "1.0.0";

    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        String jvmVersion = System.getProperty("java.version");
        Runtime runtime = Runtime.getRuntime();
        long heapUsedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long heapMaxMB = runtime.maxMemory() / (1024 * 1024);

        HealthResponse health = new HealthResponse(
                "UP",
                SERVICE_NAME,
                APP_VERSION,
                Instant.now(),
                jvmVersion,
                uptime,
                "RuntimeHeal AI backend service is operational and ready.",
                heapUsedMB,
                heapMaxMB
        );

        log.debug("Health check invoked. Uptime: {} ms, JVM: {}, Heap: {}/{} MB", uptime, jvmVersion, heapUsedMB, heapMaxMB);
        return ResponseEntity.ok(health);
    }
}
