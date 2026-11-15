package com.runtimeheal.backend.llm.provider;

import java.util.Map;

public interface DiagnosticLlmProvider {

    String getProviderId();

    String getProviderName();

    String getModel();

    boolean isConfigured();

    boolean isAvailable();

    LlmProviderResponse generate(String prompt, String systemPrompt) throws Exception;

    default Map<String, Object> getHealthStatus() {
        boolean configured = isConfigured();
        boolean available = configured && isAvailable();
        return Map.of(
                "status", available ? "CONNECTED" : configured ? "UNREACHABLE" : "NOT_CONFIGURED",
                "provider", getProviderId(),
                "service", getProviderName(),
                "model", getModel(),
                "enabled", true,
                "configured", configured
        );
    }
}
