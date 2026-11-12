# VDYP deployment runbook

## How the pipeline works

This pipeline builds and promotes `backend`, `frontend`, `batch`, and `log-exporter`
in GHCR and deploys them to OpenShift using Helm. Database (Liquibase), API gateway,
and PostgreSQL cluster changes are separate procedures; see
[Other deployment procedures](#other-deployment-procedures).

| Event | Workflow | Action |
| --- | --- | --- |
| Same-repository PR opened, updated, or reopened (build paths only) | Build and Deploy to Openshift Dev When PR | Build all four images and deploy dev |
| Push to `main` (build paths only) | Build and Deploy to Openshift Dev When PR | Build all four images and deploy dev |
| Manual build: `all` | Build and Deploy to Openshift Dev When PR | Build all four images and deploy dev |
| Manual build: one component | Build and Deploy to Openshift Dev When PR | Build only that component; no deployment |
| GitHub prerelease published | Deploy existing release candidate to test | Check existing SHA images, apply release tag, deploy test |
| GitHub stable release published, or prerelease marked stable | Promote existing release to production | Check existing SHA images, apply release tag, deploy prod |
| Manual promotion | Promote existing commit images and deploy | Check existing SHA images, apply release tag, deploy selected environment |

The build paths are `backend/`, `batch/`, `log-exporter/`, `frontend/`, `lib/`,
`buildtools/`, `charts/`, `nats/`, `.github/`, `pom.xml`, and `.dockerignore`. A PR or
push that changes only other files (for example `docs/` or `db/`) does not build or
deploy.

Images are built with the first **12 characters of the source commit SHA** as their
tag. PR builds also add a PR-number alias, but no workflow uses that alias to select
images.

Dev deploys the 12-character SHA tag. Test and prod deploy the release tag copied
from those SHA-tagged images. Full SHAs are retained internally for checkout and
OCI revision labels. Application and NATS charts come from the same source commit.
Dev and release workflows call `.deployer.yml` directly. It selects
`values-<environment>.yaml` unless the caller supplies an explicit `values` override.
No current caller supplies an override.

The workflows contain the necessary shell steps directly. There are no JavaScript
delivery helpers, digest manifests, custom deployment-evidence records, or automated
CI/test-acceptance checks in the promotion path.

## Versioning and release branches

Current practice, based on the repository history:

- The application version is the root `pom.xml` `<version>` (for example `1.2.1`).
  Release work is prepared on a `release/<version>` branch and merged to `main`.
- Release tags use the form `<version>-rc.<n>` for candidates and `<version>` for the
  stable release (for example `1.2.1-rc.1` and `1.2.1`). Both are placed on the same
  merge commit on `main`.
- **The pom version must equal the release version before the images are built.**
  The build reads the pom version and bakes it into the frontend (`APP_VERSION`).
  Promotion only adds a registry tag; it does not change the version shown in the
  application. If the pom version is wrong, fix it in a new commit and build again.

Version inputs use the form `1.2.1`, `v1.2.1`, or `1.2.1-rc.1`, without build metadata,
and are limited to 63 characters because they are also used in Kubernetes labels.

## Responsibilities before release

The release publisher must verify:

1. The intended source commit has a successful **Java CI with Maven and SonarCloud**
   run, including Maven tests and formatter validation. If the frontend changed, the
   PR that introduced the change also has a successful **Frontend CI with Node** run
   (that workflow runs on PRs only, not on pushes to `main`).
2. All four images were successfully built for that commit.
3. The root `pom.xml` version matches the release version.
4. Any Liquibase changes in the release have been applied to the target environment
   first (see [Database migrations](#database-migrations)).
5. Before production, that release candidate has been deployed to test and accepted.
6. External production approval is complete before publishing a stable release or
   manually selecting `prod`.

The pipeline checks image availability, not these approvals or test results.
Maven/Sonar continues to run separately; dev deployment does not wait for it.

Tags are mutable. Rebuilding the same source SHA can replace its images, and rerunning
promotion can replace release aliases. **Do not rebuild a tested/released SHA or
reuse its release version for different content.** Use a new commit/version for changes.
The pipeline does not prove that production receives the exact bytes previously
tested; it relies on this tag discipline and the publisher's checks.

## Prerequisites

- The `GITHUB_TOKEN` permissions declared by the workflows must be allowed: repository
  reads and package writes for builds and promotion.
- Each GitHub environment (`dev`, `test`, `prod`) must define the secrets required by
  `.deployer.yml`: `oc_namespace`, `oc_token`, `VITE_SSO_AUTH_SERVER_URL`,
  `VITE_SSO_CLIENT_ID`, `VITE_SSO_REALM`, `VITE_SSO_REDIRECT_URI`,
  `VITE_SSO_TOKEN_ISSUER`, `VITE_API_URL`, `KEYCLOAK_CLIENT_SECRET`,
  `COMS_KEYCLOAK_PUBLICKEY`, `NATS_PASSWORD`, `POSTGRES_PASSWORD`, `VDYP_DB_PASSWORD`,
  `OBJECTSTORAGE_SECRETACCESSKEY`, and `COMS_SERVICE_PASSWORD`.
- Each environment must also define the environment variables (`vars.*`) referenced
  by `.deployer.yml`, including database names and users, COMS and object storage
  settings, CORS and service URLs, API gateway JWT settings, and batch tuning values.
  A missing variable is passed to Helm as an empty value and does not fail the
  workflow, so compare a new environment against an existing one.
- The batch worker registry cleanup variables must be set in each environment:
  `BATCH_WORKER_REGISTRY_CLEANUP_ENABLED` (normally `true`), `BATCH_WORKER_REGISTRY_CLEANUP_RETENTION`
  (Spring duration, normally `1d`) and `BATCH_WORKER_REGISTRY_CLEANUP_CRON` (Spring cron with seconds,
  normally `0 0 3 * * *`, UTC). An empty value overrides the application default rather than falling
  back to it, and prevents the batch service from starting or running the cleanup.
- Existing environment protection rules still apply; the pipeline adds no approval job.
- Retain the SHA and release image tags needed for promotion and rollback.
- Restrict registry write access. A 12-character SHA collision or manually reassigned
  tag is not protected by an artifact-evidence system; investigate it before releasing.

## Development builds

For same-repository PRs affecting the build paths, opening, updating, or reopening
the PR calls `build-images.yml` and then deploys dev. Fork PRs do not publish images
or receive OpenShift deployment credentials.

For PR events, `github.sha` is GitHub's temporary merge commit, which can differ from
the branch-head and final merged commits. Main builds use the actual pushed commit.
Never infer a release image from its PR number. Use the intended commit's SHA tag.

In Actions, **Build and Deploy to Openshift Dev When PR** also accepts manual runs:

- Choose the branch/ref to build.
- Select `all` to build all four components and deploy dev.
- Select one component for a build-only run.

**Java CI with Maven and SonarCloud runs automatically** whenever a PR is opened,
updated, or reopened, and whenever code is pushed to `main`. It has no path filter.
Normal PR and main development does not require a manual CI run. GitHub branch
protection/rulesets separately determine whether passing checks are required before
a PR can merge.

Only when releasing directly from a branch commit that lacks a successful CI run
for that exact commit, build the images and manually run **Java CI with Maven and
SonarCloud** for that same ref. Confirm both runs used the same commit; a branch may
move between runs. A PR merge-commit validation does not validate a different branch
head. Note that a manual `all` build also deploys that commit to the shared dev
environment. To build without deploying, run the workflow four times, once for each
component. For releases from `main`, use the automatic build and CI runs for the
merged commit.

Builds are serialized per short SHA/component. All deployments into an environment
share a deployment lock and do not automatically cancel an active Helm rollout.
Dev is shared: another PR may deploy after yours. GitHub can replace pending runs,
so inspect the actual completed deployment rather than assuming every queued run ran.

## Release candidate to test

1. Choose the source commit (normally the `main` merge commit of the
   `release/<version>` branch). Verify CI, confirm the pom version, and confirm all
   four images exist with its 12-character SHA tag.
2. Apply any Liquibase changes for this release to test (see
   [Database migrations](#database-migrations)).
3. Create a Git tag such as `1.2.1-rc.1` at that exact commit.
4. Publish a GitHub Release with **Set as a pre-release** selected. Publishing from
   a draft works; the workflow uses the `published` event and prerelease flag.
5. The workflow checks all four source images before tagging any of them. A missing
   image or registry error fails the workflow; it does not start a build.
6. Existing images receive the release tag and Helm deploys that tag to test.
7. Verify the application and complete acceptance testing before production.

## Production release

1. Verify test acceptance and complete the separate production approval process.
2. Confirm no images under the source SHA tag have been rebuilt/replaced since testing.
3. Apply any Liquibase changes for this release to prod.
4. Publish a stable GitHub Release such as `1.2.1` at the **same source commit** as
   the tested candidate.
5. Production promotion checks the four SHA images, applies the stable version tag,
   and deploys that version with charts from the same commit.

Marking the candidate prerelease as stable also triggers the `released` event, but
it deploys the candidate tag name (for example `1.2.1-rc.1`) to production. Prefer
publishing a new stable release with the final version tag.

The workflow does not inspect prior test deployments or CI results. Publishing the
release indicates the publisher has completed those checks.

Releases created by a workflow using `GITHUB_TOKEN` may not trigger another workflow.
Use the GitHub UI, an appropriately authorized release tool, or manual promotion.

## Manual promotion and rollback

Run **Promote existing commit images and deploy** with:

- `source_sha`: exactly 12 lowercase hexadecimal characters.
- `release_version`: the version tag to apply.
- `target_environment`: `dev`, `test`, or `prod`.

The short SHA must resolve unambiguously to a commit in the fetched repository history.
There is no independent chart-ref override. Manual promotion uses the same existence
checks and never builds images. CI verification, test acceptance, and production
approval remain the operator's responsibility.

To roll back, obtain the required external approval, confirm the previous SHA-tagged
images still represent the desired build, and manually promote that SHA and its
original version. Check database/configuration compatibility first: this procedure
does not reverse Liquibase migrations, and the backend validates the schema at
startup, so an older build may fail to start against a newer schema. Rollback also
redeploys the NATS chart from the older commit, but existing JetStream streams and
consumers are not changed (see [NATS](#nats)).

## Database migrations

Schema changes are applied with the **Liquibase DDL application** workflow
(`.github/workflows/liquibase-ddl.yml`), not by the application pipeline.

1. Run the workflow from the ref being released (select the release tag or commit
   in **Use workflow from**), choose the target environment, and leave `TAG` as
   `latest`.
2. The workflow builds the `vdyp-liquibase` image from `db/liquibase`, then installs
   the `charts/liquibase` chart, whose hook jobs apply the admin, `app-vdyp`, and
   `batch` changelogs.
3. Run it **before** deploying application images that need the new schema. The
   backend starts with Hibernate schema validation and fails if the schema is
   missing changes.
4. Do not run it at the same time as another Liquibase run or an application
   deployment to the same environment; the workflow has no deployment lock.

Known limitations of the current workflow:

- The chart always deploys `ghcr.io/bcgov/vdyp-liquibase:latest`. A non-default `TAG`
  value is pushed but is not the image that runs. Leave `TAG` as `latest`.
- Because `latest` is mutable and shared by all environments, the image that runs is
  whatever was built most recently. Do not start another Liquibase run until the
  current one has finished.
- Liquibase migrations are not rolled back by this pipeline.

## NATS

`.deployer.yml` installs the NATS chart (`charts/nats`) on every deployment. A
post-install hook runs `nats/apply-jetstream.sh`, which **creates** missing streams and
consumers but skips any that already exist. Changes to an existing definition in
`nats/streams` or `nats/consumers` are therefore not applied by a deployment and need
a manual `nats stream edit` or `nats consumer edit` (or a planned delete and recreate)
in each environment.

## Other deployment procedures

| Procedure | Workflow | Trigger |
| --- | --- | --- |
| Database schema | Liquibase DDL application (`liquibase-ddl.yml`) | Manual, per environment |
| API gateway configuration | publish-gateway-config (`deploy-api-gateway.yml`) | Manual, per environment |
| PostgreSQL cluster | `openshift-deploy-crunchy.yml` | Manual, per environment |
| PostgreSQL restore | Openshift DB restore (`openshift-restore-crunchy.yml`) | Manual; after a restore, see `backend/ORPHAN-FILE-CLEANUP.md` |
| Security scan | OWASP ZAP full scan (`zap-full-scan.yml`) | Weekly schedule and manual |
| Javadoc | `publish-javadoc.yml` | Push to `main` and manual |

## Troubleshooting

| Failure | Recovery |
| --- | --- |
| Missing SHA-tagged image | Run the normal build workflow for the intended commit, verify CI and image contents, then retry promotion. Rebuilding a tested SHA requires renewed testing. |
| Ambiguous or unknown short SHA | Check the commit exists on a fetched branch/tag and select the correct unique prefix. Never substitute a PR number or `latest`. |
| Registry authentication/network error | Repair credentials/connectivity and retry; no build fallback occurs. |
| Partial version tagging after registry failure | Confirm the source tags have not changed, then retry. Deployment starts only after all tagging succeeds. |
| Helm failure | Inspect Helm history and OpenShift events. `--atomic` applies separately to NATS and the application, not to the whole pipeline. |
| Helm remains pending after manual cancellation | Inspect and recover the Helm release explicitly. The workflow does not automatically uninstall it. |
| Backend fails to start with a schema validation error | Liquibase changes for this release have not been applied. Run the Liquibase workflow for the environment, then redeploy. |
| Application shows the wrong version | The pom version was not updated before the images were built. Fix it in a new commit, rebuild, and promote under the new SHA. |
| Batch requests are not delivered after a NATS definition change | Existing streams and consumers are not updated by deployments. Edit them manually (see [NATS](#nats)). |

Inspect images with:

```sh
docker buildx imagetools inspect ghcr.io/bcgov/nr-vdyp/backend:<12-character-sha>
docker buildx imagetools inspect ghcr.io/bcgov/nr-vdyp/backend:<release-version>
```

Helm deployment packages use `0.0.0-sha-<12-character-source-SHA>` as their chart
version, for example `0.0.0-sha-abcdef123456`. This valid SemVer identifies the chart's
source commit consistently across dev, test, and prod. The override applies during
packaging; the repository's `Chart.yaml` is not updated. The package's application
version is set separately to the deployed image tag (SHA or release version).
The app chart declares no dependencies; deployment packages the checked-out chart
without resolving the historical stale `Chart.lock` (which still lists a Bitnami
PostgreSQL dependency). If dependencies are introduced, update the lock and restore
locked dependency handling.
