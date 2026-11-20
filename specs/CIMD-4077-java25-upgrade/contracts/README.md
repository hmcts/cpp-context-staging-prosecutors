# Contract Impact — none (shape-preserving upgrade)

This feature is a JVM/framework upgrade. The service's externally-visible contracts are
**unchanged in shape**:

- **Command API / messaging RAML** (`staging-prosecutors-command-api.raml`,
  `staging-prosecutors-command-handler.messaging.raml`) — no command added/removed/changed.
- **Query API RAML** (`stagingprosecutors-query-api.raml`) — endpoints unchanged. **One edit**:
  `baseUri` host path corrected from `.../stagingprosecutors-query-api/...` to
  `.../stagingprosecutors-service/...`. This is the deployment URL the generated remote client
  targets — not a request/response shape change. No consumer is affected (the merged-WAR root is
  the correct runtime path).
- **JSON schemas** (namespace `http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/...`) —
  unchanged.
- **Subscriptions / publications** (`subscriptions-descriptor.yaml` ×2,
  `public-publications-descriptor.yaml`) and **event-sources** (`event-sources.yaml`) — unchanged.
- **Cross-context public events** (consumed: `public.prosecutioncasefile.*`,
  `public.sjp.case-document-uploaded`; published: `public.stagingprosecutors.cps-serve-*`) —
  unchanged; remain wire-compatible with the `17.x` downstream contexts.

XML *namespace* migrations in `beans.xml` / `persistence.xml` are deployment-descriptor format
changes, not API contracts.

**Conclusion**: no new or modified interface contract requiring a contract file under this
directory. Spec-validator should confirm contract↔implementation symmetry is preserved (i.e. the
descriptors and schemas are byte-unchanged apart from the documented `baseUri` line).
