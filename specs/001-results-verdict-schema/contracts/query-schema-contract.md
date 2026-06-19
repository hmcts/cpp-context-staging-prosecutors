# Query Schema Contract: hmcts.results.v1

**Endpoint**: `GET /v1/results/{ouCode}`  
**RAML name**: `hmcts.results.v1`  
**Media type**: `application/vnd.hmcts.results.v1+json`

## Schema File Change

**Path**: `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/schema/hmcts.results.v1.json`

| Field | Before | After |
|-------|--------|-------|
| `"ref"` | `http://justice.gov.uk/core/courts/informantRegisterDocument/prosecutorResult.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json` |

The updated URI points to the results context's new local `prosecutorResult.json` schema (created in CIMD-3915), which includes the `verdict` object on offences.

## Example File Change

**Path**: `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/example/hmcts.results.v1.json`

The existing example is updated to add:
1. One offence **with** a `verdict` object (demonstrating the new shape)
2. One offence **without** a `verdict` field (demonstrating backward compatibility)

### Verdict object shape in example

```json
"verdict": {
  "verdictCode": "G",
  "verdictDate": "2026-04-13",
  "verdictType": "FOUND_GUILTY"
}
```

### Absent verdict (no field — not null, not {})

```json
{
  "offenceCode": "PS90011",
  "orderIndex": 2,
  "offenceTitle": "...",
  "pleaValue": "NOT_GUILTY"
}
```

## Backward Compatibility

The updated schema reference at `http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json` (results context's new local version) defines `verdict` as optional on every offence. Existing consumers that do not read the `verdict` field are unaffected.

## Out of Scope

`hmcts.get-case-results.v1.json` example — no active RAML endpoint; out of scope for this change.
