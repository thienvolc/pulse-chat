# Project Coding Rules

## Architecture
- Use a hybrid style: **layered boundaries** (`api`, `app`, `domain`, `infrastructure`) with **domain-by-feature modules** under `domain/<feature>`.
- Prefer package-by-domain inside `domain` (for example `domain/vehicle`, `domain/rental`) with subpackages `dto`, `entity`, `repository`, `service`, `mapper`, `constant`.
- Dependency direction:
  - `api -> app, domain`
  - `app -> domain, infrastructure (shared technical helpers only)`
  - `domain -> domain, infrastructure (selected adapters/services/utils)`
  - `infrastructure -> spring/external libs (+ shared domain constants/exceptions only when needed)`
- No reverse dependency from lower layers to controller/api.

## Package Placement
- `api.rest`: controllers, endpoint routing, request binding/validation, auth principal extraction.
- `app.service`: shared app-level factories (for example unified response factory).
- `app.aop`: cross-cutting exception model and global handlers.
- `domain/<feature>/service`: use-case orchestration and business rules.
- `domain/<feature>/entity`: JPA entities and small domain helper behavior.
- `domain/<feature>/repository`: Spring Data persistence interfaces.
- `domain/<feature>/dto/request|response|dto`: feature request/response contracts.
- `domain/<feature>/mapper`: explicit manual mapping between entity and DTO.
- `infrastructure/config` + `infrastructure/config/prop`: framework wiring and typed properties.
- `infrastructure/service` + `infrastructure/adapter`: JWT/filter/storage/external API integrations.
- `infrastructure/util` and `app/util`: small stateless technical helpers only.

## Controller Rules
- Controllers expose HTTP only.
- No business logic in controllers.
- No direct repository access.
- Validate input (`@Valid`, bean validation annotations) and delegate to service.
- Use role checks at endpoint boundary (`@PreAuthorize`) where needed.
- Return the repo’s consistent response wrapper pattern (`ResponseDto` via response factory), except protocol-specific callback endpoints that require raw provider response.

## Service/Application Rules
- Services own use-case orchestration.
- Business rules belong in service methods (and minimal entity helper behavior where already established).
- Keep methods small, explicit, and split into private helper steps (`validateX`, `tryFindX`, `createX`).
- Access persistence via repository interfaces.
- Put transaction boundaries on mutating service operations (`@Transactional`), use read-only transactions for read paths where needed.
- Throw typed business errors (`BusinessException` + `ResponseCode`) for rule violations.

## Domain Rules
- Keep domain logic close to the feature module.
- Maintain entity consistency: explicit table/column mapping, enum-as-string, relationship mapping, builder defaults where needed.
- Keep entity behavior minimal (no orchestration in entities).
- Avoid pushing domain decisions into infrastructure utilities.

## Repository/Persistence Rules
- Repositories only handle persistence.
- No HTTP logic.
- No business orchestration.
- Follow existing style:
  - derived query methods for standard access
  - `@EntityGraph` for fetch-shape control
  - focused `@Query`/locking only when needed
- Keep repositories feature-local under `domain/<feature>/repository`.

## Mapper/DTO Rules
- Do not expose entities directly through API.
- Use request/response DTOs consistently (`*Request`, `*Response`, `*Dto`).
- Keep mapping explicit in mapper classes (`@Service` mappers, manual field mapping).
- Perform numeric/type conversions in mapper layer where required.

## Security/Filter Rules
- Keep auth and filter logic isolated in infrastructure security/config/filter classes.
- JWT parsing/validation stays in filter/service, not controllers.
- Path security policy stays centralized (security constants/config).
- Do not mix security plumbing into domain services unless required by existing pattern.

## Utils/Shared Rules
- Utilities must be small, stateless, and generic.
- No dumping-ground utility classes.
- Do not place business rules in util packages.
- Domain-specific helper logic must stay in its domain module.

## Code Quality Rules
- Small methods, clear intent.
- Clear names following existing suffix patterns (`Controller`, `Service`, `Repository`, `Mapper`).
- No clever code.
- No premature abstraction.
- No duplicate logic.
- No cross-layer shortcuts.
- Prefer readability over over-engineering.
- Use Lombok consistently where the project already uses it.
