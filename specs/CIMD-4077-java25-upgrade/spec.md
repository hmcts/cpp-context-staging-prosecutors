# Feature Specification: Upgrade staging-prosecutors to Java 25 / Framework E 25.104.x

**Feature Branch**: `CIMD-4077-java25-upgrade`
**Created**: 2026-06-29
**Status**: Draft
**Input**: User description: "Upgrade the cpp-context-staging-prosecutors service from Framework E 17.104.x (Java 17 / WildFly 26 / Jakarta EE 8, javax.*) to 25.104.x (Java 25 / WildFly 40 / Jakarta EE 11, jakarta.*), following the HMCTS pilot playbook."

## Overview

The stagingprosecutors context currently runs on Framework E `17.104.x` (Java 17, WildFly 26,
Jakarta EE 8 with the `javax.*` namespace). That baseline has reached end of active support.
HMCTS has released and context-validated the `25.104.x` framework chain (Java 25, WildFly 40,
Jakarta EE 11, `jakarta.*` namespace). This feature migrates the service onto that chain so it
stays on a supported platform, inherits the virtual-thread performance and security/CVE fixes
that come with it, and remains releasable alongside the rest of the platform.

The migration follows the HMCTS pilot playbook
(`hmcts/cpp-framework-java-upgrade-pilot`, `guides/CLAUDE-upgrade-playbook.md` +
`decisions/java25-delta-from-java21.md`) and mirrors the validated reference context
`cpp-context-users-groups`.

## Clarifications

### Session 2026-06-29

- Q: How should the change be committed on the feature branch when implement finishes? → A:
  Logical commits in Conventional Commits style (team convention), no push and no PR.
- Q: Target framework version? → A: `25.104.0-M1` (milestone chain used by validated reference
  contexts; resolvable from team Artifactory). *(decided pre-spec)*
- Q: Execution scope on this machine? → A: Full build + unit tests + integration tests
  (JDK 25 + Docker stack + `CPP_DOCKER_DIR` assumed available). *(decided pre-spec)*

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Service builds and tests pass on the new stack (Priority: P1)

A platform engineer checks out the upgrade branch and runs the build. The whole reactor
compiles under Java 25, all unit tests pass, and the full Dockerised integration-test suite
passes — with no remaining `javax.*` Jakarta dependencies and no DeltaSpike on the classpath.

**Why this priority**: This is the upgrade. Without a green build + tests on the new stack, the
service cannot be released and nothing downstream can proceed.

**Independent Test**: Run `mvn clean install` then `./runIntegrationTests.sh` on a JDK 25 + Docker
environment with the `25.104.0-M1` artifacts resolvable; verify both succeed.

**Acceptance Scenarios**:

1. **Given** the upgrade branch on a JDK 25 toolchain, **When** `mvn clean install` runs,
   **Then** every module compiles and all unit tests pass.
2. **Given** the Dockerised test stack is up and `CPP_DOCKER_DIR` is set, **When**
   `./runIntegrationTests.sh` runs, **Then** the integration-test suite passes.
3. **Given** the migrated source tree, **When** the codebase is scanned for `import javax.`,
   **Then** only Java SE packages remain (no Jakarta EE `javax.*`).
4. **Given** the migrated build, **When** the classpath is scanned, **Then** no DeltaSpike
   artifact is present.

### User Story 2 - Runtime behaviour is unchanged on WildFly 40 (Priority: P1)

The deployed WAR behaves exactly as it did on the old stack: commands are accepted, domain
events are stored and projected to the viewstore, and public events are published to and
consumed from downstream contexts (PCF / results / notification / sjp). No CDI wiring or
persistence regressions appear at deploy or runtime.

**Why this priority**: A framework upgrade that silently changes behaviour is worse than no
upgrade — this context's events escape the boundary into Prosecution Case File.

**Independent Test**: The integration-test suite exercises command → event → viewstore →
public-event flows end to end; all scenarios pass against the WildFly 40 deployment.

**Acceptance Scenarios**:

1. **Given** the WAR deployed on WildFly 40, **When** the service starts, **Then** all CDI
   beans resolve (no `WELD-001408` unsatisfied-dependency failures) and health checks return
   healthy.
2. **Given** a prosecution/material submission command, **When** it is processed, **Then** the
   resulting viewstore read model and published public events match pre-upgrade behaviour.

### User Story 3 - No accidental cross-context version drift (Priority: P2)

The upgrade bumps only the framework chain and `coredomain`; pins for not-yet-upgraded
bounded contexts stay on their current `17.x` versions so this service does not force-couple to
unreleased upstream changes.

**Why this priority**: Bumping a cross-context pin to a version that doesn't exist (or that
changes a contract) breaks the build or drifts the submission→PCF contract.

**Independent Test**: Inspect the resolved dependency tree; confirm `prosecutioncasefile`,
`results`, `referencedata`, `notification.notify`, and `system.users.library` remain on `17.x`.

**Acceptance Scenarios**:

1. **Given** the upgraded poms, **When** versions are reviewed, **Then** the parent, project
   version, and `coredomain` are on `25.104.0-M1` and the listed cross-context pins are unchanged.

### Edge Cases

- A half-migrated `beans.xml` (Jakarta namespace but `version="1.1"`) silently disables CDI bean
  discovery → must land on the EE 4.0 form exactly.
- Hibernate 6 strictness surfaces only at test/runtime (implicit-`SELECT` JPQL, `!= null`
  three-valued logic, null-into-primitive, `@MapsId` foreign generator, lazy-init): apply the
  playbook fix only if a failure surfaces it (this repo's single repository is trivial).
- Jandex / JAXB classpath conflicts in `*-viewstore-persistence` unit tests after DeltaSpike
  removal (resolved via `hibernate-core` `provided` scope + explicit `jakarta.xml.bind-api` test dep).
- Artemis groupId change (`org.apache.activemq` → `org.apache.artemis`) leaves the
  integration-test pom with a missing-version error if not updated.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The build MUST inherit from `uk.gov.moj.cpp.common:service-parent-pom:25.104.0-M1`,
  and the project version MUST be `25.0.0-JAVA_25-SNAPSHOT` across all 26 `pom.xml` files.
- **FR-002**: `coredomain.version` MUST be `25.104.0-M1`; the cross-context pins
  `prosecutioncasefile`, `results`, `notification.notify`, and `system.users.library` MUST remain
  on their existing `17.x` versions. `referencedata` MUST be at the latest released `17.x`
  (`17.104.136`) to satisfy the `enforce-moj-latest-interfaces` rule (still 17.x, not 25.x).
- **FR-003**: All Jakarta EE `javax.*` imports MUST be migrated to `jakarta.*`
  (json, inject, ws.rs, jms, persistence, xml.bind, validation, enterprise, annotation).
  Java SE `javax.*` packages (net, xml.transform, xml.datatype, xml.validation, naming, crypto)
  MUST be left unchanged.
- **FR-004**: Every `META-INF/beans.xml` MUST use the Jakarta EE 4.0 namespace
  (`https://jakarta.ee/xml/ns/jakartaee`, `beans_4_0.xsd`, `version="4.0"`).
- **FR-005**: The main `persistence.xml` MUST use the Jakarta persistence 3.0 namespace; a
  test-scoped `persistence.xml` (RESOURCE_LOCAL / H2) MUST be added for repository unit tests.
- **FR-006**: DeltaSpike MUST be removed. `SubmissionRepository` MUST become a concrete
  `@ApplicationScoped` JPA bean using `@PersistenceContext`, preserving its existing behaviour
  (`save`, `findBy(UUID)`); its test MUST be migrated to JUnit 5 +
  `HibernateTestEntityManagerProvider`. No DeltaSpike or TomEE/OpenEJB dependency may remain.
- **FR-007**: The `viewstore-persistence` pom MUST use `org.hibernate.orm:hibernate-core`
  (scope `provided`), the Jakarta replacement for `javax:javaee-api`, and test deps `h2`,
  `test-utils-hibernate`, and `jakarta.xml.bind-api`.
- **FR-008**: The service WAR pom MUST declare `uk.gov.justice.event-store:event-tracking-discovery`.
- **FR-009**: The integration-test pom MUST reference Artemis under groupId
  `org.apache.artemis`.
- **FR-010**: The query-api RAML `baseUri` MUST target the merged service WAR root
  (`.../stagingprosecutors-service/...`), not the individual query-api context root.
- **FR-011**: The upgrade MUST NOT change any RAML contract, JSON schema, subscription/
  publication descriptor, event-sources, or Liquibase changelog semantics (namespace-only XML
  edits excepted) — the API/event contract is JVM-independent and stays identical.
- **FR-012**: The CI pipeline (`azure-pipelines.yaml`) MUST target a Java 25 build agent — the
  `demands` identifier MUST change from `centos8-j17` to `ubuntu-j25-postgres` (the identifier
  used by the validated reference contexts), so CI builds and integration tests run on JDK 25.

### Key Entities

- **SubmissionRepository / Submission**: the only viewstore repository + entity affected by the
  DeltaSpike→JPA migration; behaviour (lookup by id, persist) must be preserved.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `mvn clean install` completes successfully on a JDK 25 toolchain with zero
  compilation errors and zero unit-test failures.
- **SC-002**: `./runIntegrationTests.sh` completes with all integration tests passing.
- **SC-003**: A scan for `import javax.` across `src` returns only Java SE packages (0 Jakarta EE
  `javax.*` imports).
- **SC-004**: A scan of source and poms for `deltaspike` returns 0 matches.
- **SC-005**: The deployed WAR starts on WildFly 40 with all CDI beans resolved and health checks
  reporting healthy.
- **SC-006**: The resolved dependency tree shows parent/version/`coredomain` on `25.104.0-M1`
  and all five listed cross-context pins still on `17.x`.
- **SC-007**: The completed upgrade is recorded as logical Conventional Commits on the
  `CIMD-4077-java25-upgrade` branch (no push, no PR).
- **SC-008**: `azure-pipelines.yaml` demands the `ubuntu-j25-postgres` agent (no `centos8-j17`
  reference remains).

## Assumptions

- Target framework version is `25.104.0-M1` (the milestone chain used by the validated reference
  contexts); these artifacts are resolvable from the team's Artifactory.
- The build/test environment provides JDK 25, the Docker stack, and `CPP_DOCKER_DIR` (full build +
  unit + integration tests will be run on this machine).
- Cross-context contracts are wire/RAML-compatible across the version gap, so consuming/producing
  public events against `17.x` downstream contexts continues to work (consistent with the playbook
  and the reference contexts).
- WildFly `standalone.xml` is managed by `cpp-developers-docker`, not this repo, so no in-repo
  app-server config change is required here.
- Items verified absent in this repo and therefore out of scope: `material-client`,
  `system-enterprise-id`, in-repo `standalone.xml`, `org.glassfish:javax.json`, `resteasy-jaxrs`,
  and direct `Json.create*` factory misuse.

## Dependencies

- `25.104.x` framework chain in Artifactory (super-pom, parent-pom, common-bom,
  framework-parent-pom, framework-libraries, microservice-framework, event-store, platform chain,
  `service-parent-pom:25.104.0-M1`, `coredomain:25.104.0-M1`).
- `cpp-developers-docker` Artemis container at 2.53 is required for production but not a local
  development blocker (wire-compatible with 2.18 per the pilot).
- The Spec Kit workflow + the mandatory build loop (code-reviewer / qa / spec-validator agents)
  defined in `.claude/rules/workflow.md`.

## Validation status (2026-06-29) — ALL MET

- SC-001 ✅ `mvn clean install` on JDK 25 — BUILD SUCCESS, 26/26 modules, **816 unit tests**, 0 failures,
  **enforcer ON** (no skip): `referencedata` bumped `17.103.131` → `17.104.136` to satisfy `enforce-moj-latest-interfaces`.
- SC-002 ✅ `./runIntegrationTests.sh` on **WildFly 40 / JDK 25 / Jakarta EE 11** — **132 ITs, 0 failures/errors**.
- SC-003 ✅ only Java SE `javax.*` imports remain (incl. `import static` forms). SC-004 ✅ no DeltaSpike.
- SC-005 ✅ WAR deploys healthy on WildFly 40 (`/internal/metrics/ping` → `pong`, no `WELD-001408`).
- SC-006 ✅ parent/version/coredomain on `25.104.0-M1`; cross-context pins on 17.x. SC-008 ✅ CI agent → `ubuntu-j25-postgres`.
- Additional fixes found during build/IT and recorded in `research.md` D10–D13: `import static` sweep,
  JAXB-4 toolchain, generator-plugin classpaths (parsson + jakarta.xml.bind-api), JDK-25 Lombok,
  PDFBox GC `reachabilityFence`, `prosecutioncasefile-refdata` exclusion (cross-context javax CDI),
  and WireMock 2.x removal (jackson conflict).
- Runtime prerequisites: build/test on JDK 25; integration tests require the WildFly 40 / JDK 25
  stack (`cpp-developers-docker` `java-25`).
