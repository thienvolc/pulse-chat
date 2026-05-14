# Architecture Overview

## 1. Intent

Pulse Chat is a backend showcase for:

- concurrency correctness
- distributed message delivery
- CQRS-lite read/write separation
- operational recovery paths

It is intentionally implemented as a modular monolith to maximize signal-to-complexity ratio.

## 2. Top-Level Structure

Inside [`chat/src/main/java/com/pulse/chat`](./chat):

- `api/rest`
  - HTTP entrypoints
- `app`
  - shared response/error helpers
- `domain/auth`
  - authentication use cases and DTOs
- `domain/chat_core`
  - conversations, membership, messages, read state, read-model projection
- `domain/events`
  - outbox, publisher switching, replay, DLT, CQRS observability
- `domain/notification`
  - realtime fanout and delivery dedupe
- `domain/presence`
  - ownership routing for multi-instance delivery
- `domain/search`
  - Postgres-backed search
- `domain/user`
  - user entity and mapping
- `infrastructure`
  - security, config, adapters, technical wiring

## 3. Core Boundaries

### Command Side

Main command-oriented services:

- `ConversationCommandService`
- `ConversationMembershipService`
- `MessageService`
- `ReadReceiptService`

Responsibilities:

- validate and enforce business rules
- mutate JPA-backed state
- update read-model synchronously where needed
- append outbox events for async delivery

### Query Side

Main query-oriented services:

- `ConversationQueryService`
- `ConversationMembershipQueryService`
- `SearchService`

Responsibilities:

- return read-ready data
- enforce read-side access
- avoid mixing write orchestration into query paths

### Read-Model / Projection Side

Main projection components:

- `ConversationListProjectionUpdater`
- `ConversationListProjectionRebuildService`
- `ConversationListProjectionRebuildProcessor`
- `ConversationListViewRepository`

Responsibilities:

- keep `conversation_list_view` updated on command path
- rebuild projection from source-of-truth tables
- expose rebuild/read-model status for internal ops

## 4. Main Flows

### Flow A: Send Message

1. API receives `POST /api/v1/messages`
2. `MessageService` authorizes member access
3. request content is normalized and validated
4. idempotency key is checked / locked / recovered if needed
5. `MessageEntity` is persisted
6. conversation-list read model is updated synchronously
7. outbox event is appended in the same transaction
8. async publisher/consumer path handles downstream realtime delivery

Why this matters:

- visible state is available immediately
- async delivery can fail independently without losing the event

### Flow B: Read Receipt / Unread

1. API receives `POST /api/v1/messages/read`
2. `ReadReceiptService` validates membership
3. read-state table is updated
4. projection unread count is reset/adjusted
5. rebuild path can recompute the same visible state later

Why this matters:

- unread semantics are membership-aware
- runtime projection and rebuild projection are aligned by tests

### Flow C: Membership Change

1. owner adds/removes member, or member leaves
2. membership state changes in source-of-truth table
3. membership projection helper initializes or cleans projection rows
4. read-state cleanup runs on removal/leave
5. rebuild path can restore consistent view from persisted state

### Flow D: Outbox / Kafka / DLT / Replay

1. command transaction appends `event_outbox`
2. publisher worker sends event
3. on retryable failure, retry policy schedules next attempt
4. on repeated failure, item becomes `FAILED` or DLT path is used
5. replay service can re-drive failed/DLT items
6. consumer dedupe prevents duplicate visible delivery

Why this matters:

- demonstrates reliability patterns beyond basic CRUD
- provides operational recovery story, not just happy path

## 5. Distributed Realtime Path

Main pieces:

- `PresenceService`
- `DistributedRealtimeMessageConsumer`
- `ChatRealtimeNotifier`
- delivered-message dedupe repository

Delivery model:

- Kafka event is consumed
- ownership/presence determines which node should deliver to which user
- delivered-message dedupe avoids duplicate user-visible delivery
- rebalance tests prove routing behavior across instances

This is one of the strongest CV/showcase aspects of the repo.

## 6. Why Modular Monolith

Chosen instead of early microservices because:

- local development is cheaper and faster
- business and reliability logic remain easy to trace
- boundaries can still be discussed clearly in interviews
- outbox, Kafka, DLT, CQRS-lite, and routing already provide enough distributed depth

The project shows system-design maturity without paying the full operational cost of service sprawl.

## 7. Key Tradeoffs

- Full CQRS/event sourcing was intentionally avoided
  - would add complexity beyond showcase ROI
- Elasticsearch was intentionally avoided
  - Postgres search is sufficient for current scope
- Rich product features were intentionally avoided
  - keeps focus on backend architecture and correctness
- Worker runtime extraction was treated as optional
  - the repo already demonstrates strong boundaries before forcing process split

## 8. Strong Talking Points For Interview

- Why idempotency is enforced at multiple layers
- Why read-model updates are synchronous but delivery is async
- How rebuild/backfill protects against projection drift
- Why outbox + replay + DLT is more convincing than basic Kafka publish
- Why modular monolith was chosen over microservices at this stage
- How multi-instance ownership routing reduces duplicate delivery risk

## 9. Recommended Reviewer Path

If someone wants to understand the repo quickly:

1. Read [`README.md`](../README.md)
2. Read controllers in `api/rest`
3. Read `MessageService`
4. Read projection updater/rebuild classes
5. Read outbox and replay services
6. Read integration tests:
   - `Phase1MembershipReadModelIntegrationTests`
   - `Phase1ConcurrencyIdempotencyIntegrationTests`
   - `KafkaDltE2EIntegrationTests`
   - `DistributedRealtimeRebalanceIntegrationTests`
