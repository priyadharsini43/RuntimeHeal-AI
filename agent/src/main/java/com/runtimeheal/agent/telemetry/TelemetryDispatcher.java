package com.runtimeheal.agent.telemetry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Dispatches captured failure telemetry safely without interfering with application business execution.
 */
public class TelemetryDispatcher {

    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080/api/telemetry/incidents";
    private static final List<CapturedFailureEvent> EVENT_HISTORY = new CopyOnWriteArrayList<>();
    private static final List<Consumer<CapturedFailureEvent>> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile boolean httpDispatchEnabled = true;
    private static volatile String backendEndpoint = DEFAULT_BACKEND_URL;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public static void registerListener(Consumer<CapturedFailureEvent> listener) {
        LISTENERS.add(listener);
    }

    public static void setHttpDispatchEnabled(boolean enabled) {
        httpDispatchEnabled = enabled;
    }

    public static void setBackendEndpoint(String endpoint) {
        backendEndpoint = endpoint;
    }

    public static List<CapturedFailureEvent> getEventHistory() {
        return Collections.unmodifiableList(EVENT_HISTORY);
    }

    public static void clearHistory() {
        EVENT_HISTORY.clear();
    }

    /**
     * Captures a failure event and dispatches it through safe channels.
     */
    public static CapturedFailureEvent captureAndDispatch(String className, String methodName, String methodSignature,
                                                          Object[] args, Throwable thrown) {
        if (RecursionGuard.isGuarded()) {
            return null;
        }

        CapturedFailureEvent event = null;
        try {
            RecursionGuard.enter();
            event = new CapturedFailureEvent(className, methodName, methodSignature, args, thrown);

            // Record in in-memory history (capped at 500 events to avoid memory bloat)
            if (EVENT_HISTORY.size() >= 500) {
                EVENT_HISTORY.remove(0);
            }
            EVENT_HISTORY.add(event);

            // Print structured agent alert to standard error/out
            System.err.printf("[RuntimeHeal Agent] Intercepted %s in %s.%s() -> Incident ID: %s%n",
                    event.getExceptionType(), event.getClassName(), event.getMethodName(), event.getIncidentId());

            // Notify in-process listeners
            for (Consumer<CapturedFailureEvent> listener : LISTENERS) {
                try {
                    listener.accept(event);
                } catch (Throwable t) {
                    System.err.println("[RuntimeHeal Agent] Listener notification error: " + t.getMessage());
                }
            }

            // Asynchronous external HTTP dispatch if enabled
            if (httpDispatchEnabled) {
                dispatchAsyncHttp(event);
            }

            return event;
        } catch (Throwable t) {
            System.err.println("[RuntimeHeal Agent] Internal telemetry dispatch warning: " + t.getMessage());
            return event;
        } finally {
            RecursionGuard.exit();
        }
    }

    private static void dispatchAsyncHttp(CapturedFailureEvent event) {
        try {
            String jsonPayload = formatJson(event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(backendEndpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        // Silently log failure without crashing the host application
                        return null;
                    });
        } catch (Throwable t) {
            // Never propagate network dispatch errors to user application
        }
    }

    private static String formatJson(CapturedFailureEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"incidentId\":\"").append(escapeJson(event.getIncidentId())).append("\",");
        sb.append("\"timestamp\":\"").append(event.getTimestamp().toString()).append("\",");
        sb.append("\"className\":\"").append(escapeJson(event.getClassName())).append("\",");
        sb.append("\"methodName\":\"").append(escapeJson(event.getMethodName())).append("\",");
        sb.append("\"methodSignature\":\"").append(escapeJson(event.getMethodSignature())).append("\",");
        sb.append("\"exceptionType\":\"").append(escapeJson(event.getExceptionType())).append("\",");
        sb.append("\"exceptionMessage\":\"").append(escapeJson(event.getExceptionMessage())).append("\",");
        sb.append("\"stackTrace\":\"").append(escapeJson(event.getStackTrace())).append("\",");
        sb.append("\"threadName\":\"").append(escapeJson(event.getThreadName())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
