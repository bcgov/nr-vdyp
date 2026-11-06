# API Design Conventions

This document covers the public backend API (`backend/`) and the internal batch API (`batch/`). It records the conventions already in use and the rules for new endpoints.

## 1. Base paths and versioning

| API | Base path | Audience |
| --- | --- | --- |
| Backend | `/api/v8` | Frontend, API gateway consumers, batch service |
| Batch | `/vdyp-batch/api/batch` (port 8081) | Backend only (internal) |
| Backend health | `/q/health` | Platform probes |
| Backend OpenAPI | `/q/openapi` | Developers (open in dev and test; authenticated elsewhere) |

- The path version (`v8`) matches the VDYP major version, not an API revision count. Java packages for the current API use `v1` (`endpoints/v1`, `responses/v1`, `ecore.model.v1`).
- Breaking changes to `/api/v8` require coordination with the frontend client (`frontend/src/services/vdyp-api`) and with external consumers registered in the API gateway (`charts/api-gateway`).
- Add fields in a backward compatible way. Do not rename or remove fields used by the frontend or by stored JSON parameters without a migration plan.

## 2. Resources

### 2.1 Backend projection resource

All paths are relative to `/api/v8/projection`.

| Method | Path | Roles | Purpose |
| --- | --- | --- | --- |
| POST | `/hcsv` | USER, ADMIN, KONG_API_GATEWAY | Run an HCSV projection synchronously; returns a ZIP. |
| POST | `/scsv`, `/dcsv` | (scsv is public) | Defined; return `501 Not Implemented`. |
| GET | `/me` | USER, ADMIN | List the caller's projections. |
| GET | `/all` | ADMIN | List all projections. |
| GET | `/thread-capacity`, `/storage-status` | ADMIN | Batch capacity and storage information. |
| POST | `/storage-cleanup` | ADMIN | Run batch storage cleanup. |
| POST | `/new` | USER, ADMIN | Create a projection (multipart). |
| GET | `/{projectionGUID}` | USER, ADMIN, SYSTEM | Read a projection. |
| PUT | `/{projectionGUID}/params` | USER, ADMIN | Update parameters (multipart). |
| DELETE | `/{projectionGUID}` | USER, ADMIN | Delete a projection. |
| POST | `/{projectionGUID}/run` | USER, ADMIN | Queue for batch processing. |
| POST | `/{projectionGUID}/cancel` | USER, ADMIN | Cancel a queued or running projection. |
| POST | `/{projectionGUID}/duplicate` | USER, ADMIN | Copy a projection. |
| POST | `/{projectionGUID}/prioritize` | ADMIN | Prioritize a batch job. |
| PATCH | `/{projectionGUID}/progress` | SYSTEM | Batch progress update. |
| POST | `/{projectionGUID}/complete` | SYSTEM | Batch completion. |
| GET | `/{projectionGUID}/resultZip` | USER, ADMIN | Download results. |
| GET | `/{projectionGUID}/fileset/{fileSetGUID}` | USER, ADMIN, SYSTEM | Read a file set. |
| POST | `/{projectionGUID}/fileset/{fileSetGUID}/file` | USER, ADMIN, SYSTEM | Upload a file to a file set. |
| POST | `/{projectionGUID}/fileset/{fileSetGUID}/file/start` | SYSTEM | Start a large file upload. |
| POST | `/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}/complete` | SYSTEM | Complete a large file upload. |
| GET | `/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}` | USER, ADMIN | Read a file. |
| DELETE | `/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}` | USER, ADMIN | Delete a file. |

Other backend resources: `GET /api/v8` (root), `GET /api/v8/help` (parameter help, public), `POST /api/v8/admin/orphan-file-sets` (ADMIN, defaults to `dryRun=true`), and `/auth/...` (Keycloak proxy for the frontend).

### 2.2 Batch resource (internal)

All paths are relative to `/api/batch`: `POST /startWithGUIDs`, `POST /stop/{jobGuid}`, `POST /stop/projection/{projectionGuid}`, `POST /prioritize/{jobGuid}`, `GET /status/{jobGuid}`, `GET /health`, `GET /capacity`, `GET /storage`, and `POST /storage/cleanup`.

The batch API is not exposed through the route or the API gateway. See [security](security.md).

## 3. Rules for new endpoints

### 3.1 Paths and methods

- Resources are nouns; nested resources follow ownership (`/projection/{projectionGUID}/fileset/{fileSetGUID}/file`).
- Path parameters that identify records are UUIDs and are named `<entity>GUID`.
- Use `GET` for reads, `POST` for creation and for actions on a resource (`/run`, `/cancel`, `/duplicate`), `PUT` for full replacement of a sub-resource (`/params`), `PATCH` for partial updates (`/progress`) and `DELETE` for removal.
- Actions are verbs appended to the resource path. Keep them few and specific.

### 3.2 Requests and responses

- JSON for request and response bodies, except file uploads (`multipart/form-data`) and downloads (`application/octet-stream`, ZIP).
- Field names in JSON are camelCase. Projection parameter names are defined by `ParameterNames` and the `Parameters` model in `vdyp-extended-core`; do not duplicate them.
- Return API models built by `*ResourceAssembler` classes. Never return JPA entities.
- Validate inputs at the boundary. Projection parameters are validated by `ProjectionRequestParametersValidator`, which returns validation messages rather than throwing generic errors.

### 3.3 Errors

All errors use the `ApiError` body:

```json
{ "code": "BAD_REQUEST", "message": "Human readable detail" }
```

| Situation | Status | Mapper |
| --- | --- | --- |
| Invalid parameters | 400 `BAD_REQUEST` | `ProjectionValidationExceptionMapper` |
| Action not allowed in the current status | 400 `BAD_REQUEST` | `ProjectionStateExceptionMapper` |
| File upload problem | Status and code carried by the exception | `ProjectionFileUploadExceptionMapper` |
| Not authenticated | 401 | Quarkus security |
| Missing role | 403 | Quarkus security (`@RolesAllowed`) |
| Not the owner of the projection | 401 `UNAUTHORIZED` | `ProjectionUnauthorizedExceptionMapper` |
| Unknown projection or resource | 404 `NOT_FOUND` | `ProjectionNotFoundExceptionMapper`, `NotFoundExceptionMapper` |
| Orphan cleanup conflict | 409, code from the exception | `OrphanFileCleanupExceptionMapper` |
| Storage cleanup failed in the batch service | 502 `STORAGE_CLEANUP_FAILED` | `StorageCleanupExceptionMapper` |
| Not implemented | 501 | Endpoint |
| Unexpected error | 500 `INTERNAL_ERROR` | `UnhandledExceptionMapper` |

Note: ownership failures currently return 401 rather than 403. New code should follow the existing mapper; changing it is an API change.

- `code` is stable and machine readable. `message` is for humans and must not contain stack traces, SQL or secrets.
- Each new exception type gets its own mapper extending `AbstractApiExceptionMapper`.

### 3.4 Documentation

- Annotate every endpoint with MicroProfile OpenAPI annotations: `@Operation` (with `operationId`, `summary`, `description`), `@RequestBody`, and `@APIResponse` for each status code returned.
- `operationId` values are camelCase verbs (`runHcsvProjection`). The frontend client depends on them.
- Keep `backend/src/main/resources/openapi/*.swagger.yaml` consistent with the annotations.

### 3.5 Security

- Every endpoint declares `@RolesAllowed`. An endpoint without it is still authenticated by the global policy, but the intended roles must be explicit.
- Public endpoints must be listed in `quarkus.http.auth.permission.permitted.paths` and justified in review.
- Endpoints called by the batch service use the `SYSTEM` role only.
- Ownership checks (`ProjectionService.checkUserCanPerformAction`) apply to every operation on a projection, in addition to role checks.

## 4. Messaging contracts

| Subject | Type | Producer | Consumer | Payload |
| --- | --- | --- | --- | --- |
| `vdyp.batch.request.standard` | JetStream (stream `VDYP_BATCH_REQUESTS`, work queue, 7 day max age) | Backend `BatchJobPublisher` | Batch, durable consumer `VDYP_BATCH_WORKER`, explicit ack, `max_ack_pending` 1 | `BatchRequestMessage` |
| `vdyp.batch.stop.request` | Core NATS request-reply broadcast | Batch replica that received the HTTP call (`StopRemoteGateway`) | All batch replicas; the owner replies | Stop request |
| `vdyp.batch.prioritize.request` | Core NATS request-reply broadcast | Batch replica that received the HTTP call (`PrioritizeRemoteGateway`) | All batch replicas; the owner replies | Prioritize request |

- Message classes are shared by name and shape between backend and batch. Change both sides in the same pull request.
- Stream and consumer definitions live in `nats/` and are applied by `nats/apply-jetstream.sh` (which skips existing definitions; changing an existing stream requires a manual step).
- Messages must be idempotent: a request may be delivered more than once.
