package com.runtimeheal.backend.persistence.entity;

import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import com.runtimeheal.backend.recovery.model.HealingActionRecord;
import com.runtimeheal.backend.recovery.model.HealingStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "healing_audit_actions")
public class HealingActionEntity {

    @Id
    @Column(name = "action_id", length = 64, nullable = false)
    private String actionId;

    @Column(name = "incident_id", length = 64)
    private String incidentId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "class_name", length = 255)
    private String className;

    @Column(name = "method_name", length = 128)
    private String methodName;

    @Column(name = "method_signature", length = 512)
    private String methodSignature;

    @Column(name = "exception_type", length = 255)
    private String exceptionType;

    @Column(name = "strategy", length = 64)
    private String strategy;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "confidence_score")
    private double confidenceScore;

    @Column(name = "policy_reason", length = 2048)
    private String policyReason;

    @Column(name = "execution_time_ms")
    private long executionTimeMs;

    @Column(name = "diagnosis_latency_ms")
    private long diagnosisLatencyMs;

    @Column(name = "healing_execution_latency_ms")
    private long healingExecutionLatencyMs;

    @Column(name = "fallback_value", length = 1024)
    private String fallbackValue;

    public HealingActionEntity() {
    }

    public static HealingActionEntity fromModel(HealingActionRecord record) {
        if (record == null) return null;
        HealingActionEntity entity = new HealingActionEntity();
        entity.setActionId(record.getActionId());
        entity.setIncidentId(record.getIncidentId());
        entity.setTimestamp(record.getTimestamp() != null ? record.getTimestamp() : Instant.now());
        entity.setClassName(record.getClassName());
        entity.setMethodName(record.getMethodName());
        entity.setMethodSignature(record.getMethodSignature());
        entity.setExceptionType(record.getExceptionType());
        entity.setStrategy(record.getStrategy() != null ? record.getStrategy().name() : null);
        entity.setStatus(record.getStatus() != null ? record.getStatus().name() : null);
        entity.setConfidenceScore(record.getConfidenceScore());
        entity.setPolicyReason(record.getPolicyReason());
        entity.setExecutionTimeMs(record.getExecutionTimeMs());
        entity.setDiagnosisLatencyMs(record.getDiagnosisLatencyMs());
        entity.setHealingExecutionLatencyMs(record.getHealingExecutionLatencyMs());
        entity.setFallbackValue(record.getFallbackValue() != null ? String.valueOf(record.getFallbackValue()) : null);
        return entity;
    }

    public HealingActionRecord toModel() {
        RecoveryStrategy strat = strategy != null ? RecoveryStrategy.fromString(strategy) : RecoveryStrategy.NO_ACTION;
        HealingStatus stat = status != null ? HealingStatus.valueOf(status) : HealingStatus.FAILED;
        HealingActionRecord record = new HealingActionRecord(
                actionId,
                incidentId,
                className,
                methodName,
                methodSignature,
                exceptionType,
                strat,
                stat,
                confidenceScore,
                policyReason,
                executionTimeMs,
                diagnosisLatencyMs,
                healingExecutionLatencyMs,
                fallbackValue
        );
        if (timestamp != null) {
            record.setTimestamp(timestamp);
        }
        return record;
    }

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getMethodSignature() { return methodSignature; }
    public void setMethodSignature(String methodSignature) { this.methodSignature = methodSignature; }

    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getPolicyReason() { return policyReason; }
    public void setPolicyReason(String policyReason) { this.policyReason = policyReason; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public long getDiagnosisLatencyMs() { return diagnosisLatencyMs; }
    public void setDiagnosisLatencyMs(long diagnosisLatencyMs) { this.diagnosisLatencyMs = diagnosisLatencyMs; }

    public long getHealingExecutionLatencyMs() { return healingExecutionLatencyMs; }
    public void setHealingExecutionLatencyMs(long healingExecutionLatencyMs) { this.healingExecutionLatencyMs = healingExecutionLatencyMs; }

    public String getFallbackValue() { return fallbackValue; }
    public void setFallbackValue(String fallbackValue) { this.fallbackValue = fallbackValue; }
}
