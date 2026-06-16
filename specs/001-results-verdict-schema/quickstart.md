# Quickstart: CIMD-3915 — Results Query Schema Verdict Alignment

**Branch**: `CIMD-3915-results-verdict-schema`
**Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

## What this change does

Updates the staging prosecutors query-API contract schema (`hmcts.results.v1.json`) to reflect the new `verdict` object on offences that the results context (`cpp-context-results`) will return after its CIMD-3915 deployment.

**Two files change. No Java changes.**

## Files to change

| File | Change |
|------|--------|
| `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/schema/hmcts.results.v1.json` | Update `"ref"` URI from core-domain to results-context local namespace |
| `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/example/hmcts.results.v1.json` | Add one offence with `verdict` object; keep one offence without |

## Schema change (1 line)

```json
// Before
"ref": "http://justice.gov.uk/core/courts/informantRegisterDocument/prosecutorResult.json"

// After
"ref": "http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json"
```

## Verdict object shape (new in offence, optional)

```json
"verdict": {
  "verdictCode": "G",           // G | N | PSJ
  "verdictDate": "2026-04-13",  // yyyy-MM-dd; mandatory when verdictCode present
  "verdictType": "FOUND_GUILTY" // optional; from reference data
}
```

## Key commands

```bash
# Build and test affected module only
mvn -pl stagingprosecutors-query/stagingprosecutors-query-api -am clean install

# Full reactor build
mvn clean install
```

## What is NOT changing

- `ResultsQueryApi.java` — pure pass-through, no changes needed
- `ResultsQueryApiTest.java` — routing tests still pass; no schema validation in tests
- Viewstore, event store, subscriptions, processors, listeners — none affected
- `hmcts.get-case-results.v1.json` example — out of scope (no active RAML route)

## Constitution gates (must not skip)

- **Principle I** (Contract First) — schema file is the ONLY change; no Java follows
- **Principle II** (Three-layer) — N/A; not an event-touching change
- **Principle VIII** (TDD) — N/A; no Java production code change; schema-only update
