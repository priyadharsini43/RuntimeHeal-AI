package com.runtimeheal.backend.persistence.entity;

import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "telemetry_incidents")
public class IncidentEntity {

    @Id
    @Column(name = "incident_id", length = 64, nullable = false)
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

    @Column(name = "exception_message", length = 2048)
    private String exceptionMessage;

    @Lob
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "thread_name", length = 128)
    private String threadName;

    @Column(name = "argument_types", length = 512)
    private String argumentTypes;

    public IncidentEntity() {
    }

    public static IncidentEntity fromDto(AgentTelemetryEvent dto) {
        if (dto == null) return null;
        IncidentEntity entity = new IncidentEntity();
        entity.setIncidentId(dto.getIncidentId());
        entity.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now());
        entity.setClassName(dto.getClassName());
        entity.setMethodName(dto.getMethodName());
        entity.setMethodSignature(dto.getMethodSignature());
        entity.setExceptionType(dto.getExceptionType());
        entity.setExceptionMessage(dto.getExceptionMessage());
        entity.setStackTrace(dto.getStackTrace());
        entity.setThreadName(dto.getThreadName());
        if (dto.getArgumentTypes() != null) {
            entity.setArgumentTypes(String.join(",", dto.getArgumentTypes()));
        }
        return entity;
    }

    public AgentTelemetryEvent toDto() {
        String[] args = (argumentTypes != null && !argumentTypes.isBlank())
                ? argumentTypes.split(",")
                : new String[0];
        return new AgentTelemetryEvent(
                incidentId,
                timestamp,
                className,
                methodName,
                methodSignature,
                exceptionType,
                exceptionMessage,
                stackTrace,
                threadName,
                args
        );
    }

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

    public String getExceptionMessage() { return exceptionMessage; }
    public void setExceptionMessage(String exceptionMessage) { this.exceptionMessage = exceptionMessage; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }

    public String getArgumentTypes() { return argumentTypes; }
    public void setArgumentTypes(String argumentTypes) { this.argumentTypes = argumentTypes; }
}
