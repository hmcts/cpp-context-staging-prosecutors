---
description: "Task list — Java 25 / Framework E 25.104.x upgrade"
---

# Tasks: Upgrade staging-prosecutors to Java 25 / Framework E 25.104.x

**Input**: Design documents from `specs/CIMD-4077-java25-upgrade/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/README.md, quickstart.md

**Tests**: INCLUDED — TDD is constitution-mandated (Principle VIII) and the spec's acceptance
criteria require `mvn test` and `./runIntegrationTests.sh` to pass.

**Note on shape**: a framework upgrade is an atomic change — nothing is testable until the whole
reactor builds. Phases are therefore ordered by *build dependency*; user-story labels
([US1] builds+tests, [US2] runtime unchanged, [US3] no version drift) are kept for traceability.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task)
- File paths are repo-root-relative.

---

## Phase 1: Setup

**Purpose**: confirm the environment can build the target stack before changing anything.

- [X] T001 Confirm on branch `CIMD-4077-java25-upgrade`, `java -version` reports 25, and the target parent resolves: `mvn dependency:get -Dartifact=uk.gov.moj.cpp.common:service-parent-pom:25.104.0-M1:pom` (and `coredomain:25.104.0-M1`)
- [X] T002 [P] Record baseline metrics for later verification: `grep -rl "import javax\." --include=*.java . | grep -v /target/ | wc -l` (expect 121) and `grep -rln deltaspike --include=*.java --include=pom.xml . | grep -v /target/`

**Checkpoint**: JDK 25 + Artifactory confirmed → safe to start editing.

---

## Phase 2: Foundational (POM version chain — BLOCKS all compilation)

**⚠️ CRITICAL**: nothing compiles until the parent/version chain resolves.

- [X] T003 Bump parent in root `pom.xml`: `uk.gov.moj.cpp.common:service-parent-pom` `17.104.1` → `25.104.0-M1`
- [X] T004 Bump project `<version>` `17.104.63-SNAPSHOT` → `25.104.0-M1-SNAPSHOT` across all 26 `pom.xml` (root + every module, including nested `<parent><version>`). Verify: `grep -rl 17.104.63-SNAPSHOT --include=pom.xml . | grep -v /target/` returns nothing
- [X] T005 [US3] In root `pom.xml`, set `coredomain.version` → `25.104.0-M1`; confirm the cross-context pins `prosecutioncasefile.version`, `results.version`, `referencedata.version`, `notification.notify.version`, `system.users.library.version` are LEFT on their existing `17.x` values

**Checkpoint**: reactor resolves the 25.104.x parent/BOM; compilation can be attempted.

---

## Phase 3: User Story 1 — Service builds and unit tests pass (Priority: P1) 🎯 MVP

**Goal**: whole reactor compiles on Java 25 and `mvn clean install` is green, with no Jakarta-EE
`javax.*` imports and no DeltaSpike.

**Independent Test**: `mvn clean install` succeeds; `grep` checks SC-003/SC-004 are clean.

### Namespace & descriptor migration

- [X] T006 [US1] Migrate `javax.*` → `jakarta.*` across all main + test Java sources (the EE packages: json, inject, ws.rs, jms, persistence, xml.bind, validation, enterprise, annotation). LEAVE Java SE untouched (`javax.net`, `javax.xml.transform`, `javax.xml.datatype`, `javax.xml.validation`, `javax.naming`, `javax.crypto`). Verify after: `grep -rn "import javax\." --include=*.java . | grep -v /target/` shows only SE packages
- [X] T007 [P] [US1] Update all 10 `*/src/main/resources/META-INF/beans.xml` to the Jakarta EE 4.0 form (`xmlns=https://jakarta.ee/xml/ns/jakartaee`, `beans_4_0.xsd`, `version="4.0"`, keep `bean-discovery-mode="all"`)
- [X] T008 [US1] Update `stagingprosecutors-viewstore/stagingprosecutors-viewstore-persistence/src/main/resources/META-INF/persistence.xml` to `https://jakarta.ee/xml/ns/persistence` v3.0; drop explicit `<class>` listings; keep persistence-unit name + `<jta-data-source>`

### DeltaSpike → JPA repository (TDD — test first)

- [X] T009 [US1] **(TEST FIRST)** Create `stagingprosecutors-viewstore/stagingprosecutors-viewstore-persistence/src/test/resources/META-INF/persistence.xml` (RESOURCE_LOCAL, H2, `hbm2ddl=create`, `NON_KEYWORDS=VALUE`) and rewrite `.../src/test/java/.../persistence/repository/SubmissionRepositoryTest.java` to JUnit 5 + static `@RegisterExtension HibernateTestEntityManagerProvider` (preserving the save→findBy assertions). It MUST fail against the not-yet-migrated repository
- [X] T010 [US1] Convert `.../persistence/repository/SubmissionRepository.java` from `@Repository interface extends EntityRepository<Submission, UUID>` to a concrete `@ApplicationScoped` class: `@PersistenceContext(unitName="…") EntityManager`; `findBy(UUID)` → `em.find(...)` (null on miss); `save(Submission)` → `em.merge(...)`. Make T009 green
- [X] T011 [US1] Update `.../stagingprosecutors-viewstore-persistence/pom.xml`: remove `persistence-deltaspike`, `deltaspike-test-control-module-api/-impl`, `deltaspike-cdictrl-openejb`, `openejb-core`, `openejb-server`; change `org.hibernate:hibernate-core` → `org.hibernate.orm:hibernate-core` scope `provided`; replace `javax:javaee-api` with the Jakarta equivalent (provided); add test deps `com.h2database:h2`, `uk.gov.justice.utils:test-utils-hibernate`, `jakarta.xml.bind:jakarta.xml.bind-api`. Check `JsonArrayConverterTest` (and any other test in the module) for `BaseTransactionalJunit4Test`/DeltaSpike and migrate likewise

### Targeted pom / RAML fixes

- [X] T012 [P] [US1] Add `uk.gov.justice.event-store:event-tracking-discovery` (BOM-managed version) to `stagingprosecutors-service/pom.xml`
- [X] T013 [P] [US1] In `stagingprosecutors-integration-test/pom.xml`, change Artemis groupId `org.apache.activemq` → `org.apache.artemis` for `artemis-jms-client`
- [X] T014 [P] [US1] In `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/stagingprosecutors-query-api.raml`, change `baseUri` from `.../stagingprosecutors-query-api/...` → `.../stagingprosecutors-service/...`
- [X] T015 [P] [US1] In `azure-pipelines.yaml`, change the `demands` identifier `centos8-j17` → `ubuntu-j25-postgres` (line ~29; leave the `pool:` name unchanged) so CI builds/ITs run on JDK 25 (FR-012 / SC-008)

### Build & unit-test gate

- [X] T016 [US1] Iterative compile loop: `mvn clean install -DskipTests -Denforcer.skip=true`; fix compile errors using the research.md D8 watch-list (do NOT commit the `-Denforcer.skip` flag)
- [X] T017 [US1] Full build + unit tests with enforcer ON: `mvn clean install`; fix any unit-test failures (esp. migrated repository test)
- [X] T018 [US1] Verify SC-003 (`grep -rn "import javax\." --include=*.java . | grep -v /target/` → SE only) and SC-004 (`grep -rn deltaspike --include=*.java --include=pom.xml . | grep -v /target/` → empty)

**Checkpoint**: `mvn clean install` green on JDK 25; namespace + DeltaSpike migration complete.

---

## Phase 4: User Story 2 — Runtime behaviour unchanged on WildFly 40 (Priority: P1)

**Goal**: the WAR deploys and behaves identically on WildFly 40 / JDK 25.

**Independent Test**: full Dockerised IT suite passes.

- [X] T019 [US2] Run `./runIntegrationTests.sh` (Docker up, `CPP_DOCKER_DIR` set); triage failures against research.md D8 (implicit-SELECT JPQL, `!= null`, primitive-null, lazy-init, `JsonObjectBuilder.add(null)`, Artemis `setBrokerURL`, pull-mechanism, `EventDiscoveryTimerBean`/`resetEventSubscriptionStatusTable`) and apply only the fixes a real failure surfaces. Re-run until green
- [X] T020 [US2] Confirm clean deployment in `cpp-developers-docker/.../wildfly/log/server.log`: no `WELD-001408`, `/internal/metrics/ping` → `pong`, healthchecks healthy

**Checkpoint**: ITs green; runtime parity confirmed.

---

## Phase 5: User Story 3 — No cross-context version drift (Priority: P2)

**Goal**: only the framework chain + coredomain moved; downstream pins untouched.

**Independent Test**: dependency tree / pom inspection.

- [X] T021 [US3] Verify resolved versions: parent + project version + `coredomain` on `25.104.0-M1`; `prosecutioncasefile`/`results`/`referencedata`/`notification.notify`/`system.users.library` still `17.x` (`grep` root pom + spot-check `mvn dependency:tree`)

**Checkpoint**: no drift.

---

## Phase 6: Build Loop & Polish (Cross-Cutting)

**Purpose**: run the constitution's mandatory review loop (Principle IV) and finalise.

- [X] T022 Run the `code-reviewer` agent (read-only) over the diff; apply fixes myself until PASS (layering, framework idioms, no `System.out`, no Spring, no stray `javax.*`)
- [X] T023 Run the `spec-validator` agent; confirm RAML/JSON-schema/descriptor symmetry preserved (only the `baseUri` line changed) until COMPLIANT
- [X] T024 Run the `qa` agent; confirm TDD discipline (repo test authored before/with the production conversion) and tests pass until PASS
- [ ] T025 Run quickstart.md verification end-to-end; commit the work as logical Conventional Commits (`build:`/`refactor:` etc.) on `CIMD-4077-java25-upgrade` — **no push, no PR**

---

## Dependencies & Execution Order

- **Phase 1 (Setup)**: no deps.
- **Phase 2 (Foundational POM chain)**: after Setup. BLOCKS all of Phase 3+ (nothing compiles otherwise).
- **Phase 3 (US1)**: after Phase 2. T009 (test) before T010 (prod). T016 before T017. T006–T015 mostly independent edits; T016/T017 gate them.
- **Phase 4 (US2)**: after Phase 3 (needs a green build to deploy).
- **Phase 5 (US3)**: verification; can run any time after Phase 2 but reported after build is green.
- **Phase 6 (Build loop)**: after Phases 3–5; loops until all agents PASS, then commit.

### Parallel opportunities

- T007, T012, T013, T014, T015 touch different files and can run alongside the T006 sweep.
- The DeltaSpike chain (T009 → T010 → T011) is sequential.
- Build gates (T016, T017, T019) are serialisation points.

---

## Implementation Strategy

1. **Foundational first** (Phase 2) — get the reactor resolving the 25.104.x parent/BOM.
2. **MVP = US1** (Phase 3) — compile + unit-test green; this is the bulk of the work.
3. **Validate runtime** (US2) — ITs on WildFly 40.
4. **Confirm no drift** (US3), then **run the build loop** (Phase 6) and commit.

Total: 25 tasks — US1: 13, US2: 2, US3: 2 (T005 + T021), Setup: 2, Build loop/polish: 4.
