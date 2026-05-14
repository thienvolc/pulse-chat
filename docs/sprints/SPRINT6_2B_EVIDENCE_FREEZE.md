# Sprint 6.2B Evidence Freeze

Frozen at: `2026-05-07 00:04 ICT`

## Scope Frozen
- Minimal CQRS observability snapshot for internal ops.
- Lag-lite visibility between write-model latest message time and read-model latest projected message time.
- No new infra, no schema versioning, no durable metrics history.

## Verification Commands (Executed)
From `pulse-chat/chat`:

1. `mvn -q -Dtest=InternalCqrsStatusHttpIntegrationTests,InternalOpsConversationListHttpIntegrationTests,Sprint62CqrsIntegrationTests test`

## Evidence Notes
- New internal endpoint:
  - `GET /api/v1/internal/cqrs/status`
- CQRS snapshot groups data into:
  - `commandPath`
  - `readModel`
  - `replay`
- `commandPath` exposes lightweight outbox health:
  - `latestWriteMessageAt`
  - `outboxPendingCount`
  - `outboxFailedCount`
  - `oldestOutboxFailedAgeSeconds`
- `readModel` exposes lag-lite visibility:
  - `rebuildInProgress`
  - `lastRebuildFinishedAt`
  - `projectionRowCount`
  - `latestProjectedMessageAt`
  - `estimatedLagSeconds`
- `replay` exposes replay counters already used by existing event ops:
  - `replaySuccessCount`
  - `replayFailCount`
  - `dltPendingCount`

## Local Assumptions
- Verification used existing `test` profile and H2-backed integration setup.
- HTTP test overrides `PresenceService` with `@MockitoBean` so CQRS observability validation does not depend on local Redis.
- Lag-lite is intentionally approximate and operational, not broker-offset lag or CDC lag.

## Result
- Sprint 6.2B targeted verification passed locally.
