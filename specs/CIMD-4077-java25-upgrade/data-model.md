# Phase 1 Data Model

This upgrade changes **no persisted data shapes**. The only data-layer change is *how* the single
viewstore repository is implemented (DeltaSpike interface → concrete JPA bean). The entity and the
read-model schema are untouched.

## Entity: `Submission` (unchanged)

`uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission` — the viewstore read-model row
for a staged submission. Mapped to `java:/DS.stagingprosecutors`; schema owned by
`stagingprosecutors-viewstore-liquibase` (no changelog change in this feature).

| Field | Notes |
|---|---|
| `submissionId` (UUID) | `@Id` |
| `submissionStatus` (String) | |
| `caseUrn` (String) | |
| `ouCode` (String) | |
| `errors` / `warnings` (JsonArray) | via `JsonArrayConverter` (`@Converter`) |
| `caseWarnings` / `defendantWarnings` (JsonArray) | via `JsonArrayConverter` |
| `type` (`SubmissionType` enum) | |
| `receivedAt` / `completedAt` (ZonedDateTime) | |
| `cpsCase` (Boolean) | getter `isCpsCase()` null-safe → false |

> Only change at entity level: `javax.persistence.*` → `jakarta.persistence.*` import swaps in the
> entity, enum, and `JsonArrayConverter`. No column, type, or constraint changes.

## Repository contract: `SubmissionRepository`

**Before** (DeltaSpike): `@Repository public interface SubmissionRepository extends
EntityRepository<Submission, UUID> {}` — inherited `save`, `findBy`, etc.

**After** (concrete JPA bean) — the only two operations actually used by callers:

| Operation | Signature | Implementation | Semantics |
|---|---|---|---|
| Find by id | `Submission findBy(UUID id)` | `entityManager.find(Submission.class, id)` | returns `null` when not found (matches DeltaSpike + existing caller null-checks) |
| Persist/update | `Submission save(Submission s)` | `entityManager.merge(s)` | insert-or-update; returns managed instance |

Wiring: `@ApplicationScoped` class; `@PersistenceContext(unitName = "<viewstore PU name>")
EntityManager entityManager;` (NOT `@Inject`).

### Callers (no change required)

`@Inject SubmissionRepository` is identical for a concrete `@ApplicationScoped` bean. Verified
callers in the event listener/processor use only `submissionRepository.save(...)` and
`submissionRepository.findBy(<uuid>)` followed by a null check — both preserved.

## State transitions

None introduced. Submission lifecycle (received → successful / rejected / pending-with-warnings,
etc.) is driven by domain/public events and is unchanged by this upgrade.
