# Phase 0 Research — Java 25 / 25.104.x Upgrade

All "unknowns" for this upgrade are version targets and migration mechanics, resolved against the
HMCTS pilot (`hmcts/cpp-framework-java-upgrade-pilot`) and the validated reference context
`cpp-context-users-groups@java-25-wildfly-40-upgrade-spike`. No open NEEDS CLARIFICATION remain.

## D1 — Target framework version

- **Decision**: Parent `uk.gov.moj.cpp.common:service-parent-pom` → `25.104.0-M1`; project version
  → `25.104.0-M1-SNAPSHOT` (all 26 poms); `coredomain.version` → `25.104.0-M1`.
- **Rationale**: The reference context users-groups root pom on the java-25 branch uses exactly
  `service-parent-pom:25.104.0-M1`, `25.104.0-M1-SNAPSHOT`, `coredomain:25.104.0-M1` — same parent
  GAV as this project. The pilot status doc confirms the full chain (super/parent/common-bom/
  framework-parent/framework-libraries/microservice-framework/event-store + platform chain) is in
  Artifactory at the M-levels the parent transitively pins.
- **Alternatives considered**: Java 21 / `17.104.x` fallback (parked — not the active path);
  later milestone (none required — M1 parent is what reference contexts shipped on).

## D2 — Cross-context pins

- **Decision**: Leave `prosecutioncasefile`, `results`, `referencedata`, `notification.notify`,
  `system.users.library` on their current `17.x` versions.
- **Rationale**: Those contexts are not yet upgraded; their RAML/JSON contracts are wire-compatible
  across the gap (playbook + material-client analysis confirm RAML content is identical between
  17.x and 25.x branches for an upgrade-only change). Bumping them would force-couple to unreleased
  upstream artifacts and risk contract drift into PCF.
- **Alternatives considered**: Bump everything to 25.x — rejected (artifacts don't exist; drift risk).

## D3 — javax → jakarta namespace scope

- **Decision**: Swap Jakarta EE packages only: `javax.json`, `javax.inject`, `javax.ws.rs`,
  `javax.jms`, `javax.persistence`, `javax.xml.bind`, `javax.validation`, `javax.enterprise.*`,
  `javax.annotation`. **Leave Java SE untouched**: `javax.net`, `javax.xml.transform`,
  `javax.xml.datatype`, `javax.xml.validation`, `javax.naming`, `javax.crypto`.
- **Rationale**: Jakarta EE 9+ relocated only the EE namespaces; Java SE `javax.*` packages are
  unchanged. Confirmed by inventory: the 4 SE families present here (`net`, `xml.transform`,
  `xml.datatype`, `xml.validation`) must stay.
- **Verification**: `grep -rn "import javax\." --include=*.java src | grep -v target` must show only
  SE packages afterwards.

## D4 — beans.xml / persistence.xml namespaces

- **Decision**: `beans.xml` → `https://jakarta.ee/xml/ns/jakartaee` + `beans_4_0.xsd` +
  `version="4.0"`, `bean-discovery-mode="all"`. `persistence.xml` → `https://jakarta.ee/xml/ns/
  persistence` v3.0, drop explicit `<class>` listings (Hibernate 6 auto-detects `@Entity`). Add a
  test `persistence.xml` (RESOURCE_LOCAL, H2, `hbm2ddl=create`).
- **Rationale**: Playbook §3/§4. A half-migrated `beans.xml` (Jakarta namespace + `version="1.1"`)
  silently disables CDI discovery → `WELD-001408`. The exact EE 4.0 form is mandatory.
- **Alternatives considered**: none — namespace forms are fixed.

## D5 — DeltaSpike → JPA repository migration

- **Decision**: `SubmissionRepository` (empty `@Repository interface extends
  EntityRepository<Submission, UUID>`) → concrete `@ApplicationScoped` class with
  `@PersistenceContext(unitName="…")  EntityManager`, implementing `findBy(UUID)` via `em.find`
  (returns `null` when absent — preserves DeltaSpike semantics) and `save(Submission)` via
  `em.merge`. Migrate `SubmissionRepositoryTest` from `@RunWith(CdiTestRunner.class)` +
  `BaseTransactionalJunit4Test` to JUnit 5 + static `@RegisterExtension
  HibernateTestEntityManagerProvider`.
- **Rationale**: Playbook §6 Pattern 1 + §7. Callers use only `save()` and `findBy(UUID)`
  (verified across listener/processor). `em.find` matches DeltaSpike's null-on-miss contract used by
  the listeners' `findBy(...)` then null-check pattern — so **no caller change** is needed and the
  null-return behaviour is preserved (avoids the `NoResultException` trap).
- **Critical detail**: use `@PersistenceContext`, **not** `@Inject`, for the `EntityManager`
  (CDI 4.0 / Weld 5 does not expose `EntityManager` as a bean → `WELD-001408`).
- **Alternatives considered**: keep DeltaSpike (impossible on EE 11); generate via Spring Data
  (forbidden — not a Spring service).

## D6 — viewstore-persistence pom changes

- **Decision**: Remove `persistence-deltaspike`, `deltaspike-test-control-module-api/-impl`,
  `deltaspike-cdictrl-openejb`, `openejb-core`, `openejb-server`. Change
  `org.hibernate:hibernate-core` → `org.hibernate.orm:hibernate-core` scope `provided`. Replace
  `javax:javaee-api` (provided) with the Jakarta equivalent provided by the platform BOM. Add test
  deps: `com.h2database:h2`, `uk.gov.justice.utils:test-utils-hibernate`,
  `jakarta.xml.bind:jakarta.xml.bind-api`. Drop `test-utils-persistence`/`test-utils-common` from
  this module if they drag in conflicting `org.jboss:jandex`.
- **Rationale**: Playbook §2.4 + the jandex/JAXB gotchas. `hibernate-core` at `provided` keeps the
  annotation processor happy and wins jandex resolution (`io.smallrye:jandex:3.x`). Hibernate 6.6
  declares `jakarta.xml.bind-api` optional, so tests using `HibernateTestEntityManagerProvider` need
  it explicitly.

## D7 — Three targeted fixes

- **Decision (service WAR)**: add `uk.gov.justice.event-store:event-tracking-discovery` to
  `stagingprosecutors-service/pom.xml`. **Rationale**: this context has an EVENT_LISTENER; without
  it Weld fails with `WELD-001408 ... EventSubscriptionDiscoverer @RestDiscoverer`. Version BOM-managed.
- **Decision (Artemis groupId)**: in `stagingprosecutors-integration-test/pom.xml` change
  `org.apache.activemq:artemis-jms-client` → `org.apache.artemis:artemis-jms-client`.
  **Rationale**: 25.104.x BOM manages Artemis under the new groupId; the old one has no managed
  version → "version is missing" build error.
- **Decision (query-api baseUri)**: change RAML `baseUri` from
  `.../stagingprosecutors-query-api/query/api/rest/stagingprosecutors` to
  `.../stagingprosecutors-service/query/api/rest/stagingprosecutors`. **Rationale**: generated
  remote client must hit the merged service WAR root, else internal access-control queries miss the
  deployment and hit WireMock's catch-all.

## D8 — Hibernate 6 runtime gotchas (watch-list, reactive)

- **Decision**: Do not pre-emptively rewrite queries. Apply playbook fixes only if a build/IT
  failure surfaces them: implicit-`SELECT` JPQL, `!= null` three-valued logic, null-into-primitive
  (`@Version` caveat), `@MapsId` foreign-generator, `LazyInitializationException`,
  `JsonObjectBuilder.add(null)` NPE, constraint-violation trace assertions.
- **Rationale**: This repo has a single trivial repository and `em.find`-only access — none of the
  JPQL/lazy/`@MapsId` patterns are present. Watching is cheaper than speculative edits.

## D9 — Not applicable (verified absent)

`material-client`, `system-enterprise-id`, in-repo `standalone.xml`, `org.glassfish:javax.json`,
`resteasy-jaxrs`, direct `Json.create*` factory misuse — all confirmed absent by grep, so the
corresponding playbook sections are skipped.

## D10 — Issues surfaced during build (not in the playbook for this service)

Discovered while building on JDK 25; each fixed in lockstep:

- **`import static javax.*` missed by the first sweep** — the initial regex only matched
  `import javax.`; 3 main + 12 IT files used `import static javax.{json,ws.rs,xml.bind}.*`. Fixed
  and re-verified. (SC-003 grep must cover both forms.)
- **JAXB / CP20 schema module** (`stagingprosecutors-cps-schema`): the parent BOM pins
  `jaxb-runtime` at 2.3.1 (javax-era) and the legacy `maven-jaxb2-plugin` 0.15.2 generates
  `javax.xml.bind` code. Migrated to JAXB 4: `org.jvnet.jaxb:jaxb-maven-plugin:4.0.8` (generates
  `jakarta.xml.bind`) + reactor-wide dM pin of `org.glassfish.jaxb:jaxb-runtime` 4.0.5 (explicit,
  because the BOM hard-pins 2.3.1 and transitive consumers like command-api otherwise resolve 2.3.1
  → runtime `ClassNotFoundException: org.glassfish.jaxb.runtime.v2.ContextFactory`). Also
  `javax.activation:activation` → `jakarta.activation:jakarta.activation-api`.
- **Compiler / JDK**: build runs on JDK 25; `maven-compiler-plugin` 3.10.1 compiles `release 25`
  fine on JDK 25 (no plugin bump needed).
- **`jakarta.platform:jakarta.jakartaee-api`** replaces `javax:javaee-api` in 10 module poms
  (plain `provided` deps) and the 4 generator-plugin dependency blocks (keeping
  `${javaee-api.version}`).
- **Generator-plugin classpaths**: declare `messaging-adapter`, `messaging-client`, and
  `rest-client` generator plugins once in the root `<build><plugins>` with `org.eclipse.parsson`
  (JSON-P provider; `org.glassfish:jakarta.json` is an OSGi bundle that registers no ServiceLoader
  provider) and `jakarta.xml.bind:jakarta.xml.bind-api` 2.3.x (generators reference
  `javax.xml.bind.SchemaOutputResolver`). Merged into every module's execution.
- **Lombok on JDK 25**: BOM pins 1.18.26 (no JDK 25 support) and JDK 23+ no longer auto-runs
  classpath annotation processors. Pinned Lombok 1.18.42 (root dM) and added
  `annotationProcessorPaths` for Lombok to the integration-test compiler config (its test model
  classes are the only Lombok users).
- **Artemis groupId** `org.apache.activemq` → `org.apache.artemis` (D7).

## D11 — PDFBox document-unbundling: JDK 25 GC robustness

- **Symptom**: `COSStream has been closed` intermittently during page extraction/save in
  `event-processor` PDF tests (PDFBox 2.0.x + JDK 25 `Cleaner`/GC reclaiming the document's backing
  store mid-operation). Reliably reproduced under `-Xmx64m -XX:+UseSerialGC`.
- **Fix (production-hardening)**: `java.lang.ref.Reference.reachabilityFence(pdDocument)` in
  `PDFExtractor.splitIntoSections` (keeps the source document + its COSStreams reachable through the
  whole extraction). Plus the test fixture (`PDFTestHelper`) now serialises and reloads documents
  fully into main memory (`MemoryUsageSetting.setupMainMemoryOnly`) — a parsed PDF with no
  Cleaner-managed scratch store, mirroring production `PDDocument.load`.
- **Version**: kept PDFBox at the platform BOM's 2.0.36 — the fix makes it deterministic
  (0/12 at default heap). No version pin needed.

## D12 — Cross-context javax coupling: `prosecutioncasefile-refdata` removed

- **Finding**: deploying on WildFly 40 first failed with `WELD-001408: Unsatisfied dependencies for
  type ReferenceDataQueryService` — from `prosecutioncasefile-refdata-17.0.71.jar`, a javax-based
  CDI-bean jar bundled in the WAR as an **unused transitive** of `prosecutioncasefile-command-api:raml`.
- **Decision**: exclude `prosecutioncasefile-refdata` from the command-api dependency. Nothing in
  this service references the `…refdata` package (only `…json.schemas`, `…domain.event`,
  `…command.api`), and it was the **only** bundled javax-CDI cross-context jar. This removes the
  CDI-4.0 deployment blocker **without** requiring a Jakarta `prosecutioncasefile` release — i.e.
  the "freeze cross-context pins at 17.x" assumption holds because the offending coupling is dropped.

## D13 — WireMock / jackson conflict in ITs

- **Symptom**: every IT errored in setup with
  `NoSuchMethodError: JsonProperty.isRequired()` returning `OptBoolean` — `jackson-databind` 2.21.x
  calls the new API, but legacy `com.github.tomakehurst:wiremock:2.35.2`'s `wiremock-jre8-standalone`
  fat jar bundles an old unrelocated `jackson-annotations` that shadows 2.21.
- **Decision**: drop the legacy `com.github.tomakehurst:wiremock` dependency; `wiremock-test-utils`
  already supplies the jackson-2.21-aligned `org.wiremock:wiremock:3.13.2` (same
  `com.github.tomakehurst.wiremock` package — ITs compile unchanged). Mirrors the reference contexts.

## Validation results (2026-06-29)

- **Unit**: `mvn clean install` on JDK 25 → BUILD SUCCESS, 26/26 modules, **816 tests**, 0 failures.
- **Runtime**: WAR deploys healthy on **WildFly 40.0.0.Final / JDK 25 / Jakarta EE 11** (Camunda 7.24
  base image; `cpp-developers-docker` `java-25` branch) — `/internal/metrics/ping` → `pong`, no WELD.
- **Integration**: `./runIntegrationTests.sh` (failsafe) → **132 ITs, 0 failures, 0 errors** —
  covering submissions, CPS-serve, **PDF unbundling**, schema/business-rule validation, auth, and
  PCF/SJP response handling.
- **Enforcer**: `enforce-moj-latest-interfaces` flagged `referencedata-query-api` being behind the
  latest released interface. Fixed by bumping `referencedata.version` `17.103.131` → `17.104.136`
  (latest released, still 17.x). Full build now passes with the enforcer ON — no skip flag needed.
