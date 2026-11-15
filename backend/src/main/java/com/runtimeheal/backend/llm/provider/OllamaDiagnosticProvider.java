package com.runtimeheal.backend.llm.provider;

import com.runtimeheal.backend.llm.client.OllamaClient;
import com.runtimeheal.backend.llm.model.OllamaGenerateResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "runtimeheal.ai", name = "provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaDiagnosticProvider implements DiagnosticLlmProvider {

    private final OllamaClient ollamaClient;

    public OllamaDiagnosticProvider(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Override
    public String getProviderId() {
        return "ollama";
    }

    @Override
    public String getProviderName() {
        return "Local Ollama";
    }

    @Override
    public String getModel() {
        return ollamaClient.getProperties().getModel();
    }

    @Override
    public boolean isConfigured() {
        return ollamaClient.getProperties().isEnabled();
    }

    @Override
    public boolean isAvailable() {
        return ollamaClient.isAvailable();
    }

    @Override
    public LlmProviderResponse generate(String prompt, String systemPrompt) throws Exception {
        OllamaGenerateResponse response = ollamaClient.generate(prompt, systemPrompt);
        return new LlmProviderResponse(response.getModel(), response.getResponse());
    }
}
