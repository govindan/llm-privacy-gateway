package com.company.privacygateway.llm.provider;

import com.company.privacygateway.configuration.LlmProperties;
import com.company.privacygateway.llm.ollama.OllamaLlmProvider;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaLlmProviderTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsControlledErrorWhenOllamaUnavailable() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            byte[] body = "{\"error\":\"boom\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        LlmProperties properties = new LlmProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setModel("test-model");
        properties.setTimeoutSeconds(2);

        OllamaLlmProvider provider = new OllamaLlmProvider(properties);

        assertThatThrownBy(() -> provider.chat(new LlmRequest("hello")))
                .isInstanceOf(LlmProviderException.class)
                .hasFieldOrPropertyWithValue("code", "LLM_UNAVAILABLE");
    }

    @Test
    void failsClosedWhenModelMissing() {
        LlmProperties properties = new LlmProperties();
        properties.setBaseUrl("http://127.0.0.1:9");
        properties.setModel(" ");

        OllamaLlmProvider provider = new OllamaLlmProvider(properties);

        assertThatThrownBy(() -> provider.chat(new LlmRequest("hello")))
                .isInstanceOf(LlmProviderException.class)
                .hasFieldOrPropertyWithValue("code", "LLM_MODEL_MISSING");
    }

    @Test
    void chatReturnsAssistantContent() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            byte[] response = """
                    {"model":"test-model","message":{"role":"assistant","content":"ok"}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        LlmProperties properties = new LlmProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setModel("test-model");

        OllamaLlmProvider provider = new OllamaLlmProvider(properties);
        LlmResponse response = provider.chat(new LlmRequest("<<PERSON:AABBCC>>"));

        assertThat(response.content()).isEqualTo("ok");
        assertThat(response.model()).isEqualTo("test-model");
    }
}
