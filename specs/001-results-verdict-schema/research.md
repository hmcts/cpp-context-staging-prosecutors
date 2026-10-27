# Research: Update Results Query Schema for Structured Verdict

**Date**: 2026-06-16 | **Branch**: `CIMD-3915-results-verdict-schema`

## Decision 1: What `"ref"` means in `hmcts.results.v1.json`

**Decision**: The `"ref"` field in the schema file is a CPP-framework RAML documentation convention — not a standard JSON Schema `"$ref"`. It acts as a URI pointer to the external schema document that defines the full response shape. It is **not** resolved at build or runtime by the framework's dispatch mechanism; the schema is purely for RAML contract documentation.

**Evidence**: All other `staging-prosecutors-query-api` schemas (`hmcts.cjs.submission.json`, etc.) use standard `"$ref"` within their `properties`. `hmcts.results.v1.json` is a thin wrapper schema with no `type` or `properties` of its own — it delegates entirely to the referenced external document. The `ResultsQueryApiTest` does not reference or validate against this schema file at all.

**Rationale**: Understanding this means the `"ref"` update is documentation-only; it does not affect build compilation, runtime dispatch, or test outcomes. The change is purely a contract accuracy update.

**Alternatives considered**:
- Inline expansion of the full `prosecutorResult` JSON Schema — rejected: would duplicate the results context's schema definition and create maintenance drift; the `"ref"` convention was chosen originally to delegate to the upstream definition.
- Leave `"ref"` pointing to core-domain — rejected: after CIMD-3915 the results context response no longer matches the core-domain schema (the offence no longer has a flat `verdictCode`; it has a `verdict` object); leaving the old ref makes the staging prosecutors contract documentation wrong.

---

## Decision 2: Target URI for the updated schema `"ref"`

**Decision**: Update `"ref"` from:  
`http://justice.gov.uk/core/courts/informantRegisterDocument/prosecutorResult.json`  
to:  
`http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json`

**Rationale**: The results context CIMD-3915 creates a local `prosecutorResult.json` at namespace `http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json`. This is the schema that will govern the `results.prosecutor-results` query response going forward. The staging prosecutors schema should reference this new URI to remain accurate.

**Confirmation**: The results context's new schema (`results-domain-common/src/main/resources/json/schema/informantRegisterDocument/prosecutorResult.json`) has `$id: http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json`. The offences within it reference `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterOffence.json`, which defines the `verdict` object.

**Alternatives considered**:
- Full inline expansion — rejected: see Decision 1.
- Keeping the core-domain ref and adding an `additionalProperties` allowance — rejected: changes meaning of the schema; incorrect documentation.

---

## Decision 3: Verdict Object Shape

**Decision**: The `verdict` object on an offence has the following structure (derived from the results context's new `verdict.json` and `informantRegisterOffence.json`):

```json
"verdict": {
  "verdictCode": "G",          // string, optional: G | N | PSJ
  "verdictDate": "2026-04-13", // string yyyy-MM-dd, mandatory when verdictCode present
  "verdictType": "FOUND_GUILTY" // string, optional
}
```

Co-dependency rule (from `verdict.json` `dependencies` block): if `verdictCode` is present then `verdictDate` must also be present, and vice versa. `verdictType` is always optional.

**Verdict absent**: when no verdict is recorded, the `verdict` field is omitted entirely from the offence (not present as `null` or `{}`).

---

## Decision 4: No Java Production Code Changes

**Decision**: `ResultsQueryApi.java` and all other Java files are unchanged.

**Rationale**: `ResultsQueryApi.getResults` is a pure pass-through:

```java
final JsonEnvelope resultsResponseEnvelope = requester.request(resultsQueryEnvelope);
return envelopeFrom(
    metadataFrom(resultsResponseEnvelope.metadata()).withName(GET_RESULTS).build(),
    resultsResponseEnvelope.payloadAsJsonObject());
```

The payload is forwarded as-is (`payloadAsJsonObject()`) without any field-level parsing or transformation. The `verdict` object will flow through transparently once the results context returns it.

---

## Decision 5: TDD Applicability

**Decision**: Constitution Principle VIII (TDD) applies to Java production code behaviour changes. This feature has no Java production code changes. No new failing-then-passing unit tests are required.

**Note**: The existing `ResultsQueryApiTest` tests routing and pass-through behaviour only. It does not validate the response payload schema. Adding a JSON Schema validation test (e.g. validating the example file against the updated schema using a JSON Schema validator in a dedicated test class) would be good engineering practice but is not mandated by Principle VIII for a schema-file-only change.

---

## Decision 6: `hmcts.get-case-results.v1.json` — Out of Scope

**Decision**: `stagingprosecutors-query-api/src/raml/json/example/hmcts.get-case-results.v1.json` is out of scope.

**Rationale**: This file is an example file (not a schema file) that uses the `"ref"` convention to reference `nonPoliceProsecutorResult.json`. There is no corresponding RAML endpoint in `stagingprosecutors-query-api.raml` that maps to `hmcts.get-case-results.v1`. It appears to be an unused or deferred artifact. Updating it as part of this change would introduce scope creep; it can be addressed in a separate cleanup ticket if needed.

---

## Scope Confirmation — Three-Layer Impact

| Layer | Impact | Reason |
|-------|--------|--------|
| Command side | **None** | No new commands, no new domain events |
| Event listener | **None** | No new viewstore projections |
| Event processor | **None** | No new public events published |
| Query API (schema) | **Schema + example file update only** | `hmcts.results.v1.json` — see below |

**Constitution Principle II does not apply** — this is not an event-touching change. It is a query contract documentation update. Principle I is satisfied: the schema file (contract) is the only artefact changed, and no Java follows it.
