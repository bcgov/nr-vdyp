# Java Conventions

These conventions describe how Java code is written in this repository today. They apply to `lib/`, `backend/`, `batch/`, `buildtools/` and `vdyp-test-oracle/`. Where a module differs, the module is named.

## 1. Language and build

- Java 17 language level (`maven.compiler.release` 17). The backend and batch container builds use GraalVM 21 to produce native images.
- Maven multi-module build from the root `pom.xml`. Shared dependency versions are declared in the root `dependencyManagement` section. The backend imports the Quarkus BOM and the batch service imports the Spring Boot BOM; versions of framework dependencies come from those BOMs.
- Group ID `ca.bc.gov.nrs.vdyp`. Packages start with `ca.bc.gov.nrs.vdyp` followed by the module area (for example `...vdyp.forward`, `...vdyp.backend.services`, `...vdyp.batch.service`).

## 2. Formatting

- Indentation uses tabs (see `.editorconfig`). LF line endings, UTF-8, final newline.
- Formatting is enforced by the Eclipse formatter profile at `buildtools/src/main/resources/eclipse/formatter.xml` through `formatter-maven-plugin`.
  - Apply: `mvn formatter:format`
  - Check (run in CI): `mvn formatter:validate`
- Changes to the formatter profile require agreement from the team.
- A trailing `//` is used in some fluent chains (for example Spring Security configuration and enum lists) to stop the formatter joining lines. Keep these where they exist.

## 3. Naming

| Element | Convention | Examples |
| --- | --- | --- |
| JPA entities | `<Name>Entity` | `ProjectionEntity`, `VDYPUserEntity` |
| API and service models | `<Name>Model` | `ProjectionModel`, `ProjectionStatusCodeModel` |
| Entity to model mappers | `<Name>ResourceAssembler` | `ProjectionResourceAssembler` |
| Repositories | `<Name>Repository` | `ProjectionRepository`, `JobOwnershipRepository` |
| Services | `<Name>Service` | `ProjectionService`, `BatchJobLaunchService` |
| JAX-RS resources (backend) | `<Name>Endpoint` | `ProjectionEndpoint` |
| Spring controllers (batch) | `<Name>Controller` | `BatchController` |
| Exception mappers | `<Exception>Mapper` | `ProjectionNotFoundExceptionMapper` |
| Configuration mappings | `<Name>Config` (backend), `<Name>Properties` (batch) | `ProjectionLimitsConfig`, `BatchProperties` |
| UUID identifiers | Suffix `GUID` in Java, `_guid` in SQL | `projectionGUID`, `projection_guid` |
| Code values | Upper case string constants on the model | `ProjectionStatusCodeModel.READY` |
| Unit tests | `<ClassUnderTest>Test` | `ZipOutputFileResolverTest` |
| Integration tests | `IT<Name>` | `ITDataBased`, `ITDataDriven` |

## 4. Computation library (`lib/`)

The library is a port of VDYP7 and has stricter rules than the rest of the code base.

- Numerical behaviour must match VDYP7. Keep `float` where the original uses single precision, preserve the order of operations, intermediate rounding and clamping, and do not replace formulas with algebraically equivalent forms.
- Comments link Java fields and methods to their VDYP7 origin, for example `// FIP_P/POLYDESC` (common block and variable) or a routine name. Keep these comments when moving or renaming code.
- Optional values in the model use `Optional<T>` rather than `null` (for example `Optional<Float> siteIndex`).
- Lookup tables use the `MatrixMap` family (`MatrixMap2`, `MatrixMap3`, `MatrixMap4`) keyed by codes such as genus and region.
- Input and output go through `FileResolver`. Stage code does not open files directly.
- Applications extend `VdypApplication`, `VdypProcessingApplication` or `VdypStartApplication` and are started with `VdypApplication.runApp(...)`.

## 5. Services (`backend/`, `batch/`)

- Constructor injection is preferred. Fields injected by the container are `final`.
- Keep endpoints and controllers thin: validate input, call a service, return a response. Business rules live in services.
- The backend maps entities to API models with `*ResourceAssembler` classes; entities are never returned from endpoints.
- Persistence in the backend uses Hibernate ORM with Panache repositories (`PanacheRepositoryBase`). Transactions are declared with `@Transactional` on service methods.
- Java `record` types are used for immutable data carriers (messages, DTOs, error bodies).
- Classes serialized by Jackson in the backend must be registered for reflection (`@RegisterForReflection` or `VdypBackendReflectionConfiguration`) or they fail in the native image.

## 6. Errors

- Domain errors are specific exception types (`ProjectionNotFoundException`, `ProjectionStateException`, `BatchPartitionException`, and so on).
- In the backend, each exception type has an exception mapper extending `AbstractApiExceptionMapper`, which returns the standard `ApiError` body (`code`, `message`). See [API design](api-design.md).
- Do not catch and ignore exceptions. Either handle them, wrap them with context, or let them propagate to a mapper.

## 7. Logging

- Use SLF4J: `private static final Logger logger = LoggerFactory.getLogger(MyClass.class);`
- Logback is the backend for all modules (through `quarkus-logging-logback` in the backend).
- Use parameterized messages (`logger.debug("Adding role for {}", idp)`), not string concatenation.
- Do not log tokens, secrets, or full file contents. User identifiers may be logged at debug level.
- Projection progress, error and debug logs requested by users are separate outputs produced by `vdyp-extended-core`, not application logs.

## 8. Configuration

- Backend: `application.properties` with Quarkus profiles (`%dev.`, `%test.`, `%prod.`). Use typed configuration mappings in `backend/.../config`.
- Batch: `application.properties` with `${ENV_VAR:default}` placeholders. Bind groups of settings to `@ConfigurationProperties` classes.
- Every environment-specific value must be overridable by an environment variable. Secrets must not have real default values.

## 9. Dependencies

- Add new dependency versions to the root `dependencyManagement` unless the dependency is managed by the Quarkus or Spring Boot BOM.
- Prefer libraries already in use: Apache Commons (Lang, IO, Math, CLI), OpenCSV, Jackson.
- Lombok and MapStruct are not used. Do not introduce them without team agreement.
