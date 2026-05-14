# Pulse Chat Sprint Plan

## Planning Principles
- Priority order: `P0 -> P1 -> P2 -> P3`.
- Keep scope small and demoable per sprint.
- Every sprint must end with runnable verification (`mvn -q test`) and feature-level checks.

## Definition of Done (DoD) Checklist
- [ ] Feature has happy-path + failure-path tests.
- [ ] If message flow is touched: concurrency or idempotency checks are included.
- [ ] Local runbook exists (how to run + how to verify expected behavior).
- [ ] `mvn -q test` passes on the sprint branch before merge.

## Sprint 1 (P0) - Channel/Room Foundation
### Goal
Move from direct-only conversation to room/channel-ready domain with membership lifecycle.

### Scope
- Room/channel entity model (`direct`, `group` ready).
- Membership lifecycle APIs: create, join, leave, list members.
- Authorization checks around membership actions.
- Preserve compatibility for current direct conversation flow.

### Deliverables
- Domain + repository + service + API endpoints for room/membership.
- Integration tests for membership transitions and access control.
- Updated internal docs for local verification.

### Verification
- Create room, join/leave members, send message as member succeeds.
- Non-member send/history is rejected.
- `mvn -q test` passes.

## Sprint 2 (P0) - Unread + Read Receipt + Idempotency E2E
### Goal
Make message delivery state trustworthy under retries and concurrent traffic.

### Scope
- Per-user read state / read receipt model.
- Unread counter semantics aligned with read receipts.
- Idempotency key across API -> DB -> event -> consumer.
- Duplicate message protection in consumer side.

### Deliverables
- Read receipt endpoints and service logic.
- Idempotency persistence and validation logic.
- Concurrent/integration tests for duplicate/retry scenarios.

### Verification
- Replayed same request/event does not create duplicate visible effects.
- Unread count remains correct after concurrent send + read updates.
- `mvn -q test` passes.

### Status
- [x] Sprint 2 scope delivered (read state, unread semantics, idempotency E2E, consumer dedupe).
- [x] Sprint 2 deliverables completed (endpoint/service, persistence/validation, concurrent/integration tests).
- [x] Sprint 2 verification satisfied by current repo evidence.

### Done Checklist (Evidence Snapshot)
- [x] Read receipt model + endpoint + service are present.
- [x] Unread reset/read alignment is implemented and covered by tests.
- [x] Idempotency key flow is enforced across API/service/DB with replay-safe behavior.
- [x] Consumer-side duplicate event protection is implemented and tested.
- [x] Build gate evidence recorded (`mvn -q test`) in current DoD matrix.

## Sprint 3 (P0/P1) - Kafka Reliability: Retry, DLT, Replay
### Goal
Harden async delivery pipeline for operational safety.

### Scope
- Retry/backoff policy tuning for consumer.
- DLT routing policy and failure classification.
- Replay tool (internal API or CLI) for DLT/outbox failed items.
- Minimal metrics for retry/DLT visibility.

### Deliverables
- Replay endpoint/command with guardrails.
- Tests for retry -> DLT -> replay success path.
- Ops notes for common incident handling.

### Verification
- Injected consumer failure moves message to DLT after retry budget.
- Replay returns message to normal flow and processes successfully.
- `mvn -q test` passes.

### Status
- [x] Outbox retry/failure classification implemented (retryable vs non-retryable payload failures).
- [x] Replay tool delivered for failed outbox items with guardrails (`limit`, `dryRun`).
- [x] Minimal outbox reliability metrics endpoint delivered (failed/pending/replay counters + oldest failed age).
- [x] Integration tests for `retry -> FAILED` and `replay -> success` added.
- [x] DLT replay bridge implemented (`dlt_events` ingest + replay endpoint + integration tests).
- [x] Full broker-level Kafka E2E (`main topic fail -> Kafka DLT topic -> ingest listener`) delivered with embedded broker test.

## Sprint 4 (P1) - Read-Model Hardening + Performance
### Goal
Make projection/rebuild reliable and scalable as data grows.

### Scope
- Split projection runtime updater vs rebuild/backfill service.
- Batch/chunk rebuild (reduce N+1 pattern).
- Basic index/perf review for key read-model queries.
- Add performance smoke test for rebuild throughput.

### Deliverables
- Refactored projection/rebuild modules.
- Performance report (baseline vs after changes).
- Internal command/runbook for rebuild/backfill.

### Verification
- Rebuild works on seeded large dataset without timeouts.
- Query count/latency trends improve vs baseline.
- `mvn -q test` passes.

## Sprint 4.1 Execution Checklist (Balanced)
### Goal
Ship minimal-but-solid rebuild hardening without over-engineering.

### Checklist
- [x] Add explicit Sprint 4.1 plan + execution checklist.
- [x] Harden projection rebuild with chunked processing (`batchSize`), avoid full-table one-shot loops.
- [x] Reuse batch queries for latest message/member lookup in rebuild path.
- [x] Add rebuild run status snapshot (`startedAt`, `finishedAt`, `durationMs`, `processedConversations`, `rebuiltRows`, `batchSize`).
- [x] Expose internal status endpoint for rebuild observability.
- [x] Add integration test for rebuild + status snapshot correctness.
- [ ] Add perf smoke seed test (medium dataset) and capture baseline numbers.
- [x] Add perf smoke seed test (medium dataset) and capture baseline numbers.
- [x] Document index/perf review notes for `conversation_list_view` query path.

### Execution Notes
- Scope kept small: no migration strategy, no new infra component.
- Keep compatibility: existing rebuild endpoint still works, now supports `batchSize` query param.
- Perf smoke baseline (test profile, H2): `batch100=1278ms`, `batch300=1018ms`, `rows=240`.

## Sprint 4.2 Execution Checklist (Closeout)
### Goal
Close Sprint 4 with safer operations and PostgreSQL-oriented verification workflow.

### Checklist
- [x] Add concurrency guardrail for rebuild (reject parallel rebuild run).
- [x] Expose rebuild in-progress signal via status snapshot.
- [x] Add integration coverage for rebuild lock behavior.
- [x] Add PostgreSQL benchmark + EXPLAIN runbook for local evidence collection.
- [x] Freeze Sprint 4 official evidence (test reports + runbook snapshot).
- [x] Add internal release checklist for rebuild/debug endpoints.
- [x] Capture and freeze PostgreSQL benchmark numbers in DoD matrix (after local Postgres run).

## Sprint 5 (P2) - Search and Internal Ops Polish
### Goal
Improve user-facing utility and developer operations.

### Scope
- Expand Postgres search filters (conversation, sender, time range).
- Internal ops API polish: ownership, replay status, rebuild status.
- Add local observability dashboard checklist (core metrics only).

### Deliverables
- Search API enhancements with tests.
- Internal ops endpoint hardening and docs.
- Simple runbook for local troubleshooting.

### Verification
- Search returns correct scoped results across filters.
- Internal ops endpoints provide actionable debug data.
- `mvn -q test` passes.

### Sprint 5 Execution Checklist (Balanced 60/40)
- [x] Expand Postgres search filters: `conversationId`, `senderId`, `fromTime`, `toTime`.
- [x] Close authorization gap for search by enforcing conversation membership in query path.
- [x] Add HTTP integration tests for search happy path + non-member data isolation.
- [x] Add golden-flow HTTP integration test: `register -> create conversation -> send -> search -> history`.
- [x] Add minimal local observability checklist snapshot to sprint evidence.

## Sprint 6 (P3) - Long-Term Productization
### Goal
Prepare for richer collaboration and long-term maintainability.

### Scope
- Group moderation roles/permissions.
- Policy engine evolution plan (rate-limit/rules).
- Benchmark and architecture notes for scale roadmap.

### Deliverables
- Permission model extensions.
- Design doc for next-scale architecture decisions.
- Benchmark artifacts and tuning recommendations.

### Verification
- Permission checks enforce role boundaries.
- Benchmark scenarios are reproducible locally.
- `mvn -q test` passes.

## Sprint 2.5 (P0/P1) - Cleanup A -> B
### Goal
Stabilize clean-code baseline after Sprint 2 before deeper feature work.

### Checklist A (Quick Wins)
- [x] Centralize event type/version/key constants.
- [x] Replace manual Kafka JSON string building with `ObjectMapper` serialization.
- [x] Split `DistributedRealtimeMessageConsumer` into smaller private steps.
- [x] Externalize Kafka consumer retry/backoff/concurrency tuning from hardcoded literals.

### Checklist B (Balanced Refactor)
- [x] Extract idempotency lock strategy behind interface (`IdempotencyLockManager`).
- [x] Move in-memory lock implementation to dedicated component (`InMemoryIdempotencyLockManager`).
- [x] Extract message idempotency flow (normalize/find/recover) to dedicated service.
- [x] Add dedicated integration tests for multi-instance lock strategy alternatives (future Redis/DB lock).

### Verification
- Targeted tests: message read HTTP + idempotency + distributed consumer routing.
- Full build gate: `mvn -q test`.

## Sprint B2 (P1) - Clean Structure Hardening
### Goal
Reduce technical debt before Sprint 5 by improving separation of concerns and readability without changing external behavior.

### Checklist
- [x] Split internal ops controller by bounded concern (projection/event/presence) while keeping same API paths.
- [x] Refactor projection rebuild orchestration to remove duplicated lock/batch/status setup logic.
- [x] Replace local magic numbers in internal-ops integration test with named constants.
- [x] Keep behavior/API contract unchanged and pass regression tests.

### Verification
- `mvn -q -Dtest=InternalOpsConversationListHttpIntegrationTests,ConversationListProjectionRebuildIntegrationTests test`
- `mvn -q test`

## Sprint 6.1 (P0/P2) - Distributed Rebalance Proof + Worker Extraction Decision
### Goal
Strengthen CV-grade distributed/concurrency evidence with multi-instance routing proof, then decide whether to extract worker runtime.

### Scope
- Multi-instance rebalance integration test using Embedded Kafka (`>1` partition).
- Verify ownership routing correctness when consumer group rebalances across nodes.
- Decision package for optional extraction of `notification/events worker` into a separate runtime while keeping API monolith.

### Checklist A (P0 - Must Do)
- [x] Add Embedded Kafka integration test with topic partitions `>= 2`.
- [x] Simulate multi-instance consumers with distinct `instanceId` ownership contexts.
- [x] Verify message delivery routes only to owner node after rebalance.
- [x] Verify duplicate fanout is prevented during rebalance window.
- [x] Add failure-path test: one consumer down -> rebalance -> remaining consumer continues delivery.
- [x] Capture test evidence snapshot (test class + run command + key assertions) in DoD matrix.

### Checklist B (P2 - Optional, Decision-Gated)
- [x] Write architecture note: `monolith API + extracted worker runtime` boundary (responsibilities, contracts, failure modes).
- [x] Define minimal extraction scope: outbox publisher + distributed realtime consumer + DLT replay worker.
- [x] Document rollout/rollback plan and local docker profile for split runtime.
- [x] Decision gate: only implement extraction if A checklist is green and no regression in `mvn -q test`.

### Deliverables
- Rebalance integration test suite for ownership routing.
- Evidence note for distributed behavior under rebalance.
- Worker extraction ADR/decision note (implement or defer with rationale).

### Verification
- `mvn -q -Dtest=*Rebalance* test`
- `mvn -q -Dtest=DistributedRealtimeMessageConsumerRoutingTest,KafkaDltE2EIntegrationTests test`
- `mvn -q test`
- If optional extraction is implemented: local split-runtime smoke via docker/profile must pass.

### Status
- [x] Sprint 6.1 completed.

## Sprint 6.B (P1) - Cleanup Hardening Before CQRS Deepening
### Goal
Reduce technical debt on projection/outbox/realtime paths before adding deeper CQRS/read-receipt/idempotency work.

### Scope
- Refactor hotspot classes with mixed responsibilities.
- Improve readability, naming, and separation of concerns.
- Keep public behavior and API contracts stable.
- Strengthen verification baseline for projection/outbox/realtime paths.

### Non-Goals
- No new business feature.
- No architecture rewrite.
- No new infrastructure dependency.
- No API path changes unless explicitly required by tests.

### Checklist A (Quick Wins)
- [x] Split `ConversationListProjectionRebuildService` long methods into small intent-revealing steps.
- [x] Remove inline magic fallback/magic strings in projection/presence paths.
- [x] Split `DistributedRealtimeMessageConsumer` flow into explicit stages: parse -> resolve owner -> dedupe -> persist -> notify.
- [x] Replace internal ops `Map.of(...)` payloads with small response DTOs where cleanup is low-risk.

### Checklist B (Balanced Refactor)
- [x] Separate rebuild orchestration from rebuild runtime/status concerns.
- [x] Refactor `ConversationListProjectionUpdater` to reduce domain lookup and peer-resolution ambiguity.
- [x] Refactor `OutboxPublishExecutor` to isolate payload parsing, publish success path, and failure/retry state transitions.
- [x] Reduce duplication between `OutboxReplayService` and `DltReplayService` with lightweight shared flow structure.
- [x] Improve `PresenceService` expressiveness for ownership data and Redis key/field constants.

### Deliverables
- Smaller, more focused hotspot classes in projection/outbox/realtime modules.
- No behavior regression on rebuild, replay, or distributed delivery flows.
- Updated sprint evidence with targeted regression commands and cleanup notes.

### Verification
- `mvn -q -Dtest=ConversationListProjectionRebuildIntegrationTests test`
- `mvn -q -Dtest=*Replay*,*Outbox*,*Dlt* test`
- `mvn -q -Dtest=*DistributedRealtime*,*Routing*,*Rebalance* test`
- `mvn -q -Dtest=*InternalOps*,*Presence* test`
- `mvn -q test`

### Definition of Done
- [x] Existing public behavior remains unchanged.
- [x] Hotspot methods are shorter and easier to read.
- [x] No critical magic strings/constants remain inline in ownership/projection paths.
- [x] Replay and realtime flows still pass targeted regression tests.
- [x] Full build gate passes with `mvn -q test`.
- [x] Cleanup evidence is recorded in sprint notes/runbook.

### Status
- [x] Sprint 6.B completed.

### Evidence Notes
- Cleanup evidence freeze: `SPRINT6B_EVIDENCE_FREEZE.md`
- Full build gate executed locally with `rtk mvn -q test`, exit code `0`.
- Additional test hardening applied so HTTP message/search/read flows no longer require a live local Redis instance during full-suite execution.

## Sprint 6.2 (P0/P1) - CQRS Command/Read-Model Hardening
### Goal
Make `command path vs read-model path` explicit, testable, and operationally observable without adding new major infrastructure.

### Scope
- Clarify and enforce CQRS-lite boundaries:
  - Command path: write + outbox/event publication.
  - Read path: projection/read-model query.
- Read-model consistency guardrails and status visibility.
- Authorization hardening on read side.
- Keep current event contract stable; no versioning expansion in this sprint.

### Checklist A (P0 - Must Do)
- [x] Add concise architecture note for command/read-model flow and consistency model.
- [x] Add/complete projection lag or rebuild status visibility endpoint for read-model ops.
- [x] Add integration tests: command success -> read-model visible state correctness.
- [x] Add negative integration tests: non-member/non-owner rejected on read endpoints.
- [x] Add rebuild/backfill safety test to ensure no data corruption on rerun.

## Sprint 6.2 Execution Checklist (Balanced, No Versioning Scope)
### Step 1. Boundary Note
- [x] Add `SPRINT6_2_CQRS_NOTE.md`:
  - command path today
  - read path today
  - sync vs async consistency rules
  - what is intentionally not CQRS yet

### Step 2. Correctness Tests
- [x] Add integration test: create conversation -> send message -> `listConversations` reflects last message/unread/read-model state.
- [x] Add integration test: member leave -> read-model no longer returns removed member row.
- [x] Add rebuild/backfill rerun safety test: running rebuild twice keeps stable row count and stable visible state.

### Step 3. Read-Side Authorization
- [x] Add/confirm negative integration tests for read endpoints:
  - non-member history rejected
  - non-member membership list rejected
  - non-member search returns no data / policy result

### Step 4. Ops Visibility
- [x] Extend internal projection status payload with lightweight read-model visibility:
  - projection row count
  - latest projected message timestamp if available
  - rebuild in-progress / last rebuild status
- [x] Keep endpoint stable at `/api/v1/internal/conversation-list/rebuild/status`.

### Step 5. Evidence Freeze
- [x] Update `DOD_EVIDENCE_MATRIX.md` for Sprint 6.2A.
- [x] Create `SPRINT6_2A_EVIDENCE_FREEZE.md`.
- [x] Record final verification commands and local assumptions.

### Checklist B (P1 - Balanced, Current Scope)
- [x] Add minimal CQRS observability counters snapshot:
  - outbox pending/failed
  - projection status / lag-lite visibility
  - replay success/failure
- [x] Freeze sprint evidence (test report + runbook snapshot + DoD matrix updates).

### Out of Scope
- Elasticsearch/search advanced.
- Full schema registry or dedicated shared event-schema module extraction.
- Event versioning expansion beyond current stable payload/envelope.

### Deliverables
- CQRS flow note + runbook updates.
- Integration tests for command/read-model correctness and read-side authorization.
- Event version compatibility tests (lightweight).
- Evidence freeze for reproducible verification.

### Verification
- `mvn -q -Dtest=*Projection*,*ConversationList*,*Read*,*Membership* test`
- `mvn -q -Dtest=*Outbox*,*Dlt*,*Idempotency* test`
- `mvn -q test`

## Sprint 7 (P1) - Room Membership + Read/Unread Completion (CV Scope)
### Goal
Finish the room/membership and read/unread story enough to look like a real chat backend, without expanding into product-heavy collaboration features.

### Scope
- Strengthen `GROUP` room semantics with explicit room metadata and ownership.
- Tighten membership lifecycle rules with a minimal role model.
- Make unread/read behavior membership-aware and rebuild-safe.
- Preserve current modular-monolith + CQRS-lite direction.

### Non-Goals
- No thread/reply system.
- No reactions, mentions, attachments, mute/archive/pin.
- No invite-link or join-request workflow.
- No full per-message seen-by matrix for group chat.
- No new infrastructure dependency.

### Execution Checklist
#### Step 1. Room Metadata and Ownership
- [x] Add `roomName` support for `GROUP` conversations only.
- [x] Add explicit `ownerId` on room/group aggregate.
- [x] Enforce validation: direct conversation does not carry room metadata.
- [x] Add/update DTO/API coverage so room name is visible on room-oriented responses where needed.

#### Step 2. Minimal Membership Role Model
- [x] Introduce minimal membership role model:
  - `OWNER`
  - `MEMBER`
- [x] Persist membership role on conversation membership records.
- [x] Ensure direct conversations do not expose unnecessary room-role behavior.

#### Step 3. Membership Policy Hardening
- [x] Owner can add member to group room.
- [x] Owner can remove member from group room.
- [x] Member can leave group room.
- [x] Owner cannot leave room if it would orphan the room.
- [x] Non-member cannot list members, send messages, read history, search, or mark read.
- [x] Add policy/service tests for allowed and forbidden transitions.

#### Step 4. Membership-Aware Read/Unread Semantics
- [x] Define unread baseline for newly joined member:
  - messages created before join are not counted as unread
- [x] Ensure unread only counts messages visible within active membership window.
- [x] Ensure removed/left member no longer keeps stale conversation-list rows.
- [x] Ensure read-state cleanup/update rules are explicit after leave/remove.
- [x] Verify rebuild/backfill recomputes unread correctly with membership-aware rules.

#### Step 5. Read-Model and CQRS-Lite Consistency
- [x] Update projection path to create/delete/update room list rows according to membership changes.
- [x] Keep command-path updates and rebuild-path recomputation logically aligned.
- [x] Add at least one test proving runtime projection result matches rebuild result after membership transitions.

#### Step 6. API and Test Coverage
 - [x] Add HTTP integration tests for:
    - owner add member
    - owner remove member
    - member leave room
    - owner orphan-room leave rejection
    - non-member access rejection
 - [x] Add integration tests for unread/read behavior around:
    - join after existing messages
    - leave/remove cleanup
    - rebuild after membership changes
 - [x] Add at least one concurrency-safe regression if unread logic changes command path behavior.

### Deliverables
- Group room ownership + room metadata baseline.
- Minimal role-based membership rules (`OWNER`, `MEMBER`).
- Membership-aware unread/read semantics with rebuild-safe evidence.
- Integration/HTTP evidence strong enough for CV discussion and demo.

### Definition of Done
- [x] Group room has explicit owner and valid room metadata.
- [x] Membership transitions are policy-guarded and tested.
- [x] Unread/read semantics are correct for join/leave/remove paths.
- [x] Projection and rebuild produce consistent visible state after membership changes.
- [x] HTTP integration tests cover happy path + forbidden path.
- [x] `mvn -q -Dtest=*Membership*,*Read*,*Projection*,*ConversationList* test` passes.
- [x] `mvn -q test` passes before merge.

### Status
- [x] Sprint 7 completed.

### Evidence Notes
- `MembershipHttpIntegrationTests` covers owner add/remove, member leave, owner orphan-room leave rejection, and non-member rejection.
- `Phase1MembershipReadModelIntegrationTests` covers room metadata, membership policy, late-join unread baseline, remove/leave cleanup, and rebuild alignment.
- `Phase1ConcurrencyIdempotencyIntegrationTests` covers concurrent unread regression and idempotency replay safety.
- Shared authorization guardrail now goes through `ConversationAccessPolicyService` so member/owner checks are reused across membership, message, and read flows.
- Projection equivalence guard now exists in `Phase1MembershipReadModelIntegrationTests` to keep runtime projection semantics aligned with rebuild semantics for membership/read paths.
- Verified locally with `mvn -q -Dtest=*Membership*,*Read*,*Projection*,*ConversationList* test` and `mvn -q test`.

### Stop Line
- Stop after room ownership, minimal roles, membership-aware unread, and rebuild-safe projection are complete.
- Move any richer collaboration feature to backlog unless it clearly strengthens `concurrency`, `distributed`, or `system-design` value for CV.

## Sprint 7.B (P1) - Cleanup Hardening After Sprint 7
### Goal
Reduce technical debt introduced or exposed by Sprint 7 while keeping room/membership/read-unread behavior stable.

### Scope
- Refactor hotspot classes with mixed responsibilities.
- Remove ambiguous or misleading behavior in membership/query paths.
- Improve naming, separation of concerns, and abstraction boundaries.
- Keep public API behavior stable unless a path is intentionally unsupported already.

### Non-Goals
- No new room/product feature.
- No richer role model beyond `OWNER` / `MEMBER`.
- No architecture rewrite into microservices or full DDD rewrite.
- No infra expansion.
- No API contract expansion for collaboration-heavy features.

### Checklist A (Quick Wins)
- [x] Clean `MessageService` code style noise:
  - remove unused imports
  - remove stray blank lines
  - keep method layout consistent
- [x] Make unsupported `joinConversation` intent explicit:
  - rename internal semantics or
  - throw a clearer business error/message for unsupported self-join flow
- [x] Remove ambiguous fallback in `ConversationQueryService`:
  - stop defaulting missing aggregate to `DIRECT`
  - replace with fail-fast or explicit guarded fallback/logging
- [x] Tighten expressive naming where cleanup is low-risk:
  - `assertMember` -> clearer require/check semantics if feasible
  - status/helper names in projection rebuild path

### Checklist B (Balanced Refactor)
- [x] Refactor `ConversationMembershipService` to separate:
  - membership rule/orchestration
  - read-model maintenance side effects
  - read-state/projection cleanup
- [x] Extract membership projection maintenance helper from `ConversationMembershipService`:
  - init row for new member
  - backfill latest visible state
  - cleanup row/read-state on remove/leave
- [x] Refactor `MessageService#sendInternal` into smaller intent-revealing steps:
  - authorize
  - resolve idempotency hit
  - validate/normalize
  - persist/recover unique violation
  - project + publish outbox
- [x] Refactor `ConversationListProjectionRebuildService` to separate:
  - rebuild trigger/locking/runtime status
  - rebuild page loading/data assembly
  - conversation state application
- [x] Reduce abstraction mixing in rebuild flow so business projection rules stay in policy/helper and orchestration stays in service
- [x] Split `Phase1IntegrationTests` by bounded concern:
  - auth/basic flow
  - membership/read-model semantics
  - concurrency/idempotency

### Checklist C (Longer-Term, Only If Cleanup B Still Leaves Friction)
- [x] Reduce or retire `ConversationService` compatibility facade once call sites are clean enough
- [ ] Introduce clearer application-level use-case boundaries for:
  - membership commands
  - message commands
  - projection/rebuild operations
- [ ] Formalize read-model equivalence test structure as its own test suite instead of embedding it in mixed integration spec

### Recommended Refactor Order
1. `ConversationQueryService.java`
2. `MessageService.java`
3. `ConversationMembershipService.java`
4. New helper extracted from membership side effects
5. `ConversationListProjectionRebuildService.java`
6. `Phase1BasicFlowIntegrationTests.java`, `Phase1MembershipReadModelIntegrationTests.java`, `Phase1ConcurrencyIdempotencyIntegrationTests.java`
7. `ConversationService.java` if still needed

### Deliverables
- Smaller, more focused chat-core hotspot classes.
- Clearer unsupported-path semantics.
- Less ambiguous query/read-model behavior.
- Test suite split by concern with projection equivalence guard retained.
- No behavior regression for membership, unread/read, idempotency, or CQRS-lite flows.

### Verification
- `mvn -q -Dtest=Phase1BasicFlowIntegrationTests,Phase1MembershipReadModelIntegrationTests,Phase1ConcurrencyIdempotencyIntegrationTests,MembershipHttpIntegrationTests,MessageReadHttpIntegrationTests test`
- `mvn -q -Dtest=*Projection*,*ConversationList* test`
- `mvn -q -Dtest=*Idempotency*,*Read*,*Membership* test`
- `mvn -q test`

### Definition of Done
- [ ] Unsupported paths are explicit, not ambiguous.
- [ ] No critical fallback behavior silently hides inconsistent state.
- [ ] Hotspot methods/classes are smaller and easier to reason about.
- [ ] Membership logic no longer mixes core rule flow with too many projection cleanup details.
- [ ] Projection rebuild flow has cleaner separation between runtime/status/orchestration/state-application concerns.
- [ ] Test structure is easier to navigate by bounded concern.
- [ ] Full build gate passes with `mvn -q test`.
- [ ] Cleanup evidence is recorded in sprint notes/runbook.

### ROI Priority
- `Quick wins`: High
- `Balanced refactor`: Highest overall ROI
- `Longer-term`: Only if repo continues to grow after Sprint 7

## Sprint 8 (P0) - CV / Showcase Polish
### Goal
Make the repo easier to understand, demo, and discuss in CV or interview settings without expanding product scope.

### Scope
- Add a CV-ready `README.md` at repo root.
- Add one architecture overview doc with system boundaries, core flows, and tradeoffs.
- Make local demo/test entrypoints obvious for reviewers.
- Clarify what is intentionally out of scope so the project stays focused on concurrency/distributed/system-design value.

### Checklist
- [x] Add Sprint 8 polish checklist to `SPRINTS.md`.
- [x] Create repo-root `README.md`:
  - project summary
  - stack
  - architecture snapshot
  - key features
  - local run/test commands
  - demo flows
- [x] Create `ARCHITECTURE_OVERVIEW.md`:
  - module boundaries
  - command path vs read-model path
  - realtime/distributed path
  - outbox/DLT/replay path
  - tradeoffs and non-goals
- [x] Add explicit "What this project demonstrates" section for CV/interview talking points.
- [x] Add clear "Out of scope" section to avoid product-heavy drift.
- [x] Verify docs match current code and commands.

### Deliverables
- A reviewer can understand the repo in under 5 minutes.
- A recruiter/interviewer can map the repo to backend/system-design skills quickly.
- Local setup and key demo/test flows are discoverable from the root docs.

### Verification
- Open `README.md` and confirm all commands/paths exist in the repo.
- Run `mvn -q test` from `chat/` after doc changes to keep branch green.

## Backlog Parking Lot (Not in Current Sprint Train)
- Full microservice decomposition.
- Cassandra/secondary datastore expansion.
- Advanced stream processing beyond current product need.
