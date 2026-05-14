# Sprint 6.2 CQRS Note

## Intent
Make Pulse Chat's current command path and read path explicit without forcing a full CQRS rewrite.

## Current Model
Pulse Chat is a modular monolith with **CQRS-lite** behavior.

- Some reads already use a materialized read-model.
- Some reads still intentionally use the write model directly.
- Writes remain transaction-first, with async delivery concerns handled by outbox/Kafka.

## Command Path

### Conversation create / membership mutate
- `ConversationCommandService` writes conversation + members.
- `ConversationMembershipService` writes join/leave changes.
- Both update `conversation_list_view` through `ConversationListProjectionService`.

### Message send
- `MessageService#send(...)` validates membership and message policy.
- Message is persisted to the write model (`messages`).
- `conversation_list_view` is updated synchronously in the same transaction.
- `OutboxService` appends `message-created` for async delivery/replay reliability.

## Read Path

### Read-model backed
- `GET /api/v1/conversations`
- Backed by `conversation_list_view` through `ConversationQueryService`

This path is optimized for UI-friendly conversation listing:
- peer summary
- last message snippet/time
- unread count

### Write-model backed
- `GET /api/v1/messages`
- `GET /api/v1/search/messages`

These reads still use the primary message store because:
- source-of-truth correctness matters more than denormalized speed right now
- scope stays smaller than introducing a second message read store

## Consistency Model

### Synchronous consistency
- Conversation list projection updates triggered by conversation create/join/leave/send/read happen in-process.
- For current scope, the user should observe conversation-list changes immediately after a successful command.

### Asynchronous consistency
- Realtime fanout and distributed delivery are async via outbox/Kafka/consumer flow.
- Replay/DLT/rebuild tooling exists for operational recovery, not for normal request correctness.

## What Is Intentionally Not CQRS Yet
- Message history is not projected into a separate read store.
- Search is not backed by a dedicated search index.
- No event versioning or shared schema module is introduced in Sprint 6.2.
- No projection lag based on stream offsets is computed.

## Sprint 6.2 Goal
Strengthen this CQRS-lite model by making it:
- explicit in docs
- testable at command -> read-model boundaries
- observable enough for rebuild/read-model operations

without changing the architecture style of the repo.
