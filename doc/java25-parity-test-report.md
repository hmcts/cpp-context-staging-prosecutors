# Java 25 parity test report — staging-prosecutors

**Branch:** `dev/java25` (`0a90939`), tests on `dev/java25-java17-parity`.
**Baseline:** Java 17 `main` (`5591f79`), tests on `dev/CIMD-4111-parity-with-j25`.
**Jira:** CIMD-4113. **Date:** 2026-10-19.

## What this is

These are characterisation ("golden master") tests, following the team guide *Parity Testing Java17 → Java25*. The same scenarios and assertions run against the Java 17 `main` branch and against this Java 25 branch:
- On **Java 17 every test passes**. That records how the service behaved before the upgrade.
- On **Java 25 a failing parity test is a behaviour change introduced by the upgrade.** Each one is either fixed in production code or explicitly accepted, in which case the Java 25 test is changed to pin the new behaviour.

Only the test harness and the `javax`/`jakarta` imports differ between the two copies. No production code was changed.

Apart from import renames (`javax.*` → `jakarta.*`), the upgrade changes three production classes: `SubmissionRepository` (DeltaSpike → plain JPA), `ResultsV1ResponseTransformer` (`Json` builders → framework `JsonObjects` builders) and `PDFExtractor` (a `reachabilityFence` for the JDK 25 GC). The behaviour changes below come from the persistence rewrite, Hibernate 6 and Jackson 2.21.

## Results

| Run | Tests | Failed | Notes |
|---|---|---|---|
| Java 17 `main` + parity tests, unit (JDK 17) | 442 | **0** | |
| Java 17 `main` + parity tests, `runIntegrationTests.sh` (WildFly 26) | 137 | **0** | 24 classes, 0 skipped |
| Java 25 this branch, unit (JDK 25) | 442 | **4** | all 4 are parity tests; every other test passes |
| Java 25 this branch, `runIntegrationTests.sh` (WildFly 40) | 137 | **1** | the 1 is `JavaUpgradeParityIT#spI01`; same 24 classes and per-class counts as Java 17 |

## Behaviour changes found (tests fail on Java 25)

| ID | Severity | What changed | Test (failing on J25) | Recommendation |
|---|---|---|---|---|
| SP-P04 / SP-I01 (BC-08) | **Major** | The v1 and v2 submission queries return `receivedAt` and `completedAt` as `ZonedDateTime.toString()` of the entity read back from the viewstore. Hibernate 5 read the `datetime` column back in the `UTC` region; Hibernate 6 reads it back as `ZoneOffset.UTC`. The response changes from `2026-10-19T12:49:11.033Z[UTC]` (J17) to `2026-10-19T12:49:11.033Z` (J25), confirmed on WildFly 40 + PostgreSQL. The instant is the same; the string format seen by prosecuting-authority API consumers is not. The v3 query already normalises to `Z` and is unchanged. | `viewstore-persistence` `SubmissionRepositoryParityTest#spP04_…` (1); `integration-test` `JavaUpgradeParityIT#spI01_…` (1) | **Fix**, or agree with the API owners to **accept**. To restore J17, render with `withZoneSameInstant(ZoneId.of("UTC"))` in the four `SubmissionQueryView` handlers that call `toString()` directly. If accepted (`…Z` is the standard ISO-8601 form), pin `…Z` in both J25 tests. |
| SP-M06 (BC-08) | Low | Jackson 2.21 reads `"2024-06-01T09:00:00.000Z"` into a `ZonedDateTime` with zone `ZoneOffset.UTC` (`…Z`) instead of the `UTC` region (`…Z[UTC]`). Affects `receivedDateTime` on the unbundle events. `toString()` and `equals` change. The only output use formats it with a literal `'Z'` pattern, so no payload changes. Writing is unchanged (`2024-06-01T09:00:00.000Z`), and so is metadata `createdAt` (`…Z` on both). | `event-processor` `ZonedDateTimeJsonParityTest#spM06_shouldReadReceivedDateTimeWithTheJ17Zone` (1) | **Accept.** Pin `…Z` in the J25 test. |
| SP-P02 | Medium, environment-dependent | On Java 17 the framework `EntityManagerProducer`, which the DeltaSpike repository used, set the JVM default time zone to UTC. The Java 25 `SubmissionRepository` uses `@PersistenceContext` and never calls it. `received_at` / `completed_at` are columns without time zone, so if the JVM is not in UTC they are stored as local time. The service Dockerfile does not set a time zone itself; the J25 build only sets `-Duser.timezone=UTC` for tests. When the JVM is in UTC the stored value is unchanged (`spP02_shouldStoreReceivedAtAsUtcWallClock` passes on both). | `viewstore-persistence` `SubmissionRepositoryParityTest#spP02_shouldSetJvmDefaultTimeZoneToUtcWhenRepositoryIsUsed` (1) | **Verify** that every environment's image runs in UTC, then **Accept**. Otherwise fix by setting UTC explicitly (e.g. `-Duser.timezone=UTC` in the image). |
| SP-P03 | Low | `save()` was DeltaSpike's "persist if new, else merge", which returns the same managed instance for a new entity. The Java 25 `save()` always merges and returns a managed copy, leaving the argument detached. No production caller (`SubmissionEventListener`, `CpsServeMaterialReceivedListener`) uses the return value or changes the argument after `save`. | `viewstore-persistence` `SubmissionRepositoryParityTest#spP03_…` (1) | **Accept.** Pin the new contract in the J25 test. |

## Checked and unchanged (tests pass on both)

| ID | Area | Test |
|---|---|---|
| SP-M01 / SP-M02 (BC-20) | Access-control kbases load the same rules: COMMAND_API 17, QUERY_API 8. | `command-api` `CommandApiRuleCountParityTest`, `query-api` `QueryApiRuleCountParityTest` |
| SP-M03 (BC-11) | The v1 results response is byte-identical after the switch to the framework `JsonObjects` builders (Parsson): JSON nulls, decimals, large integers, `-0.0` written as `0.0`, escapes, non-ASCII and key order. | `query-api` `ResultsV1ResponseTransformerParityTest` |
| SP-M04 | The CPS XML material path is unchanged under Jakarta XML Binding 4: the re-marshalled CP20 XML is byte-identical, and the four schema-validation 400 messages are identical. | `cps-schema` `Cp20XmlParityTest` |
| SP-M05 | PDF unbundling (PDFBox 2.0.24 → 2.0.36, plus the JDK 25 `reachabilityFence`) produces the same sections, page counts and page text for three bundles, following the production load/split/save path. | `event-processor` `PdfUnbundlingParityTest` |
| SP-M06 (write, metadata) | The framework mapper writes `receivedDateTime` as `2024-06-01T09:00:00.000Z`, and metadata `createdAt` reads as `2024-06-01T09:00Z`, on both. | `event-processor` `ZonedDateTimeJsonParityTest` (2 of 3) |
| SP-P01 (BC-01/02) | `findBy` with an unknown id returns null on both (`em.find`), so the query view still answers 404. | `viewstore-persistence` `SubmissionRepositoryParityTest#spP01_…`; `QuerySubmissionV2IT` |
| SP-P02 (UTC JVM) | With the JVM in UTC, `received_at` is stored as the UTC wall-clock time on both. | `viewstore-persistence` `SubmissionRepositoryParityTest#spP02_shouldStoreReceivedAtAsUtcWallClock` |
| SP-I02 (BC-16) | `/stagingprosecutors-service/internal/metrics/ping` answers 200 on both. | `integration-test` `JavaUpgradeParityIT#spI02_…` |

Also checked by review, with no test needed:
- BC-04: no primitive entity fields (`Submission.isCpsCase` is a `Boolean`).
- BC-05: no JPQL in production code.
- BC-06: no lazy associations.
- BC-07: `liquibase.hub.mode` was removed.
- BC-09/10/22: no Activiti, Camunda or Tika use in this context. Tika arrives transitively with `file-service-utils`, and the material/unbundle ITs pass on both.
- No expected/golden test files were changed on the Java 25 branch; only the viewstore test harness (`persistence.xml` replaces the DeltaSpike test-container properties).

## Review item without a test

**SP-P05.** `SubmissionRepository` now gets its `EntityManager` from the container (`@PersistenceContext`) instead of the framework's request-scoped producer, the same change as SPI-P01. With event-stream self-healing enabled, the framework's `EntityManagerFlushInterceptor` flushes the producer's EntityManager, which the repository no longer writes to. Listener SQL errors therefore surface at JTA commit, outside the framework's event error handling. This cannot be reproduced deterministically in a test. To restore the Java 17 behaviour, inject the framework `EntityManager` (`@Inject EntityManager`) in the repository; otherwise accept and document it.

## How to run

```bash
# Java 25 unit tests (this branch): expect exactly the 4 parity failures above
JAVA_HOME=<jdk-25> mvn -o clean install -Denforcer.skipMoJLatestInterfacesRule=true -Dmaven.test.failure.ignore=true

# Java 25 ITs: cpp-developers-docker on branch java-25. The failure-ignore flag lets the
# build reach the ITs despite the red parity unit tests; expect the 1 JavaUpgradeParityIT failure
JAVA_HOME=<jdk-25> CPP_DOCKER_DIR=<cpp-developers-docker> \
  MAVEN_ARGS="-Denforcer.skipMoJLatestInterfacesRule=true -Dmaven.test.failure.ignore=true" ./runIntegrationTests.sh
```

Run the same tests on `dev/CIMD-4111-parity-with-j25` with JDK 17, and cpp-developers-docker on `java-17` for the ITs, to see the baseline pass.

## Notes for maintainers

- When a fix lands, the matching parity test should start passing. Keep the tests as regression guards.
- The J25 viewstore harness creates the schema with `hbm2ddl`. On Hibernate 6+ that makes `submission.received_at` a column *with* time zone, so the `received_at` tests first change it back to `TIMESTAMP`, to match Liquibase (`datetime`) and the Java 17 harness. Without that, SP-P04 reads back `10:00+01:00` instead of the production behaviour.
- The J17 viewstore harness (HSQLDialect over H2) cannot look a UUID-keyed entity up after the persistence context is cleared, so the parity tests read back by `caseUrn` instead.
- When switching cpp-developers-docker between `java-17` and `java-25` by hand, remove the `containers_cpp-wildfly-*` volumes first: the configuration volume keeps the other WildFly version's `standalone.xml` and WildFly will not boot. `runIntegrationTests.sh` recreates these volumes itself.
