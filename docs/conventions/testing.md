# Testing Conventions

## 1. Overview

| Area | Framework | Unit tests | Integration tests | Run |
| --- | --- | --- | --- | --- |
| `lib/` | JUnit 5, Hamcrest, EasyMock | `**/Test*.java`, `**/*Test.java` | `**/IT*.java` (Failsafe) | `mvn -pl lib/<module> -am verify` |
| `backend/` | JUnit 5, Quarkus Test (Dev Services), Mockito | `**/Test*.java`, `**/Scenario*.java`, `**/*Test.java` | `ITDataDriven`, `ITProjectionDebugLogging` | `mvn -pl backend verify` (Docker required) |
| `batch/` | JUnit 5, Spring Boot Test, Mockito, Testcontainers | `**/*Test.java`, `**/*Tests.java` | Scenario tests in `scenarios/` | `mvn -pl batch test` |
| `frontend/` | Cypress | `*.cy.ts` next to source | Cypress e2e in `cypress/e2e` | `npm run test:unit`, `npm run test:comp` |
| Whole repository | | | | `mvn -Pcoverage package verify formatter:validate` (CI) |

Skip switches: `-DskipTests`, `-DskipUnitTests`, `-DskipIntegrationTests`.

Coverage is collected with JaCoCo (`-Pcoverage`) and aggregated by `report-aggregate/`, then analysed by SonarCloud in `.github/workflows/maven.yml`.

## 2. Java tests

### 2.1 General rules

- Use JUnit 5 (`junit-jupiter`). JUnit 4 is available only for legacy cases; do not use it in new tests.
- Name test classes `<ClassUnderTest>Test` and place them in the same package as the class under test.
- Use Hamcrest matchers (`assertThat(actual, is(expected))`) for assertions. This is the dominant style in `lib/`.
- Use EasyMock in `lib/` for mocks (`VdypEasyMock` adds helpers). The backend and batch services use Mockito, with Quarkus `@InjectMock` or Spring mock beans where a container managed bean must be replaced.
- Use parameterized tests (`junit-jupiter-params`) for tables of inputs and expected values.
- Do not depend on test execution order or on the local machine (time zone, locale, absolute paths).

### 2.2 Computation library

- Reuse helpers in `lib/vdyp-common/src/test/java/ca/bc/gov/nrs/vdyp/test`: `TestUtils`, `ProcessingTestUtils`, `VdypMatchers`, `VdypEasyMock`, `MockFileResolver`, `FloatCoefficientsProvider`, `DoubleCoefficientsProvider`.
- Use `VdypMatchers` for floating point comparisons (closeness with tolerance) rather than exact equality.
- Control files and coefficient data for tests are in `lib/vdyp-common/src/test/resources/ca/bc/gov/nrs/vdyp/test`.
- Data-driven integration tests (`ITDataBased` in FIP, VRI and Forward) compare output against reference data from VDYP7. A change in their output is a change in model behaviour and must be justified.
- `vdyp-test-oracle` runs comparisons against the legacy VDYP7 implementation for wider regression checks.

### 2.3 Backend

- Tests are `@QuarkusTest` classes and use Quarkus Dev Services to start PostgreSQL in a container. Docker must be running.
- The `%test` profile drops and recreates the schema, seeds data from `devdb-init.sql`, disables API gateway JWT verification, and points S3 at a local endpoint.
- Test data for projections (HCSV samples, expected outputs) is in `backend/src/test/resources/test-data`.
- `Scenario*` tests (`endpoints/projection/hcsv/scenarios`) run complete HCSV projections through the endpoint for named scenarios. `src/test/resources/vdyp7-scenario-runner` contains a Python runner used to produce comparable VDYP7 output.

### 2.4 Batch service

- Tests mirror the main package structure. End-to-end style tests are in `scenarios/`.
- Test settings are in `src/test/resources/application-test.properties`; test data in `src/test/resources/test-data`.
- Changes to ownership, leases, shutdown, retry or NATS handling require tests that cover failure and recovery paths, not only the success path.

## 3. Frontend

- Specs are colocated with source files as `*.cy.ts`.
- `npm run test:unit` runs Cypress component specs for `services`, `stores`, `composables`, `validation` and `utils`.
- `npm run test:comp` runs component specs in `src/components`.
- `npm run test:e2e-open` and `npm run test:comp-open` open the Cypress runner interactively.
- Run a single spec with `npx cypress run --component --spec "src/utils/util.cy.ts"`.
- Existing specs stub the service and API layer (for example `cy.stub(apiClient, 'getUserProjections')`) rather than using `cy.intercept`. Follow that pattern, and do not call real backends from component tests.

## 4. Definition of done for tests

A change is ready for review when:

1. New or changed behaviour has tests at the lowest practical level.
2. `mvn -Pcoverage package verify formatter:validate` passes for the affected Java modules.
3. `npm run lint:check`, `npm run type-check`, `npm run test:unit` and `npm run test:comp` pass for frontend changes.
4. Integration tests for an affected projection stage pass when computation code changed.
5. No test is disabled without a linked ticket (`VDYP-nnnn`) explaining why.
