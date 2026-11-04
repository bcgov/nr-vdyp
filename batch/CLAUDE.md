# CLAUDE.md (batch)

Guidance for work in the VDYP batch processing service. Read the root `CLAUDE.md` first; this file adds batch-specific detail.

## Purpose

`batch` (`vdyp-batch`) runs large projections. It consumes batch requests from NATS JetStream, partitions the input, runs each partition through `vdyp-extended-core` with Spring Batch, aggregates results, stores them through COMS and S3, and reports progress back to the backend. Several workers can run at once; job ownership leases let a worker recover jobs abandoned by another.

## Stack

- Spring Boot (version set by `spring-boot.version` in `batch/pom.xml`), Spring Batch, Spring Web, Actuator with Prometheus metrics, Spring Security with OAuth2 client.
- PostgreSQL (database `batch`; H2 for some tests). The Spring Batch schema is not auto-created (`spring.batch.jdbc.initialize-schema=never`); it is managed by `db/liquibase/batch-changelog.json`.
- NATS (`jnats`) for requests, prioritization and stop signals.
- Serves on port `8081` with context path `/vdyp-batch`.

## Code layout (`ca.bc.gov.nrs.vdyp.batch`)

| Package | Contents |
| --- | --- |
| `VdypBatchApplication` | Spring Boot entry point. |
| `configuration` | Job and step wiring: `BatchConfiguration`, item reader, processor and writer, retry and skip policies, `BatchProperties`, async configuration. |
| `service` | Job launch, input partitioning, projection, result aggregation, metrics, prioritization, stop and shutdown drain, recovery metadata. |
| `messaging` | NATS configuration and properties, `consumer`, `message`. |
| `ownership` | Worker identity, job leases (`JobOwnershipService`, `OwnedJobRegistry`), readiness health indicator. |
| `client` | `coms` and `vdyp` (backend) clients. |
| `controller` | `BatchController` REST endpoints. |
| `persistence` | JPA `model` and `repository`. |
| `exception`, `model`, `util` | Domain exceptions, DTOs, constants and helpers. |

Configuration is in `src/main/resources/application.properties`. Almost every value has an environment variable override with a local default (for example `BATCH_DB_HOST`, `BATCH_THREAD_POOL_SIZE`, `BATCH_OWNERSHIP_ENABLED`). Tests use `src/test/resources/application-test.properties`.

## Build, run and test

```bash
# From the repository root, build dependencies first
mvn -pl batch -am install -DskipTests

# Run locally (needs PostgreSQL, NATS, COMS and MinIO, for example via docker compose)
cd batch && ./mvnw spring-boot:run

# Tests
mvn -pl batch test
mvn -pl batch test -Dtest=SomeTest
```

- Surefire includes `**/*Test.java` and `**/*Tests.java`.
- Tests mirror the main package structure; end-to-end style tests are in `scenarios`. Test data is in `src/test/resources/test-data`.

## Rules for changes

- Graceful shutdown and job recovery are central. Changes to threading, shutdown timeouts, ownership leases or NATS acknowledgement must keep these invariants: a job is owned by one worker at a time, a lease that expires can be recovered, and a shutting-down worker drains or releases its jobs before exit. Add tests for any change in this area.
- Keep timeout properties consistent with each other (for example `spring.lifecycle.timeout-per-shutdown-phase`, `batch.thread-pool.await-termination-seconds`, `batch.ownership.shutdown-wait`) and with the Helm chart settings in `charts/`.
- Schema changes go in `db/liquibase/batch-changelog.json`, never through Spring Batch auto-initialization.
- Projection logic belongs in `lib/`. The batch service should orchestrate, not compute.
- Message formats shared with `backend` (for example `BatchRequestMessage`) must be changed on both sides together, and NATS stream or consumer changes go in `nats/` at the repository root.
- Add new settings to `BatchProperties` (or the relevant properties class) with an environment variable override and a safe default.
