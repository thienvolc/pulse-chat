# Sprint 6.B Evidence Freeze

Frozen at: `2026-05-06 23:13 ICT`

## Scope Frozen
- Cleanup hardening on projection, outbox replay/publish, distributed realtime consumer, presence, and internal ops payloads.
- No public API path change.
- No business feature added.

## Verification Commands (Executed)
From `pulse-chat/chat`:

1. `rtk mvn -q -Dtest=ConversationListProjectionRebuildIntegrationTests,InternalOpsConversationListHttpIntegrationTests,Phase1BasicFlowIntegrationTests,Phase1MembershipReadModelIntegrationTests,Phase1ConcurrencyIdempotencyIntegrationTests test`
2. `rtk mvn -q -Dtest=DistributedRealtimeMessageConsumerRoutingTest,DistributedRealtimeRebalanceIntegrationTests,KafkaDltE2EIntegrationTests test`
3. `rtk mvn -q -Dtest=OutboxReplayIntegrationTests,DltReplayIntegrationTests test`
4. `rtk mvn -q -Dtest=MessageReadHttpIntegrationTests,SearchHttpIntegrationTests,GoldenFlowHttpIntegrationTests test`
5. `rtk mvn -q test`

## Cleanup Notes
- `ConversationListProjectionRebuildService` was split into smaller orchestration/runtime helper steps.
- `ConversationListProjectionUpdater` now isolates peer resolution and row building more clearly.
- `DistributedRealtimeMessageConsumer` now reads as explicit stages: parse -> owned recipients -> dedupe -> mark -> notify.
- `OutboxPublishExecutor` isolates publish success/failure and retry transitions.
- `OutboxReplayService` and `DltReplayService` share lightweight replay helpers via `ReplaySupport`.
- `PresenceService` centralizes Redis constants and ownership representation.
- Internal ops controllers now use small response records instead of inline `Map.of(...)` payloads where low-risk.

## Test Hardening Notes
- `MessageReadHttpIntegrationTests`
- `SearchHttpIntegrationTests`
- `GoldenFlowHttpIntegrationTests`

These HTTP integration tests now override `PresenceService` with `@MockitoBean` so full-suite execution does not depend on a live local Redis instance just to exercise message/search HTTP flows.

## Result
- Full build gate passed locally with `rtk mvn -q test`.
