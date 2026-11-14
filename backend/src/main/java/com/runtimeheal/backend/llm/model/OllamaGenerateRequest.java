package com.runtimeheal.backend.llm.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Request payload for Ollama's POST /api/generate endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaGenerateRequest {

    private String model;
    private String prompt;
    private String system;
    private boolean stream = false;
    private String format = "json";
    private Map<String, Object> options;

    public OllamaGenerateRequest() {
    }

    public OllamaGenerateRequest(String model, String prompt, String system, double temperature) {
        this.model = model;
        this.prompt = prompt;
        this.system = system;
        this.stream = false;
        this.format = "json";
        this.options = Map.of("temperature", temperature);
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}
