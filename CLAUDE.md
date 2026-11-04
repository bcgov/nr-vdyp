# CLAUDE.md

Guidance for Claude when working in this repository. Subprojects may define their own `CLAUDE.md` files with module-specific detail; those take precedence for work inside that subproject.

## Project overview

VDYP (Variable Density Yield Projection) produces yield predictions for unmanaged British Columbia forest stands for inventory and timber supply applications. This repository is a Java 17 port of the original VDYP7 Fortran/C code base, plus a web API, a batch processing service, a Vue frontend, and the deployment infrastructure for BC Government OpenShift.

- Group ID: `ca.bc.gov.nrs.vdyp`
- Root artifact: `vdyp-root` (Maven multi-module, packaging `pom`)
- Java packages: `ca.bc.gov.nrs.vdyp.*`

## Repository layout

| Path | Purpose |
| --- | --- |
| `lib/` | Core computation library and command line utilities (Maven aggregator). |
| `lib/vdyp-common` | Shared model, control file parsing, computation and estimation methods used by all stages. |
| `lib/vdyp-fip` | FIPSTART stage (FIP inventory input to VDYP polygons). |
| `lib/vdyp-vri` | VRISTART stage (VRI inventory input to VDYP polygons). |
| `lib/vdyp-forward` | Forward growth projection. |
| `lib/vdyp-back` | Back (backwards) growth projection. |
| `lib/vdyp-sindex` | Site index library. |
| `lib/vdyp-si32` | SI32 site index functionality. |
| `lib/vdyp-extended-core` | Higher-level projection orchestration shared by the backend and batch services. |
| `lib/vdyp-debug-logging` | Request debug logging support. |
| `lib/vdyp-integration-tests` | Integration test framework and shared test data. |
| `backend/` | REST API (Quarkus, JAX-RS). Package `ca.bc.gov.nrs.vdyp.backend`. |
| `batch/` | Batch projection service (Spring Boot, Spring Batch, NATS messaging, PostgreSQL). Package `ca.bc.gov.nrs.vdyp.batch`. |
| `frontend/` | Vue 3 + TypeScript + Vite SPA with Keycloak authentication. Tests use Cypress. Not part of the Maven build. |
| `vdyp-test-oracle/` | Runner that compares Java output against the legacy VDYP7 implementation. |
| `buildtools/` | Build support, including the Eclipse formatter profile at `buildtools/src/main/resources/eclipse/formatter.xml`. |
| `report-aggregate/` | Aggregated JaCoCo coverage report. |
| `db/liquibase/` | Liquibase changelogs (`vdyp-changelog.json`, `batch-changelog.json`, `postgres-changelog.json`) and scripts. |
| `db/eap/` | Enterprise Architect logical data model. |
| `initdb/` | Local database initialization script. |
| `nats/` | NATS JetStream stream and consumer definitions. |
| `charts/` | Helm charts (`app`, `api-gateway`, `crunchy-postgres`, `liquibase`, `nats`) with per-environment values. |
| `log-exporter/` | Log export container. |
| `tools/` | Python developer tools (for example, comparing VDYP7 and VDYP8 output). See `tools/README.md`. |
| `R/` | R package wrapper (`VDYPinR`). |
| `docs/` | Operational documentation, including `docs/deployment-runbook.md`. |
| `.github/workflows/` | CI/CD: Maven build and Sonar, frontend CI, image builds, OpenShift deploys, release and promotion. |

## Build and test

Requires JDK 17 and Maven. Docker is required for the backend test suite (Testcontainers).

```bash
# Full build with tests (from the repository root)
mvn clean install

# Build a single module and the modules it depends on
mvn -pl lib/vdyp-forward -am install

# Skip tests
mvn install -DskipTests
mvn install -DskipUnitTests          # skip Surefire only
mvn install -DskipIntegrationTests   # skip Failsafe only

# CI equivalent (coverage, verification, formatting check)
mvn -Pcoverage package verify formatter:validate
```

Test conventions:

- Unit tests (Surefire) match `**/Test*.java` and `**/*Test.java`.
- Integration tests (Failsafe) are configured per module: `IT*.java` in `lib/`, specific `IT*` classes in `backend/`. See the subproject `CLAUDE.md` files.
- Test libraries: JUnit 5, Hamcrest, EasyMock, Instancio. Prefer Hamcrest matchers for assertions to match existing tests.

Frontend (run inside `frontend/`):

```bash
npm ci
npm run dev           # local development server
npm run type-check
npm run lint:check    # CI lint; use `npm run lint` to auto-fix
npm run test:unit     # Cypress component runner for services, stores, composables, validation, utils
npm run test:comp     # Cypress component tests
npm run build
```

Local full stack: `docker compose up` starts PostgreSQL, Liquibase migrations, MinIO, COMS, NATS, the backend, the batch service, and the UI. See `docker-compose*.yml` for overrides.

## Code style

- Java, JSON, and XML use **tabs** for indentation (see `.editorconfig`). Line endings are LF, files end with a newline, encoding is UTF-8.
- The Liquibase changelog JSON files are the exception and use two spaces.
- Java formatting is enforced with the Eclipse profile through Maven. Run the formatter after changing Java code and before committing:

  ```bash
  mvn formatter:format     # apply
  mvn formatter:validate   # check only (CI runs this)
  ```

- Do not modify `buildtools/src/main/resources/eclipse/formatter.xml` without team agreement.
- When building historical commits (for example, during `git bisect`), pass `-Dformatter.skip` to avoid unrelated formatting changes.

## Working guidelines for Claude

- Much of `lib/` is a faithful port of VDYP7 numerical code. Preserve computational behaviour exactly, including floating point types (`float` versus `double`), order of operations, and rounding. Do not "simplify" formulas or refactor numeric code unless asked; small changes can alter projection output.
- Comments frequently reference the original VDYP7 routine names. Keep those references when editing.
- Shared dependency versions are managed in the root `pom.xml` `dependencyManagement` section. Add or bump versions there rather than in module POMs, unless the module uses its own BOM (Quarkus in `backend`, Spring Boot in `batch`).
- Database schema changes go through Liquibase changelogs in `db/liquibase/`, never through ad hoc SQL.
- Generated output lives in `target/` directories; do not edit or commit it.
- Commit messages commonly reference Jira tickets in the form `VDYP-1234`. Include the ticket key when one is known.
- Build, release, promotion, and rollback procedures are in `docs/deployment-runbook.md`. Consult it before changing workflows in `.github/workflows/` or Helm charts in `charts/`.

## Subproject documentation

Additional `CLAUDE.md` files are expected at the subproject level (for example `lib/`, `backend/`, `batch/`, `frontend/`). Existing READMEs that provide context:

- `lib/README.md`
- `backend/README.md`
- `frontend/README.md`
- `charts/app/README.md`
- `tools/README.md`
- `log-exporter/README.md`
