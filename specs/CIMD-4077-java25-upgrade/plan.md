# Implementation Plan: Upgrade staging-prosecutors to Java 25 / Framework E 25.104.x

**Branch**: `CIMD-4077-java25-upgrade` | **Date**: 2026-06-29 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/CIMD-4077-java25-upgrade/spec.md`

## Summary

Migrate the stagingprosecutors context from Framework E `17.104.x` (Java 17 / WildFly 26 /
Jakarta EE 8, `javax.*`) to `25.104.x` (Java 25 / WildFly 40 / Jakarta EE 11, `jakarta.*`),
following the HMCTS pilot playbook and mirroring the validated reference context
`cpp-context-users-groups`. The change is overwhelmingly mechanical (pom version bumps, a
`javax.*`→`jakarta.*` namespace swap across 121 files, XML-namespace updates to `beans.xml` /
`persistence.xml`) plus one small behavioural migration (the lone DeltaSpike `SubmissionRepository`
→ a concrete JPA bean) and three targeted fixes (service-WAR dependency, Artemis groupId,
query-api `baseUri`). No RAML/JSON-schema/descriptor/event-shape semantics change.

## Technical Context

**Language/Version**: Java 17 → **Java 25**
**Primary Dependencies**: Justice Services Framework / CPP `service-parent-pom` `17.104.1` →
`25.104.0-M1`; `coredomain` `17.104.4` → `25.104.0-M1`; WildFly 26 → 40; Jakarta EE 8 → 11;
Hibernate 5 → 6.6 (via WildFly 40); Artemis client groupId `org.apache.activemq` →
`org.apache.artemis`. Cross-context pins (`prosecutioncasefile`, `results`, `referencedata`,
`notification.notify`, `system.users.library`) stay on `17.x`.
**Storage**: Event store `DS.eventstore` + viewstore `java:/DS.stagingprosecutors` (Liquibase +
JPA/Hibernate; DeltaSpike removed).
**Testing**: JUnit + Mockito (surefire); repository unit test → JUnit 5 +
`HibernateTestEntityManagerProvider`; Dockerised IT harness `./runIntegrationTests.sh` (failsafe,
JSONAssert, Cucumber).
**Target Platform**: WildFly 40 on JDK 25 (Docker), deployed as `stagingprosecutors-service` WAR.
**Project Type**: Multi-module Maven reactor (26 poms), CQRS / event-sourced microservice.
**Performance Goals**: No regression; inherits WildFly virtual-thread concurrency for free.
**Constraints**: API/event contracts must remain byte-identical (JVM-independent); cross-context
pins must not drift; commit style Conventional Commits; no push/PR.
**Scale/Scope**: 11 top-level modules; 121 Java files with `javax.*` imports; 10 `beans.xml`;
1 `persistence.xml`; 1 DeltaSpike repository.

## Constitution Check

*GATE: evaluated against Constitution v1.0.0.*

| Principle | Status | Notes |
|---|---|---|
| I. RAML/JSON-Schema Contract First | PASS | No command/event/schema shape change. Only contract-file edit is the query-api `baseUri` (deployment URL, not message shape). |
| II. CQRS Three-Layer Discipline | PASS | Namespace migration touches all three layers uniformly and in lockstep; **no domain/public event shape changes**, so no layer drifts. Plan documents all three are touched mechanically only. |
| III. CPP Framework Idioms — No Manual Rolling | DEVIATION (justified) | Constitution says "Deltaspike repositories only". DeltaSpike has **no Jakarta EE 10/11 support** — removed and replaced with a framework-idiomatic CDI `@ApplicationScoped` + `@PersistenceContext` JPA bean (the pilot's mandated pattern). No Spring, no hand-rolled JMS/JDBC. See Complexity Tracking. |
| IV. Spec-Driven Build Loop | PASS | Runs through specify→clarify→plan→tasks→analyze→implement with code-reviewer/qa/spec-validator agents. |
| V. HMCTS CPP Standards Compliance | DEVIATION (justified) | Constitution pins Java 17 + `service-parent-pom:17.104.x`. This feature **is** the platform upgrade Principle V anticipates; it moves to Java 25 + `25.104.0-M1`. A constitution amendment (V + Tech Stack + rule files) should follow separately. See Complexity Tracking. |
| VI. Schema-Subscription Symmetry | PASS | No event added/removed/renamed; descriptors and schemas unchanged. |
| VII. No System.out/err — SLF4J | PASS | No new logging introduced. |
| VIII. Test-Driven Development | PASS | The one behavioural change (DeltaSpike→JPA repository) is TDD: migrate `SubmissionRepositoryTest` to the new harness first, then convert the production class. Everything else is mechanical refactor (Principle VIII exempt). |

**Gate result**: PASS with two justified, documented deviations (III, V) — both inherent to a
framework upgrade and recorded in Complexity Tracking. No unjustified violations.

## Project Structure

### Documentation (this feature)

```text
specs/CIMD-4077-java25-upgrade/
├── plan.md              # This file
├── spec.md              # Feature spec (/speckit.specify + /speckit.clarify)
├── research.md          # Phase 0 — version + migration-mechanics decisions
├── data-model.md        # Phase 1 — Submission entity + repository contract
├── quickstart.md        # Phase 1 — how to build/verify the upgrade
├── contracts/README.md  # Phase 1 — contract-impact note (no shape changes)
├── checklists/requirements.md
└── tasks.md             # Phase 2 (/speckit.tasks — not created here)
```

### Source Code (repository root) — areas touched

```text
pom.xml                                              # parent + version + coredomain
stagingprosecutors-*/**/pom.xml                      # 25 module poms: version (+ artemis/event-tracking-discovery)
stagingprosecutors-*/**/src/main/java/**/*.java      # javax.* -> jakarta.* (121 files)
stagingprosecutors-*/**/src/test/java/**/*.java      # javax.* -> jakarta.* + repo test migration
stagingprosecutors-*/**/src/main/resources/META-INF/beans.xml          # 10 files -> EE 4.0
stagingprosecutors-viewstore/stagingprosecutors-viewstore-persistence/
├── pom.xml                                          # drop deltaspike/openejb; hibernate-core org.hibernate.orm provided; add h2/test-utils-hibernate/jakarta.xml.bind-api; javaee-api -> jakarta
├── src/main/java/.../repository/SubmissionRepository.java          # iface -> @ApplicationScoped JPA bean
├── src/test/java/.../repository/SubmissionRepositoryTest.java      # JUnit5 + HibernateTestEntityManagerProvider
├── src/main/resources/META-INF/persistence.xml     # jakarta persistence 3.0
└── src/test/resources/META-INF/persistence.xml      # NEW (RESOURCE_LOCAL / H2)
stagingprosecutors-service/pom.xml                   # + event-tracking-discovery
stagingprosecutors-integration-test/pom.xml          # artemis groupId
stagingprosecutors-query/stagingprosecutors-query-api/src/raml/stagingprosecutors-query-api.raml   # baseUri
```

**Structure Decision**: No structural change — the existing 11-module reactor is preserved. The
upgrade edits files in place; no modules added or removed.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Principle III — remove DeltaSpike ("Deltaspike repositories only") | DeltaSpike has no Jakarta EE 10/11 support; cannot run on WildFly 40 / CDI 4.0. Pilot mandates a concrete `@ApplicationScoped` + `@PersistenceContext` JPA bean. | Keeping DeltaSpike is impossible on the target stack — the WAR fails CDI startup. No simpler alternative. |
| Principle V — Java 17 → 25, parent 17.104.x → 25.104.0-M1 | Java 17 / WildFly 26 baseline is end-of-support; this is the sanctioned platform upgrade, already validated on reference contexts. | Staying on 17.104.x leaves the service unsupported and unable to release with the upgraded platform. A follow-on constitution amendment will realign V + Tech Stack + rule files. |

> Follow-up (out of scope here): amend constitution to update Principle V (Java/parent),
> Principle III (persistence), and the Technology Stack section once the estate standardises on
> 25.104.x. Track separately.
