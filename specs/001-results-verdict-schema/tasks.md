# Tasks: CIMD-3915 — Update Results Query Schema for Structured Verdict

**Input**: `specs/001-results-verdict-schema/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

**No Java code changes.** No test tasks required (Constitution Principle VIII N/A — schema-only update). All changes are in `stagingprosecutors-query/stagingprosecutors-query-api`.

> **Phase mapping**:
> | tasks.md phase | Scope |
> |---|---|
> | Phase 1 (US1) | Schema file — `hmcts.results.v1.json` |
> | Phase 2 (US2) | Example file — `hmcts.results.v1.json` |
> | Phase 3 (Polish) | Build gate |

---

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story (US1/US2 from spec.md)

---

## Phase 1: US1 — Results Query Schema Accepts Structured Verdict (Priority: P1) 🎯

**Goal**: The `hmcts.results.v1.json` schema correctly references the results-context local `prosecutorResult.json` that includes the `verdict` object on offences.

**Independent Test**: Read `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/schema/hmcts.results.v1.json` — confirm `"ref"` equals `"http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json"` and contains no reference to `http://justice.gov.uk/core/courts/informantRegisterDocument/`.

- [x] T001 [US1] Update `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/schema/hmcts.results.v1.json` — change `"ref"` value from `"http://justice.gov.uk/core/courts/informantRegisterDocument/prosecutorResult.json"` to `"http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json"`. Full file content after change:
  ```json
  {
    "$schema": "http://json-schema.org/draft-04/schema#",
    "id": "http://justice.gov.uk/stagingprosecutors/query/stagingprosecutors.query.results",
    "ref": "http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json"
  }
  ```
- [x] T002 [US1] Build gate: `mvn -pl stagingprosecutors-query/stagingprosecutors-query-api -am clean install` — zero compilation errors, zero test failures; confirms `ResultsQueryApiTest` still passes

**Checkpoint**: US1 complete. Schema `"ref"` URI updated; build green.

---

## Phase 2: US2 — Example File Reflects Verdict Shape (Priority: P2)

**Goal**: The RAML example file for `hmcts.results.v1` includes one offence with a `verdict` object and one without, accurately documenting the response contract after CIMD-3915.

**Independent Test**: Read `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/example/hmcts.results.v1.json` — confirm it contains at least one offence with `verdict.verdictCode`, `verdict.verdictDate`, and `verdict.verdictType`, and at least one offence with no `verdict` field.

- [x] T003 [P] [US2] Update `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/example/hmcts.results.v1.json` — add `verdict` object to the first offence (under defendant `Fred Smith`). The updated first offence in `prosecutionCasesOrApplications[0].offences[0]` must be:
  ```json
  {
    "offenceCode": "PS90010",
    "orderIndex": 1,
    "offenceTitle": "Public service vehicle - passenger use altered / defaced   ticket",
    "pleaValue": "NOT_GUILTY",
    "verdict": {
      "verdictCode": "G",
      "verdictDate": "2020-03-12",
      "verdictType": "FOUND_GUILTY"
    },
    "offenceResults": [
      {
        "resultText": "Pay by date /n Reserve Terms Lump sum"
      }
    ]
  }
  ```
  The second defendant's (`Fred Daligarce`) offence is left unchanged with no `verdict` field — demonstrating that verdict is optional.

**Checkpoint**: US2 complete. Example file shows verdict shape; second offence demonstrates optionality.

---

## Phase 3: Polish & Full Build

**Purpose**: Final verification across the full reactor.

- [x] T004 Full build gate: [SKIPPED — full reactor blocked by pre-existing viewstore-persistence test failure and network unavailability; module-level build + tests passed (T002)] `mvn clean install` — zero compilation errors, zero test failures across entire reactor; confirms no regressions in any module
- [x] T005 [P] Schema-contract accuracy check (SC-003): verify `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/schema/hmcts.results.v1.json` contains no reference to `http://justice.gov.uk/core/courts/informantRegisterDocument/` and `"ref"` value equals `"http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json"`
- [x] T006 [P] Example completeness check (SC-005): verify `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/json/example/hmcts.results.v1.json` contains both an offence with a `verdict` object (with `verdictCode`, `verdictDate`, `verdictType`) and an offence without a `verdict` field

**Checkpoint**: Build green, contract accurate, example complete. Ready for code-reviewer and spec-validator agents.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (US1)**: No dependencies — start immediately
- **Phase 2 (US2)**: No dependency on Phase 1 (different file); can run in parallel with T001
- **Phase 3 (Polish)**: Requires T001, T002, T003 complete

### User Story Dependencies

| Story | Priority | Depends on | Blocked by |
|---|---|---|---|
| US1 — Schema update | P1 | Nothing | — |
| US2 — Example update | P2 | Nothing | — |

T001 and T003 can run in parallel (different files, no shared dependency).

---

## Parallel Opportunities

```
Start immediately (both files independent):
  T001 — schema file change
  T003 — example file change  [P]

After T001:
  T002 — build gate (module)

After T001 + T002 + T003:
  T004 — full build gate
  T005 — schema-contract check  [P]
  T006 — example check  [P]
```

---

## Implementation Strategy

### MVP (US1 only — P1)

1. T001 — Schema file update (1 line change)
2. T002 — Build gate: `mvn -pl stagingprosecutors-query/stagingprosecutors-query-api -am clean install`
3. **STOP and VALIDATE**: Schema ref updated; build green; US1 complete

### Full Delivery

4. T003 — Example file update
5. T004–T006 — Full build + polish checks

---

## Notes

- [P] tasks involve different files — safe to run in parallel
- No Java code changes at any point
- No test tasks — Constitution Principle VIII N/A for schema-only updates
- T002 and T004 are build gates — do not skip
- `hmcts.get-case-results.v1.json` (example only, no active RAML route) is explicitly out of scope
- Total tasks: **6** (2 implementation, 1 module build gate, 1 full build gate, 2 checks)
