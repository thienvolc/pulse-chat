# Sprint 3 Runbook - Outbox Reliability

## Scope
- Verify retry/failure behavior for outbox publish.
- Replay failed outbox items safely.
- Inspect minimal reliability metrics.

## Endpoints
- `POST /api/v1/internal/outbox/replay?dryRun=true`
- `POST /api/v1/internal/outbox/replay?limit=50`
- `GET /api/v1/internal/outbox/metrics`
- `POST /api/v1/internal/dlt/replay?dryRun=true`
- `POST /api/v1/internal/dlt/replay?limit=50`

All `/api/v1/internal/**` endpoints require authenticated JWT.

## Local Verification
1. Create messages through normal API flow to generate outbox rows.
2. Simulate publisher failure (test profile/mocked publisher in integration tests).
3. Confirm failed rows exist using metrics endpoint (`failedCount > 0`).
4. Run dry-run replay to inspect candidate size.
5. Run replay with bounded `limit`.
6. Confirm metrics update (`replaySuccessCount` increases, `failedCount` decreases).

## DLT Replay Verification (Application-level)
1. Ingest a DLT payload through capture/consumer path.
2. Confirm `dltPendingCount` increases in `/outbox/metrics`.
3. Run `POST /api/v1/internal/dlt/replay?dryRun=true`.
4. Run `POST /api/v1/internal/dlt/replay?limit=50`.
5. Verify replay result counters (`processed`, `replayed`, `failed`) and DB status transition.

## Guardrails
- Replay default limit: `50`.
- Replay max limit: `200`.
- Replay only claims rows currently in `FAILED`.

## Kafka Broker E2E Verification
1. Run broker-level E2E test:
   - `mvn -q -Dtest=KafkaDltE2EIntegrationTests test`
2. Expected behavior:
   - Consumer failure routes event to Kafka DLT topic.
   - DLT ingest persists `dlt_events` in `PENDING`.
   - Replay transitions item to `REPLAYED`.
   - Delivered message row exists for replayed message.

## Evidence Freeze
- Sprint 3 freeze timestamp: `2026-05-01 19:56 ICT`.
- Full gate used for freeze: `mvn -q test`.

---

# Sprint 4 Addendum - Read-Model Rebuild

## Internal Ops Endpoints
- `POST /api/v1/internal/conversation-list/rebuild?batchSize=200`
- `GET /api/v1/internal/conversation-list/rebuild/status`

## Verification Commands
1. `mvn -q -Dtest=ConversationListProjectionRebuildIntegrationTests test`
2. `mvn -q -Dtest=InternalOpsConversationListHttpIntegrationTests test`
3. `mvn -q -Dtest=ConversationListPerformanceSmokeTests test`

## Query/Index Notes
- Conversation list read path uses `findByUserIdOrderByLastMessageAtDescCreatedAtDesc`.
- Existing index `idx_conversation_list_user_last_msg (userId,lastMessageAt)` supports the sort/filter hotspot.
- Rebuild path now uses batched conversation paging + `conversationId in (...)` member fetch + grouped latest-message fetch to reduce one-by-one query pattern.
