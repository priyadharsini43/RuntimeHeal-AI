package com.runtimeheal.backend.sample.dto;

import java.time.Instant;

/**
 * Structured error response payload returned when a sample runtime exception occurs.
 */
public class SampleErrorResponse {

    private String incidentId;
    private Instant timestamp;
    private int status;
    private String error;
    private String exceptionType;
    private String message;
    private String path;

    public SampleErrorResponse() {
    }

    public SampleErrorResponse(String incidentId, Instant timestamp, int status, String error,
                               String exceptionType, String message, String path) {
        this.incidentId = incidentId;
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.exceptionType = exceptionType;
        this.message = message;
        this.path = path;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
