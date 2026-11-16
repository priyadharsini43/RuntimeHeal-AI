package com.runtimeheal.backend.llm.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.llm.config.GroqProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "runtimeheal.ai", name = "provider", havingValue = "groq")
public class GroqDiagnosticProvider implements DiagnosticLlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GroqDiagnosticProvider.class);
    private static final long AVAILABILITY_CACHE_MILLIS = 30_000;

    private final GroqProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private volatile long lastAvailabilityCheck;
    private volatile boolean lastAvailability;

    @Autowired
    public GroqDiagnosticProvider(GroqProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    GroqDiagnosticProvider(GroqProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String getProviderId() {
        return "groq";
    }

    @Override
    public String getProviderName() {
        return "Groq Cloud";
    }

    @Override
    public String getModel() {
        return properties.getModel();
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public boolean isAvailable() {
        if (!isConfigured()) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastAvailabilityCheck < AVAILABILITY_CACHE_MILLIS) {
            return lastAvailability;
        }

        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - lastAvailabilityCheck < AVAILABILITY_CACHE_MILLIS) {
                return lastAvailability;
            }

            lastAvailability = checkAvailability();
            lastAvailabilityCheck = now;
            return lastAvailability;
        }
    }

    private boolean checkAvailability() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl("/models")))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Groq model availability check returned HTTP {}", response.statusCode());
                return false;
            }

            JsonNode models = objectMapper.readTree(response.body()).path("data");
            if (!models.isArray()) {
                return false;
            }
            for (JsonNode model : models) {
                if (properties.getModel().equals(model.path("id").asText())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Groq availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public LlmProviderResponse generate(String prompt, String systemPrompt) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Groq requires GROQ_API_KEY, RUNTIMEHEAL_GROQ_MODEL, and RUNTIMEHEAL_GROQ_FREE_ONLY=true.");
        }

        Map<String, Object> payload = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", properties.getTemperature(),
                "response_format", Map.of("type", "json_object")
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl("/chat/completions")))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Groq API returned HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText();
        if (content.isBlank()) {
            throw new IOException("Groq returned an empty diagnostic response.");
        }

        return new LlmProviderResponse(root.path("model").asText(properties.getModel()), content);
    }

    private String apiUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + path : baseUrl + path;
    }
}
