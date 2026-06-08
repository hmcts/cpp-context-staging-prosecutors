# Service Identity

- **Service:** cpp-context-staging-prosecutors
- **Description:** Prosecution-submission staging/ingestion context. Receives prosecution and material submissions (charge / requisition / SJP / summons prosecutions, applications, material submissions, CPS-served documents PET/BCM/PTPH/COTR, POCA emails, document unbundling), validates and stages them as event-sourced aggregates, projects a read-model viewstore, and publishes public events to downstream contexts (Prosecution Case File, results, notification). Also consumes responses from PCF and sjp.
- **Bounded context:** `stagingprosecutors` (one of many CPP contexts).
- **Programme:** Crime Common Platform (CPP).
- **Organisation:** HMCTS / Ministry of Justice.

## Technology Stack

| Component         | Value                                                                |
|-------------------|----------------------------------------------------------------------|
| Build tool        | Maven (multi-module reactor; root `pom.xml`, `stagingprosecutors`)   |
| Language          | Java 17 (CI demand `centos8-j17`)                                    |
| Framework         | Justice Services Framework / CPP `service-parent-pom:17.104.x` (CDI/Deltaspike) |
| Packaging         | WAR (`stagingprosecutors-service`) → WildFly via Docker              |
| Annotations       | `@ServiceComponent`, `@Handles`, `@ApplicationScoped`                |
| Boilerplate       | Lombok (permitted, already in use)                                   |
| Persistence       | Liquibase changelogs + Deltaspike repositories (event-store, aggregate-snapshot, viewstore, event-buffer) |
| Messaging         | ActiveMQ (Docker for ITs); JMS queue + topics                        |
| Tests             | JUnit + Mockito (unit, surefire); framework's IT harness (`runIntegrationTests.sh`, failsafe); JSONAssert; Cucumber |
| CI                | Azure DevOps Pipelines (`azure-pipelines.yaml` + `hmcts/cpp-azure-devops-templates`) |
| Quality gate      | SonarQube in CI (project `uk.gov.moj.cpp.staging.prosecutors:stagingprosecutors`) |
| Java packaging    | Root namespace `uk.gov.moj.cpp.staging.*`                            |

## Constraints

- Maven is the current build tool. Future migration to Gradle is allowed but requires coordinating constitution + rule files + CI pipeline together (see Constitution Principle V).
- Java 17 only — prefer explicit types in public APIs
- Use the framework's `@ServiceComponent` + `@Handles` for command/event handling — NOT hand-rolled JMS listeners
- DI: CDI (`@ApplicationScoped` / `@Inject`); Lombok permitted for boilerplate; never Spring (`@Autowired` / `@Component` / `@Service`)
- Aggregate state mutation must go through the aggregate's `apply(event)` replay (`ProsecutionSubmission` / `ApplicationSubmission` / `CpsSubmission` / `MaterialSubmission` / `UnbundleSubmission` / `PocaEmailAggregate`)
- Event listeners and processors must use converter classes in `converter/` packages — NOT inline mapping
- Contracts (RAML, JSON schemas, `subscriptions-descriptor.yaml`, `public-publications-descriptor.yaml`, `event-sources.yaml`) update FIRST, Java second (Constitution Principle I)
- Schema additions / removals / renames update both the subscription/publication descriptor AND the JSON schema in lockstep (Constitution Principle VI)
- Logging via SLF4J only — no `System.out` / `System.err` (Constitution Principle VII)
- Test-Driven Development is mandatory (Constitution Principle VIII)

## Build & Test Commands

```bash
# Full build + unit tests
mvn clean install

# Build, no tests
mvn clean install -DskipTests

# Unit tests only
mvn test

# Single module with deps
mvn -pl stagingprosecutors-command/stagingprosecutors-command-handler -am clean install

# Single unit test
mvn -pl <module> test -Dtest=ClassName#methodName

# Integration tests (requires Dockerised env up; CPP_DOCKER_DIR must be set)
./runIntegrationTests.sh

# Single IT against running env
mvn -pl stagingprosecutors-integration-test test -Dit.test=ClassNameIT

# Framework JMX commands
./runSystemCommand.sh           # help / list
./runSystemCommand.sh CATCHUP   # run one
```

## Key version pins (`pom.xml`)

- Parent: `uk.gov.moj.cpp.common:service-parent-pom:17.104.x` (currently 17.104.1); artifact `stagingprosecutors` (currently `17.104.62-SNAPSHOT`), groupId `uk.gov.moj.cpp.staging.prosecutors`
- Cross-context / notable pins to keep aligned: `prosecutioncasefile` (the submission→PCF contract), `results`, `notification.notify`, `referencedata`, `coredomain`, `system.users.library`
- When bumping any of these, also check the matching schema/RAML classifier dep is on the same version
