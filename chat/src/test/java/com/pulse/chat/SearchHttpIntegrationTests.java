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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SearchHttpIntegrationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PresenceService presenceService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void searchMessages_filtersBySenderAndTimeRange() {
        String aliceToken = register(uniqueUsername("search_alice"));
        String bobToken = register(uniqueUsername("search_bob"));
        UUID bobId = userIdFromToken(bobToken);
        UUID aliceId = userIdFromToken(aliceToken);

        UUID conversationId = createGroupConversation(aliceToken, List.of(bobId));
        sendMessage(aliceToken, conversationId, "alpha-from-alice");
        sendMessage(bobToken, conversationId, "beta-from-bob");

        Instant from = Instant.now().minus(5, ChronoUnit.MINUTES);
        Instant to = Instant.now().plus(5, ChronoUnit.MINUTES);

        String query = "/api/v1/search/messages?keyword=from&conversationId=" + conversationId
                + "&senderId=" + aliceId
                + "&fromTime=" + from.toString()
                + "&toTime=" + to.toString()
                + "&page=0&size=20";
        HttpResponse<String> response = exchange("GET", query, aliceToken, null);
        assertThat(response.statusCode()).isEqualTo(200);
        List<Map<String, Object>> content = readPageContent(response);
        assertThat(content).isNotEmpty();
        assertThat(content).allMatch(row -> String.valueOf(row.get("senderId")).equals(aliceId.toString()));
        assertThat(content).allMatch(row -> String.valueOf(row.get("conversationId")).equals(conversationId.toString()));
    }

    @Test
    void searchMessages_nonMemberCannotSeeConversationMessages() {
        String ownerToken = register(uniqueUsername("search_owner"));
        String memberToken = register(uniqueUsername("search_member"));
        String outsiderToken = register(uniqueUsername("search_outsider"));
        UUID memberId = userIdFromToken(memberToken);

        UUID conversationId = createGroupConversation(ownerToken, List.of(memberId));
        sendMessage(ownerToken, conversationId, "secret-message-for-members");

        HttpResponse<String> outsiderResponse = exchange(
                "GET",
                "/api/v1/search/messages?keyword=secret&conversationId=" + conversationId + "&page=0&size=20",
                outsiderToken,
                null
        );
        assertThat(outsiderResponse.statusCode()).isEqualTo(200);
        List<Map<String, Object>> content = readPageContent(outsiderResponse);
        assertThat(content).isEmpty();
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
