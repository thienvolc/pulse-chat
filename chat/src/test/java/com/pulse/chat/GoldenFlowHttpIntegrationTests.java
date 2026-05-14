package com.pulse.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.chat.domain.presence.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GoldenFlowHttpIntegrationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PresenceService presenceService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void goldenFlow_registerCreateSendSearchHistory() {
        String aliceToken = register(uniqueUsername("gold_alice"));
        String bobToken = register(uniqueUsername("gold_bob"));
        UUID bobId = userIdFromToken(bobToken);

        UUID conversationId = createGroupConversation(aliceToken, List.of(bobId));
        sendMessage(aliceToken, conversationId, "golden-flow-message");

        HttpResponse<String> searchResponse = exchange(
                "GET",
                "/api/v1/search/messages?keyword=golden-flow-message&conversationId=" + conversationId + "&page=0&size=20",
                aliceToken,
                null
        );
        assertThat(searchResponse.statusCode()).isEqualTo(200);
        List<Map<String, Object>> searchContent = readPageContent(searchResponse);
        assertThat(searchContent).isNotEmpty();

        HttpResponse<String> historyResponse = exchange(
                "GET",
                "/api/v1/messages?conversationId=" + conversationId + "&page=0&size=20",
                bobToken,
                null
        );
        assertThat(historyResponse.statusCode()).isEqualTo(200);
        List<Map<String, Object>> historyContent = readPageContent(historyResponse);
        assertThat(historyContent).isNotEmpty();
        assertThat(historyContent).anyMatch(row -> "golden-flow-message".equals(String.valueOf(row.get("content"))));
    }

    private UUID createGroupConversation(String token, List<UUID> memberUserIds) {
        Map<String, Object> request = Map.of("initialMemberUserIds", memberUserIds);
        HttpResponse<String> response = exchange("POST", "/api/v1/conversations/group", token, request);
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> data = castMap(readBodyAsMap(response).get("data"));
        return UUID.fromString(String.valueOf(data.get("conversationId")));
    }

    private void sendMessage(String token, UUID conversationId, String content) {
        HttpResponse<String> response = exchange(
                "POST",
                "/api/v1/messages",
                token,
                Map.of("conversationId", conversationId, "content", content)
        );
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private String register(String username) {
        Map<String, String> request = Map.of("username", username, "password", "password123");
        HttpResponse<String> response = exchange("POST", "/api/v1/auth/register", null, request);
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> data = castMap(readBodyAsMap(response).get("data"));
        return String.valueOf(data.get("accessToken"));
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private UUID userIdFromToken(String token) {
        String[] jwtParts = token.split("\\.");
        assertThat(jwtParts.length).isEqualTo(3);
        try {
            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(jwtParts[1]));
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            return UUID.fromString(String.valueOf(payload.get("sub")));
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse token subject", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readPageContent(HttpResponse<String> response) {
        Map<String, Object> root = readBodyAsMap(response);
        Map<String, Object> page = castMap(root.get("data"));
        return (List<Map<String, Object>>) page.get("content");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private HttpResponse<String> exchange(String method, String path, String bearerToken, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json");
            String bodyJson = body == null ? "" : objectMapper.writeValueAsString(body);
            switch (method) {
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(bodyJson));
                default -> builder.GET();
            }
            if (bearerToken != null) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new RuntimeException("HTTP exchange failed", ex);
        }
    }

    private Map<String, Object> readBodyAsMap(HttpResponse<String> response) {
        try {
            return objectMapper.readValue(response.body(), Map.class);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse response body", ex);
        }
    }
}
