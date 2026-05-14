package com.pulse.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class InternalOpsConversationListHttpIntegrationTests {
    private static final long WAIT_IN_PROGRESS_TIMEOUT_MS = 10_000;
    private static final long WAIT_IDLE_TIMEOUT_MS = 15_000;
    private static final long REBUILD_RETRY_TIMEOUT_MS = 20_000;
    private static final int POLL_INTERVAL_MS = 100;
    private static final int RETRY_SLEEP_MS = 150;
    private static final int CONFLICT_TEST_SEEDED_CONVERSATIONS = 180;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void rebuildAndStatusEndpoints_workViaHttp() {
        waitUntilNotInProgress();
        String token = register(uniqueUsername("ops"));

        HttpResponse<String> rebuildResponse = postRebuildWithRetry(token, 50);
        assertThat(rebuildResponse.statusCode()).isEqualTo(200);
        Map<String, Object> rebuildData = castMap(readBodyAsMap(rebuildResponse).get("data"));
        assertThat(Integer.parseInt(String.valueOf(rebuildData.get("rebuiltRows")))).isGreaterThanOrEqualTo(0);

        HttpResponse<String> statusResponse = exchange(
                "GET",
                "/api/v1/internal/conversation-list/rebuild/status",
                token,
                null
        );
        assertThat(statusResponse.statusCode()).isEqualTo(200);
        Map<String, Object> statusData = castMap(readBodyAsMap(statusResponse).get("data"));
        assertThat(statusData.get("startedAt")).isNotNull();
        assertThat(statusData.get("finishedAt")).isNotNull();
        assertThat(Integer.parseInt(String.valueOf(statusData.get("batchSize")))).isEqualTo(50);
        assertThat(Long.parseLong(String.valueOf(statusData.get("projectionRowCount")))).isGreaterThanOrEqualTo(0L);
        assertThat(statusData).containsKey("latestProjectedMessageAt");
    }

    @Test
    void rebuildEndpoint_invalidBatchSize_usesMinimumGuardrail() {
        waitUntilNotInProgress();
        String token = register(uniqueUsername("ops2"));
        HttpResponse<String> rebuildResponse = postRebuildWithRetry(token, 0);
        assertThat(rebuildResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> statusResponse = exchange(
                "GET",
                "/api/v1/internal/conversation-list/rebuild/status",
                token,
                null
        );
        assertThat(statusResponse.statusCode()).isEqualTo(200);
        Map<String, Object> statusData = castMap(readBodyAsMap(statusResponse).get("data"));
        assertThat(Integer.parseInt(String.valueOf(statusData.get("batchSize")))).isEqualTo(1);
        assertThat(Long.parseLong(String.valueOf(statusData.get("projectionRowCount")))).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void rebuildAsync_whenInProgress_syncRebuildIsRejected() throws Exception {
        waitUntilNotInProgress();
        String token = register(uniqueUsername("ops3"));
        // Seed enough data so async rebuild keeps running long enough for conflict check.
        String peerToken = register(uniqueUsername("ops4"));
        UUID peerId = userIdFromToken(peerToken);
        for (int i = 0; i < CONFLICT_TEST_SEEDED_CONVERSATIONS; i++) {
            createGroupConversation(token, java.util.List.of(peerId));
        }

        HttpResponse<String> asyncStart = exchange(
                "POST",
                "/api/v1/internal/conversation-list/rebuild/async?batchSize=1",
                token,
                null
        );
        assertThat(asyncStart.statusCode()).isEqualTo(200);

        waitUntilInProgress(token);

        HttpResponse<String> syncWhileRunning = exchange(
                "POST",
                "/api/v1/internal/conversation-list/rebuild?batchSize=1",
                token,
                null
        );
        assertThat(syncWhileRunning.statusCode()).isEqualTo(409);
        assertThat(String.valueOf(readBodyAsMap(syncWhileRunning).get("message")))
                .contains("projection.rebuild.in_progress");
        waitUntilNotInProgress();
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

    private UUID createGroupConversation(String token, java.util.List<UUID> memberUserIds) {
        Map<String, Object> request = Map.of("initialMemberUserIds", memberUserIds);
        HttpResponse<String> response = exchange("POST", "/api/v1/conversations/group", token, request);
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> data = castMap(readBodyAsMap(response).get("data"));
        return UUID.fromString(String.valueOf(data.get("conversationId")));
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

    private void waitUntilInProgress(String token) throws Exception {
        long deadline = System.currentTimeMillis() + WAIT_IN_PROGRESS_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            HttpResponse<String> statusResponse = exchange(
                    "GET",
                    "/api/v1/internal/conversation-list/rebuild/status",
                    token,
                    null
            );
            if (statusResponse.statusCode() == 200) {
                Map<String, Object> statusData = castMap(readBodyAsMap(statusResponse).get("data"));
                if (Boolean.parseBoolean(String.valueOf(statusData.get("inProgress")))) {
                    return;
                }
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new AssertionError("Rebuild did not enter inProgress state within timeout");
    }

    private void waitUntilNotInProgress() {
        String token = register(uniqueUsername("ops_wait"));
        long deadline = System.currentTimeMillis() + WAIT_IDLE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            HttpResponse<String> statusResponse = exchange(
                    "GET",
                    "/api/v1/internal/conversation-list/rebuild/status",
                    token,
                    null
            );
            if (statusResponse.statusCode() == 200) {
                Map<String, Object> statusData = castMap(readBodyAsMap(statusResponse).get("data"));
                if (!Boolean.parseBoolean(String.valueOf(statusData.get("inProgress")))) {
                    return;
                }
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting rebuild to finish", ex);
            }
        }
        throw new AssertionError("Rebuild still in progress after timeout");
    }

    private HttpResponse<String> postRebuildWithRetry(String token, int batchSize) {
        long deadline = System.currentTimeMillis() + REBUILD_RETRY_TIMEOUT_MS;
        HttpResponse<String> latest = null;
        while (System.currentTimeMillis() < deadline) {
            latest = exchange(
                    "POST",
                    "/api/v1/internal/conversation-list/rebuild?batchSize=" + batchSize,
                    token,
                    null
            );
            if (latest.statusCode() == 200) {
                return latest;
            }
            if (latest.statusCode() != 409) {
                return latest;
            }
            try {
                Thread.sleep(RETRY_SLEEP_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while retrying rebuild request", ex);
            }
        }
        return latest;
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
