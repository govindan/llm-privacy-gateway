package com.company.privacygateway.api;

import com.company.privacygateway.auth.JwtTokenService;
import com.company.privacygateway.llm.provider.LlmProvider;
import com.company.privacygateway.llm.provider.LlmRequest;
import com.company.privacygateway.llm.provider.LlmResponse;
import com.company.privacygateway.privacy.detection.SensitiveDataDetector;
import com.company.privacygateway.privacy.policy.SensitiveEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndAuditIntegrationTest {

    private static final String TEXT = "Contact John Smith at john.smith@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private SensitiveDataDetector detector;

    @MockitoBean
    private LlmProvider llmProvider;

    @BeforeEach
    void stubCollaborators() {
        when(detector.detect(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            if (!text.contains("John Smith")) {
                return List.of();
            }
            return List.of(
                    entity("PERSON", text, "John Smith"),
                    entity("EMAIL", text, "john.smith@example.com"));
        });
        when(llmProvider.chat(any(LlmRequest.class)))
                .thenAnswer(invocation -> {
                    LlmRequest request = invocation.getArgument(0);
                    return new LlmResponse("Summary about " + request.prompt(), "test-model");
                });
    }

    @Test
    void analyzeRequiresJwtAndWritesAuditWithoutPlaintext() throws Exception {
        mockMvc.perform(post("/api/v1/privacy/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"c-auth","text":"%s"}
                                """.formatted(TEXT)))
                .andExpect(status().isUnauthorized());

        String token = jwtTokenService.mint(
                "user-1", "demo-tenant", false, Instant.now().plus(1, ChronoUnit.HOURS));

        MvcResult result = mockMvc.perform(post("/api/v1/privacy/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"c-auth","text":"%s"}
                                """.formatted(TEXT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sanitizedText").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("John Smith"))))
                .andExpect(jsonPath("$.entities[0].type").exists())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("John Smith").doesNotContain("john.smith@example.com");

        mockMvc.perform(get("/api/v1/audit/conversations/c-auth")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].operation").value("ANALYZE"))
                .andExpect(jsonPath("$.events[0].entityType").exists());

        List<String> metadata = jdbcTemplate.query(
                "SELECT metadata FROM audit_event WHERE conversation_id = ?",
                (rs, rowNum) -> rs.getString(1),
                "c-auth");
        assertThat(metadata).isNotEmpty();
        for (String value : metadata) {
            assertThat(value)
                    .doesNotContain("John Smith")
                    .doesNotContain("john.smith@example.com")
                    .doesNotContain("<<");
        }
    }

    @Test
    void chatUsesReidentifyClaim() throws Exception {
        String unauthorized = jwtTokenService.mint(
                "user-1", "demo-tenant", false, Instant.now().plus(1, ChronoUnit.HOURS));
        String authorized = jwtTokenService.mint(
                "user-1", "demo-tenant", true, Instant.now().plus(1, ChronoUnit.HOURS));

        MvcResult noReid = mockMvc.perform(post("/api/v1/chat")
                        .header("Authorization", "Bearer " + unauthorized)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"c-chat-1","message":"%s"}
                                """.formatted(TEXT)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode noReidJson = objectMapper.readTree(noReid.getResponse().getContentAsString());
        assertThat(noReidJson.path("response").asText())
                .doesNotContain("John Smith")
                .contains("<<PERSON:");

        MvcResult withReid = mockMvc.perform(post("/api/v1/chat")
                        .header("Authorization", "Bearer " + authorized)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"c-chat-2","message":"%s"}
                                """.formatted(TEXT)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode reidJson = objectMapper.readTree(withReid.getResponse().getContentAsString());
        assertThat(reidJson.path("response").asText()).contains("John Smith");
    }

    private static SensitiveEntity entity(String type, String text, String value) {
        int start = text.indexOf(value);
        int startCp = text.codePointCount(0, start);
        int endCp = startCp + value.codePointCount(0, value.length());
        return new SensitiveEntity(type, startCp, endCp, 0.9);
    }
}
