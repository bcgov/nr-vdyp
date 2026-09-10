# S3 orphan cleanup after database cutover

After a db restore and cutover some files may be stored in BCGov Storage that have no 
reference in the system. These orphaned files will need an automated claenup system.

1. Stop projection creation, uploads, batch workers, COMS synchronization and other
   storage writers in the old and new environments throughout preview and deletion.
   A folder can exist before its database transaction commits.
2. Verify the restored database and intended S3 endpoint/bucket. Do not run against
   environments sharing the same bucket and `vdyp/fileset/` namespace.
3. With an ADMIN bearer token, preview:
   `POST /api/v8/admin/coms/orphan-file-sets` (defaults to `dryRun=true`).
   Save and review the JSON report.
4. Delete using `POST /api/v8/admin/coms/orphan-file-sets?dryRun=false`.
   This performs a fresh inventory and reference check, not a saved preview.
   Run only one maintenance operation at a time across all backend replicas.
5. Review failures, rerun preview and retry as needed before resuming writers.

## Configuration

The service uses the existing `vdyp.coms.s3` endpoint, bucket, access ID and secret
access key. `vdyp.coms.s3.region` (`VDYP_COMS_S3_REGION`) defaults to `us-east-1`;
set it for your storage signing region. Path-style addressing supports MinIO.

S3 credentials need `s3:ListBucketVersions` and `s3:DeleteObjectVersion` for the
configured bucket/prefix, or provider equivalents. This includes deleting the
explicit `null` version in unversioned/suspended buckets. Storage must support
ListObjectVersions; unsupported listing and access errors abort instead of being
interpreted as an empty bucket. File downloads are not required. COMS credentials
need access to relevant registrations and permission to remove them.

`vdyp.coms.orphan-cleanup.deletion-enabled` retains its current default of `true`.
Set `VDYP_COMS_ORPHAN_CLEANUP_DELETION_ENABLED=false` to disable destructive runs
(HTTP 409 `DELETION_DISABLED`). Preview always remains non-destructive.

## Scope and results

All pages of versions and delete markers under `vdyp/fileset/` are inventoried
before deletion. Zero-byte objects, folder markers, nested files and S3-only files
are included. Only UUID file-set path segments qualify: `vdyp/fileset/{UUID}` or
`vdyp/fileset/{UUID}/...`. Malformed keys appear in `skippedKeys` and are untouched.
The physical S3 bucket and shared parent prefix are never deleted. Empty COMS-only
file-set registrations are also checked.

`scanned` counts S3 versions/delete markers. Each folder reports `key`, `bucketIds`
(possibly empty), distinct `objects`, `versions` (including delete markers),
`bytes` across data versions, `outcome` and optional `detail`. Outcomes are
`REFERENCED`, `ORPHAN` (preview), `DELETED` or `FAILED`.

A database file-set UUID protects its entire folder. Cached COMS bucket references
also protect the folder, including nested registrations. S3 inventory, COMS listing
and initial database checks finish before deletion; references are rechecked before
each orphan is deleted. Database errors abort further work. COMS unavailability
aborts preflight because cached references cannot be evaluated without its listing.

S3 versions/delete markers are permanently removed by exact key and version ID.
A fresh S3 listing must show the folder empty before removing any COMS registrations,
deepest child first and non-recursively. Failures leave remaining registrations
and return `FAILED`; earlier successful deletions cannot be rolled back. A COMS
deletion failure leaves an empty registered folder discoverable on the next run.

Inventory is held in memory and requests are synchronous; large inventories need
appropriate heap and request timeouts. Locking is per backend instance, not across
replicas. Incomplete multipart uploads are outside this process. COMS-specific
sync limits and its zero-byte discovery limitation no longer apply.
