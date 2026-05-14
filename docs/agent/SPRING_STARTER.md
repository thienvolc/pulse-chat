# Spring Starter Style Guide

This document defines the default implementation style for a general-purpose Spring Boot starter, preserving the architecture and coding discipline from `STYLE_CONTRACT.md`.

## 1. Executive Summary
- Use a hybrid structure: top-level layered boundaries with domain-by-feature modules.
- Keep controllers thin and service-driven.
- Keep business logic in domain services, not in controllers/repositories/utils.
- Use explicit mapper classes for Entity/DTO transformations.
- Use centralized error handling with typed business exceptions and response codes.
- Use stateless security filters/config separated into infrastructure.
- Keep utility code technical, stateless, and cross-feature only.
- Keep methods small, readable, and decomposed by intent.
- Prefer consistency over clever abstractions.

## 2. Package and Layering Rules
Top-level base package (example): `com.example.starter`

- `api.rest`
  - HTTP endpoints, request binding, `@Valid`, auth principal extraction.
- `app.aop`
  - Global exception handling and shared exception model.
- `app.dto.response`
  - Shared API response envelope and error response models.
- `app.service`
  - Shared app-level factories (for example response factory).
- `app.util`
  - Request-level generic helpers.
- `domain.<feature>`
  - Feature modules with `constant`, `dto`, `entity`, `mapper`, `repository`, `service`, optional `validation`/`util`.
- `infrastructure.config` and `infrastructure.config.prop`
  - Framework wiring and typed external properties.
- `infrastructure.service`
  - Technical services (JWT, storage, crypto, health, etc.).
- `infrastructure.adapter`
  - Third-party/external system gateways.
- `infrastructure.util` and `infrastructure.constant`
  - Technical helpers/constants.

## 3. Layer Responsibility Matrix
| Layer | Allowed responsibility | Forbidden responsibility | Example type |
|---|---|---|---|
| API | Routes, binding, validation, delegation, auth boundary | Business rules, repository calls | `*Controller` |
| App | Shared response/error contracts, global handlers | Feature orchestration | `ResponseFactory`, `*ExceptionHandler` |
| Domain Service | Use-case orchestration, business rules, transaction boundaries | HTTP and servlet concerns | `*Service` |
| Domain Mapper | Entity/DTO transformation | Persistence/business decisions | `*Mapper` |
| Repository | Persistence contracts, query/fetch tuning | Business workflows, response shaping | `*Repository` |
| Entity | Data model, relationships, minimal helper behavior | Multi-step orchestration | `*Entity` |
| Infrastructure | Security/filter/config/adapters | Feature business logic | `*Config`, `*Filter`, `*Adapter` |

## 4. Dependency Direction Rules
Allowed direction:
- `api -> app, domain`
- `app -> domain, infrastructure (shared technical helpers only)`
- `domain -> domain, infrastructure (selected adapters/services/utils)`
- `infrastructure -> spring/external (+ shared domain constants/exceptions if needed)`

Forbidden direction:
- `repository -> api`
- `entity -> controller`
- `controller -> repository`
- `infrastructure -> owning business workflows`

## 5. Coding Style Rules
- Package names lowercase; multi-word feature packages may use underscore if already established.
- Class naming by role: `*Controller`, `*Service`, `*Repository`, `*Mapper`.
- DTO naming: `*Request`, `*Response`, `*Dto`.
- Prefer Java `record` for request DTOs.
- Constructor injection with `@RequiredArgsConstructor` and `final` fields.
- Keep functions small and single-purpose.
- Use private helper methods (`validateX`, `tryFindX`, `createX`) for readability.
- Use typed business exceptions for rule violations.
- Keep control flow explicit and simple.

## 6. Controller Style
- Annotate with `@RestController` and route annotations.
- Validate input with `@Valid` and bean validation.
- Extract boundary context only (auth principal, request metadata).
- Delegate all business behavior to services.
- Return a consistent response envelope for normal API endpoints.
- Use explicit status codes (`201`, `204`, etc.) when required.

## 7. Service / Application Style
- Services are the use-case layer.
- Keep transactional boundaries in service methods.
- Pattern: validate -> fetch/create -> mutate/persist -> map/return.
- Use repository interfaces only for persistence access.
- Reuse domain services for cross-feature orchestration.

## 8. Repository / Persistence Style
- Repository interfaces extend Spring Data repositories.
- Prefer derived method names first.
- Use `@EntityGraph` for fetch-shape control where mapping needs related entities.
- Use focused custom `@Query` and locking only when required.
- Keep repositories free of business branching.

## 9. Entity / Domain Style
- Explicit JPA mapping (`@Table`, `@Column`, relationships).
- Use `@Enumerated(EnumType.STRING)` for enums.
- Keep entities mostly data-centric with minimal helper behavior.
- Keep auditing/timestamps consistent (DB-managed or explicit, but consistent per module).

## 10. Filter / Security / Config Style
- Security is infrastructure-owned.
- Keep filter chain, auth policy, and JWT logic isolated from domain rules.
- Keep path-level policies centralized in config/constants.
- Keep method-level authorization at endpoint boundary when needed.

## 11. Utils and Shared Code Rules
Allowed:
- Stateless technical helpers (pagination/date/encoding/i18n/request helpers).

Forbidden:
- Domain/business logic in util classes.
- Feature orchestration in shared helpers.

## 12. Reuse and Design Pattern Catalog
| Pattern | When to use | Avoid when |
|---|---|---|
| Response factory + envelope | Standard REST endpoints | Provider callback requiring strict raw format |
| BusinessException + ResponseCode | Business validation and rule failures | Low-level technical faults better handled globally |
| Manual mapper service | DTO/Entity boundaries | Direct entity exposure in API |
| Service decomposition helpers | Complex use-cases | One-line pass-through methods |
| Repository fetch tuning (`@EntityGraph`) | Read models with relation access | Simple existence/id checks |
| External adapter layer | Third-party integration | Pure internal domain logic |

## 13. New Feature Implementation Checklist
1. Create `domain/<feature>` module with standard subpackages as needed.
2. Add thin controller endpoint in `api.rest`.
3. Add service orchestration in `domain/<feature>/service`.
4. Add repository interfaces and queries in `domain/<feature>/repository`.
5. Add DTO + mapper for request/response boundaries.
6. Add validation annotations and business validations.
7. Apply `@Transactional` where mutation occurs.
8. Add/extend response codes and exception handling if new rule failures exist.
9. Keep security policy updates centralized in config/constants.
10. Verify consistency with existing naming and package placement.

## 14. Anti-Patterns to Avoid
- Fat controllers with business logic.
- Repositories containing orchestration/business rules.
- Utilities becoming business dumping grounds.
- Returning entities directly from API.
- Cross-layer shortcuts (controller -> repository, infra -> business flow ownership).
- Inconsistent response/error format across normal endpoints.
- Premature abstractions that obscure intent.
