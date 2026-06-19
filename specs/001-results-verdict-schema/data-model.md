# Data Model: Update Results Query Schema for Structured Verdict

**Branch**: `CIMD-3915-results-verdict-schema` | **Date**: 2026-06-16

## Overview

No viewstore or event-store schema changes. No Liquibase migrations. This feature is a query contract documentation update only.

## Changed Schema Artefact

**File**: `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/schema/hmcts.results.v1.json`

### Before (current)

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "id": "http://justice.gov.uk/stagingprosecutors/query/stagingprosecutors.query.results",
  "ref": "http://justice.gov.uk/core/courts/informantRegisterDocument/prosecutorResult.json"
}
```

### After (target)

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "id": "http://justice.gov.uk/stagingprosecutors/query/stagingprosecutors.query.results",
  "ref": "http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json"
}
```

**Change**: The `"ref"` URI is updated from the core-domain namespace to the results-context local namespace that includes the verdict-aware schema.

---

## Verdict Object (new field on offence, optional)

Defined in results context at `http://justice.gov.uk/results/courts/informantRegisterDocument/verdict.json`.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `verdictCode` | string | No (co-dep with verdictDate) | `"G"` (FOUND_GUILTY), `"N"` (FOUND_NOT_GUILTY), `"PSJ"` (PROVED_SJP) |
| `verdictDate` | string (yyyy-MM-dd) | No (co-dep with verdictCode) | Date verdict was recorded |
| `verdictType` | string | No | `"FOUND_GUILTY"`, `"FOUND_NOT_GUILTY"`, `"PROVED_SJP"` — from reference data |

**Co-dependency rule**: `verdictCode` and `verdictDate` must either both be present or both be absent. `verdictType` is independent.

**Absent verdict**: when no verdict is recorded, the `verdict` field is omitted from the offence entirely (not `null`, not `{}`).

---

## Full Response Structure (after CIMD-3915)

The `GET /v1/results/{ouCode}` response shape, as defined by the results context's updated `prosecutorResult.json`:

```
prosecutorResult
├── startDate (string, required)
├── endDate (string, optional)
├── prosecutionAuthorityId (string, required)
├── prosecutionAuthorityCode (string, required)
├── prosecutionAuthorityName (string, optional)
├── prosecutionAuthorityOuCode (string, optional)
├── majorCreditorCode (string, optional)
└── hearingVenues[] (array, minItems 1)
    └── hearingVenue
        ├── courtHouse (string, required)
        ├── ljaName (string, optional)
        └── courtSessions[] (array, minItems 1)
            └── courtSession
                ├── courtRoom (string, required)
                ├── hearingStartTime (string, required)
                └── defendants[] (array, minItems 1)
                    └── defendant
                        ├── name (string, required)
                        ├── address1 (string, required)
                        ├── [address2–5, postCode, dateOfBirth, nationality, firstName, lastName] (optional)
                        └── prosecutionCasesOrApplications[]
                            └── caseOrApplication
                                ├── caseOrApplicationReference (string, required)
                                ├── offences[]
                                │   └── offence
                                │       ├── offenceCode (string, required)
                                │       ├── orderIndex (integer, required)
                                │       ├── offenceTitle (string, required)
                                │       ├── originatingCaseUrn (string, optional)
                                │       ├── pleaValue (string, optional)
                                │       ├── verdict (object, optional) ← NEW
                                │       │   ├── verdictCode (string, optional)
                                │       │   ├── verdictDate (string yyyy-MM-dd, optional)
                                │       │   └── verdictType (string, optional)
                                │       └── offenceResults[] (optional)
                                └── results[]
```

---

## No Viewstore Changes

The staging prosecutors viewstore (`java:/DS.stagingprosecutors`) stores prosecution submission state — not informant register or result data. No Liquibase migrations are needed.
