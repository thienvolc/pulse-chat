# Sprint 6.2A Evidence Freeze

Frozen at: `2026-05-06 23:43 ICT`

## Scope Frozen
- CQRS-lite boundary note for command path vs read-model path.
- Read-model correctness coverage for send, leave, and rebuild-rerun flows.
- Lightweight internal projection status visibility.
- Read-side authorization regression coverage for non-member access.

## Verification Commands (Executed)
From `pulse-chat/chat`:

1. `mvn -q -Dtest=Sprint62CqrsIntegrationTests,ConversationListProjectionRebuildIntegrationTests,InternalOpsConversationListHttpIntegrationTests,MessageReadHttpIntegrationTests test`

## Evidence Notes
- `SPRINT6_2_CQRS_NOTE.md` documents current boundary:
  - command path remains synchronous on write side
  - conversation list remains projection-backed on read side
  - history/search are intentionally not migrated to a separate read store yet
- `ConversationListProjectionRebuildService` now rebuilds unread counters from persisted read-state plus message history instead of relying only on latest-message projection.
- `/api/v1/internal/conversation-list/rebuild/status` now exposes:
  - `projectionRowCount`
  - `latestProjectedMessageAt`
  - existing rebuild status snapshot fields
- Read-side authorization evidence is intentionally split by endpoint style:
  - HTTP history reject: `MessageReadHttpIntegrationTests#history_nonMember_isRejected`
  - HTTP membership reject: `MembershipHttpIntegrationTests#nonMember_listMembers_isRejected`
  - HTTP search policy: `SearchHttpIntegrationTests#searchMessages_nonMemberCannotSeeConversationMessages`

## Local Assumptions
- Verification used the existing `test` profile with in-memory H2 setup used by current integration tests.
- Sprint 6.2A does not add event versioning, schema registry work, or async-only projection lag semantics.
- Targeted verification was used instead of full-suite execution because this freeze is for Sprint 6.2A slice only.

## Result
- Sprint 6.2A targeted verification passed locally.
