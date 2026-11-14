package com.runtimeheal.backend.llm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.llm.config.OllamaProperties;
import com.runtimeheal.backend.llm.model.OllamaGenerateRequest;
import com.runtimeheal.backend.llm.model.OllamaGenerateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Low-level HTTP client interfacing with the locally hosted Ollama instance.
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public OllamaClient(OllamaProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public OllamaClient(OllamaProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * Sends a generation request to Ollama POST /api/generate with JSON format enforcement.
     */
    public OllamaGenerateResponse generate(String prompt, String systemPrompt) throws Exception {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Ollama LLM integration is disabled in configuration.");
        }

        String url = properties.getBaseUrl() + "/api/generate";
        OllamaGenerateRequest requestPayload = new OllamaGenerateRequest(
                properties.getModel(),
                prompt,
                systemPrompt,
                properties.getTemperature()
        );

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        log.debug("Sending diagnostic generation request to Ollama at {} (model: {})", url, properties.getModel());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), OllamaGenerateResponse.class);
    }

    /**
     * Checks if Ollama is running and whether the target model is installed.
     */
    public boolean isAvailable() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode models = root.get("models");
            if (models != null && models.isArray()) {
                String target = properties.getModel().toLowerCase();
                for (JsonNode modelNode : models) {
                    JsonNode name = modelNode.get("name");
                    if (name != null && name.asText().toLowerCase().startsWith(target.split(":")[0])) {
                        return true;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.debug("Ollama availability check failed: {}", e.getMessage());
            return false;
        }
    }

    public OllamaProperties getProperties() {
        return properties;
    }
}
