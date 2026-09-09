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

The destination's own backups include both repo1 and repo2, exactly as for deployment. Therefore both restore modes require the normal S3 deployment settings for **future destination backups**, even though reading a PVC backup does not itself need S3 credentials.

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

## Chart restore values and manual installation

Use a copy of the chart with deployment tokens resolved by the normal deployment process, or supply a complete resolved environment values file. The repository's raw `#{...}#` placeholders are not directly deployable; do not duplicate the database configuration into a new restore chart. Match the backup's PostgreSQL major version and any extension images, using an appropriate chart revision for historical recovery.

Create `restore-values.yaml` containing only recovery-specific overrides. For PVC:

```yaml
fullnameOverride: crunchy-postgres-recovered
restore:
  enabled: true
  repository: repo1
  sourceCluster: crunchy-postgres-prod
  backupLabel: "20260906-080000F"
  recoveryTarget: ""
```

For S3:

```yaml
fullnameOverride: crunchy-postgres-recovered
restore:
  enabled: true
  repository: repo2
  sourceCluster: crunchy-postgres-prod
  backupLabel: "20260906-100000F"
  recoveryTarget: ""
  s3:
    directoryName: vdyp-pgbackrest-prod
```

Replace example labels with successful backups. `sourceCluster` is a name guard for S3 and is not looked up. For PITR, set `recoveryTarget: "2026-09-08T10:00:00-07:00"` to the actual target. S3 source `bucket`, `endpoint` and `region` can be overridden under `restore.s3`; otherwise they use normal chart settings. Recovery and future backups reuse `pgBackRest.repos.configuration.secretName`, which must already exist in the same namespace.

Render, validate and install from a token-resolved chart (example location `staging/crunchy-postgres`):

```sh
helm lint staging/crunchy-postgres -f restore-values.yaml
helm template crunchy-postgres-recovered staging/crunchy-postgres -f restore-values.yaml --show-only templates/PostgresCluster.yaml > restore-cluster.yaml
oc -n <namespace> create --dry-run=server -f restore-cluster.yaml
helm install crunchy-postgres-recovered staging/crunchy-postgres -n <namespace> -f restore-values.yaml
```

Use `helm install` with a new release/cluster name. Check `oc get postgrescluster <destination>` first and do not adopt an existing database. For both restore modes, install in the original namespace. Do not use `--atomic`, which can remove resources needed to investigate failed recovery. Do not print full rendered charts or upload them as artifacts because they include S3 credentials.

The chart skips bootstrap SQL for recovery. All other database settings come from the normal chart. Both restore paths create new data/WAL/backup PVCs. The restored release references the existing environment S3 Secret without creating or adopting it, and writes S3 backups to `/pgbackrest/postgres-operator/vdyp-pgbackrest-<destination>/repo2`. The original source directory is used only for reading. The chart rejects identical source/destination names and identical S3 source/destination directories.

Persist the override file for later Helm upgrades. Keep `restore.enabled: true` and the same destination identity: these bootstrap values do not request another restore on an initialized cluster, and disabling them would change the chart's backup path and Secret ownership. Changing backup label or recovery target on an existing cluster is not a recovery operation; use a new destination for another restore.

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

Record the running application release/image versions, Helm values, deployment replica counts and current database Secret references. Pause automatic deployment and migration jobs during the recovery. Put the application into maintenance, stop new submissions, drain or explicitly account for in-flight work, and stop backend, batch and COMS writers. Suspend relevant CronJobs and autoscaling controllers so they cannot recreate writers while configuration changes are being made.

Discover the actual resource names with `oc -n <namespace> get deployments,cronjobs,hpa`; scale the selected writer deployments to zero with `oc -n <namespace> scale deployment/<name> --replicas=0`. Check for KEDA ScaledObjects or other controllers too. Record what was suspended so it can be restored later. Do not scale the database down as part of this step.

For planned recovery with no intended data loss, stop writers **before the final backup/recovery target is selected**, and ensure their final transactions are archived and recovered. For incident PITR, explicitly account for data intentionally excluded after the target. Writes made to the original after the restored point do not appear automatically in the destination.

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

Restoration now uses the chart's normal pod labels. The `app.kubernetes.io/name` label comes from the chart name or `nameOverride`, not necessarily `fullnameOverride`. Inspect the destination's actual labels before changing `global.databaseAlias`; the destination cluster name alone is not sufficient to predict that label. Configure destination policies using its actual labels, such as `postgres-operator.crunchydata.com/cluster: crunchy-postgres-recovered`, and allow TCP 5432 from the backend, batch, COMS and migration pods. Check egress policies and DNS access as well. The chart installs the same-namespace access policy for recovery too, using a release-specific resource name to avoid conflict with the original release. Verify connectivity from application pods and preserve the intended namespace access restrictions.

Update the Liquibase chart's `crunchy.pguserSecretName`, `crunchy.adminSecretName` and `crunchy.batchSecretName` to destination Secrets. Its current Job templates consume `host` and `jdbc-uri` from those Secrets, so changing `crunchy.host` alone is insufficient. The templates in `charts/liquibase/values.yaml` still generate source-cluster names; adjust the persisted recovery deployment configuration before running the DDL workflow again. Confirm the application version is compatible with recovered Liquibase history; do not automatically run migrations just to test restoration.

### 4. Deploy and validate before reopening traffic

Render/review the application Helm changes and deploy through the normal application deployment process, retaining the writer pause until ready to validate. Changes to Secret references create new pod templates; if only Secret contents changed, restart the affected deployments because environment variables are read at pod startup.

Restore the recorded deployment replica counts for controlled validation while maintenance access is still in place. Watch `oc -n <namespace> rollout status deployment/<name>` for each application. Verify backend database access, a representative VDYP operation, a controlled batch job, and COMS metadata/object access. Check logs for authentication, DNS, TLS and permission errors. Confirm sessions reach the destination and that the original has no application writers.

Database restoration does not rewind NATS messages, object storage or other external state. Reconcile queued jobs and COMS objects with the recovered database before resuming consumers; otherwise old messages may replay work or database metadata may refer to a different object state.

Reenable traffic, scheduled work and autoscaling after these checks. Record the cutover time and the first successful application transactions.

### 5. Keep configuration and rollback deliberate

The restored cluster is owned by its own Helm release, named after the destination. Normal manual DB deployment still targets `crunchy-postgres-<environment>`; keep it paused until routine deployment targets the accepted destination. Upgrade the destination release with the same environment configuration and persisted restore overrides. Keep `restore.enabled`, destination name and restore settings unchanged: they preserve shared Secret usage and the destination S3 path and do not trigger in-place recovery. Do not rerun the restore workflow to perform an upgrade; it deliberately uses install-only behavior. Validate new repo1 and repo2 backups before considering recovery complete.

Before the destination accepts writes, reverting the application configuration can return clients to the original if its data is suitable. After destination writes begin, switching back would lose or diverge those writes: stop writers and plan data reconciliation or another recovery first. Retain the original cluster and recovery evidence until the agreed acceptance period ends, then retire it explicitly.

Reference: [Crunchy 5.7 PostgresCluster API](https://access.crunchydata.com/documentation/postgres-operator/latest/references/crd/5.7.x/postgrescluster).



