package com.runtimeheal.backend.telemetry.dto;

import java.time.Instant;

/**
 * Data Transfer Object for telemetry events transmitted by the ByteBuddy agent.
 */
public class AgentTelemetryEvent {

    private String incidentId;
    private Instant timestamp;
    private String className;
    private String methodName;
    private String methodSignature;
    private String exceptionType;
    private String exceptionMessage;
    private String stackTrace;
    private String threadName;
    private String[] argumentTypes;

    public AgentTelemetryEvent() {
    }

    public AgentTelemetryEvent(String incidentId, Instant timestamp, String className, String methodName,
                               String methodSignature, String exceptionType, String exceptionMessage,
                               String stackTrace, String threadName, String[] argumentTypes) {
        this.incidentId = incidentId;
        this.timestamp = timestamp;
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
        this.stackTrace = stackTrace;
        this.threadName = threadName;
        this.argumentTypes = argumentTypes;
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String[] getArgumentTypes() {
        return argumentTypes;
    }

    public void setArgumentTypes(String[] argumentTypes) {
        this.argumentTypes = argumentTypes;
    }
}
