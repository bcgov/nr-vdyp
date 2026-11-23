# npm

Commands and techniques for npm projects (`package.json` with `package-lock.json`). Run every command in the folder that holds the affected `package.json`, which is the folder part of the alert's `manifest_path`.

For Yarn or pnpm, use the equivalents: `resolutions` (Yarn) or `pnpm.overrides` (pnpm) in place of `overrides`, and `yarn why` or `pnpm why` in place of `npm ls`.

## Trace (step 3)

List each resolved copy in the lockfile, with its version and whether it is a development dependency:

```bash
node -e '
const l=require("./package-lock.json").packages; const name=process.argv[1];
for (const [k,v] of Object.entries(l)) if (k.endsWith("node_modules/"+name))
  console.log(k, v.version, v.dev ? "dev" : "prod");' <package>
npm ls <package> --all
```

The path of each lockfile entry shows its parent: `node_modules/js-beautify/node_modules/glob` is the copy that `js-beautify` uses.

## Research options (step 4)

- Versions in a major line: `npm view <package>@<major> version`
- A parent's dependencies in its newest release: `npm view <parent> dependencies`
- Supported Node versions: `npm view <package>@<version> engines`
- How a parent uses the package: grep its installed code, for example `grep -rn "require('glob')\|from 'glob'" node_modules/<parent>/`

## Apply (step 4)

**Direct dependency**: `npm install <package>@<version>`. Check afterwards that `package.json` records the version the way the project's policy requires. If the project pins exact versions, an `.npmrc` with `save-exact=true` makes `npm install` record them; without it, npm writes a `^` range.

**Transitive dependency**: add or extend an `overrides` block in `package.json`. Write the value on the right the way the project's version policy requires (an exact version such as `14.0.6` when the project pins, which `save-exact` does not enforce for overrides). Only the selector on the left of a key may be a range.

- Use a **range-limited key** when other copies of the package sit outside the vulnerable range and must stay where they are:

  ```json
  "overrides": {
    "minimatch@>=7.0.0 <11.0.1": "11.0.9"
  }
  ```

- Use a **plain key** when only vulnerable copies exist:

  ```json
  "overrides": {
    "glob": "14.0.6"
  }
  ```

- Use a **nested override** to change the package only under one parent:

  ```json
  "overrides": {
    "js-beautify": { "glob": "14.0.6" }
  }
  ```

Known behaviour:

- npm does not always apply a range-limited key. In one project, a range-limited `minimatch` key applied, but the same form for `glob` was silently ignored. Always confirm the result in step 5. If a key did not take effect, tell the user and propose the plain or nested form; switch only after the user agrees.
- If an override does not change an entry that is already locked, delete that package's entries from `package-lock.json` (and its folder in `node_modules`) and run `npm install` again so npm re-resolves it.

Regenerate the lockfile with `npm install --no-audit --no-fund`. Do not hand-edit version numbers in `package-lock.json`.

If the project pins exact versions, confirm that no range slipped in:

```bash
grep -nE '"[\^~]' package.json
```

The command should print nothing (a range selector inside an override key, such as `"minimatch@>=7.0.0 <11.0.1"`, does not match it).

## Verify (step 5)

```bash
npm ls <package> --all
npm audit
```

## Without Dependabot (step 2)

`npm audit --json` lists vulnerable packages with their advisories and the paths that pull them in.

## Checks (step 6)

Start with `npm ci --no-audit --no-fund`, then run the scripts CI runs (see the `scripts` block in `package.json` and the CI workflow). Common ones are lint, type check, build and tests. Browser-based test runners such as Cypress and Playwright can take several minutes and may need a specific browser installed; check the project's notes for a fallback.
