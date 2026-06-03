# Pulse Chat

A backend showcase built around the hard parts of chat systems: reliable async delivery, distributed realtime routing, idempotent writes, and read-model separation — all within a modular monolith that keeps complexity visible rather than hidden.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3, Spring Web, Spring Security |
| Persistence | Spring Data JPA, PostgreSQL |
| Messaging | Spring Kafka |
| Realtime | Spring WebSocket (STOMP) |
| Cache / Routing | Redis |
| Auth | JWT (`jjwt`) |
| Utilities | Lombok |

---

## Architecture Overview

```
api/rest          ← HTTP entrypoints, no business logic
domain/
  auth            ← register, login, JWT issuance
  chat_core       ← conversations, membership, message command path, read model
  events          ← outbox, publisher switching, DLT capture, replay
  notification    ← realtime fanout, consumer-side dedupe
  presence        ← online status, instance ownership routing
  search          ← full-text message search (PostgreSQL)
  user            ← user entity
app/              ← shared concerns: error handling, response wrapper
infrastructure/   ← security config, property binding, runtime wiring
```

The project is structured as a **modular monolith**: one deployable unit with well-defined internal boundaries. Each domain module owns its entities, repositories, and use cases. Cross-domain calls go through explicit service interfaces, not shared tables.

---

## Core Features

### 1. Idempotent Message Send

**Intent:** A client retry — caused by timeout, lag, or double-tap — must never create duplicate messages.

**How it works:**
- Client supplies an optional `idempotency-key` per send request.
- Before inserting, the system checks for an existing message matching `(senderId, conversationId, idempotencyKey)`.
- If two concurrent requests race past the pre-check, a `UNIQUE` constraint on the DB catches the collision. The loser catches `DataIntegrityViolationException` and re-queries the winner's result.
- A Redis distributed lock serializes concurrent sends for the same key, eliminating the race window in the common case.

**Result:** Sending the same request twice always returns the same `messageId`. The message appears once.

---

### 2. Outbox Pattern — Guaranteed Event Delivery

**Intent:** A message persisted to the DB must always produce a downstream event — even if Kafka is down at the moment of send.

**How it works:**
- The send-message transaction writes both the `MessageEntity` and an `EventOutboxEntity` in a single DB commit. If the commit fails, neither exists. If it succeeds, the event is guaranteed to be dispatched eventually.
- A background scheduler polls `PENDING` outbox records, claims them (`PENDING → PROCESSING`) with a row-level lock, publishes to Kafka, then marks them `SENT`.
- Failed records transition to `FAILED` and are eligible for replay. The claim query uses an intentional status update to prevent double-processing across concurrent workers.

**Result:** Event delivery is decoupled from the HTTP response. Kafka outages do not cause silent message loss.

---

### 3. Dead Letter Capture & Replay

**Intent:** Events that repeatedly fail consumer-side processing must not be silently discarded — they need to be recoverable.

**How it works:**
- Kafka consumer retries failed events up to a configured limit using Spring Kafka's retry support.
- Exhausted events are captured into a `DeadLetterEventEntity` with full payload, failure reason, and retry count.
- An internal API triggers replay: failed records are re-published to the original topic, with status transitions tracked (`PENDING → REPLAYED` or `FAILED`).
- Dry-run mode lets operators preview what would be replayed before committing.

**Result:** No event is permanently lost. Recovery is operator-initiated, auditable, and safe to re-run.

---

### 4. Distributed Realtime Routing

**Intent:** In a multi-instance deployment, a WebSocket push must reach the specific server instance that holds the target user's connection — not broadcast to all.

**How it works:**
- Each API request calls `markOnline(userId)` + `claimOwnership(userId)`, writing the current `instanceId` into Redis with a configurable TTL.
- When a Kafka consumer receives a `MessageCreated` event, it fetches all conversation members from the DB (one query), then checks which of those users are owned by the current instance.
- Ownership checks for N members are sent as a single Redis pipeline — one round-trip regardless of member count — instead of N sequential `HGET` calls.
- Only users owned by the current instance receive a WebSocket push. Others will be handled by their respective instances consuming from the same Kafka topic.

**Result:** Correct fan-out across instances. No duplicate pushes. Redis overhead is O(1) network round-trips per Kafka event.

---

### 5. Consumer-Side Deduplication

**Intent:** Kafka's at-least-once delivery means a consumer may see the same event more than once. The user must never receive a duplicate notification.

**How it works:**
- Before pushing over WebSocket, the consumer calls `DeliveryRecorder.recordIfNotDuplicate(messageId, userId)`.
- This writes a dedup record in its own `@Transactional` boundary — committed before the WebSocket push, not wrapped around it.
- If the record already exists (unique constraint violation), the push is skipped.
- Separating the DB commit from the WebSocket push prevents a common bug: if the push succeeds but the transaction later rolls back, the next retry would push again.

**Result:** Each user receives each message notification exactly once, even under retry storms or consumer rebalance.

---

### 6. CQRS-lite Read Model

**Intent:** Loading a user's conversation list should not require joining multiple tables at query time.

**How it works:**
- A dedicated `conversation_list_view` table is maintained as a projection. It stores pre-computed fields: last message snippet, last message timestamp, unread count, and peer info.
- The projection is updated inline on every write event: message created, member joined, member left, conversation read.
- If the projection drifts (e.g. after a bug fix or data migration), a rebuild API re-derives the full state from source tables in paginated batches. Each page runs in its own transaction to avoid long-held DB connections.
- Rebuild tracks progress in-memory and exposes a status endpoint so the operation is observable.

**Result:** Conversation list = single table scan, no joins, accurate unread counters, recoverable if it ever goes stale.

---

## API Reference

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Login and receive JWT |
| `POST` | `/api/v1/conversations/direct` | Create a direct conversation |
| `POST` | `/api/v1/conversations/group` | Create a group conversation |
| `GET` | `/api/v1/conversations` | List conversations for the authenticated user |
| `GET` | `/api/v1/conversations/{id}/members` | List members of a conversation |
| `POST` | `/api/v1/conversations/{id}/members` | Add a member to a group |
| `DELETE` | `/api/v1/conversations/{id}/members/{userId}` | Remove a member from a group |
| `DELETE` | `/api/v1/conversations/{id}/members/me` | Leave a group |
| `POST` | `/api/v1/messages` | Send a message |
| `GET` | `/api/v1/messages` | Get message history for a conversation |
| `POST` | `/api/v1/messages/read` | Mark a conversation as read |
| `GET` | `/api/v1/search/messages` | Full-text message search |
| `GET` | `/api/v1/presence/{userId}` | Check if a user is online |
| `GET` | `/api/v1/internal/...` | Internal ops: outbox replay, DLT replay, rebuild |

---

## Local Setup

**Prerequisites:** Java 21, Maven, PostgreSQL, Redis, Kafka

```bash
# Run the application
cd chat
mvn spring-boot:run
```

Environment variables expected (or configure in `application.yml`):

```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_KAFKA_BOOTSTRAP_SERVERS
SPRING_DATA_REDIS_HOST
APP_JWT_SECRET
APP_REALTIME_INSTANCE_ID   # unique per instance; auto-generated if omitted
```

> Tests use H2 in-memory and do not require PostgreSQL, Redis, or Kafka.

```bash
cd chat
mvn -q test
```
