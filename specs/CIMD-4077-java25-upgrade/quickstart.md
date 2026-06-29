# Quickstart — building & verifying the Java 25 upgrade

## Prerequisites

- **JDK 25** on the build path (`java -version` → 25).
- Maven with access to the team Artifactory carrying the `25.104.x` chain
  (`service-parent-pom:25.104.0-M1`, `coredomain:25.104.0-M1`, framework + platform M-levels).
- For integration tests: Docker stack up and `CPP_DOCKER_DIR` exported to a local checkout of
  `hmcts/cpp-developers-docker`.

## Build & test

```bash
# 1. Iterative compile (local only — never commit -Denforcer.skip)
mvn clean install -DskipTests -Denforcer.skip=true

# 2. Full build + unit tests (final gate — enforcer ON)
mvn clean install

# 3. Integration tests (Dockerised; resets event store)
./runIntegrationTests.sh
```

> Do **not** run `mvn verify` directly for ITs — `runIntegrationTests.sh` restarts Docker and
> resets the event store; a bare `mvn verify` causes aggregate-stream pollution → false failures.

## Acceptance checks (map to spec Success Criteria)

```bash
# SC-003: no Jakarta-EE javax.* left — only Java SE packages should appear
grep -rn "import javax\." --include="*.java" . | grep -v /target/

# SC-004: no DeltaSpike anywhere
grep -rn "deltaspike" --include="*.java" --include="pom.xml" . | grep -v /target/   # → empty

# SC-006: version targets + cross-context pins
grep -n "service-parent-pom\|<version>\|coredomain.version" pom.xml
grep -n "prosecutioncasefile.version\|results.version\|referencedata.version\|notification.notify.version\|system.users.library.version" pom.xml
```

- **SC-001**: `mvn clean install` green on JDK 25.
- **SC-002**: `./runIntegrationTests.sh` green.
- **SC-005**: WAR deploys on WildFly 40 — `/internal/metrics/ping` returns `pong`, health checks
  healthy, no `WELD-001408` in `cpp-developers-docker/.../wildfly/log/server.log`.
- **SC-007**: changes committed as Conventional Commits on `CIMD-4077-java25-upgrade` (no push/PR).

## If something breaks

Consult the playbook gotcha catalogue (mirrored in `research.md` D8). Most likely here:
- `WELD-001408 EntityManager` → repository used `@Inject` instead of `@PersistenceContext`.
- `WELD-001408 EventSubscriptionDiscoverer` → `event-tracking-discovery` missing from service pom.
- `NoSuchMethodError ... jandex` or `NoClassDefFoundError jakarta/xml/bind/JAXBException` in
  viewstore-persistence tests → fix the pom test deps per research D6.
- Artemis "version is missing" → groupId not switched to `org.apache.artemis` in the IT pom.
