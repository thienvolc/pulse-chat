# Sprint 4 Evidence Freeze (Official)

Frozen at: `2026-05-01 21:16 ICT`

## Scope Frozen
- Sprint 4.1: Read-model rebuild hardening + status/perf smoke.
- Sprint 4.2: Rebuild concurrency guardrail + async rebuild conflict behavior.

## Verification Commands (Executed)
From `pulse-chat/chat`:

1. `mvn -q clean -Dtest=ConversationListProjectionRebuildIntegrationTests test`
2. `mvn -q -Dtest=InternalOpsConversationListHttpIntegrationTests,ConversationListProjectionRebuildIntegrationTests test`
3. `mvn -q test`

## Test Report Snapshot
- `chat/target/surefire-reports/com.pulse.chat.ConversationListProjectionRebuildIntegrationTests.txt`
  - `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- `chat/target/surefire-reports/com.pulse.chat.InternalOpsConversationListHttpIntegrationTests.txt`
  - `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

## Feature Evidence (Code)
- Rebuild lock/conflict code:
  - `chat/src/main/java/com/pulse/chat/domain/common/constant/ResponseCode.java`
  - `chat/src/main/java/com/pulse/chat/domain/chat_core/readmodel/ConversationListProjectionRebuildService.java`
- Rebuild/status endpoints:
  - `chat/src/main/java/com/pulse/chat/api/rest/InternalOpsController.java`
- Rebuild service facade:
  - `chat/src/main/java/com/pulse/chat/domain/chat_core/readmodel/ConversationListProjectionService.java`
- Integration tests:
  - `chat/src/test/java/com/pulse/chat/ConversationListProjectionRebuildIntegrationTests.java`
  - `chat/src/test/java/com/pulse/chat/InternalOpsConversationListHttpIntegrationTests.java`

## Notes
- PostgreSQL benchmark numeric freeze is still optional/manual and depends on local Postgres runbook execution.

## PostgreSQL Benchmark + EXPLAIN Evidence (Captured)
- Environment:
  - Docker Postgres `postgres:13` (`pulse-postgres`)
  - Dataset seeded directly in `conversation_list_view`: ~200,000 rows (100 users x 2,000 rows/user)
- Target query:
  - `WHERE user_id = <uuid> ORDER BY last_message_at DESC NULLS LAST, created_at DESC LIMIT N`
- EXPLAIN (ANALYZE, BUFFERS) evidence:
  - Planner path used: `Bitmap Index Scan on idx_conversation_list_user_last_msg`
  - No full table scan on target read query.
- 3-run median execution time (ms):
  - `LIMIT 50`: `0.45`
  - `LIMIT 100`: `0.52`
  - `LIMIT 200`: `0.54`
  - `LIMIT 300`: `0.65`
- Caveat:
  - This evidence is query-path benchmark on real Postgres.
  - App-level rebuild benchmark via HTTP was blocked by local JDBC auth mismatch (`password authentication failed for user pulse`) in this machine state.
