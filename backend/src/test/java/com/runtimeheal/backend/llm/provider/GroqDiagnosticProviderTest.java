package com.runtimeheal.backend.llm.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runtimeheal.backend.llm.config.GroqProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GroqDiagnosticProviderTest {

    private HttpServer server;
    private AtomicReference<String> requestBody;
    private AtomicReference<String> authorization;
    private GroqDiagnosticProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        requestBody = new AtomicReference<>();
        authorization = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/openai/v1/models", exchange -> respond(exchange, 200,
                "{\"data\":[{\"id\":\"test-model\"}]}"));
        server.createContext("/openai/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200,
                    "{\"model\":\"test-model\",\"choices\":[{\"message\":{\"content\":\"{\\\"rootCause\\\":\\\"test\\\"}\"}}]}");
        });
        server.start();

        GroqProperties properties = new GroqProperties();
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        properties.setFreeOnly(true);
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1");
        provider = new GroqDiagnosticProvider(properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void checksConfiguredModelAndReturnsChatCompletionContent() throws Exception {
        assertTrue(provider.isAvailable());

        LlmProviderResponse response = provider.generate("diagnose", "system");

        assertEquals("test-model", response.model());
        assertEquals("{\"rootCause\":\"test\"}", response.content());
        assertEquals("Bearer test-key", authorization.get());
        assertTrue(requestBody.get().contains("\"response_format\":{\"type\":\"json_object\"}"));
    }

    @Test
    void refusesToCallGroqWhenCredentialsOrModelAreMissing() {
        GroqProperties properties = new GroqProperties();
        properties.setApiKey("");
        properties.setModel("");
        properties.setFreeOnly(true);
        GroqDiagnosticProvider unconfigured = new GroqDiagnosticProvider(properties, new ObjectMapper());

        assertFalse(unconfigured.isConfigured());
        assertFalse(unconfigured.isAvailable());
        assertThrows(IllegalStateException.class, () -> unconfigured.generate("prompt", "system"));
    }

    @Test
    void refusesToCallGroqUnlessFreeOnlyIsExplicitlyEnabled() {
        GroqProperties properties = new GroqProperties();
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        GroqDiagnosticProvider paidMode = new GroqDiagnosticProvider(properties, new ObjectMapper());

        assertFalse(paidMode.isConfigured());
        assertFalse(paidMode.isAvailable());
        assertThrows(IllegalStateException.class, () -> paidMode.generate("prompt", "system"));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
