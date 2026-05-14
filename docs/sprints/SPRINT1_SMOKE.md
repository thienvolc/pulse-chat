# Sprint 1 Smoke Verification (Local)

## Prerequisites
- Docker services up: PostgreSQL, Redis, Kafka.
- App running with profile/config that can reach above services.

## Commands
1. Start infrastructure:
```powershell
docker compose -f infrastructure/docker-compose.yaml up -d
```

2. Run test baseline:
```powershell
cd chat
mvn -q test
```

## Functional Smoke Steps
1. Register 3 users (`owner`, `member`, `outsider`).
2. Create `GROUP` conversation with `owner + member`.
3. Verify `GET /api/v1/conversations/{id}/members` returns owner/member.
4. Try `outsider` send message to group -> must be forbidden.
5. `outsider` joins via `POST /api/v1/conversations/{id}/members/me`.
6. `outsider` send message -> success.
7. `outsider` leaves via `DELETE /api/v1/conversations/{id}/members/me`.
8. `outsider` history call -> forbidden.

## Expected Signals
- HTTP status/response code consistent with `conversation.forbidden` on non-member paths.
- Conversation list rows created for joined members and removed on leave.
- No failing tests in `mvn -q test`.
