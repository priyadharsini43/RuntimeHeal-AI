package com.runtimeheal.agent.telemetry;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Structured telemetry event captured by the ByteBuddy agent upon intercepting an unhandled exception.
 */
public class CapturedFailureEvent {

    private final String incidentId;
    private final Instant timestamp;
    private final String className;
    private final String methodName;
    private final String methodSignature;
    private final String exceptionType;
    private final String exceptionMessage;
    private final String stackTrace;
    private final String threadName;
    private final String[] argumentTypes;

    public CapturedFailureEvent(String className, String methodName, String methodSignature,
                                Object[] args, Throwable thrown) {
        this.incidentId = "INC-AGENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.timestamp = Instant.now();
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.exceptionType = thrown != null ? thrown.getClass().getName() : "Unknown";
        this.exceptionMessage = thrown != null ? thrown.getMessage() : "";
        this.stackTrace = extractStackTrace(thrown);
        this.threadName = Thread.currentThread().getName();
        this.argumentTypes = extractArgumentTypes(args);
    }

    private static String extractStackTrace(Throwable thrown) {
        if (thrown == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        return sw.toString();
    }

    private static String[] extractArgumentTypes(Object[] args) {
        if (args == null || args.length == 0) {
            return new String[0];
        }
        return Arrays.stream(args)
                .map(arg -> arg != null ? arg.getClass().getName() : "null")
                .toArray(String[]::new);
    }

    public String getIncidentId() {
        return incidentId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public String getThreadName() {
        return threadName;
    }

    public String[] getArgumentTypes() {
        return argumentTypes;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s at %s.%s(): %s",
                incidentId, exceptionType, className, methodName, exceptionMessage);
    }
}
