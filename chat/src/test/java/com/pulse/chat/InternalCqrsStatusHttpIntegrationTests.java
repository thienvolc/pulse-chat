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
class InternalCqrsStatusHttpIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PresenceService presenceService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void cqrsStatus_exposesMinimalObservabilitySnapshot() {
        String ownerToken = register(uniqueUsername("cqrs_ops_owner"));
        String memberToken = register(uniqueUsername("cqrs_ops_member"));
        UUID memberId = userIdFromToken(memberToken);

        UUID conversationId = createGroupConversation(ownerToken, List.of(memberId));
        sendMessage(ownerToken, conversationId, "cqrs-status-message");
        HttpResponse<String> rebuildResponse = exchange(
                "POST",
                "/api/v1/internal/conversation-list/rebuild?batchSize=50",
                ownerToken,
                null
        );
        assertThat(rebuildResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> statusResponse = exchange(
                "GET",
                "/api/v1/internal/cqrs/status",
                ownerToken,
                null
        );
        assertThat(statusResponse.statusCode()).isEqualTo(200);

        Map<String, Object> data = castMap(readBodyAsMap(statusResponse).get("data"));
        Map<String, Object> commandPath = castMap(data.get("commandPath"));
        Map<String, Object> readModel = castMap(data.get("readModel"));
        Map<String, Object> replay = castMap(data.get("replay"));

        assertThat(commandPath.get("latestWriteMessageAt")).isNotNull();
        assertThat(Long.parseLong(String.valueOf(commandPath.get("outboxPendingCount")))).isGreaterThanOrEqualTo(0L);
        assertThat(Long.parseLong(String.valueOf(commandPath.get("outboxFailedCount")))).isGreaterThanOrEqualTo(0L);
        assertThat(Long.parseLong(String.valueOf(commandPath.get("oldestOutboxFailedAgeSeconds")))).isGreaterThanOrEqualTo(-1L);

        assertThat(Boolean.parseBoolean(String.valueOf(readModel.get("rebuildInProgress")))).isFalse();
        assertThat(readModel.get("lastRebuildFinishedAt")).isNotNull();
        assertThat(Long.parseLong(String.valueOf(readModel.get("projectionRowCount")))).isGreaterThan(0L);
        assertThat(readModel.get("latestProjectedMessageAt")).isNotNull();
        assertThat(Long.parseLong(String.valueOf(readModel.get("estimatedLagSeconds")))).isGreaterThanOrEqualTo(0L);

        assertThat(Long.parseLong(String.valueOf(replay.get("replaySuccessCount")))).isGreaterThanOrEqualTo(0L);
        assertThat(Long.parseLong(String.valueOf(replay.get("replayFailCount")))).isGreaterThanOrEqualTo(0L);
        assertThat(Long.parseLong(String.valueOf(replay.get("dltPendingCount")))).isGreaterThanOrEqualTo(0L);
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

    private UUID createGroupConversation(String token, List<UUID> memberUserIds) {
        Map<String, Object> request = Map.of("initialMemberUserIds", memberUserIds);
        HttpResponse<String> response = exchange("POST", "/api/v1/conversations/group", token, request);
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> data = castMap(readBodyAsMap(response).get("data"));
        return UUID.fromString(String.valueOf(data.get("conversationId")));
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
