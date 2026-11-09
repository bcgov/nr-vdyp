# CLAUDE.md (lib)

Guidance for work in the core computation library. Read the root `CLAUDE.md` first; this file adds library-specific detail.

## Purpose

`lib/` is the Java port of the VDYP7 computation engine. It contains the processing stages that turn forest inventory input into yield projections, plus the site index libraries they depend on. The `backend` and `batch` services consume it through `vdyp-extended-core`.

## Modules and dependencies

| Module | Package | Role |
| --- | --- | --- |
| `vdyp-debug-logging` | `...vdyp.logging` | Request-scoped debug logging support. |
| `vdyp-common` | `...vdyp.application`, `common`, `controlmap`, `io`, `math`, `model`, `processing_state` | Shared foundation: application framework, control file and coefficient parsing, data model, math and estimation routines. |
| `vdyp-sindex` | `...vdyp.sindex` | Site index curves and calculators (port of the SINDEX library; entry point `Sindxdll`). |
| `vdyp-si32` | `...vdyp.si32` | SI32 layer over `vdyp-sindex` (BEC, CFS, site and VDYP helpers; entry point `SI32`). |
| `vdyp-fip` | `...vdyp.fip` | FIPSTART stage. Entry point `FipStart`. |
| `vdyp-vri` | `...vdyp.vri` | VRISTART stage. Entry point `VriStart`. |
| `vdyp-forward` | `...vdyp.forward` | Forward growth projection. Entry point `VdypForwardApplication`. |
| `vdyp-back` | `...vdyp.back` | Back growth projection. Entry point `VdypBackApplication`. |
| `vdyp-extended-core` | `...vdyp.ecore` | Projection orchestration (`ProjectionRunner`, `PolygonProjectionRunner`, `ComponentRunner`), API v1 models, CSV and HCSV input and output. Used by `backend` and `batch`. |
| `vdyp-integration-tests` | `...vdyp.integration_tests` | Shared integration test framework (for example `IntermediateDataBasedIntegrationTest`) and shared test data. |

Typical flow: input is read by FIPSTART or VRISTART, which produce VDYP polygons; Forward (or Back) then projects them. `vdyp-extended-core` sequences these stages for a projection request.

## Application framework

- Each stage is a `VdypApplication` subclass started with `VdypApplication.runApp(...)`. Processing stages extend `VdypProcessingApplication` and supply a `Processor`; start stages extend `VdypStartApplication`.
- Stages are configured by control files (`.CTR`, for example `VDYP.CTR`, `FIPSTART.CTR`, `VRISTART.CTR`). Control files are parsed into a control map (`controlmap` package) that holds coefficient tables and file references.
- File access goes through the `FileResolver` abstraction (`FileSystemFileResolver`, `ZipOutputFileResolver`, `FailoverFileResolver`). Do not open files directly in stage code; use the resolver so tests can substitute `MockFileResolver`.
- Parsers live under `io/parse` (`coe` for coefficient files, `control`, `model`, `streaming`, `value`). Writers live under `io/write`.

## Build and test

Run from the repository root so the parent POM is used.

```bash
mvn -pl lib/vdyp-forward -am install          # one module plus its dependencies
mvn -pl lib/vdyp-forward test -Dtest=SomeTest  # single unit test class
mvn -pl lib -amd install -DskipIntegrationTests
```

- Unit tests: `**/Test*.java`, `**/*Test.java` (Surefire).
- Integration tests: `**/IT*.java` (Failsafe, configured in `lib/pom.xml`). Examples are `ITDataBased`, `ITFipStart` and `ITVriStart` in the FIP, VRI and Forward modules. They are data-driven and slower; skip them with `-DskipIntegrationTests` during iteration but run them before finishing a change to computation code.
- Shared test helpers are in `vdyp-common/src/test/java/ca/bc/gov/nrs/vdyp/test`: `TestUtils`, `ProcessingTestUtils`, `VdypMatchers`, `VdypEasyMock`, `MockFileResolver`, and the `*CoefficientsProvider` classes. Reuse them instead of writing new fixtures.
- Test control files and coefficient data are in `vdyp-common/src/test/resources/ca/bc/gov/nrs/vdyp/test`.
- Assertions use Hamcrest and the custom `VdypMatchers`. Mocks use EasyMock.

## Rules for changes

- Numerical fidelity to VDYP7 is the primary requirement. Preserve `float` versus `double`, the order of operations, intermediate rounding, and clamping exactly as written. A change that looks like a harmless cleanup can change projection output.
- Many methods carry comments naming the VDYP7 routine they port (for example a Fortran subroutine name). Keep these comments and update them if a method is split or moved.
- When behaviour must change, add or update a unit test that pins the expected numeric result, and run the integration tests for the affected stage.
- Prefer adding code to the module that owns the concept. Put logic in `vdyp-common` only when more than one stage needs it.
- Do not edit generated output under `target/` or commit it.
