package com.company.privacygateway.llm;

import com.company.privacygateway.privacy.detection.SensitiveDataDetector;
import com.company.privacygateway.privacy.policy.SensitiveEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class OllamaRequestBoundaryTest {

    private static final String BANKING_TEXT = """
            John Smith, customer ID CUST123456, contacted the bank from
            john.smith@example.com.

            He claims that account 987654321 was charged €20,000 on
            12 August 2026.

            Transaction reference TX998877.
            """.strip();

    private static final List<String> PROTECTED_ORIGINALS = List.of(
            "John Smith",
            "john.smith@example.com",
            "CUST123456",
            "987654321",
            "TX998877",
            "12 August 2026");

    private static HttpServer ollamaStub;
    private static final AtomicReference<byte[]> capturedBodyBytes = new AtomicReference<>();

    @DynamicPropertySource
    static void ollamaProperties(DynamicPropertyRegistry registry) throws IOException {
        ollamaStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ollamaStub.createContext("/api/chat", exchange -> {
            capturedBodyBytes.set(exchange.getRequestBody().readAllBytes());
            byte[] response = """
                    {"model":"test-model","message":{"role":"assistant","content":"Summary over tokens."}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        ollamaStub.start();
        int port = ollamaStub.getAddress().getPort();
        registry.add("llm.base-url", () -> "http://127.0.0.1:" + port);
        registry.add("llm.model", () -> "test-model");
        registry.add("llm.provider", () -> "ollama");
    }

    @AfterAll
    static void stopStub() {
        if (ollamaStub != null) {
            ollamaStub.stop(0);
        }
    }

    @MockitoBean
    private SensitiveDataDetector detector;

    @Autowired
    private SanitizedLlmService sanitizedLlmService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void stubDetector() {
        capturedBodyBytes.set(null);
        when(detector.detect(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            if (!text.contains("John Smith")) {
                return List.of();
            }
            return List.of(
                    entity("PERSON", text, "John Smith"),
                    entity("CUSTOMER_ID", text, "CUST123456"),
                    entity("EMAIL", text, "john.smith@example.com"),
                    entity("ACCOUNT_NUMBER", text, "987654321"),
                    entity("AMOUNT", text, "€20,000"),
                    entity("DATE", text, "12 August 2026"),
                    entity("TRANSACTION_ID", text, "TX998877"));
        });
    }

    @Test
    void sanitizedPromptReachesOllamaWithoutProtectedOriginals() throws Exception {
        SanitizedLlmService.SanitizedLlmResult result = sanitizedLlmService.chat(
                "demo-tenant", "conv-ollama-boundary", BANKING_TEXT, "user-1", false);

        assertThat(result.llmResponse().content()).isEqualTo("Summary over tokens.");
        assertThat(result.privacy().sanitizedText()).contains("€20,000");

        byte[] rawBytes = capturedBodyBytes.get();
        assertThat(rawBytes).isNotNull().isNotEmpty();
        String rawBody = new String(rawBytes, StandardCharsets.UTF_8);

        assertAbsentAsUtf8(rawBody, PROTECTED_ORIGINALS);
        assertAbsentAfterJsonDecode(rawBody, PROTECTED_ORIGINALS);
        assertAbsentAsUnicodeEscapes(rawBody, PROTECTED_ORIGINALS);

        assertThat(rawBody).contains("€20,000");
        assertThat(rawBody).contains("<<PERSON:");
        assertThat(rawBody).contains("<<EMAIL:");
    }

    private void assertAbsentAsUtf8(String rawBody, List<String> originals) {
        for (String original : originals) {
            assertThat(rawBody)
                    .as("raw HTTP body must not contain UTF-8 substring %s", original)
                    .doesNotContain(original);
        }
    }

    private void assertAbsentAfterJsonDecode(String rawBody, List<String> originals) throws Exception {
        JsonNode root = objectMapper.readTree(rawBody);
        String reEncoded = objectMapper.writeValueAsString(root);
        String content = root.path("messages").path(0).path("content").asText();
        assertThat(content).isNotBlank();

        for (String original : originals) {
            assertThat(reEncoded)
                    .as("JSON-decoded body must not contain %s", original)
                    .doesNotContain(original);
            assertThat(content)
                    .as("Ollama message content must not contain %s", original)
                    .doesNotContain(original);
        }
    }

    private void assertAbsentAsUnicodeEscapes(String rawBody, List<String> originals) {
        for (String original : originals) {
            assertThat(rawBody)
                    .as("raw body must not contain \\uXXXX form of %s", original)
                    .doesNotContain(unicodeEscape(original));
        }
    }

    private static String unicodeEscape(String value) {
        StringBuilder sb = new StringBuilder();
        value.codePoints().forEach(cp -> sb.append(String.format("\\u%04x", cp)));
        return sb.toString();
    }

    private static SensitiveEntity entity(String type, String text, String value) {
        int start = text.indexOf(value);
        int startCp = text.codePointCount(0, start);
        int endCp = startCp + value.codePointCount(0, value.length());
        return new SensitiveEntity(type, startCp, endCp, 0.9);
    }
}
