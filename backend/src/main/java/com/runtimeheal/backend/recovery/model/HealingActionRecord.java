package com.runtimeheal.backend.recovery.model;

import com.runtimeheal.backend.llm.model.RecoveryStrategy;

import java.time.Instant;

/**
 * Immutable audit log record capturing every automated self-healing action attempted by the engine.
 */
public class HealingActionRecord {

    private String actionId;
    private String incidentId;
    private Instant timestamp;
    private String className;
    private String methodName;
    private String methodSignature;
    private String exceptionType;
    private RecoveryStrategy strategy;
    private HealingStatus status;
    private double confidenceScore;
    private String policyReason;
    private long executionTimeMs;
    private long diagnosisLatencyMs;
    private long healingExecutionLatencyMs;
    private Object fallbackValue;

    public HealingActionRecord() {
        this.timestamp = Instant.now();
    }

    public HealingActionRecord(String actionId, String incidentId, String className, String methodName,
                               String methodSignature, String exceptionType, RecoveryStrategy strategy,
                               HealingStatus status, double confidenceScore, String policyReason,
                               long executionTimeMs, Object fallbackValue) {
        this(actionId, incidentId, className, methodName, methodSignature, exceptionType,
                strategy, status, confidenceScore, policyReason, executionTimeMs, 0L, executionTimeMs, fallbackValue);
    }

    public HealingActionRecord(String actionId, String incidentId, String className, String methodName,
                               String methodSignature, String exceptionType, RecoveryStrategy strategy,
                               HealingStatus status, double confidenceScore, String policyReason,
                               long executionTimeMs, long diagnosisLatencyMs, long healingExecutionLatencyMs,
                               Object fallbackValue) {
        this.actionId = actionId;
        this.incidentId = incidentId;
        this.timestamp = Instant.now();
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.exceptionType = exceptionType;
        this.strategy = strategy;
        this.status = status;
        this.confidenceScore = confidenceScore;
        this.policyReason = policyReason;
        this.executionTimeMs = executionTimeMs;
        this.diagnosisLatencyMs = diagnosisLatencyMs;
        this.healingExecutionLatencyMs = healingExecutionLatencyMs;
        this.fallbackValue = fallbackValue;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
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

    public RecoveryStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(RecoveryStrategy strategy) {
        this.strategy = strategy;
    }

    public HealingStatus getStatus() {
        return status;
    }

    public void setStatus(HealingStatus status) {
        this.status = status;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getPolicyReason() {
        return policyReason;
    }

    public void setPolicyReason(String policyReason) {
        this.policyReason = policyReason;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public long getDiagnosisLatencyMs() {
        return diagnosisLatencyMs;
    }

    public void setDiagnosisLatencyMs(long diagnosisLatencyMs) {
        this.diagnosisLatencyMs = diagnosisLatencyMs;
    }

    public long getHealingExecutionLatencyMs() {
        return healingExecutionLatencyMs;
    }

    public void setHealingExecutionLatencyMs(long healingExecutionLatencyMs) {
        this.healingExecutionLatencyMs = healingExecutionLatencyMs;
    }

    public Object getFallbackValue() {
        return fallbackValue;
    }

    public void setFallbackValue(Object fallbackValue) {
        this.fallbackValue = fallbackValue;
    }
}
