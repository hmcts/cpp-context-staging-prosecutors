# Architecture & Domain Rules

## Three Layers (CQRS / Event-Sourced)

```
1. Command side (handler → aggregate → domain event)
       ↓ writes to event store (java:/app/stagingprosecutors-service/DS.eventstore)
       ↓ published to JMS topic stagingprosecutors.event

2. Event listener (projects events → viewstore tables)
       ↓ projects to java:/DS.stagingprosecutors

3. Event processor (consumes domain events + inbound public events → publishes public events)
       ↓ public.event for Prosecution Case File / results / notification
```

Every change touching events MUST be reasoned about across **all three layers**. Breaking one without the others produces silent data drift — and here it escapes the context boundary into Prosecution Case File.

- **Command side** — commands arrive on the `stagingprosecutors.handler.command` queue, dispatched by `@Handles` to handler classes which ask the aggregate to perform the command; the aggregate emits domain events. State is rebuilt by replaying events via `apply(...)`.
- **Event listener** — projects domain events into the viewstore (`java:/DS.stagingprosecutors`). Lives under `stagingprosecutors-event/stagingprosecutors-event-listener`. Converters map events → viewstore entities.
- **Event processor** — consumes domain events AND inbound public events (PCF responses, sjp document uploads), and publishes public events to downstream contexts. Lives under `stagingprosecutors-event/stagingprosecutors-event-processor`. Heavy use of converters.

## Domain Concepts

| Concept                 | Description                                                                                                                              |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| ProsecutionSubmission   | Aggregate for an incoming prosecution submission (charge / requisition / SJP / summons).                                                 |
| ApplicationSubmission   | Aggregate for an application submission.                                                                                                  |
| CpsSubmission           | Aggregate for CPS-served documents (PET / BCM / PTPH / COTR, update-COTR).                                                                |
| MaterialSubmission      | Aggregate for material submissions (submit / reject / pending-with-warnings).                                                            |
| UnbundleSubmission      | Aggregate for document unbundling (record unbundle result / unbundled-document results).                                                 |
| PocaEmailAggregate      | Aggregate for POCA email receipt and validation.                                                                                         |
| Domain event            | Internal event written to the event store. Examples: `prosecution-received`, `sjp-prosecution-received`, `submission-successful(-with-warnings)`, `material-submitted`, `cps-material-submitted`, `material-submission-successful/rejected`, `submission-rejected`, `cps-serve-*-received`, `document-unbundled(-v2)`, `poca-document-validated/not-validated`. |
| Public event (out)      | Published on `public.event` via `public-publications-descriptor.yaml` (e.g. `public.stagingprosecutors.cps-serve-pet-received`).         |
| Public event (in)       | Consumed from `public.event`: `public.sjp.case-document-uploaded`, `public.prosecutioncasefile.{prosecution-rejected, material-rejected(-v2)(-with-warnings), material-pending-with-warnings, prosecution-submission-succeeded}`. |
| Command                 | Inbound request via `stagingprosecutors.handler.command`. Declared in RAML, dispatched by `@Handles`. Examples: charge-prosecution, sjp-prosecution, summons-prosecution, requisition-prosecution, submit-application, submit-material, submit-cps-material, submit-cps-serve-{bcm,cotr,pet,ptph}, submit-cps-update-cotr, receive-poca-email, record-document-unbundle-result, record-unbundled-document-results, reject-material, reject-submission, update-submission-status. |
| Viewstore               | Read model `java:/DS.stagingprosecutors`, populated by listeners. Schema managed by `stagingprosecutors-viewstore-liquibase`.            |
| Event store             | Append-only log `DS.eventstore`. Source of truth for aggregate state. Schema managed by `event-repository-liquibase`.                     |

## Authoritative Routing Files (always re-read before reasoning about a flow)

- `stagingprosecutors-event-sources/src/yaml/event-sources.yaml` — event-source streams (`stagingprosecutors` topic + `DS.eventstore`; `public` → `public.event`).
- `stagingprosecutors-event/stagingprosecutors-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions.
- `stagingprosecutors-event/stagingprosecutors-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions (own domain events + inbound public events).
- `stagingprosecutors-event/stagingprosecutors-event-processor/src/yaml/public-publications-descriptor.yaml` — published public events.
- `stagingprosecutors-command/stagingprosecutors-command-handler/src/raml/staging-prosecutors-command-handler.messaging.raml` — command → handler mapping.
- `stagingprosecutors-command/stagingprosecutors-command-api/src/raml/staging-prosecutors-command-api.raml` and `stagingprosecutors-query/stagingprosecutors-query-api/src/raml/stagingprosecutors-query-api.raml` — HTTP APIs.
- `stagingprosecutors-service/src/main/descriptors/resource-descriptor.yml` — datasources, command queue, topics, service mapping.
- Per-command/per-event JSON schemas (single namespace `cpp.moj.gov.uk/staging/prosecutors`).

## Module Layout

- `stagingprosecutors-common` — shared utils / value objects
- `stagingprosecutors-command/stagingprosecutors-command-api` — RAML + JSON schemas
- `stagingprosecutors-command/stagingprosecutors-command-handler` — `@Handles` handlers
- `stagingprosecutors-domain/stagingprosecutors-domain-aggregates` — the six submission aggregates
- `stagingprosecutors-domain/stagingprosecutors-domain-events` — event POJOs / schemas
- `stagingprosecutors-domain/stagingprosecutors-domain-event-processor` — domain-level event processing
- `stagingprosecutors-domain/stagingprosecutors-domain-transformation` — transformations (incl. anonymisation)
- `stagingprosecutors-domain/stagingprosecutors-domain-values`, `-domain-value-schema` — value objects + schema
- `stagingprosecutors-event/stagingprosecutors-event-listener` — listeners + converters → viewstore
- `stagingprosecutors-event/stagingprosecutors-event-processor` — processors + converters → public events; inbound public-event handling
- `stagingprosecutors-event-sources` — `event-sources.yaml`
- `stagingprosecutors-query/stagingprosecutors-query-api`, `-query-view` — query RAML + read services over the viewstore
- `stagingprosecutors-viewstore` — Liquibase migrations + Deltaspike persistence
- `stagingprosecutors-service` — composite packaging WAR; `resource-descriptor.yml` wires datasources / queue / topics
- `stagingprosecutors-healthchecks`, `stagingprosecutors-test-utils`, `stagingprosecutors-integration-test` (`*IT.java` via `runIntegrationTests.sh`)

## Adding a New Command

1. **RAML first.** Add the command to `staging-prosecutors-command-handler.messaging.raml` (and the command-api RAML) with the right media type (e.g. `application/vnd.stagingprosecutors.command.<name>+json`).
2. **JSON schema.** Add the command payload schema (single namespace).
3. **Handler.** Add `@Handles("<command-name>")` on a `@ServiceComponent(COMMAND_HANDLER)` class; method takes `Envelope<CommandPayload>`.
4. **Aggregate.** If the command mutates state, the handler asks the aggregate to perform it; the aggregate emits a domain event and rebuilds state via `apply(event)`.
5. **Listener.** If the new event updates the viewstore: subscription entry + JSON schema + listener method + converter.
6. **Processor.** If the new event triggers a public event or downstream interaction: subscription (or publication) entry + JSON schema + processor method + converter.
7. **Tests.** Failing unit tests for handler, aggregate, listener (if touched), processor (if touched), converters (if touched). Then production code. Then IT exercising the end-to-end flow.

## Adding a New Domain Event

- Add the event's JSON schema (single namespace).
- Update the listener AND/OR processor `subscriptions-descriptor.yaml` (the two subscribe to overlapping but not identical sets — wire it to the component(s) that consume it; document any unaffected).
- For a published public event, add it to `public-publications-descriptor.yaml`.
- Update `event-sources.yaml` if a new topic is introduced.
- Add the listener/processor method + converter, and the failing-then-passing tests.

## Adding a Public-Event Subscription (incoming from another context)

1. **Subscription entry.** Add to listener and/or processor `subscriptions-descriptor.yaml` for the `public` source (e.g. `public.prosecutioncasefile.*`, `public.sjp.*`).
2. **JSON schema.** Add the public-event schema (matches the upstream context's contract version).
3. **Listener / processor method.** With `@Handles("<public-event-name>")` and `Envelope<PayloadType>`.
4. **Converter.** Map the public-event payload → either a viewstore entity (listener) or a domain command / status update (processor).
5. **Tests.** Unit tests for the listener/processor + converter. IT simulating the public-event arrival.

## Out-of-Scope (do not add)

- Hand-rolled JMS listeners — use the framework's `@Handles`
- Hand-rolled JDBC — use Liquibase changelogs and Deltaspike repositories
- Ad-hoc `ObjectMapper` instances — use the framework's configured mapper
- Manual JSON schema validation — the framework validates incoming envelopes against subscription-declared schemas
- Spring annotations (`@Autowired`, `@Component`, `@Service`) — this service uses CDI (Lombok IS allowed)
- Cross-context coupling beyond declared public events — never call another context's command API directly; publish/consume public events instead

## Common Gotchas

1. **Schema-subscription drift** — adding a `subscriptions-descriptor.yaml` / `public-publications-descriptor.yaml` entry without the matching JSON schema produces a runtime 500 on dispatch. Constitution Principle VI makes this a review-blocker.
2. **Three-layer drift** — modifying a domain event without updating the listener AND processor (where each consumes it) is the most common silent-data-drift bug, and here it leaks into Prosecution Case File. Constitution Principle II makes this a review-blocker.
3. **Forgetting the inbound public-event side** — this context reacts to PCF responses (`prosecution-submission-succeeded`, `prosecution-rejected`, `material-rejected`, `material-pending-with-warnings`) and `public.sjp.case-document-uploaded`; a submission-status change often needs the inbound handler updated too.
4. **Liquibase registration** — adding a changelog file without registering it in the right registry (event-store / aggregate-snapshot / viewstore / event-buffer) means it never applies in CI's IT setup.
5. **Wrong `@ServiceComponent` value** — `COMMAND_HANDLER` vs `EVENT_LISTENER` vs `EVENT_PROCESSOR` are NOT interchangeable; the framework dispatches based on the value.
6. **Cross-context pin drift** — bumping `prosecutioncasefile` / `results` / `notification.notify` / `referencedata` / `coredomain` versions in `pom.xml` requires bumping the matching schema/RAML classifier dep to the same version, or the submission→PCF contract drifts.
