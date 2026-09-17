# VDYP deployment runbook

## How the pipeline works

This pipeline builds and promotes `backend`, `frontend`, `batch`, and `log-exporter`
in GHCR and deploys them to OpenShift using Helm. Liquibase and infrastructure
changes remain separate procedures.

| Event | Action |
| --- | --- |
| PR opened, updated, or reopened | Build all four images and deploy dev |
| Push to main | Build all four images and deploy dev |
| Manual build: all | Build all four images and deploy dev |
| Manual build: one component | Build only that component; no deployment |
| GitHub prerelease published | Check existing SHA images, apply release tag, deploy test |
| GitHub stable release published / prerelease marked stable | Check existing SHA images, apply release tag, deploy prod |
| Manual promotion | Check existing SHA images, apply release tag, deploy selected environment |

Images are built with the first **12 characters of the source commit SHA** as their
tag. PR builds also add a PR-number alias, but no workflow uses that alias to select
images.

Dev deploys the 12-character SHA tag. Test and prod deploy the release tag copied
from those SHA-tagged images. Full SHAs are retained internally for checkout and
OCI revision labels. Application and NATS charts come from the same source commit.
Dev and release workflows call `.deployer.yml` directly. It selects
`values-<environment>.yaml` unless the caller supplies an explicit `values` override.

The workflows contain the necessary shell steps directly. There are no JavaScript
delivery helpers, digest manifests, custom deployment-evidence records, or automated
CI/test-acceptance checks in the promotion path.

## Responsibilities before release

The release publisher must verify:

1. The intended source commit has a successful **Java CI with Maven and SonarCloud**
   run, including Maven tests and formatter validation.
2. All four images were successfully built for that commit.
3. Before production, that release candidate has been deployed to test and accepted.
4. External production approval is complete before publishing a stable release or
   manually selecting `prod`.

The pipeline checks image availability, not these approvals or test results.
Maven/Sonar continues to run separately; dev deployment does not wait for it.

Tags are mutable. Rebuilding the same source SHA can replace its images, and rerunning
promotion can replace release aliases. **Do not rebuild a tested/released SHA or
reuse its release version for different content.** Use a new commit/version for changes.
The pipeline does not prove that production receives the exact bytes previously
tested; it relies on this tag discipline and the publisher's checks.

## First rollout and prerequisites

- Merge the new workflows before relying on them. Existing PR/`latest` images are
  not automatically converted to SHA-tagged images.
- Allow the declared `GITHUB_TOKEN` permissions: repository reads and package writes
  for builds/promotion. The pipeline does not require custom deployment-record writes.
- Preserve existing `dev`, `test`, and `prod` environment variables and secrets,
  including OpenShift credentials and `NATS_PASSWORD`.
- Existing environment protection rules still apply; this change adds no approval job.
- Check branch protection required-check names after extracting the reusable builder.
- Retain the SHA and release image tags needed for promotion and rollback.
- Restrict registry write access. A 12-character SHA collision or manually reassigned
  tag is not protected by an artifact-evidence system; investigate it before releasing.

## Development builds

For same-repository PRs affecting the configured build paths, opening, updating, or
reopening the PR calls `build-images.yml` and then deploys dev. Fork PRs do not publish
images or receive OpenShift deployment credentials.

For PR events, `github.sha` is GitHub's temporary merge commit, which can differ from
the branch-head and final merged commits. Main builds use the actual pushed commit.
Never infer a release image from its PR number. Use the intended commit's SHA tag.

In Actions, **Build and Deploy to Openshift Dev When PR** also accepts manual runs:

- Choose the branch/ref to build.
- Select `all` to build all four components and deploy dev.
- Select one component for a build-only run.

**Java CI with Maven and SonarCloud runs automatically** whenever a PR is opened,
updated, or reopened, and whenever code is pushed to `main`. Normal PR and main
development does not require a manual CI run. These triggers remain unchanged;
GitHub branch protection/rulesets separately determine whether passing checks are
required before a PR can merge. This workflow change does not modify those settings.

Only when releasing directly from a branch commit that lacks a successful CI run
for that exact commit, run a full build and manually run **Java CI with Maven and
SonarCloud** for that same ref. Confirm both runs used the same commit; a branch may
move between runs. A PR merge-commit validation does not validate a different branch
head. For releases from `main`, use the automatic build and CI runs for the merged commit.

Builds are serialized per short SHA/component. All deployments into an environment
share a deployment lock and do not automatically cancel an active Helm rollout.
Dev is shared: another PR may deploy after yours. GitHub can replace pending runs,
so inspect the actual completed deployment rather than assuming every queued run ran.

## Release candidate to test

1. Choose the source commit. Verify CI manually and confirm all four images exist
   with its 12-character SHA tag.
2. Create a Git tag such as `8.1.0-rc1` at that exact commit.
3. Publish a GitHub Release with **Set as a pre-release** selected. Publishing from
   a draft works; the workflow uses the `published` event and prerelease flag.
4. The workflow checks all four source images before tagging any of them. A missing
   image or registry error fails the workflow; it does not start a build.
5. Existing images receive the release tag and Helm deploys that tag to test.
6. Verify the application and complete acceptance testing before production.

Version inputs use the form `8.1.0`, `v8.1.0`, or `8.1.0-rc1`, without build metadata,
and are limited to 63 characters because they are also used in Kubernetes labels.

## Production release

1. Verify test acceptance and complete the separate production approval process.
2. Confirm no images under the source SHA tag have been rebuilt/replaced since testing.
3. Publish a stable GitHub Release such as `8.1.0` at the **same source commit** as
   the tested candidate. Marking a prerelease stable also triggers the `released` event.
4. Production promotion checks the four SHA images, applies the stable version tag,
   and deploys that version with charts from the same commit.

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
original version. Check database/configuration compatibility first. This procedure
does not reverse Liquibase migrations.

## Troubleshooting

| Failure | Recovery |
| --- | --- |
| Missing SHA-tagged image | Run the normal build workflow for the intended commit, verify CI and image contents, then retry promotion. Rebuilding a tested SHA requires renewed testing. |
| Ambiguous or unknown short SHA | Check the commit exists on a fetched branch/tag and select the correct unique prefix. Never substitute a PR number or `latest`. |
| Registry authentication/network error | Repair credentials/connectivity and retry; no build fallback occurs. |
| Partial version tagging after registry failure | Confirm the source tags have not changed, then retry. Deployment starts only after all tagging succeeds. |
| Helm failure | Inspect Helm history and OpenShift events. `--atomic` applies separately to NATS and the application, not to the whole pipeline. |
| Helm remains pending after manual cancellation | Inspect and recover the Helm release explicitly. The workflow does not automatically uninstall it. |

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
without resolving the historical stale `Chart.lock`. If dependencies are introduced,
update the lock and restore locked dependency handling.

