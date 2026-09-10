# Database restoration

Run **Openshift DB restore** to restore into a new Helm release and PostgresCluster. It calls the existing **Openshift DB deploy/update** workflow, reusing its environment-variable substitution and `charts/crunchy-postgres` chart. Database versions, users, instances, storage, Patroni, PgBouncer, monitoring and backup schedules are defined once in that chart. Crunchy's operator creates the actual OpenShift restore Job; there is no custom Node.js restore code.

## Inputs

| Input | Meaning |
| --- | --- |
| ENVIRONMENT_NAME | Environment that wrote the backup: dev, test or prod |
| REPOSITORY | `repo1` for PVC or `repo2` for S3 (default) |
| TARGET_CLUSTER | New Helm release and cluster name; must not exist |
| BACKUP_LABEL | Exact successful backup label from the selected repository |
| RECOVERY_TARGET | Optional PITR timestamp with timezone; blank restores backup consistent state |
| POSTGRES_VERSION | Optional major-version override for historical backups; blank uses the shared chart |

The workflow uses the same GitHub environment variables and `oc_namespace`/`oc_token` secrets as deployment. Its token needs permissions for Helm release Secrets and chart resources (PostgresClusters, Secrets, ConfigMaps and NetworkPolicies). Recovery validation also needs pod/Job inspection and database exec access. Ensure namespace quota covers another cluster and backup PVC, and network policies permit repository and application traffic.

## Choose and submit a recovery

If the source exists, find its repository host and list backups:

```sh
oc -n <namespace> get pods -l postgres-operator.crunchydata.com/cluster=crunchy-postgres-<environment>
oc -n <namespace> exec <repo-host-pod> -c pgbackrest -- pgbackrest --stanza=db --repo=1 info
oc -n <namespace> exec <repo-host-pod> -c pgbackrest -- pgbackrest --stanza=db --repo=2 info
```

Use a label from the selected repository; repo1 and repo2 have different schedules. For PITR, the selected backup must finish before the target and all required WAL must remain archived. Full-backup retention does not imply the same PITR coverage.

For PVC, the source `crunchy-postgres-<environment>`, repository host and backup PVC must remain accessible in the selected namespace. The workflow checks the source PostgreSQL major version and repo1 definition. An orphaned backup PVC alone is insufficient for this path; reconstructing its repository host/configuration is a separate operation. Prefer S3 recovery when the source resources are lost.

For S3, the source cluster need not exist. The workflow reads the original path `/pgbackrest/postgres-operator/vdyp-pgbackrest-<environment>/repo2` using the configured S3 credentials. If restoring a backup of a previously recovered cluster, use manual chart overrides with its actual source directory instead of that original environment path.

Run the restore workflow with a unique destination. It validates the generated PostgresCluster against the installed CRD and calls `helm install`, never `upgrade --install`, for recovery. **Workflow success means submission, not completed recovery.** Proceed to monitoring below.

## Shared S3 credentials

Restores always run in the original namespace and reuse the environment S3 Secret for recovery and subsequent backups. The original deployment retains ownership of this Secret. Keep it available when retiring the original database or Helm release; removing that release can remove the shared Secret. Credential rotation through the original deployment applies to both clusters.

## Monitor and validate recovery

For either repository:

```sh
oc -n <namespace> describe postgrescluster <destination>
oc -n <namespace> get pods,jobs -l postgres-operator.crunchydata.com/cluster=<destination>
oc -n <namespace> logs job/<restore-job-name> --all-containers=true
```

Wait for the restore Job to finish and the expected database replicas to become ready. On the restored primary:

```sh
oc -n <namespace> exec <primary-pod> -c database -- psql -X -U postgres -d postgres -c "SELECT pg_is_in_recovery();"
oc -n <namespace> exec <primary-pod> -c database -- psql -X -U postgres -d postgres -c "\l"
```

Recovery must return `false`. Check representative records, row counts and Liquibase history in VDYP, batch and COMS against the selected recovery point, then validate application-role connections. Preserve backup label, recovery target, logs, elapsed time and results. A running database is not by itself sufficient proof of successful business-data recovery.

Resources and the Helm release remain available on failure. Inspect the release with `helm status <destination> -n <namespace>` and the operator Job logs; do not rerun recovery against an initialized destination. Use a new destination for another attempt. After acceptance or a completed drill, explicitly retire unwanted Helm releases and inspect retained PVCs; do not delete the source backup repository.

## Application cutover

### 1. Establish the write boundary

Record the running application release/image versions, Helm values, deployment replica counts and current database
Secret references. Ensure system is not subject to deployments or projections before starting the cutover.

Discover the actual resource names with `oc -n <namespace> get deployments,cronjobs,hpa`; scale the selected writer
deployments to zero with `oc -n <namespace> scale deployment/<name> --replicas=0`.

### 2. Change application Secret references

Update `charts/app/values-<environment>.yaml` (or the equivalent persistent Helm overrides) with the actual destination role names:

| Helm value | Destination example |
| --- | --- |
| `backend.env.VDYP_DB_SECRET_NAME` | `crunchy-postgres-recovered-pguser-app-vdyp` |
| `backend.env.VDYP_PROXY_DB_SECRET_NAME` | `crunchy-postgres-recovered-pguser-proxy-vdyp-rest` |
| `batch.env.BATCH_DB_SECRET_NAME` | `crunchy-postgres-recovered-pguser-batch` |
| `coms.db_secret` | `crunchy-postgres-recovered-pguser-coms` |

These role suffixes match the current app values; verify the destination's `spec.users` and Secret names with `oc -n <namespace> get secrets -l postgres-operator.crunchydata.com/cluster=crunchy-postgres-recovered`. Do not print credential values into logs.

Backend reads `user`, `password`, `host` and `port` from its proxy-role Secret and `dbname` from its app-role Secret. Batch and COMS read their connections from their respective Secrets. Consequently, updating these references changes both credentials and endpoint. `VDYP_PROXY_DB_SECRET_NAME` names the database proxy role; it does not by itself select PgBouncer. The templates currently consume the `host`/`port` keys rather than `pgbouncer-host`/`pgbouncer-port`.

Validate a connection using each destination application role before reopening traffic; a successful local `psql -U postgres` check does not validate application authentication. Secrets are namespace-local: if applications remain in another namespace, provision connection Secrets there with the destination service's fully qualified DNS name and credentials, and permit cross-namespace traffic.

### 3. Reconcile network policies and migration configuration

`global.databaseAlias` is used in legacy generated host strings and database NetworkPolicy selectors. Changing it alone does not change the Secret-based connections above. Inspect the rendered chart before updating it: some host strings prepend the Helm release name, whereas the operator's service is `<destination>-primary`.

Update the Liquibase chart's `crunchy.pguserSecretName`, `crunchy.adminSecretName` and `crunchy.batchSecretName` to destination Secrets. Its current Job templates consume `host` and `jdbc-uri` from those Secrets, so changing `crunchy.host` alone is insufficient. The templates in `charts/liquibase/values.yaml` still generate source-cluster names; adjust the persisted recovery deployment configuration before running the DDL workflow again. Confirm the application version is compatible with recovered Liquibase history; do not automatically run migrations just to test restoration.

### 4. Keep configuration and rollback deliberate

The restored cluster is owned by its own Helm release, named after the destination. Normal manual DB deployment still targets `crunchy-postgres-<environment>`; keep it paused until routine deployment targets the accepted destination. Upgrade the destination release with the same environment configuration and persisted restore overrides. Keep `restore.enabled`, destination name and restore settings unchanged: they preserve shared Secret usage and the destination S3 path and do not trigger in-place recovery. Do not rerun the restore workflow to perform an upgrade; it deliberately uses install-only behavior. Validate new repo1 and repo2 backups before considering recovery complete.

Before the destination accepts writes, reverting the application configuration can return clients to the original if its data is suitable. After destination writes begin, switching back would lose or diverge those writes: stop writers and plan data reconciliation or another recovery first. Retain the original cluster and recovery evidence until the agreed acceptance period ends, then retire it explicitly.



