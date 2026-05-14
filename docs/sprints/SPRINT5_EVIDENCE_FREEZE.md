# Sprint 5 Evidence Freeze (Official)

Frozen at: `2026-05-01 23:22 ICT`

## Scope Frozen
- Sprint 5 (P2): Search and Internal Ops Polish (balanced 60/40).
- Search filter extension (`conversationId`, `senderId`, `fromTime`, `toTime`).
- Search authorization hardening (member-only visibility).
- Golden-flow HTTP integration for core user journey.

## Verification Commands (Executed)
From `pulse-chat/chat`:

1. `mvn -q -Dtest=SearchHttpIntegrationTests,GoldenFlowHttpIntegrationTests test`
2. `mvn -q test`

## Test Report Snapshot
- `chat/target/surefire-reports/com.pulse.chat.SearchHttpIntegrationTests.txt`
  - `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- `chat/target/surefire-reports/com.pulse.chat.GoldenFlowHttpIntegrationTests.txt`
  - `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `chat/target/surefire-reports/com.pulse.chat.InternalOpsConversationListHttpIntegrationTests.txt`
  - `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

## Execution Snapshot (This Freeze)
- `2026-05-01 23:19 ICT`: executed `mvn -q test` on local machine, exit code `0`.
- `2026-05-01 23:20 ICT`: verified surefire reports for Sprint 5 core gates (search, golden flow, internal ops HTTP) all green.

## Feature Evidence (Code)
- Search API:
  - `chat/src/main/java/com/pulse/chat/api/rest/SearchController.java`
- Search service:
  - `chat/src/main/java/com/pulse/chat/domain/search/service/SearchService.java`
- Search query/membership guard:
  - `chat/src/main/java/com/pulse/chat/domain/chat_core/repository/MessageRepository.java`
- Internal ops split controllers:
  - `chat/src/main/java/com/pulse/chat/api/rest/InternalProjectionOpsController.java`
  - `chat/src/main/java/com/pulse/chat/api/rest/InternalEventOpsController.java`
  - `chat/src/main/java/com/pulse/chat/api/rest/InternalPresenceOpsController.java`

## Minimal Local Observability Checklist Snapshot
1. HTTP gate:
   - verify `/actuator/health` responds `UP` (or project health endpoint equivalent).
2. Search path visibility:
   - execute one search request with member token and one with non-member token.
   - confirm non-member request rejected (4xx) or empty-by-policy.
3. Internal ops visibility:
   - call internal projection status endpoint and capture one status payload.
4. Kafka toggle safety:
   - run once with event publish disabled, once enabled.
   - confirm business flow still succeeds when disabled.
5. Build gate:
   - `mvn -q test` must pass before merge/release.

## Notes
- This freeze focuses on app-level search correctness and access control behavior.
- Deep infra benchmark (Kafka throughput, long-run soak) is intentionally out of Sprint 5 scope.
