package com.runtimeheal.backend.persistence.entity;

import com.runtimeheal.backend.llm.model.DiagnosisSeverity;
import com.runtimeheal.backend.llm.model.LlmDiagnosticReport;
import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "diagnostic_reports")
public class DiagnosticReportEntity {

    @Id
    @Column(name = "incident_id", length = 64, nullable = false)
    private String incidentId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "root_cause", length = 1024)
    private String rootCause;

    @Lob
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "severity", length = 32)
    private String severity;

    @Column(name = "confidence_score")
    private double confidenceScore;

    @Column(name = "suggested_recovery_strategy", length = 64)
    private String suggestedRecoveryStrategy;

    @Column(name = "reason_for_suggestion", length = 2048)
    private String reasonForSuggestion;

    @Column(name = "source", length = 64)
    private String source;

    @Column(name = "latency_millis")
    private long latencyMillis;

    @Lob
    @Column(name = "raw_model_response", columnDefinition = "TEXT")
    private String rawModelResponse;

    public DiagnosticReportEntity() {
    }

    public static DiagnosticReportEntity fromModel(LlmDiagnosticReport report) {
        if (report == null) return null;
        DiagnosticReportEntity entity = new DiagnosticReportEntity();
        entity.setIncidentId(report.getIncidentId());
        entity.setTimestamp(report.getTimestamp() != null ? report.getTimestamp() : Instant.now());
        entity.setRootCause(report.getRootCause());
        entity.setExplanation(report.getExplanation());
        entity.setSeverity(report.getSeverity() != null ? report.getSeverity().name() : "MEDIUM");
        entity.setConfidenceScore(report.getConfidenceScore());
        entity.setSuggestedRecoveryStrategy(report.getSuggestedRecoveryStrategy() != null ? report.getSuggestedRecoveryStrategy().name() : "NO_ACTION");
        entity.setReasonForSuggestion(report.getReasonForSuggestion());
        entity.setSource(report.getSource());
        entity.setLatencyMillis(report.getLatencyMillis());
        entity.setRawModelResponse(report.getRawModelResponse());
        return entity;
    }

    public LlmDiagnosticReport toModel() {
        DiagnosisSeverity sev = severity != null ? DiagnosisSeverity.fromString(severity) : DiagnosisSeverity.MEDIUM;
        RecoveryStrategy strat = suggestedRecoveryStrategy != null ? RecoveryStrategy.fromString(suggestedRecoveryStrategy) : RecoveryStrategy.NO_ACTION;
        LlmDiagnosticReport report = new LlmDiagnosticReport(
                incidentId,
                rootCause,
                explanation,
                sev,
                confidenceScore,
                strat,
                reasonForSuggestion,
                source,
                latencyMillis,
                rawModelResponse
        );
        if (timestamp != null) {
            report.setTimestamp(timestamp);
        }
        return report;
    }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getSuggestedRecoveryStrategy() { return suggestedRecoveryStrategy; }
    public void setSuggestedRecoveryStrategy(String suggestedRecoveryStrategy) { this.suggestedRecoveryStrategy = suggestedRecoveryStrategy; }

    public String getReasonForSuggestion() { return reasonForSuggestion; }
    public void setReasonForSuggestion(String reasonForSuggestion) { this.reasonForSuggestion = reasonForSuggestion; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public long getLatencyMillis() { return latencyMillis; }
    public void setLatencyMillis(long latencyMillis) { this.latencyMillis = latencyMillis; }

    public String getRawModelResponse() { return rawModelResponse; }
    public void setRawModelResponse(String rawModelResponse) { this.rawModelResponse = rawModelResponse; }
}
