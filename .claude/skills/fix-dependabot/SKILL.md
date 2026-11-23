---
name: fix-dependabot
description: Fix open Dependabot security alerts, or other reported vulnerable dependencies, in the current repository. Learns the project's own rules and checks, fetches the alerts with the gh CLI (or a package audit tool when Dependabot is not reachable), traces each vulnerable package to the manifest and parent that pulls it in, presents fix options for the user to choose from, applies the chosen change, verifies it and runs the project's checks. Works for npm, Maven and other ecosystems. Use this skill whenever the user mentions Dependabot, security alerts, vulnerable dependencies, CVEs or GHSA advisories, npm audit or other audit findings, or asks to patch, bump or upgrade a dependency for security reasons, even if they do not say "Dependabot".
---

# Fix Dependabot alerts

This skill resolves open Dependabot alerts with the smallest change that moves every vulnerable copy of a package to a patched version, and proves it with the checks the project's CI runs. It works in any repository: the project-specific rules come from the project itself, not from this skill.

## The user decides

The user chooses which alerts to fix and which fix to apply. Claude researches, presents the options and recommends one, but does not change any file until the user has chosen. A dependency change can alter the build for the whole team, and the right trade-off (for example, a major-version pin against waiting for a parent release) is a judgement the user owns.

Stop and wait for the user's answer at each checkpoint marked **Checkpoint** below. Use the AskUserQuestion tool where it is available, with the recommended option first. Do not treat an earlier approval as covering a later checkpoint, and do not continue past a checkpoint because the answer seems obvious.

If new information changes the picture after the user has chosen (for example, the chosen override does not take effect, or a check fails), stop, explain what changed, and ask again rather than switching to another fix on your own.

## Ecosystem references

Steps 3 to 6 need commands for each package ecosystem. Read only the reference files for the ecosystems in the alerts the user chose:

| Ecosystem | Reference |
| --- | --- |
| npm (`package.json`, `package-lock.json`) | `references/npm.md` |
| Maven (`pom.xml`) | `references/maven.md` |

For an ecosystem with no reference file (for example Gradle, pip, Go modules, NuGet or Bundler), follow the same steps with that ecosystem's equivalents: its dependency tree command, its mechanism for forcing a transitive version (constraints, resolutions, `replace` directives and so on), and its audit tool. Tell the user that no reference exists, so they know the commands are less proven.

## 1. Learn the project

Before fetching anything, find the rules this project already has. Read, where they exist:

- `CLAUDE.md` and `AGENTS.md` files at the root and in the folders that hold the affected manifests,
- `CONTRIBUTING.md` and the README files near the manifests,
- the CI workflow files (`.github/workflows/`, `.gitlab-ci.yml`, `Jenkinsfile`, `azure-pipelines.yml`),
- package manager configuration such as `.npmrc`, `.nvmrc`, `.tool-versions`, `.mvn/` and `gradle.properties`.

From them, note:

- **Version policy**: whether versions must be exact or may use ranges, and where shared versions are managed (for example a root `dependencyManagement` section, a BOM or a version catalog).
- **Checks**: the commands CI runs for each affected part of the repository (build, lint, type check, tests, format check), and any known problems with running them locally.
- **Runtime versions**: the language and tool versions CI and the container images use (for example the Node version in the workflow and the `Dockerfile`).
- **Commit convention**: ticket key format (for example `ABC-1234`), message style and branch rules.

If the project says nothing about version policy, ask the user rather than assuming, because the answer changes how every fix is written. Use what you learn in every later step; the project's rules take precedence over the defaults in this skill and its references.

## 2. Fetch the open alerts

### Confirm GitHub CLI access

Find the repository with `gh repo view --json nameWithOwner --jq .nameWithOwner`, then run `gh auth status`. The Dependabot alerts API needs:

- a token with the `repo` scope (or `security_events` for a public-only view), and
- SAML single sign-on authorization for the organization, if the organization enforces it.

Two failure modes are easy to misread:

- `401 Bad credentials`: the stored token is invalid. Ask the user to run `gh auth login -h github.com --web`.
- `403` with the message "Rate Limit Exceeded" and a documentation link about SAML single sign-on: this is almost never a rate limit. Check `gh api rate_limit --jq .resources.core` to confirm. The real cause is a token with no scopes (`Token scopes: none`) or one that is not authorized for the organization. Ask the user to sign in through the browser flow, or to add the scopes and choose **Configure SSO** on their personal access token.

Re-authentication is the user's action. Do not try to handle tokens yourself.

### Fetch

```bash
gh api "repos/<owner>/<repo>/dependabot/alerts?state=open&per_page=100" \
  --jq '.[] | {number, pkg: .dependency.package.name, eco: .dependency.package.ecosystem, manifest: .dependency.manifest_path, scope: .dependency.scope, rel: .dependency.relationship, severity: .security_advisory.severity, ghsa: .security_advisory.ghsa_id, summary: .security_advisory.summary, vulnerable: .security_vulnerability.vulnerable_version_range, patched: .security_vulnerability.first_patched_version.identifier}'
```

### When Dependabot is not available

If the repository is not on GitHub, Dependabot is turned off, or the user cannot get access, say so and offer to work from the ecosystem's audit tool instead (each reference file names it). Audit results can differ from Dependabot's, so tell the user which source the alerts came from.

Summarise the alerts for the user in a table (number, package, ecosystem, manifest, severity, vulnerable range, first patched version) before changing anything.

**Checkpoint: which alerts.** Ask the user which alerts to work on, unless they already named them. Work only on the alerts they choose.

## 3. Trace each vulnerable package

Find every copy of the package in the dependency tree and the parent that requires it, using the commands in the ecosystem reference. Read the lockfile or the resolved tree rather than trusting what is installed locally, because the local install can be out of date with the lockfile that Dependabot scans.

For each copy, record whether it falls inside the vulnerable range. Copies outside the range (for example an old major version that the advisory does not cover) must be left alone, because forcing them up a major version can break their parent.

## 4. Choose the fix

Research every option that could work, in this order of preference:

1. **Direct dependency**: bump its version where it is declared, or where the project manages it.
2. **Upgrade the parent** if a newer parent release depends on a patched version. Check the parent's published dependencies rather than assuming: the newest release of a parent can still require a vulnerable version.
3. **Upgrade the managing platform or BOM** if the project gets the version from one (for example Spring Boot or Quarkus) and a newer patch release of it includes the fix.
4. **Pin the transitive version** when none of the above helps, using the ecosystem's override mechanism.

Pick the latest patched release in the same major version unless that release has its own advisory. Before proposing a jump across a major version, check that the parent's calls still exist in the new version (grep the parent's installed code for how it uses the package) and that the new version supports the runtime versions you found in step 1.

**Checkpoint: which fix.** For each alert, present the viable options to the user. For each option, give:

- the exact change (file, key and version, written the way the project's version policy requires),
- the copies of the package it moves, and any it leaves alone,
- the risk (for example, a major-version jump, a parent upgrade with its own changes, or a pin that must be removed later),
- whether you found evidence that it works (the parent's calls, supported runtime versions, release notes).

Say which option you recommend and why, then wait for the user to choose. Also offer "do not fix now" when the risk is high or the alert only affects development dependencies. Apply only the option the user chose, following the ecosystem reference and the project's rules from step 1. Regenerate lockfiles with the package manager; never hand-edit resolved versions in a lockfile.

## 5. Verify the fix

Confirm that every copy of the package is now at or above the patched version, using the verification commands in the ecosystem reference, and that the change follows the project's version policy.

If any copy is still vulnerable, tell the user what remains and return to the step 4 checkpoint. Do not report success based on the edit alone.

**Checkpoint: run the checks.** Show the user the verification result and the list of checks you found in step 1 for the affected parts of the repository, with a note on any that take a long time or need extra services (for example Docker or a browser). Ask whether to run all of them, a subset, or none.

## 6. Run the checks

Run the checks the user chose, the way CI runs them. Use the project's own commands and its notes on known local problems.

- Install from the lockfile first (for example `npm ci`), so the checks run against exactly what was committed.
- Run long checks in the background, and write the full output to a log file rather than filtering it through `grep`; a filtered run can hide the reason for a non-zero exit.
- If a check cannot run locally (a missing browser, Docker or service), say so and say what it would have covered. Use a documented fallback from the project if one exists; otherwise ask the user.

### When a check fails

Report the failure to the user first. **Checkpoint: investigate.** Finding out whether the change caused it means temporarily restoring the original dependencies, so ask before doing it. If the user agrees, back up the changed manifests and lockfiles, restore the original versions (for example with `git show HEAD:<path>`), reinstall, run only the failing check, then restore the fix and reinstall again. Report the failure as pre-existing only if it fails the same way on the original dependencies.

## 7. Report and do not commit

Tell the user:

- a table of each alert with the version before and after,
- the change made, the option the user chose, and the alternatives that were offered,
- the result of every check, including any check that could not run and why, and any pre-existing failure with the evidence.

Do not stage or commit. Ask the user to review and commit, and suggest a commit message that names the resolved alerts:

- Follow the commit convention from step 1. If the project uses ticket keys and none is known for this work, ask the user for one.
- Name the alert numbers and packages, for example: `ABC-1234: Fix Dependabot alerts 290 and 291 (glob, minimatch)`.
