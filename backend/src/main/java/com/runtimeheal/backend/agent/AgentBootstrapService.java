package com.runtimeheal.backend.agent;

import com.runtimeheal.agent.RuntimeHealAgent;
import com.runtimeheal.agent.telemetry.CapturedFailureEvent;
import com.runtimeheal.agent.telemetry.TelemetryDispatcher;
import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import com.runtimeheal.backend.telemetry.service.TelemetryStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * Bootstraps and monitors the ByteBuddy Java Agent within the Spring Boot container.
 * Supports both premain (-javaagent) and dynamic runtime attach.
 */
@Service
public class AgentBootstrapService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(AgentBootstrapService.class);

    private final TelemetryStoreService telemetryStore;
    private volatile String attachMode = "UNATTACHED";
    private volatile boolean agentActive = false;

    public AgentBootstrapService(TelemetryStoreService telemetryStore) {
        this.telemetryStore = telemetryStore;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        initializeAgent();
    }

    public synchronized void initializeAgent() {
        if (agentActive) {
            return;
        }

        // Register in-JVM direct listener to stream captured agent events directly into telemetry store
        TelemetryDispatcher.registerListener(this::handleAgentEvent);

        if (RuntimeHealAgent.isInitialized()) {
            attachMode = "PREMAIN";
            agentActive = true;
            log.info("RuntimeHeal Agent initialized via PREMAIN (-javaagent flag).");
        } else {
            log.info("Attempting in-process dynamic attach for RuntimeHeal Agent...");
            boolean success = RuntimeHealAgent.install();
            if (success) {
                attachMode = "DYNAMIC_ATTACH";
                agentActive = true;
                log.info("RuntimeHeal Agent attached dynamically to running JVM successfully.");
            } else {
                attachMode = "ATTACH_FAILED";
                log.warn("Could not dynamically attach RuntimeHeal Agent. Run with -javaagent for premain attachment.");
            }
        }
    }

    private void handleAgentEvent(CapturedFailureEvent captured) {
        if (captured == null) return;

        AgentTelemetryEvent dto = new AgentTelemetryEvent(
                captured.getIncidentId(),
                captured.getTimestamp(),
                captured.getClassName(),
                captured.getMethodName(),
                captured.getMethodSignature(),
                captured.getExceptionType(),
                captured.getExceptionMessage(),
                captured.getStackTrace(),
                captured.getThreadName(),
                captured.getArgumentTypes()
        );

        telemetryStore.recordIncident(dto);
    }

    public Map<String, Object> getAgentStatus() {
        return Map.of(
                "agentName", "RuntimeHeal AI ByteBuddy Agent",
                "status", agentActive ? "ACTIVE" : "INACTIVE",
                "attachMode", attachMode,
                "targetPackage", "com.runtimeheal.backend.sample",
                "instrumentationEngine", "ByteBuddy 1.14.19",
                "incidentsIntercepted", telemetryStore.getTotalIncidentCount()
        );
    }

    public boolean isAgentActive() {
        return agentActive;
    }

    public String getAttachMode() {
        return attachMode;
    }

    public Map<String, Object> attachToExternalPid(String pid) {
        if (pid == null || pid.trim().isEmpty()) {
            return Map.of("status", "FAILED", "message", "Target process ID (PID) cannot be null or empty");
        }

        File agentJar = resolveAgentJar();
        if (agentJar == null || !agentJar.exists()) {
            return Map.of(
                    "status", "FAILED",
                    "pid", pid,
                    "message", "RuntimeHeal agent JAR not found. Ensure agent module is packaged (agent/target/runtimeheal-agent-0.0.1-SNAPSHOT.jar)."
            );
        }

        try {
            log.info("[Agent Bootstrap] Attempting dynamic ByteBuddy attachment to external PID: {} with JAR: {}", pid, agentJar.getAbsolutePath());
            ByteBuddyAgent.attach(agentJar, pid);
            log.info("[Agent Bootstrap] Successfully attached RuntimeHeal agent to external PID: {}", pid);
            return Map.of(
                    "status", "ATTACHED",
                    "pid", pid,
                    "agentJar", agentJar.getAbsolutePath(),
                    "message", "RuntimeHeal AI Agent successfully attached to external JVM process " + pid
            );
        } catch (Exception e) {
            log.error("[Agent Bootstrap] Failed to attach agent to PID {}: {}", pid, e.getMessage(), e);
            return Map.of(
                    "status", "FAILED",
                    "pid", pid,
                    "message", "Attachment failed: " + e.getMessage()
            );
        }
    }

    private File resolveAgentJar() {
        String customPath = System.getProperty("runtimeheal.agent.jar");
        if (customPath != null) {
            File custom = new File(customPath);
            if (custom.exists()) return custom;
        }

        String[] potentialPaths = {
                "agent/target/runtimeheal-agent-0.0.1-SNAPSHOT.jar",
                "../agent/target/runtimeheal-agent-0.0.1-SNAPSHOT.jar",
                "target/runtimeheal-agent-0.0.1-SNAPSHOT.jar"
        };

        for (String path : potentialPaths) {
            File f = new File(path);
            if (f.exists()) return f;
        }

        return null;
    }
}
