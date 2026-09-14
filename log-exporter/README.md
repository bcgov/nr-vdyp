# Backend access logs and daily export

This document covers access-log collection, PVC storage, scheduled S3 exports,
and the Maintaining Vendor's operational and security review responsibilities.

## Responsibility, security review and manual retention

The Maintaining Vendor is responsible for reviewing this logging and export
process as part of the security review process at least monthly. The review
must cover log coverage and content, failed or missed exports, successful archive
retrieval, retained source files, duplicate captures, storage capacity and which
files still need to be retained.

During each review, the Maintaining Vendor may manually delete S3 archives and
remaining source files that no longer need to be retained, i.e. older than 2 year 
plus the current. 

Archive retention is currently manual; the exporter does not automatically expire
S3 objects. The Maintaining Vendor will review this manual policy after at least
one month of data has been collected, using observed volume, duplication,
storage growth and review effort to decide whether to revise or automate it.
There is no automatic two-years-plus-current-year deletion rule configured.

This manual retention review is separate from the exporter's existing automatic
cleanup of unchanged source files after verified upload, described below.

## What is logged

The dev, test and prod chart values enable Quarkus HTTP access logs. Completed
HTTP requests are recorded, including authentication/authorization rejections
(401/403) and health probes. Each record contains the remote address,
authenticated principal (or `-`), timestamp and timezone, method, URL path,
protocol, response status, response size and the `X-Forwarded-For` in lieu of ip.

The full forwarded chain is recorded as received; a missing header is `-`.
Treat it as proxy-supplied evidence, not a verified client identity. Logging does
not change which proxy headers Quarkus trusts. Query strings, bodies, cookies
and authorization headers are excluded.

```text
10.0.0.10 - [14/Sep/2026:12:00:00 +0000] "GET /api/v8/projection HTTP_1_1" 401 - ip="203.0.113.10, 10.0.0.5"
```

## Storage and rotation

The backend mounts `<release-name>-logs` at `/work/logs`. Top-level `logs`
settings manage this ReadWriteMany PVC independently of the backend; size and
storage class are configured through `logs.size` and `logs.storageClassName`
(default `netapp-file-standard`). Storage must support concurrent mounts across
nodes and advisory file locks. OpenShift supplies pod UID/group permissions.

Each backend pod writes `access-security-<pod-uid>.log`. Separate names prevent
replicas from sharing an active file, and files remain on the PVC after pod
replacement. Other file loggers may use `/work/logs` with distinct names; the
access-log exporter only collects `access-security*.log` files.

Quarkus rotates daily using `logs.export.timeZone`, set to UTC in dev, test
and prod. The chart supplies this as the backend native runtime's `user.timezone`;
it affects the whole backend. Existing files retain their original date semantics
until new pods use the updated configuration. Rotation is lazy: a later request
triggers rotation, and the next export collects any late rotations.

Do not disable `logs.enabled` or uninstall the release before preserving required
logs: Helm can delete the PVC. If upgrading from the earlier
`<backend-fullname>-access-logs` PVC, export or migrate its contents first; Helm
does not rename or migrate stored files.

Local development leaves access logging disabled unless
`VDYP_ACCESS_LOG_ENABLED=true`. Create the configured log directory first
(default `logs`).

## Scheduled export and source cleanup

The chart creates `<release>-access-log-export`, a CronJob running at 06:00 UTC
each day, unaffected by daylight-saving time. Its Bash script uses ZIP and AWS
CLI, mounts the logs PVC read/write, and builds the archive in temporary storage.
It selects:

- All dated rotations named `access-security*YYYY-MM-DD.log` or
  `access-security*YYYY-MM-DD.N.log`, regardless of date.
- All undated `access-security*.log` files last modified strictly before today's
  midnight UTC. Files modified at or after midnight remain.

Selection uses filenames and filesystem metadata; it never parses record dates.
Selected files are copied byte-for-byte. The ZIP
includes `manifest.tsv` with each file's byte count and SHA-256 hash. Work scales
with the selected files, although their directory must still be enumerated.

No pod lookup, dedicated service account or pod-list permission is needed; token
mounting is disabled. Modification time does not prove a file is closed. A quiet
pod may still have an old file open; deleting it can cause later writes to go to
an unlinked file until the writer reopens its log. The Maintaining Vendor should
consider this behavior when reviewing log coverage and source cleanup.  However this is 
unlikely given the time which the logs are archived vs the time they would have rotated
and the policies regarding scale up/down.

Uploads use HTTPS, path-style S3 requests and Content-MD5 for server-side payload
validation, followed by verification of the remote size and SHA-256 metadata.
Only then does cleanup delete eligible source files whose identity, size,
modification/change timestamps and checksum remain unchanged. Upload or
verification failure prevents deletion. Set `logs.export.deleteUploadedLogs:
false` to retain all source files for manual handling.

Default destination:

```text
s3://<bucket>/access-logs/<namespace>/access-logs_YYYY-MM-DD-<zip-sha256>.zip
```

The date labels the export batch (yesterday by default), not the dates of every
log included. Different ZIP hashes preserve distinct captures. Repeated captures
can overlap; account for duplicates during review and downstream processing.

## Configuration and failure handling

`logs.export` in each app values file controls schedule, timezone, resources,
scratch space and S3 destination. Bucket, endpoint and credentials default to the
COMS object-storage settings supplied by the deployment workflow. Credentials
are held in a Kubernetes Secret. For separate credentials, set
`logs.export.s3.existingSecret` to a Secret containing `AWS_ACCESS_KEY_ID` and
`AWS_SECRET_ACCESS_KEY`, optionally `AWS_SESSION_TOKEN`. The `bucket`, `endpoint`,
`region` and `prefix` settings override the destination. Export credentials need
PutObject and GetObject (for HEAD verification) on the export prefix; manual
archive deletion requires appropriate separate storage permissions.

Deploy an image tag containing `log-exporter`, which is built and promoted with
the application images. The job installs no packages at runtime and requires the
existing PVC to be present and bound.

Scheduled jobs do not overlap; a PVC lock also serializes manual runs. S3 requests
retry, and Kubernetes retries failed pods up to three times within a two-hour
Job deadline. No eligible files means no upload or deletion. Missing or changed
selected files detected during collection fail the run. Archives exceeding the
5 GiB single PUT limit fail without deleting source logs.

The Maintaining Vendor should investigate failed exports and storage pressure
when detected, as well as during monthly security reviews. Missed schedules
beyond the one-hour starting deadline are not replayed automatically, but the
next successful run collects all eligible files still on the PVC.

## Manual runs and recovery

Create a one-off Job from the deployed CronJob, using a unique name for each run:

```sh
oc create job -n <namespace> --from=cronjob/nr-vdyp-dev-access-log-export access-export-manual-001
oc logs -n <namespace> -f job/access-export-manual-001
```

Manual runs use the same selection and cleanup rules. For a test without source
deletion, generate a manifest with `oc create job --from=cronjob/...
--dry-run=client -o yaml`, set its `DELETE_UPLOADED_LOGS` environment value to
`"false"`, then apply it. `EXPORT_DATE=YYYY-MM-DD` changes only the batch label,
not selection or the current-day midnight cutoff.

For manual recovery through a running backend pod:

```sh
oc exec -n <namespace> <backend-pod> -- ls -R /work/logs
oc cp -n <namespace> <backend-pod>:/work/logs ./access-logs
```

`oc cp` requires `tar` in the container. Prefer completed files for a stable copy;
a growing file must be collected again. Preserve namespace, pod UID and filename
when uploading through the approved OCIO Object Management process. Verify the
stored copy before removing source files that are still required.

## Deployment verification

The Maintaining Vendor should verify an allowed request, an unauthenticated
protected request (401), and a valid identity lacking the required role (403).
Check status, principal where available and the expected forwarded chain in the
PVC files, with no bearer tokens or query strings. Ingress may append or replace
a supplied `X-Forwarded-For` value. Replace a pod and confirm its files remain
accessible, then verify a manual export and retrieve its ZIP and manifest.
