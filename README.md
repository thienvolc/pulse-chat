# Pulse Chat

Pulse Chat is a Java/Spring backend showcase focused on concurrency, distributed delivery, CQRS-lite read models, and operational reliability.

The project is intentionally built as a modular monolith so core backend concerns can be shipped and explained clearly without hiding complexity behind a microservice split.

## What This Project Demonstrates

- Modular monolith design with clear feature boundaries
- JWT-based auth and secured REST APIs
- Direct chat and group chat membership lifecycle
- CQRS-lite separation between command path and conversation-list read model
- Idempotent send-message flow across API, DB, and event publishing
- Outbox pattern for reliable async event delivery
- Kafka-based realtime fanout with retry, DLT, and replay support
- Distributed ownership routing for multi-instance realtime delivery
- Rebuild/backfill workflow for read-model recovery

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Validation
- Spring WebSocket
- Spring Kafka
- PostgreSQL
- Redis
- H2 for tests
- Lombok
- JWT (`jjwt`)

## Repository Layout

- [`chat/`](./chat): main Spring Boot application
- [`SPRINTS.md`](./docs/SPRINTS.md): implementation backlog and execution history
- [`DOD_EVIDENCE_MATRIX.md`](./docs/DOD_EVIDENCE_MATRIX.md): delivery evidence snapshot by sprint
- [`ARCHITECTURE_OVERVIEW.md`](./docs/ARCHITECTURE_OVERVIEW.md): architecture, flows, and tradeoffs
- sprint/runbook files at repo root: frozen evidence for specific milestones

## Architecture Snapshot

Main modules inside `chat/src/main/java/com/pulse/chat/domain`:

- `auth`: register/login and JWT flow
- `chat_core`: conversation, membership, message command path, read receipt, read-model rebuild
- `presence`: ownership and instance routing support
- `notification`: realtime delivery and consumer-side dedupe
- `events`: outbox, publisher switching, DLT capture, replay, CQRS observability
- `search`: Postgres-backed message search
- `user`: user entity and mapping

High-level style:

- API layer in `api/rest`
- Feature modules in `domain/...`
- Shared app concerns in `app/...`
- Security/config/runtime wiring in `infrastructure/...`

## Core Features

### Chat and Membership

- Create direct conversations
- Create group conversations with owner and room name
- Add/remove members
- Member leave flow with orphan-room protection
- Authorization guardrails for non-members

### Message Delivery

- Send message with optional idempotency key
- History query with membership enforcement
- Read receipt / mark-read flow
- Membership-aware unread counters

### CQRS-lite Read Model

- Conversation list projection updated on command path
- Rebuild and backfill support for read-model recovery
- Projection equivalence tests between runtime updates and rebuild path

### Distributed / Reliability

- Outbox persistence in same transaction as message creation
- Switchable event publisher mode
- Kafka consumer retry and failure classification
- DLT capture and replay workflow
- Delivered-message dedupe on consumer side
- Multi-instance ownership routing and rebalance test coverage

## Key API Areas

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/conversations/direct`
- `POST /api/v1/conversations/group`
- `GET /api/v1/conversations`
- `GET /api/v1/conversations/{id}/members`
- `POST /api/v1/conversations/{id}/members`
- `DELETE /api/v1/conversations/{id}/members/{memberUserId}`
- `DELETE /api/v1/conversations/{id}/members/me`
- `POST /api/v1/messages`
- `GET /api/v1/messages`
- `POST /api/v1/messages/read`
- `GET /api/v1/search/messages`
- internal ops endpoints under `/api/v1/internal/...`

## Local Run

Application module:

```bash
cd chat
```

Run tests:

```bash
mvn -q test
```

Run the application:

```bash
mvn spring-boot:run
```

Notes:

- Tests run with H2 and do not require PostgreSQL.
- Production-style local setup is designed around PostgreSQL + Redis + Kafka.
- Some sprint evidence/runbook files at repo root describe local infra and verification flows in more detail.

## Demo Flows

### 1. Basic Chat Flow

- register two users
- create direct conversation
- send message
- fetch conversation list
- fetch message history

### 2. Membership and Read Model Flow

- create group conversation
- add member
- send messages before and after join
- verify unread/read behavior
- remove or leave member
- verify projection row cleanup

### 3. Reliability Flow

- send message
- persist outbox item
- simulate consumer/publisher failure
- observe retry / failed state / DLT
- replay and recover delivery

## Verification Commands

From `chat/`:

```bash
mvn -q test
```

Focused examples used during development:

```bash
mvn -q "-Dtest=MembershipHttpIntegrationTests,Phase1MembershipReadModelIntegrationTests,Sprint62CqrsIntegrationTests" test
mvn -q "-Dtest=ConversationListProjectionRebuildIntegrationTests,ConversationListPerformanceSmokeTests" test
mvn -q "-Dtest=KafkaDltE2EIntegrationTests,OutboxReplayIntegrationTests,IdempotencyLockStrategyIntegrationTests" test
```

## Tradeoffs

- Chosen architecture: modular monolith over microservices
  - faster delivery
  - easier local setup
  - still strong enough to demonstrate boundaries and distributed concerns
- CQRS-lite instead of full CQRS/event sourcing
  - explicit read-model separation
  - less accidental complexity
- Postgres-backed search instead of Elasticsearch
  - enough for showcase scope
  - avoids unnecessary infrastructure

## Out of Scope

- Rich collaboration features like reactions, attachments, threads, mentions
- Advanced full-text/search cluster architecture
- Full microservice decomposition
- Heavy role/permission product modeling beyond `OWNER` and `MEMBER`
- Product-heavy UX concerns

The repo is optimized for backend engineering discussion, not for becoming a full chat product clone.
