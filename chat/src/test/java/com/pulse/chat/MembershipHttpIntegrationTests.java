package com.pulse.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import com.pulse.chat.infrastructure.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MembershipHttpIntegrationTests {

    @LocalServerPort
    private int port;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void membershipEndpoints_ownerAddListRemoveAndMemberLeave_viaHttp() {
        String owner = uniqueUsername("own");
        String member = uniqueUsername("mem");
        String outsider = uniqueUsername("out");
        String extra = uniqueUsername("ext");

        String ownerToken = register(owner);
        String memberToken = register(member);
        String outsiderToken = register(outsider);
        String extraToken = register(extra);

        UUID memberId = userIdFromToken(memberToken);
        UUID conversationId = createGroupConversation(ownerToken, List.of(memberId));

        UUID outsiderId = userIdFromToken(outsiderToken);
        UUID extraId = userIdFromToken(extraToken);

        HttpResponse<String> addResponse = exchange(
                "POST",
                "/api/v1/conversations/" + conversationId + "/members",
                ownerToken,
                Map.of("userId", outsiderId)
        );
        assertThat(addResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> listResponse = exchange(
                "GET",
                "/api/v1/conversations/" + conversationId + "/members",
                ownerToken,
                null
        );
        assertThat(listResponse.statusCode()).isEqualTo(200);
        Map<String, Object> data = castMap(readBodyAsMap(listResponse).get("data"));
        List<String> members = ((List<?>) data.get("memberUserIds"))
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        assertThat(members).contains(outsiderId.toString());

        HttpResponse<String> removeResponse = exchange(
                "DELETE",
                "/api/v1/conversations/" + conversationId + "/members/" + outsiderId,
                ownerToken,
                null
        );
        assertThat(removeResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> addExtraResponse = exchange(
                "POST",
                "/api/v1/conversations/" + conversationId + "/members",
                ownerToken,
                Map.of("userId", extraId)
        );
        assertThat(addExtraResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> leaveResponse = exchange(
                "DELETE",
                "/api/v1/conversations/" + conversationId + "/members/me",
                extraToken,
                null
        );
        assertThat(leaveResponse.statusCode()).isEqualTo(200);
    }

    @Test
    void nonMember_listMembers_isRejected() {
        String owner = uniqueUsername("o2");
        String member = uniqueUsername("m2");
        String outsider = uniqueUsername("x2");

        String ownerToken = register(owner);
        String memberToken = register(member);
        String outsiderToken = register(outsider);

        UUID memberId = userIdFromToken(memberToken);
        UUID conversationId = createGroupConversation(ownerToken, List.of(memberId));

        HttpResponse<String> response = exchange(
                "GET",
                "/api/v1/conversations/" + conversationId + "/members",
                outsiderToken,
                null
        );
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(String.valueOf(readBodyAsMap(response).get("message"))).contains("conversation.forbidden");
    }

    @Test
    void createGroup_exposesOwnerAndRoomName_viaCreateAndList() {
        String ownerToken = register(uniqueUsername("grp_owner"));
        String memberToken = register(uniqueUsername("grp_member"));
        UUID ownerId = userIdFromToken(ownerToken);
        UUID memberId = userIdFromToken(memberToken);

        HttpResponse<String> createResponse = exchange(
                "POST",
                "/api/v1/conversations/group",
                ownerToken,
                Map.of(
                        "initialMemberUserIds", List.of(memberId),
                        "roomName", "Platform Guild"
                )
        );
        assertThat(createResponse.statusCode()).isEqualTo(200);
        Map<String, Object> createData = castMap(readBodyAsMap(createResponse).get("data"));
        assertThat(String.valueOf(createData.get("ownerId"))).isEqualTo(ownerId.toString());
        assertThat(String.valueOf(createData.get("roomName"))).isEqualTo("Platform Guild");

        HttpResponse<String> listResponse = exchange("GET", "/api/v1/conversations?page=0&size=20", ownerToken, null);
        assertThat(listResponse.statusCode()).isEqualTo(200);
        Map<String, Object> page = castMap(readBodyAsMap(listResponse).get("data"));
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content)
                .anyMatch(row -> "Platform Guild".equals(String.valueOf(row.get("roomName")))
                        && ownerId.toString().equals(String.valueOf(row.get("ownerId"))));
    }

    @Test
    void nonOwner_addMember_isRejected_and_ownerCannotLeave() {
        String ownerToken = register(uniqueUsername("owner_reject"));
        String memberToken = register(uniqueUsername("member_reject"));
        String outsiderToken = register(uniqueUsername("outsider_reject"));

        UUID memberId = userIdFromToken(memberToken);
        UUID outsiderId = userIdFromToken(outsiderToken);
        UUID conversationId = createGroupConversation(ownerToken, List.of(memberId));

        HttpResponse<String> nonOwnerAdd = exchange(
                "POST",
                "/api/v1/conversations/" + conversationId + "/members",
                memberToken,
                Map.of("userId", outsiderId)
        );
        assertThat(nonOwnerAdd.statusCode()).isEqualTo(403);
        assertThat(String.valueOf(readBodyAsMap(nonOwnerAdd).get("message"))).contains("conversation.owner.required");

        HttpResponse<String> ownerLeave = exchange(
                "DELETE",
                "/api/v1/conversations/" + conversationId + "/members/me",
                ownerToken,
                null
        );
        assertThat(ownerLeave.statusCode()).isEqualTo(400);
        assertThat(String.valueOf(readBodyAsMap(ownerLeave).get("message"))).contains("conversation.owner.cannot_leave");
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
