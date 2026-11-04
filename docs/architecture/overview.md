# VDYP Architecture Overview

This document describes the structure of the VDYP application as implemented in this repository. It was produced by scanning the source code, configuration, Helm charts and workflows. Where behaviour was inferred rather than read directly from code, the text says so.

Related documents:

- [Data model](data-model.md)
- [Glossary](../domain/glossary.md)
- [Conventions](../conventions/)
- [Deployment runbook](../deployment-runbook.md)

## 1. Purpose

VDYP (Variable Density Yield Projection) produces yield predictions for unmanaged British Columbia forest stands for inventory and timber supply applications. Version 8 (this repository) is a Java port of the VDYP7 computation engine, wrapped in a web application that lets users upload stand inventory files, configure projections, run them at scale, and download the results.

## 2. System context

```mermaid
C4Context
    title VDYP System Context

    Person(user, "VDYP User", "Forest analyst authenticated through IDIR, BCeID or BC Services Card")
    Person(admin, "Administrator", "Manages projections, capacity and storage")
    System_Ext(consumer, "External API Consumer", "System that calls the VDYP API with an API key")

    System(vdyp, "VDYP", "Runs stand yield projections and stores their inputs and results")

    System_Ext(sso, "BC Gov SSO (Keycloak)", "loginproxy.gov.bc.ca, realm 'standard'")
    System_Ext(gateway, "BC Gov API Gateway (Kong)", "vdyp.api.gov.bc.ca, API key authentication")
    System_Ext(s3, "BC Gov Object Storage (S3)", "Stores input and result files")

    Rel(user, vdyp, "Uses", "HTTPS, browser")
    Rel(admin, vdyp, "Administers", "HTTPS, browser")
    Rel(consumer, gateway, "Calls", "HTTPS, X-API-KEY")
    Rel(gateway, vdyp, "Forwards requests", "HTTPS, GW-JWT header")
    Rel(vdyp, sso, "Authenticates users and services", "OIDC")
    Rel(vdyp, s3, "Reads and writes files", "S3 API")
```

## 3. Containers

```mermaid
C4Container
    title VDYP Containers

    Person(user, "VDYP User / Administrator")
    System_Ext(gateway, "API Gateway (Kong)")
    System_Ext(sso, "BC Gov SSO (Keycloak)")

    System_Boundary(vdyp, "VDYP (OpenShift namespace per environment)") {
        Container(ui, "Frontend", "Vue 3, Vuetify, Caddy", "Single page application")
        Container(api, "Backend API", "Quarkus, native image", "Projections, file sets, HCSV runs")
        Container(batch, "Batch Service", "Spring Boot, Spring Batch, native image", "Large projections; several replicas")
        ContainerQueue(nats, "NATS JetStream", "NATS", "Batch request queue, control subjects")
        ContainerDb(db, "PostgreSQL", "Crunchy Postgres", "app-vdyp, batch and COMS databases")
        Container(coms, "COMS", "BC Gov COMS", "Object metadata, presigned URLs")
    }

    System_Ext(s3, "Object Storage (S3)")

    Rel(user, ui, "Uses")
    Rel(ui, sso, "Signs in")
    Rel(ui, api, "REST, bearer token")
    Rel(gateway, api, "REST, GW-JWT")
    Rel(api, nats, "Publishes requests")
    Rel(batch, nats, "Consumes requests")
    BiRel(api, batch, "Control; progress and results")
    Rel(api, db, "JDBC")
    Rel(batch, db, "JDBC")
    Rel(api, coms, "Files")
    Rel(batch, coms, "Presigned files")
    Rel(coms, s3, "Objects")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

Supporting containers not shown above: Liquibase jobs (apply schema changes to PostgreSQL), the Log Exporter (exports access logs from a PVC to object storage), and direct S3 access from the backend for file reads and writes.

| Container | Source | Technology | Notes |
| --- | --- | --- | --- |
| Frontend | `frontend/` | Vue 3, Vuetify 3, Pinia, Vite, Caddy | Runtime configuration injected as `window.config` by Caddy. |
| Backend API | `backend/` | Quarkus, Hibernate ORM with Panache, OIDC | Public API under `/api/v8`. Runs small projections in process. |
| Batch Service | `batch/` | Spring Boot, Spring Batch, jnats | Port 8081, context path `/vdyp-batch`, API under `/api/batch`. |
| Computation library | `lib/` | Plain Java 17 | Embedded in both backend and batch through `vdyp-extended-core`. |
| NATS JetStream | `nats/`, `charts/nats` | NATS 2.x | Stream `VDYP_BATCH_REQUESTS`, durable consumer `VDYP_BATCH_WORKER`. |
| PostgreSQL | `charts/crunchy-postgres`, `db/` | PostgreSQL 17 locally, Crunchy in OpenShift | Schemas managed by Liquibase. |
| COMS | `charts/app/templates/coms` | BC Gov COMS image | Deployed alongside the application. |
| Log Exporter | `log-exporter/` | Shell script, container | See `log-exporter/README.md`. |

## 4. Computation library

```mermaid
flowchart LR
    common[vdyp-common<br/>model, control files, parsers, math]
    sindex[vdyp-sindex<br/>site index curves]
    si32[vdyp-si32<br/>SI32 layer]
    fip[vdyp-fip<br/>FIPSTART]
    vri[vdyp-vri<br/>VRISTART]
    fwd[vdyp-forward<br/>Forward projection]
    back[vdyp-back<br/>Back projection]
    ecore[vdyp-extended-core<br/>projection orchestration, API v1 model, CSV IO]
    dbg[vdyp-debug-logging]

    sindex --> si32
    common --> fip
    common --> vri
    common --> fwd
    common --> back
    si32 --> ecore
    fip --> ecore
    vri --> ecore
    fwd --> ecore
    back --> ecore
    dbg --> ecore
    ecore --> backend[backend]
    ecore --> batchsvc[batch]
```

The arrows show the main dependency direction. Exact Maven dependencies are defined in each module `pom.xml`.

A projection runs in stages (`ProjectionStageCode`):

1. **Initial**: FIPSTART or VRISTART converts inventory input into VDYP polygons.
2. **Adjust**: adjustment of the initial values (defined in the enum; see the code for current use).
3. **Forward** or **Back**: grows the stand forwards or backwards in time.

`vdyp-extended-core` (`ProjectionRunner`, `PolygonProjectionRunner`, `ComponentRunner`) reads HCSV input, runs the stages for each polygon in an execution folder, and writes the requested outputs (yield tables, text reports, logs).

## 5. Key flows

### 5.1 Interactive HCSV projection

Used by external API consumers and for small runs. The request is processed synchronously in the backend and nothing is persisted.

```mermaid
sequenceDiagram
    actor C as Client
    participant GW as API Gateway
    participant API as Backend
    participant EC as vdyp-extended-core

    C->>GW: POST /api/v8/projection/hcsv (polygon file, layer file, parameters)
    GW->>API: Forward with GW-JWT
    API->>API: Authenticate (KONG_API_GATEWAY or USER/ADMIN)
    API->>EC: Run projection in execution folder
    EC-->>API: Output files
    API-->>C: ZIP archive (application/octet-stream)
```

### 5.2 Persistent batch projection

Used by the web application for projections that are saved, run asynchronously and downloaded later.

```mermaid
sequenceDiagram
    actor U as User
    participant UI as Frontend
    participant API as Backend
    participant S3 as COMS / S3
    participant N as NATS JetStream
    participant B as Batch replica
    participant DB as PostgreSQL

    U->>UI: Create projection, upload files, set parameters
    UI->>API: POST /api/v8/projection/new
    API->>DB: Insert projection (DRAFT) and file sets
    UI->>API: POST .../fileset/{fileSetGUID}/file (upload)
    API->>S3: Store object, record file_mapping
    U->>UI: Run
    UI->>API: POST /api/v8/projection/{projectionGUID}/run
    API->>DB: Status QUEUED, projection_batch_mapping
    API->>N: Publish BatchRequestMessage (vdyp.batch.request.standard)
    N->>B: Deliver to VDYP_BATCH_WORKER consumer
    B->>DB: Acquire batch_job_claim lease
    B->>API: GET projection details and file list
    B->>S3: Download inputs (presigned URLs)
    B->>B: Partition input, run Spring Batch job using vdyp-extended-core
    loop During processing
        B->>API: PATCH .../progress
        API->>DB: Status RUNNING, update counts
    end
    B->>API: Upload results to RESULT file set
    B->>API: POST .../complete (success or failure)
    API->>DB: Status READY or FAILED
    U->>UI: Download
    UI->>API: GET .../resultZip
```

### 5.3 Control operations

- **Stop or cancel**: the backend calls the batch service, and stop requests are broadcast on `vdyp.batch.stop.request` so that the replica that owns the job can act. A user cancel returns the projection to `DRAFT`; an administrator cancel sets `ADMN_CNCLD`.
- **Prioritize**: administrators can prioritize a job; the request is broadcast on `vdyp.batch.prioritize.request`.
- **Stuck detection**: the backend `ProjectionStuckDetectionJob` marks `RUNNING` projections without recent progress as `STUCK`, and returns them to `RUNNING` when progress resumes.
- **Recovery**: each batch replica registers in `batch_worker_registry` and holds a lease in `batch_job_claim` for each job. If a replica stops sending heartbeats, another replica can recover the job after the lease expires.
- **Cleanup**: `ProjectionCleanupJob` removes expired projections. Administrators can run storage cleanup (batch PVC folders) and orphan file set cleanup (S3 objects without database references, see `backend/ORPHAN-FILE-CLEANUP.md`).

## 6. Projection lifecycle

```mermaid
stateDiagram-v2
    [*] --> DRAFT: create
    DRAFT --> QUEUED: run
    QUEUED --> RUNNING: first progress update
    QUEUED --> DRAFT: user cancel
    RUNNING --> READY: complete (success)
    RUNNING --> FAILED: complete (failure)
    RUNNING --> STUCK: no progress detected
    STUCK --> RUNNING: progress resumes
    RUNNING --> DRAFT: user cancel
    RUNNING --> ADMN_CNCLD: administrator cancel
    QUEUED --> ADMN_CNCLD: administrator cancel
    FAILED --> DRAFT: edit parameters
    READY --> [*]: delete
```

Transitions are taken from `ProjectionService` and `ProjectionBatchMappingService` in the backend. The permitted actions for each status are defined in `ProjectionService.checkUserCanPerformAction`; consult it before relying on this diagram for edge cases.

## 7. Deployment

```mermaid
flowchart TB
    gh[GitHub Actions] -->|build images| ghcr[(GHCR)]
    gh -->|helm upgrade| ocp
    subgraph ocp[OpenShift namespace c50504-env]
        route[Routes] --> fe[frontend]
        route --> be[backend]
        fe --> be
        be --> pg[(Crunchy Postgres)]
        be --> nats[(NATS)]
        be --> coms[COMS]
        bt[batch replicas] --> nats
        bt --> pg
        bt --> be
        bt --> pvc[(batch PVC)]
        coms --> pg
        lq[Liquibase jobs] --> pg
        lx[log-exporter] --> logs[(logs PVC)]
    end
    kong[API Gateway] --> be
    coms --> s3[(Object Storage)]
    lx --> s3
```

- Environments are `dev`, `test` and `prod`, each with its own `values-<env>.yaml` in `charts/app`.
- Helm charts: `app` (frontend, backend, batch, COMS, network policies, secrets), `nats`, `crunchy-postgres`, `liquibase`, and `api-gateway` (Kong configuration).
- Network policies (`charts/app/templates/knp.yaml`) allow router ingress to the frontend, backend, COMS and batch, allow frontend to backend and backend to batch traffic in both directions, and restrict database access to named components.
- Images are built and promoted as described in [the deployment runbook](../deployment-runbook.md). Liquibase changes are deployed separately with `.github/workflows/liquibase-ddl.yml`.
- Local development uses `docker-compose.yml`, which runs PostgreSQL, the three Liquibase migrations, MinIO, COMS, NATS, the backend, the batch service and the frontend.

## 8. Cross-cutting concerns

| Concern | Approach |
| --- | --- |
| Authentication | Keycloak OIDC for users (frontend and backend); API gateway JWT (`GW-JWT`) for external consumers; OAuth2 client credentials for batch to backend calls. See [security conventions](../conventions/security.md). |
| Authorization | Role based with `@RolesAllowed`: `USER`, `ADMIN`, `SYSTEM`, `KONG_API_GATEWAY`. |
| Configuration | Quarkus profiles (`%dev`, `%test`, `%prod`) and Spring properties, both with environment variable overrides. Frontend runtime configuration through `window.config`. |
| Persistence | PostgreSQL. Schemas owned by Liquibase; Hibernate validates the schema at startup. |
| Messaging | NATS JetStream work queue for batch requests; core NATS subjects for stop and prioritize broadcasts. |
| File storage | COMS and S3 for inputs and results; PVC for batch working folders. |
| Observability | Logback logging; Quarkus health at `/q/health`; Spring Actuator with Prometheus metrics in batch; access logs exported by `log-exporter`. |
| Native images | Backend and batch are built as GraalVM native images. Reflection registration (`@RegisterForReflection`, `VdypBackendReflectionConfiguration`) is required for classes serialized by Jackson. |
| Quality gates | `mvn -Pcoverage package verify formatter:validate` and SonarCloud on every pull request; frontend lint, type check and Cypress tests; OWASP ZAP scan workflow. |

## 9. Observations and open items

These points were noted during the scan and may be worth recording as ADRs or backlog items.

- The batch service permits all incoming HTTP requests (`IncomingSecurityConfiguration`) and relies on network isolation. However, `batch.route.enabled` is `true` in the dev, test and prod Helm values, so the batch API may be exposed through an OpenShift Route. See [security conventions](../conventions/security.md#3-network). This should be confirmed and resolved, then recorded as an ADR.
- `batch/src/main/resources/application.properties` contains a default value for `KEYCLOAK_CLIENT_SECRET`. Defaults for secrets should be removed and the value rotated if it is a real credential.
- `backend/src/main/resources/db/migration/V1.0.0__init.sql` defines a `java_api` schema outside the Liquibase process and appears to be legacy.
- The `projection_status_code` table comment lists values (Started, Completed, Canceled) that no longer match the codes in use.
- `backend/README.md` describes the original Jetty server stub, not the current Quarkus service.
- The `/dcsv` and `/scsv` endpoints are defined but return `501 Not Implemented`.

## 10. Candidate ADRs

The repository has no ADRs yet. The following decisions are already embodied in the code and are good candidates to record first:

1. Port VDYP7 to Java with exact numerical fidelity.
2. Quarkus for the API and Spring Boot for the batch service.
3. NATS JetStream as the batch work queue.
4. Lease based job ownership for batch replicas.
5. COMS and S3 for file storage.
6. Liquibase as the only schema management mechanism.
7. GraalVM native images for backend and batch.
8. Network isolation instead of authentication for the batch API.
