package com.threadflow.issue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

class OpenAiCompatibleIssueAiProviderTest {
    private HttpServer server;
    private int port;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> receivedRequestBody = new AtomicReference<>();
    private final AtomicReference<String> receivedAuthHeader = new AtomicReference<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody = "";
    private volatile long delayMillis = 0;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/chat/completions", (HttpExchange exchange) -> {
            receivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try (InputStream in = exchange.getRequestBody()) {
                receivedRequestBody.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ignored) {}
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void isConfiguredReturnsFalseWhenFieldsAreBlank() {
        OpenAiCompatibleIssueAiProvider unconfigured = new OpenAiCompatibleIssueAiProvider(
                "", "", "", 5000, objectMapper);
        assertFalse(unconfigured.isConfigured());
        assertThrows(IllegalStateException.class, () -> unconfigured.classify("note"));

        OpenAiCompatibleIssueAiProvider partial = new OpenAiCompatibleIssueAiProvider(
                "http://localhost", "", "gpt-4o", 5000, objectMapper);
        assertFalse(partial.isConfigured());
    }

    @Test
    void classifiesTamilNoteWithExpectedRequestFormatAndAuth() throws Exception {
        String innerContent = "{\"detectedLanguage\":\"ta\",\"englishTranslation\":\"The machine stopped.\",\"category\":\"MACHINE_BREAKDOWN\",\"confidence\":0.95}";
        responseStatus = 200;
        responseBody = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                + objectMapper.writeValueAsString(innerContent) + "}}]}";

        OpenAiCompatibleIssueAiProvider provider = new OpenAiCompatibleIssueAiProvider(
                "http://127.0.0.1:" + port, "secret-test-token", "test-model-4", 5000, objectMapper);
        assertTrue(provider.isConfigured());
        assertEquals("openai-compatible", provider.providerId());
        assertEquals("test-model-4", provider.modelId());

        String result = provider.classify("இயந்திரம் பழுதாகிவிட்டது");
        assertEquals(innerContent, result);

        // Verify Authorization header
        assertEquals("Bearer secret-test-token", receivedAuthHeader.get());

        // Verify request payload only sent original note without extraneous context
        JsonNode payload = objectMapper.readTree(receivedRequestBody.get());
        assertEquals("test-model-4", payload.get("model").asText());
        assertEquals(0, payload.get("temperature").asInt());
        assertEquals("json_object", payload.get("response_format").get("type").asText());
        JsonNode messages = payload.get("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role").asText());
        assertEquals("user", messages.get(1).get("role").asText());
        assertEquals("இயந்திரம் பழுதாகிவிட்டது", messages.get(1).get("content").asText());
    }

    @Test
    void classifiesEnglishAndMixedNotes() throws Exception {
        String enContent = "{\"detectedLanguage\":\"en\",\"englishTranslation\":\"Zipper broke.\",\"category\":\"MATERIAL_SHORTAGE\",\"confidence\":0.85}";
        responseStatus = 200;
        responseBody = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                + objectMapper.writeValueAsString(enContent) + "}}]}";

        OpenAiCompatibleIssueAiProvider provider = new OpenAiCompatibleIssueAiProvider(
                "http://127.0.0.1:" + port, "test-key", "test-model", 5000, objectMapper);

        String result = provider.classify("Zipper broke on assembly line.");
        assertEquals(enContent, result);
    }

    @Test
    void throwsOnProvider500Or401Error() {
        responseStatus = 500;
        responseBody = "{\"error\":{\"message\":\"Internal error\"}}";

        OpenAiCompatibleIssueAiProvider provider = new OpenAiCompatibleIssueAiProvider(
                "http://127.0.0.1:" + port, "test-key", "test-model", 5000, objectMapper);

        assertThrows(RestClientResponseException.class, () -> provider.classify("Some note"));

        responseStatus = 401;
        assertThrows(RestClientResponseException.class, () -> provider.classify("Some note"));
    }

    @Test
    void throwsOnTimeout() {
        delayMillis = 500;
        OpenAiCompatibleIssueAiProvider provider = new OpenAiCompatibleIssueAiProvider(
                "http://127.0.0.1:" + port, "test-key", "test-model", 100, objectMapper);

        assertThrows(ResourceAccessException.class, () -> provider.classify("Slow note"));
    }

    @Test
    void throwsOnMalformedEnvelope() {
        OpenAiCompatibleIssueAiProvider provider = new OpenAiCompatibleIssueAiProvider(
                "http://127.0.0.1:" + port, "test-key", "test-model", 5000, objectMapper);

        // Missing choices
        responseStatus = 200;
        responseBody = "{\"error\": null}";
        assertThrows(IllegalArgumentException.class, () -> provider.classify("Note"));

        // Content is blank
        responseBody = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"   \"}}]}";
        assertThrows(IllegalArgumentException.class, () -> provider.classify("Note"));

        // Not valid JSON at all
        responseBody = "invalid-non-json";
        assertThrows(IllegalArgumentException.class, () -> provider.classify("Note"));
    }
}
