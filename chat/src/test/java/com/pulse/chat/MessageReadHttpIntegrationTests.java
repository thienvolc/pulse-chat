package com.pulse.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.chat.domain.presence.service.PresenceService;
import com.pulse.chat.infrastructure.service.JwtService;
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
class MessageReadHttpIntegrationTests {

    @LocalServerPort
    private int port;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PresenceService presenceService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void markRead_happyPath_viaHttp() {
        String ownerToken = register(uniqueUsername("ro"));
        String memberToken = register(uniqueUsername("rm"));

        UUID memberId = userIdFromToken(memberToken);
        UUID conversationId = createGroupConversation(ownerToken, List.of(memberId));

        Map<String, Object> sendReq = Map.of(
                "conversationId", conversationId,
                "content", "hello read path"
        );
        HttpResponse<String> sendRes = exchange("POST", "/api/v1/messages", memberToken, sendReq);
        assertThat(sendRes.statusCode()).isEqualTo(200);
        String messageId = String.valueOf(castMap(readBodyAsMap(sendRes).get("data")).get("messageId"));

        Map<String, Object> readReq = Map.of(
                "conversationId", conversationId,
                "lastReadMessageId", UUID.fromString(messageId)
        );
        HttpResponse<String> readRes = exchange("POST", "/api/v1/messages/read", ownerToken, readReq);
        assertThat(readRes.statusCode()).isEqualTo(200);
        Map<String, Object> data = castMap(readBodyAsMap(readRes).get("data"));
        assertThat(data.get("read")).isEqualTo(true);
        assertThat(String.valueOf(data.get("conversationId"))).isEqualTo(conversationId.toString());
    }

    @Test
    void markRead_nonMember_isRejected() {
        String ownerToken = register(uniqueUsername("r2o"));
        String memberToken = register(uniqueUsername("r2m"));
        String outsiderToken = register(uniqueUsername("r2x"));

        UUID memberId = userIdFromToken(memberToken);
        UUID conversationId = createGroupConversation(ownerToken, List.of(memberId));

        Map<String, Object> readReq = Map.of(
                "conversationId", conversationId
        );
        HttpResponse<String> response = exchange("POST", "/api/v1/messages/read", outsiderToken, readReq);
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(String.valueOf(readBodyAsMap(response).get("message"))).contains("conversation.forbidden");
    }

    @Test
    void history_nonMember_isRejected() {
        String ownerToken = register(uniqueUsername("hro"));
        String memberToken = register(uniqueUsername("hrm"));
        String outsiderToken = register(uniqueUsername("hrx"));

        UUID memberId = userIdFromToken(memberToken);
        UUID conversationId = createGroupConversation(ownerToken, List.of(memberId));

        HttpResponse<String> response = exchange(
                "GET",
                "/api/v1/messages?conversationId=" + conversationId + "&page=0&size=20",
                outsiderToken,
                null
        );
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(String.valueOf(readBodyAsMap(response).get("message"))).contains("conversation.forbidden");
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

    private UUID userIdFromToken(String token) {
        return UUID.fromString(jwtService.parse(token).getSubject());
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
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
                case "DELETE" -> builder.DELETE();
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
