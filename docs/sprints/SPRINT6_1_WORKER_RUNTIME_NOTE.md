# Sprint 6.1 - Worker Runtime Extraction Note (Balanced)

## Decision Summary
- Adopt a phased extraction: **separate runtime roles in the same codebase** before full service split.
- Runtime roles:
  - `api`: HTTP/API-focused runtime.
  - `worker`: async event/runtime worker.
  - default `all`: backward-compatible mode for existing local/test flows.

## Scope Implemented
- Worker-only async components are guarded by runtime role:
  - `OutboxPublisherWorker`
  - `DistributedRealtimeMessageConsumer`
  - `DltIngestConsumer`
- New profile overlays:
  - `application-api.yaml` -> `app.runtime.role=api`, `app.events.mode=kafka`
  - `application-worker.yaml` -> `app.runtime.role=worker`, `app.events.mode=kafka`

## Boundary (Monolith API + Extracted Worker Runtime)
- API runtime responsibilities:
  - Auth, conversation/message/read/search/internal HTTP APIs.
  - Command handling and DB writes.
  - Outbox append in same transaction.
  - Kafka publish (via outbox path when worker processes pending items).
- Worker runtime responsibilities:
  - Outbox publish scheduler.
  - Kafka message-created consumer for distributed realtime delivery.
  - Kafka DLT ingest consumer and replay support.

## Failure Modes
- Worker down:
  - API write path still works.
  - Outbox pending grows; replay possible when worker recovers.
- Kafka unavailable:
  - Outbox items remain pending/retry/fail based on policy.
- Redis ownership drift:
  - Delivery routes only to current owner; rebalance tests cover routing correctness.

## Rollout Plan (Local First)
1. Start infra (Postgres/Redis/Kafka).
2. Start API runtime:
   - `mvn -q spring-boot:run -Dspring-boot.run.profiles=local,api`
3. Start Worker runtime in second process:
   - `mvn -q spring-boot:run -Dspring-boot.run.profiles=local,worker`
4. Verify:
   - send message -> outbox pending decreases.
   - Kafka consumer delivers realtime to owned users.
   - DLT replay endpoints behave as expected.

## Rollback Plan
- Immediate rollback to single-process compatibility mode:
  - Run app with `local` profile only (default runtime role `all`).
- No schema/data migration required.
- No API contract change introduced by this phase.

## Next Decision Gate
- If Sprint 6.1/6.2 stability remains green, consider extracting worker as a separately packaged deployable unit.
- If not, keep current dual-profile runtime as the long-lived balanced architecture.
