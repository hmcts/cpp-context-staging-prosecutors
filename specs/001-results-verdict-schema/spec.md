# Feature Specification: Update Results Query Schema for Structured Verdict

**Feature Branch**: `CIMD-3915-results-verdict-schema`
**Created**: 2026-06-16
**Status**: Draft
**Jira**: CIMD-3915
**Scope**: `cpp-context-staging-prosecutors` — query API schema only
**Related spec**: `cpp-context-results/specs/001-informant-register-local-schema`

## Overview

The results context (`cpp-context-results`) is changing the `results.prosecutor-results` query response as part of CIMD-3915: the flat `verdictCode` string on each offence is being replaced with a structured `verdict` object containing `verdictCode`, `verdictDate`, and `verdictType`.

The staging prosecutors context exposes a pass-through query endpoint `GET /v1/results/{ouCode}` (`hmcts.results.v1`) that forwards requests to `results.prosecutor-results` and returns the response directly. The query response schema for that endpoint — `stagingprosecutors-query-api/src/raml/json/schema/hmcts.results.v1.json` — currently references the core-domain `prosecutorResult.json`. This schema must be updated to document and accept the new `verdict` object that the results context will return.

**No Java production code changes are required.** The `ResultsQueryApi` is a pure pass-through (`Requester.request` → return response as `JsonEnvelope`) and does not parse offence-level data.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Results Query Returns Structured Verdict on Offences (Priority: P1)

A consumer calling `GET /v1/results/{ouCode}` receives a response where offences that have a recorded verdict include the `verdict` object with `verdictCode`, optionally `verdictDate`, and optionally `verdictType`. The schema for the endpoint documents this shape.

**Why this priority**: This is the only change in scope. If the schema is not updated, the contract documentation is wrong and any consumer validating against it will reject valid responses from the results context after CIMD-3915 is deployed there.

**Independent Test**: Call `GET /v1/results/{ouCode}` and receive a mocked response (from the results context stub) containing an offence with `verdict.verdictCode: "G"`, `verdict.verdictDate: "2026-04-13"`, and `verdict.verdictType: "FOUND_GUILTY"`. Validate the response against the updated `hmcts.results.v1.json` schema — it must pass. Then validate a response where the offence has no `verdict` field — it must also pass (verdict is optional).

**Acceptance Scenarios**:

1. **Given** the results context returns a response where an offence carries `verdict.verdictCode: "G"`, `verdict.verdictDate: "2026-04-13"`, and `verdict.verdictType: "FOUND_GUILTY"`, **When** a consumer validates the response against `hmcts.results.v1.json`, **Then** the validation passes.

2. **Given** the results context returns a response where an offence has no `verdict` field, **When** a consumer validates the response against the schema, **Then** the validation passes (verdict is optional).

3. **Given** the results context returns a response where one offence has a verdict and another does not, **When** the response is validated, **Then** both offences independently satisfy the schema — no verdict is assumed or defaulted from one offence to another.

---

### User Story 2 - Schema Example Reflects Verdict Shape (Priority: P2)

The RAML example file `hmcts.results.v1.json` includes at least one offence with the structured `verdict` object so that the API documentation accurately reflects the response that consumers will receive after CIMD-3915.

**Why this priority**: Example files are the primary documentation artifact for API consumers. An out-of-date example leads to consumer confusion and integration errors.

**Independent Test**: Read `hmcts.results.v1.json` example file; confirm it contains an offence with a `verdict` object that includes `verdictCode`, `verdictDate`, and `verdictType`. Confirm a second offence with no `verdict` field is also present, showing that verdict is optional.

**Acceptance Scenarios**:

1. **Given** the example file exists, **When** it is read, **Then** it contains at least one offence with a `verdict` object.
2. **Given** the example file exists, **When** it is read, **Then** it contains at least one offence without a `verdict` field (to demonstrate optionality).

---

### Edge Cases

- `verdict` is entirely absent from an offence — valid; the schema must not require it.
- `verdict` object is present but `verdictType` is absent — valid; `verdictType` is optional within the verdict object.
- `verdictCode` and `verdictDate` are both absent from a present `verdict` object — this case arises only when the results context omits the verdict object entirely (per CIMD-3915 spec, an empty verdict object is normalised to absent); the staging prosecutors schema should accept an absent `verdict` field rather than an empty object.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The `stagingprosecutors-query-api/src/raml/json/schema/hmcts.results.v1.json` schema MUST be updated so that the `prosecutorResult` structure accepts an optional `verdict` object on each offence, with `verdictCode` (string), `verdictDate` (string, `yyyy-MM-dd`), and `verdictType` (string) as optional fields within it.
- **FR-002**: The updated schema MUST remain backward-compatible — a response where an offence carries no `verdict` field MUST still be valid.
- **FR-003**: The schema MUST NOT retain any `$ref` pointing to `http://justice.gov.uk/core/courts/informantRegisterDocument/prosecutorResult.json` for the new verdict shape (the core-domain type does not include the verdict object).
- **FR-004**: The `stagingprosecutors-query-api/src/raml/json/example/hmcts.results.v1.json` example MUST be updated to include at least one offence with the structured `verdict` object.
- **FR-005**: The `ResultsQueryApi.java` MUST NOT be changed — it is a pure pass-through and the response payload is forwarded verbatim.
- **FR-006**: `mvn clean install` on `stagingprosecutors-query/stagingprosecutors-query-api` MUST pass with zero errors and zero test failures after the schema changes.

### Key Entities

- **Verdict** (on offence, optional): `verdictCode` (string — e.g. `"G"`, `"N"`, `"PSJ"`), `verdictDate` (string — `yyyy-MM-dd`), `verdictType` (string — e.g. `"FOUND_GUILTY"`, `"FOUND_NOT_GUILTY"`, `"PROVED_SJP"`). All three fields are optional; `verdictCode` and `verdictDate` are co-present when set.
- **Offence** (within a defendant's prosecution case, within a court session, within a hearing venue): existing structure; `verdict` is a new optional field added to each offence.

## Success Criteria *(mandatory)*

- **SC-001**: A JSON payload representing a `hmcts.results.v1` response, where at least one offence contains `verdict.verdictCode: "G"`, validates successfully against the updated `hmcts.results.v1.json` schema.
- **SC-002**: A JSON payload where no offences contain a `verdict` field also validates successfully against the same schema (backward compatibility).
- **SC-003**: Zero `$ref` references to `http://justice.gov.uk/core/courts/informantRegisterDocument/prosecutorResult.json` remain in the schema after the update, unless retained as a reference base for unchanged parts of the structure.
- **SC-004**: `mvn clean install` on `stagingprosecutors-query/stagingprosecutors-query-api` completes with zero errors and zero test failures.
- **SC-005**: The updated example file contains both an offence with `verdict` and an offence without `verdict`.

## Assumptions

- The `ResultsQueryApi` is a pure pass-through — it does not parse, transform, or validate offence-level fields; therefore no Java production code changes are needed.
- The `hmcts.get-case-results.v1.json` example file (referencing `nonPoliceProsecutorResult.json`) has no matching RAML endpoint and is not wired to an active API route; it is out of scope for this change.
- `verdictType` is populated by the results context from its own reference data; staging prosecutors has no reference-data lookup responsibility and passes the value through verbatim.
- The results context deploys CIMD-3915 independently; staging prosecutors schema update is a non-blocking parallel change that must land before or alongside the results context change to keep contracts consistent.
- No Liquibase or viewstore changes are required — staging prosecutors has no local storage of verdict data.
