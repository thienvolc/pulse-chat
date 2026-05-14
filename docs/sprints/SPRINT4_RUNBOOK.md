# Sprint 4 Runbook - Read-Model Hardening

## Scope
- Validate rebuild chunking and operational safety.
- Collect PostgreSQL-oriented performance evidence.
- Review query/index behavior on conversation list read path.

## Preconditions
1. Start local infra:
   - `docker compose -f infrastructure/docker-compose.yaml up -d postgres redis`
2. Run app with local profile:
   - `mvn -q spring-boot:run -Dspring-boot.run.profiles=local`

## Core Internal Ops APIs
- `POST /api/v1/internal/conversation-list/rebuild?batchSize=200`
- `POST /api/v1/internal/conversation-list/rebuild/async?batchSize=200`
- `GET /api/v1/internal/conversation-list/rebuild/status`

## Verification (Tests)
1. `mvn -q -Dtest=ConversationListProjectionRebuildIntegrationTests test`
2. `mvn -q -Dtest=InternalOpsConversationListHttpIntegrationTests test`
3. `mvn -q -Dtest=ConversationListPerformanceSmokeTests test`
4. `mvn -q test`

## PostgreSQL Benchmark Checklist (Manual)
1. Seed representative data using app API/test helper script.
2. Trigger rebuild with different batch sizes:
   - `batchSize=100`
   - `batchSize=200`
   - `batchSize=300`
3. Record for each run:
   - `durationMs`
   - `processedConversations`
   - `rebuiltRows`
4. Keep median of 3 runs per batch size.

## EXPLAIN Checklist (PostgreSQL)
Target query shape:
- conversation list by `userId`, order by `lastMessageAt desc, createdAt desc`.

Run:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM conversation_list_view
WHERE user_id = '<user-uuid>'
ORDER BY last_message_at DESC NULLS LAST, created_at DESC
LIMIT 50;
```

Expected:
- planner uses `idx_conversation_list_user_last_msg` or equivalent index path.
- no full table scan for normal list query.

### Captured Sample (2026-05-01)
- Dataset: ~200k rows synthetic in `conversation_list_view`.
- Planner: `Bitmap Index Scan on idx_conversation_list_user_last_msg`.
- Execution time median (3 runs):
  - `LIMIT 50`: `0.45ms`
  - `LIMIT 100`: `0.52ms`
  - `LIMIT 200`: `0.54ms`
  - `LIMIT 300`: `0.65ms`

## Operational Guardrail
- Parallel rebuild runs are rejected with conflict response code:
  - `projection.rebuild.in_progress`
- Typical local validation:
  1. call async rebuild
  2. poll `/status` until `inProgress=true`
  3. call sync rebuild and expect `409`

## Internal Release Checklist (Rebuild/Debug Endpoints)
Use this before exposing/using internal ops endpoints in local/staging.

1. Access control
   - Confirm internal endpoints require authenticated/internal role context.
   - Verify non-authorized token/user gets rejected.
2. Endpoint contract smoke
   - `POST /api/v1/internal/conversation-list/rebuild?batchSize=200` returns `200` (or `409` if already running).
   - `POST /api/v1/internal/conversation-list/rebuild/async?batchSize=200` returns `200`.
   - `GET /api/v1/internal/conversation-list/rebuild/status` returns `startedAt/finishedAt/batchSize/inProgress`.
3. Concurrency guardrail
   - Trigger async rebuild, then call sync rebuild and confirm `409 projection.rebuild.in_progress`.
4. Observability
   - Capture one status sample while running (`inProgress=true`) and one after finish (`inProgress=false`).
   - Save command + response sample in release note or ticket.
5. Safety and rollback
   - If rebuild fails or runs too long, stop new triggers and inspect logs.
   - Re-run with smaller `batchSize` (`100`) and compare `durationMs`.
6. Build gate
   - Run `mvn -q test` and ensure pass before release tag/merge.
