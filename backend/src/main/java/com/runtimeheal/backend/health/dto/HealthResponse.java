package com.runtimeheal.backend.health.dto;

import java.time.Instant;

/**
 * Data Transfer Object representing the system health status response.
 */
public class HealthResponse {

    private String status;
    private String service;
    private String version;
    private Instant timestamp;
    private String jvmVersion;
    private long uptimeMillis;
    private String message;
    private long heapUsedMB;
    private long heapMaxMB;

    public HealthResponse() {
    }

    public HealthResponse(String status, String service, String version, Instant timestamp,
                          String jvmVersion, long uptimeMillis, String message) {
        this(status, service, version, timestamp, jvmVersion, uptimeMillis, message, 0L, 0L);
    }

    public HealthResponse(String status, String service, String version, Instant timestamp,
                          String jvmVersion, long uptimeMillis, String message,
                          long heapUsedMB, long heapMaxMB) {
        this.status = status;
        this.service = service;
        this.version = version;
        this.timestamp = timestamp;
        this.jvmVersion = jvmVersion;
        this.uptimeMillis = uptimeMillis;
        this.message = message;
        this.heapUsedMB = heapUsedMB;
        this.heapMaxMB = heapMaxMB;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getJvmVersion() {
        return jvmVersion;
    }

    public void setJvmVersion(String jvmVersion) {
        this.jvmVersion = jvmVersion;
    }

    public long getUptimeMillis() {
        return uptimeMillis;
    }

    public void setUptimeMillis(long uptimeMillis) {
        this.uptimeMillis = uptimeMillis;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getHeapUsedMB() {
        return heapUsedMB;
    }

    public void setHeapUsedMB(long heapUsedMB) {
        this.heapUsedMB = heapUsedMB;
    }

    public long getHeapMaxMB() {
        return heapMaxMB;
    }

    public void setHeapMaxMB(long heapMaxMB) {
        this.heapMaxMB = heapMaxMB;
    }
}
