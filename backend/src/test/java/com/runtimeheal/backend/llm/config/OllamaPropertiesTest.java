package com.runtimeheal.backend.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaPropertiesTest {

    @Test
    @DisplayName("Default properties should configure local Ollama on port 11434 with qwen2.5:3b")
    void testDefaultProperties() {
        OllamaProperties props = new OllamaProperties();
        assertEquals("http://localhost:11434", props.getBaseUrl());
        assertEquals("qwen2.5:3b", props.getModel());
        assertEquals(120, props.getTimeoutSeconds());
        assertEquals(0.1, props.getTemperature());
        assertTrue(props.isEnabled());
    }

    @Test
    @DisplayName("Setters and getters should update properties correctly")
    void testSettersAndGetters() {
        OllamaProperties props = new OllamaProperties();
        props.setBaseUrl("http://127.0.0.1:11434");
        props.setModel("mistral:7b");
        props.setTimeoutSeconds(30);
        props.setTemperature(0.2);
        props.setEnabled(false);

        assertEquals("http://127.0.0.1:11434", props.getBaseUrl());
        assertEquals("mistral:7b", props.getModel());
        assertEquals(30, props.getTimeoutSeconds());
        assertEquals(0.2, props.getTemperature());
        assertFalse(props.isEnabled());
    }
}
