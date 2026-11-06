# CLAUDE.md (backend)

Guidance for work in the VDYP REST API. Read the root `CLAUDE.md` first; this file adds backend-specific detail.

## Purpose

`backend` (`vdyp-backend`) is the public API used by the frontend and API gateway. It accepts projection requests and input files, stores file sets in object storage, runs small projections in process through `vdyp-extended-core`, and dispatches large jobs to the `batch` service through NATS.

Note that `backend/README.md` is largely the original OpenAPI Generator boilerplate (Jetty). The current service runs on **Quarkus**; use the commands below rather than `jetty:run`.

## Stack

- Quarkus (version set by `quarkus.platform.version` in `backend/pom.xml`), JAX-RS through Quarkus REST, Hibernate ORM with Panache, PostgreSQL.
- OIDC authentication (Keycloak) plus an API gateway JWT mechanism.
- Object storage through BC Gov COMS and S3 (MinIO locally).
- NATS JetStream for batch requests (`vdyp.nats.*` properties; default stream `VDYP_BATCH_REQUESTS`).
- Logback through `quarkus-logging-logback`.

## Code layout (`ca.bc.gov.nrs.vdyp.backend`)

| Package | Contents |
| --- | --- |
| `v1` | `VdypBackendApplication` (JAX-RS application) and native reflection configuration. |
| `endpoints/v1` | Resource classes (`ProjectionEndpoint`, `HelpEndpoint`, `RootEndpoint`, `OrphanFileCleanupEndpoint`, `AuthorizationProxyEndpoint`), plus `impl`, `mappers` and `openapi`. |
| `services` | Business logic and code table lookups. |
| `data` | `entities`, `repositories`, `models`, and `assemblers` that map between entities and API models. |
| `clients` | `COMSClient`, `S3StorageClient`, `VDYPBatchClient`. |
| `messaging` | NATS message types and `BatchJobPublisher`. |
| `security`, `filters`, `context` | Gateway JWT verification, role augmentation, OIDC user sync, current user context. |
| `scheduled` | `ProjectionCleanupJob`, `ProjectionStuckDetectionJob`. |
| `config` | Typed configuration mappings (`ProjectionLimitsConfig`, `COMSS3Config`, and others). |
| `exceptions`, `responses` | Domain exceptions and response types. |

Resources of note: `application.properties` (profile-specific settings use `%dev.`, `%test.`, `%prod.`), `openapi/*.swagger.yaml`, `devdb-init.sql` (test database seed), and `ExecutionFolderTemplate.zip`.

## Build, run and test

Docker must be running: tests use Quarkus Dev Services and Testcontainers for PostgreSQL.

```bash
# From the repository root, build dependencies first
mvn -pl backend -am install -DskipTests

# Dev mode with live reload (from backend/)
cd backend && ./mvnw quarkus:dev

# Tests
mvn -pl backend test
mvn -pl backend verify                 # includes integration tests
mvn -pl backend test -Dtest=SomeTest
```

- Unit tests (Surefire): `**/Test*.java`, `**/Scenario*.java`, `**/*Test.java`.
- Integration tests (Failsafe): `ITDataDriven` and `ITProjectionDebugLogging` in `integrationtests`.
- The `%test` profile drops and recreates the schema, disables gateway JWT verification, and uses a local S3 endpoint. Do not rely on these settings in other profiles.
- Shared helpers are in `src/test/java/ca/bc/gov/nrs/vdyp/backend/test/TestUtils.java`.

## Rules for changes

- The production schema is owned by Liquibase (`db/liquibase/vdyp-changelog.json` at the repository root). Hibernate runs with `schema-management.strategy=validate` outside tests, so every entity change needs a matching Liquibase changeset.
- Keep endpoints thin. Put logic in `services` and mapping in `data/assemblers`.
- API changes affect the frontend client in `frontend/src/services/vdyp-api`. Update both sides, and keep the OpenAPI annotations and `openapi/*.swagger.yaml` consistent.
- Add new configuration as a typed `config` mapping with a sensible default, and add any required `%test` overrides.
- Secrets and environment-specific values come from environment variables (for example `VDYP_DB_PROXY_*`). Never hard-code them.
- Admin storage operations (orphan file cleanup) are destructive. Follow `ORPHAN-FILE-CLEANUP.md` and keep `dryRun=true` as the default.
