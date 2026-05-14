# DoD Evidence Matrix - Sprint 1

## Scope
- Room/Channel foundation with membership lifecycle.
- Authorization guard for non-member access.
- Regression safety for existing direct chat flow.

## Checklist

| DoD Item | Evidence | Current Status |
|---|---|---|
| Room type model (`DIRECT/GROUP`) exists | `ConversationType`, `ConversationEntity.type` | Done |
| Direct flow regression safe | `Phase1BasicFlowIntegrationTests#createConversation_andList`, `#sendMessage_andGetHistory` | Done |
| Membership lifecycle implemented | `joinConversation`, `leaveConversation`, `listMembers` + controller endpoints | Done |
| Non-member access blocked | `Phase1MembershipReadModelIntegrationTests#groupMembership_joinLeaveAndAuthorization` (non-member send/history fail) | Done |
| Projection consistency in same transaction | create/join call projection initializer; leave deletes list-view row | Done |
| Concurrency baseline retained | `concurrentSend_updatesUnreadCountCorrectly` | Done |
| Build gate | `mvn -q test` pass | Done |
| Local infra smoke checklist exists | `SPRINT1_SMOKE.md` | Done |

## Notes
- This matrix is evidence-oriented and should be updated after each sprint verification run.

# DoD Evidence Matrix - Sprint 2

## Scope
- Per-user read state / read receipt model.
- Unread counter semantics aligned with read receipts.
- Idempotency key path across API -> DB -> outbox -> consumer dedupe.

## Checklist

| DoD Item | Evidence | Current Status |
|---|---|---|
| Read state persistence model exists | `ConversationReadStateEntity`, `ConversationReadStateRepository` | Done |
| Read receipt endpoint + service exists | `POST /api/v1/messages/read`, `ReadReceiptService` | Done |
| Unread reset on mark-read | `ConversationListViewRepository#resetUnreadForUser`, projection updater/service hooks | Done |
| Read receipt happy + failure HTTP tests | `MessageReadHttpIntegrationTests#markRead_happyPath_viaHttp`, `#markRead_nonMember_isRejected` | Done |
| Send-message idempotency contract in API | `SendMessageRequest.idempotencyKey`, `MessageController#send` | Done |
| DB-level idempotency persistence/guard | `MessageEntity` unique constraint `(senderId,conversationId,idempotencyKey)` | Done |
| Service-level replay-safe behavior | `MessageService#send(..., idempotencyKey)` pre-check + unique-race recovery | Done |
| Idempotency integration test (sequential replay) | `Phase1ConcurrencyIdempotencyIntegrationTests#sendMessage_withIdempotencyKey_isReplaySafe` | Done |
| Idempotency integration test (concurrent replay) | `Phase1ConcurrencyIdempotencyIntegrationTests#concurrentSend_sameIdempotencyKey_createsSingleMessage` | Done |
| Consumer-side dedupe for duplicate event | `DeliveredMessageEntity`, `DeliveredMessageRepository`, `DistributedRealtimeMessageConsumer` dedupe check | Done |
| Consumer dedupe test | `DistributedRealtimeMessageConsumerRoutingTest#duplicateEvent_isDedupedPerUser` | Done |
| Build gate | `mvn -q test` | Done |

## Notes
- Current e2e idempotency guarantee is scoped to message-created flow and per-user realtime delivery dedupe.
- Operational replay tooling and DLT replay runbook remain under Sprint 3 scope.

# DoD Evidence Matrix - Sprint 2.5 (Cleanup A->B)

## Scope
- Clean-code hardening for idempotency + distributed consumer path.
- Remove hardcoded infra knobs and centralize event constants.

## Checklist

| DoD Item | Evidence | Current Status |
|---|---|---|
| Event constants centralized | `domain/events/constant/EventConstants` + usage in outbox | Done |
| Kafka payload serialization hardening | `KafkaEventPublisher` uses `ObjectMapper` serialize helper | Done |
| Consumer decomposition | `DistributedRealtimeMessageConsumer` split into parse/dispatch/stat helpers | Done |
| Kafka retry/backoff/concurrency externalized | `KafkaConsumerTuningProperties` + `KafkaConsumerConfig` wiring | Done |
| Lock strategy abstraction exists | `IdempotencyLockManager`, `InMemoryIdempotencyLockManager` | Done |
| Idempotency flow extracted from `MessageService` | `MessageIdempotencyService` (`normalize/find/recover`) | Done |
| Alternative lock strategy integration check | `IdempotencyLockStrategyIntegrationTests#concurrentSend_sameKey_withNoopLock_stillPersistsSingleMessage` | Done |
| Build gate | `mvn -q test` | Pending verification |

# DoD Evidence Matrix - Sprint 3 (Kafka Reliability: Retry, DLT, Replay)

## Scope
- Harden async reliability path with retry classification, replay tooling, and minimal ops visibility.

## Checklist

| DoD Item | Evidence | Current Status |
|---|---|---|
| Retry/backoff policy tunable via config | `OutboxRetryProperties`, `OutboxRetryPolicy` | Done |
| Failure classification (retryable vs non-retryable) | `OutboxPublishExecutor` + `NonRetryableOutboxException` | Done |
| Replay tool with guardrails | `OutboxReplayService`, `POST /api/v1/internal/outbox/replay` (`limit`, `dryRun`) | Done |
| Minimal reliability metrics endpoint | `OutboxMetricsService`, `GET /api/v1/internal/outbox/metrics` | Done |
| Test `retry -> FAILED` | `OutboxReplayIntegrationTests#retryExhausted_movesOutboxItemToFailed` | Done |
| Test `replay -> success` | `OutboxReplayIntegrationTests#replayFailed_reprocessesAndSendsSuccessfully` | Done |
| DLT ingest bridge model | `DltEventEntity`, `DltCaptureService`, `DltIngestConsumer` | Done |
| DLT replay tool + guardrails | `DltReplayService`, `POST /api/v1/internal/dlt/replay` (`limit`, `dryRun`) | Done |
| DLT replay integration tests | `DltReplayIntegrationTests#dltArrival_thenReplay_success`, `#replayFailure_marksFailed_then_secondReplay_doesNotReprocessFailed` | Done |
| Ops runbook | `SPRINT3_RUNBOOK.md` | Done |
| Kafka broker-level DLT E2E (`main topic fail -> broker DLT topic -> listener ingest`) | `KafkaDltE2EIntegrationTests#kafkaE2E_failToDlt_thenReplay_success` (`@EmbeddedKafka`) | Done |
| Build gate | `mvn -q test` | Done |

## Sprint 3 Evidence Freeze
- Frozen at: `2026-05-01 19:56 ICT`
- Verification commands:
  - `mvn -q -Dtest=KafkaDltE2EIntegrationTests test`
  - `mvn -q test`

# DoD Evidence Matrix - Sprint 4 (Read-Model Hardening + Performance)

## Scope
- Projection rebuild hardening with chunked processing.
- Rebuild status observability endpoint.
- Perf smoke evidence for batch-size tradeoff.

## Checklist

| DoD Item | Evidence | Current Status |
|---|---|---|
| Rebuild supports batch/chunk processing | `ConversationListProjectionRebuildService#rebuildAll(int)` | Done |
| Rebuild avoids one-by-one member fetch | `ConversationMemberRepository#findByConversationIdIn` + grouped use in rebuild service | Done |
| Rebuild status snapshot exists | `RebuildStatusSnapshot` (`startedAt`, `finishedAt`, `durationMs`, `processedConversations`, `rebuiltRows`, `batchSize`) | Done |
| Internal ops rebuild endpoint supports batchSize | `POST /api/v1/internal/conversation-list/rebuild?batchSize=...` | Done |
| Internal ops status endpoint exists | `GET /api/v1/internal/conversation-list/rebuild/status` | Done |
| Integration test for rebuild + status correctness | `ConversationListProjectionRebuildIntegrationTests` | Done |
| HTTP integration test for internal ops rebuild/status | `InternalOpsConversationListHttpIntegrationTests` | Done |
| Perf smoke test for batch comparison | `ConversationListPerformanceSmokeTests` (sample result: `batch100=1278ms`, `batch300=1018ms`, `rows=240`) | Done |
| Build gate | `mvn -q test` | Done |

# DoD Evidence Matrix - Sprint 4.2 (Closeout)

## Scope
- Operational guardrail for rebuild execution safety.
- PostgreSQL benchmark/explain runbook for final Sprint 4 evidence.

| DoD Item | Evidence | Current Status |
|---|---|---|
| Parallel rebuild is rejected | `ConversationListProjectionRebuildService` lock + `ResponseCode.REBUILD_IN_PROGRESS` | Done |
| Rebuild status exposes running signal | `RebuildStatusSnapshot.inProgress` + `/api/v1/internal/conversation-list/rebuild/status` | Done |
| Concurrency integration coverage for rebuild guardrail | `ConversationListProjectionRebuildIntegrationTests#rebuildAll_rejectsConcurrentRunWithConflictCode` | Done |
| HTTP-level conflict coverage for in-progress rebuild | `InternalOpsConversationListHttpIntegrationTests#rebuildAsync_whenInProgress_syncRebuildIsRejected` | Done |
| PostgreSQL benchmark + EXPLAIN runbook available | `SPRINT4_RUNBOOK.md` | Done |
| PostgreSQL benchmark numbers captured in matrix | Postgres container benchmark (3 runs, median): `LIMIT 50=0.45ms`, `100=0.52ms`, `200=0.54ms`, `300=0.65ms` | Done |
| Sprint 4 official evidence freeze created | `SPRINT4_EVIDENCE_FREEZE.md` | Done |

# DoD Evidence Matrix - Sprint 6.1 (Distributed Rebalance Proof)

## Scope
- Multi-instance ownership routing verification under Kafka consumer-group rebalance.
- Failover behavior when one consumer node goes down.
- Duplicate fanout guard in rebalance window.

| DoD Item | Evidence | Current Status |
|---|---|---|
| Embedded Kafka test with `>=2` partitions | `DistributedRealtimeRebalanceIntegrationTests` (`@EmbeddedKafka(partitions = 2)`) | Done |
| Multi-instance simulation with distinct ownership contexts | Two listener containers (`node-a`, `node-b`) + per-node ownership sets in `DistributedRealtimeRebalanceIntegrationTests#createNode` | Done |
| Ownership routing validated after rebalance | `rebalanceAndFailover_routesToCurrentOwnerNode` asserts delivery to current owner set | Done |
| Duplicate fanout prevented during rebalance window | same test re-sends identical `messageId` and asserts single delivery per user on active node | Done |
| Failure path: one node down then rebalance continues delivery | same test stops `node-a`, waits `node-b` owns both partitions, verifies delivery continues | Done |
| Regression safety with existing realtime/Kafka tests | `mvn -q -Dtest=DistributedRealtimeRebalanceIntegrationTests,DistributedRealtimeMessageConsumerRoutingTest,KafkaDltE2EIntegrationTests test` | Done |
| Worker runtime role guard for async components | `OutboxPublisherWorker`, `DistributedRealtimeMessageConsumer`, `DltIngestConsumer` gated by `app.runtime.role` condition | Done |
| Split runtime profile overlays exist | `application-api.yaml`, `application-worker.yaml` | Done |
| Worker extraction architecture/rollout/rollback note | `SPRINT6_1_WORKER_RUNTIME_NOTE.md` | Done |

# DoD Evidence Matrix - Sprint 6.B (Cleanup Hardening Before CQRS Deepening)

## Scope
- Cleanup-only hardening on projection/outbox/realtime/presence/internal ops paths.
- Preserve public behavior while reducing long methods, duplication, and inline constants.
- Strengthen full-suite reproducibility by removing unnecessary Redis dependency from selected HTTP integration tests.

| DoD Item | Evidence | Current Status |
|---|---|---|
| Projection rebuild hotspot split into smaller orchestration/runtime steps | `ConversationListProjectionRebuildService` | Done |
| Projection updater ambiguity reduced | `ConversationListProjectionUpdater` helper split (`peer resolution`, `row build`) | Done |
| Realtime consumer reads as explicit staged flow | `DistributedRealtimeMessageConsumer` | Done |
| Outbox publish success/failure/retry transitions isolated | `OutboxPublishExecutor` | Done |
| Replay duplication reduced with lightweight shared helper | `ReplaySupport`, `OutboxReplayService`, `DltReplayService` | Done |
| Presence ownership constants/representation cleaned up | `PresenceService` | Done |
| Internal ops payloads use small response records instead of inline maps where low-risk | `InternalEventOpsController`, `InternalProjectionOpsController` | Done |
| Full-suite HTTP tests no longer require local Redis for message/search/read happy paths | `@MockitoBean PresenceService` in `MessageReadHttpIntegrationTests`, `SearchHttpIntegrationTests`, `GoldenFlowHttpIntegrationTests` | Done |
| Targeted regression commands passed | `ConversationListProjectionRebuildIntegrationTests`, `InternalOpsConversationListHttpIntegrationTests`, `Phase1BasicFlowIntegrationTests`, `Phase1MembershipReadModelIntegrationTests`, `Phase1ConcurrencyIdempotencyIntegrationTests`, `DistributedRealtimeMessageConsumerRoutingTest`, `DistributedRealtimeRebalanceIntegrationTests`, `KafkaDltE2EIntegrationTests`, `OutboxReplayIntegrationTests`, `DltReplayIntegrationTests` | Done |
| Full build gate | `rtk mvn -q test` | Done |
| Cleanup evidence freeze recorded | `SPRINT6B_EVIDENCE_FREEZE.md` | Done |

# DoD Evidence Matrix - Sprint 6.2A (CQRS Command/Read-Model Hardening)

## Scope
- Make CQRS-lite command/read-model boundaries explicit without adding new infrastructure.
- Prove read-model visible-state correctness on send, leave, and rebuild-rerun flows.
- Strengthen read-side authorization coverage and lightweight internal visibility.

| DoD Item | Evidence | Current Status |
|---|---|---|
| CQRS-lite architecture note exists | `SPRINT6_2_CQRS_NOTE.md` | Done |
| Command success is visible immediately on conversation-list read-model | `Sprint62CqrsIntegrationTests#commandPath_send_updatesConversationListReadModelImmediately` | Done |
| Leave flow removes obsolete read-model row | `Sprint62CqrsIntegrationTests#leaveConversation_removesProjectionRowFromReadModel` | Done |
| Rebuild rerun preserves visible read-model state | `Sprint62CqrsIntegrationTests#rebuildAll_rerun_preservesReadModelVisibleState` | Done |
| Rebuild uses persisted read-state when recomputing unread counters | `ConversationListProjectionRebuildService`, `ConversationReadStateRepository#findByConversationIdIn`, `MessageRepository#findByConversationIdIn` | Done |
| Internal projection status exposes read-model visibility fields | `ConversationListProjectionService#getReadModelStatus`, `/api/v1/internal/conversation-list/rebuild/status`, `InternalOpsConversationListHttpIntegrationTests` | Done |
| Non-member history read is rejected | `MessageReadHttpIntegrationTests#history_nonMember_isRejected` | Done |
| Non-member membership read is rejected | `MembershipHttpIntegrationTests#nonMember_listMembers_isRejected` | Done |
| Non-member search cannot read conversation content | `SearchHttpIntegrationTests#searchMessages_nonMemberCannotSeeConversationMessages` | Done |
| Targeted Sprint 6.2A verification command passes | `mvn -q -Dtest=Sprint62CqrsIntegrationTests,ConversationListProjectionRebuildIntegrationTests,InternalOpsConversationListHttpIntegrationTests,MessageReadHttpIntegrationTests test` | Done |

# DoD Evidence Matrix - Sprint 6.2B (Minimal CQRS Observability)

## Scope
- Add one lightweight internal CQRS status snapshot for command path, read-model path, and replay path.
- Expose lag-lite visibility without introducing new infra or durable metrics storage.
- Keep observability local and ops-friendly for showcase/debugging.

| DoD Item | Evidence | Current Status |
|---|---|---|
| Internal CQRS snapshot endpoint exists | `GET /api/v1/internal/cqrs/status`, `InternalCqrsOpsController` | Done |
| Command-path snapshot includes outbox health | `CqrsObservabilityService.CommandPathSnapshot`, `OutboxMetricsService#getSnapshot` | Done |
| Read-model snapshot includes lag-lite visibility | `estimatedLagSeconds`, `latestWriteMessageAt`, `latestProjectedMessageAt` in `CqrsObservabilityService` | Done |
| Replay snapshot includes replay counters + DLT pending | `CqrsObservabilityService.ReplayPathSnapshot` | Done |
| Existing projection status endpoint remains stable | `InternalProjectionOpsController`, `InternalOpsConversationListHttpIntegrationTests` | Done |
| HTTP integration test covers CQRS snapshot payload | `InternalCqrsStatusHttpIntegrationTests#cqrsStatus_exposesMinimalObservabilitySnapshot` | Done |
| Targeted Sprint 6.2B verification command passes | `mvn -q -Dtest=InternalCqrsStatusHttpIntegrationTests,InternalOpsConversationListHttpIntegrationTests,Sprint62CqrsIntegrationTests test` | Done |
