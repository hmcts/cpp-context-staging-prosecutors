# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The **stagingprosecutors** context — an HMCTS CPP digital service that **stages incoming prosecution submissions**. It receives prosecution and material submissions from prosecuting authorities (charge / requisition / SJP / summons prosecutions, applications, material submissions, CPS-served documents — PET/BCM/PTPH/COTR — POCA emails, and document unbundling), validates and stages them as event-sourced aggregates, projects a read-model viewstore, and publishes public events to downstream contexts (Prosecution Case File, results, notification). It also reacts to responses from those contexts (e.g. PCF acceptance/rejection).

It is a CQRS + event-sourced microservice built on the `uk.gov.justice` *Justice Services Framework* (parent `uk.gov.moj.cpp.common:service-parent-pom`). Java 17, packaged as a WildFly WAR (`stagingprosecutors-service`). CDI + Deltaspike; Lombok is used for boilerplate.

## Build & test

```bash
mvn clean install                       # full multi-module build + unit tests
mvn clean install -DskipTests           # build, no tests
mvn test                                # unit tests only
mvn -pl stagingprosecutors-command/stagingprosecutors-command-handler -am clean install   # one module with deps
mvn -pl <module> test -Dtest=ClassName#methodName                                          # single test
```

Unit tests run under surefire. Integration tests (`stagingprosecutors-integration-test`) require the full Docker stack and do **not** run from a plain `mvn test`.

### Integration tests
`./runIntegrationTests.sh` runs Liquibase across all stores (event log, event-log aggregate snapshot, event buffer, viewstore, system), deploys WARs + WireMock, healthchecks, then the IT suite. Requires `CPP_DOCKER_DIR` exported and pointing at a local checkout of `hmcts/cpp-developers-docker`, plus the Docker stack running.

### System commands
`./runSystemCommand.sh` wraps `framework-jmx-command-client` to run framework JMX system commands against a running instance (e.g. `./runSystemCommand.sh CATCHUP`). Run with no args to list commands.

## Architecture — three layers (CQRS / event-sourced)

Data flows command → aggregate → events → event store → (listener → viewstore) + (processor → public events). Every change touching events MUST be reasoned about across all three layers.

```
1. Command side: REST/messaging → stagingprosecutors.handler.command → @Handles handler → aggregate → domain event
       ↓ writes to java:/app/stagingprosecutors-service/DS.eventstore → topic stagingprosecutors.event
2. Event listener: projects domain events → viewstore (java:/DS.stagingprosecutors)
3. Event processor: consumes domain events + inbound public events → publishes public.event for PCF/results/notification
```

- **stagingprosecutors-command** — write side. `*-command-api` (RAML + JSON schemas), `*-command-handler` (`@Handles` handlers). Commands declared in `staging-prosecutors-command-handler.messaging.raml` (media types `application/vnd.stagingprosecutors.command.<name>+json`).
- **stagingprosecutors-domain** — `domain-aggregates` (`ProsecutionSubmission`, `ApplicationSubmission`, `CpsSubmission`, `MaterialSubmission`, `UnbundleSubmission`, `PocaEmailAggregate`: turn commands into events, rebuild state via `apply(event)`), `domain-events`, `domain-event-processor`, `domain-transformation`, `domain-values`, `domain-value-schema`.
- **stagingprosecutors-event** — `*-event-listener` projects events → viewstore via converters; `*-event-processor` consumes domain events and inbound public events, and publishes public events.
- **stagingprosecutors-event-sources** — `src/yaml/event-sources.yaml` declares the `stagingprosecutors` stream (topic `stagingprosecutors.event`, `DS.eventstore`) and the `public` stream (`public.event`).
- **stagingprosecutors-query** — read side. `*-query-api` (RAML), `*-query-view` (read services + response DTOs over the viewstore).
- **stagingprosecutors-viewstore** — Liquibase changelogs (read-model schema) + Deltaspike persistence for `java:/DS.stagingprosecutors`.
- **stagingprosecutors-service** — the deployable composite WAR; `src/main/descriptors/resource-descriptor.yml` wires datasources, the `stagingprosecutors.handler.command` queue, and the `stagingprosecutors.event` / `public.event` topics.
- **stagingprosecutors-common**, **stagingprosecutors-test-utils**, **stagingprosecutors-healthchecks**, **stagingprosecutors-integration-test**.

### Public-event relationships
- **Publishes** (`public-publications-descriptor.yaml`): `public.stagingprosecutors.cps-serve-{pet,bcm,ptph,cotr}-received`, `cps-update-cotr-received`, etc.
- **Consumes** (processor `subscriptions-descriptor.yaml`): `public.sjp.case-document-uploaded`, and PCF responses `public.prosecutioncasefile.{prosecution-rejected, material-rejected(-v2)(-with-warnings), material-pending-with-warnings, prosecution-submission-succeeded}`.

### Contracts are RAML + JSON schema
- `staging-prosecutors-command-api.raml`, `staging-prosecutors-command-handler.messaging.raml`, `stagingprosecutors-query-api.raml`.
- JSON schemas under a **single namespace** `http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/...`, referenced by `schema_uri`.
- Listener and processor each have a `subscriptions-descriptor.yaml`; the processor also has a `public-publications-descriptor.yaml`. Add/change an event by editing the schema + descriptor alongside the handler.

## CI / branching

- CI is Azure DevOps (`azure-pipelines.yaml`) using shared `hmcts/cpp-azure-devops-templates`: on PR → `context-verify`, on CI build → `context-validation`. SonarQube project `uk.gov.moj.cpp.staging.prosecutors:stagingprosecutors`; `serviceName=stagingprosecutors`; `itTestFolder=stagingprosecutors-integration-test`; pool `MDV-ADO-AGENT-AKS-01` / `centos8-j17`.
- Uses jgitflow; the develop branch is `main`. Release branches are `dev/release-*` (excluded from CI triggers). Parent `service-parent-pom:17.104.1`; module versions managed in the parent `pom.xml`.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
at `specs/001-results-verdict-schema/plan.md`
<!-- SPECKIT END -->
