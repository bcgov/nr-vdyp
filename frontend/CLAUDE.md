# CLAUDE.md (frontend)

Guidance for work in the VDYP web UI. Read the root `CLAUDE.md` first; this file adds frontend-specific detail. This project is built with npm and is not part of the Maven build.

## Stack

- Vue 3 with `<script setup lang="ts">`, TypeScript, Vite.
- Vuetify 3 for components, styled with BC Gov design tokens (`@bcgov/design-tokens`, `@bcgov/bc-sans`) and Material Design Icons.
- Pinia for state, Vue Router for routing, Axios for HTTP, `keycloak-js` for authentication.
- Cypress for component, unit-style and end-to-end tests. Storybook is available.
- Node 22 in CI (`.github/workflows/frontend-ci.yml`).
- Served in containers by Caddy (`Caddyfile`, `Dockerfile`).

## Code layout (`src/`)

| Path | Contents |
| --- | --- |
| `main.ts`, `App.vue`, `layouts/`, `plugins/` | Bootstrap, root layout, Vuetify plugin setup. |
| `router/` | Routes and navigation guards. |
| `views/` | Page-level components (`ProjectionListView`, `ProjectionDetail`, `AdminDashboardView`, and others). |
| `components/` | `common`, `core` (`App*` building blocks), `layout`, and `projection` (`input`, `list`, `reporting`). |
| `stores/` | Pinia stores: `common` (auth, alerts, notifications, unsaved changes), `projection` (app, file upload, model parameters, projection), and `reportingStore`. |
| `services/` | API access: `axiosInstance`, `apiClient`, `apiActions`, `apiErrorHandler`, domain services, `keycloak`. |
| `services/vdyp-api/` | OpenAPI Generator style TypeScript client for the backend (`apis/`, `models/`). |
| `validation/` | Input validators and validation rules for each form section. |
| `composables/`, `utils/`, `constants/`, `interfaces/`, `types/`, `styles/` | Shared logic, constants and messages, types, SCSS overrides. |

## Runtime configuration

`src/env.ts` merges `import.meta.env` with `window.config`. In containers, Caddy serves `window.config` built from environment variables (`VITE_SSO_*`, `VITE_API_URL`, `BACKEND_URL`, and others; see `Caddyfile`). Read configuration through `env`, not `import.meta.env` directly, so both local and deployed builds work. Add new variables to the `Caddyfile` and the Helm values when needed.

## Commands

```bash
npm ci
npm run dev            # Vite dev server
npm run type-check     # vue-tsc
npm run lint           # ESLint with --fix
npm run lint:check     # ESLint, as run in CI
npm run format         # Prettier on src/
npm run test:unit      # Cypress tests for services, stores, composables, validation, utils
npm run test:comp      # Cypress component tests under src/components
npx cypress run --component --spec "src/utils/util.cy.ts"   # single spec
npm run build
```

Before finishing a change, run `lint:check`, `type-check`, `test:unit` and `test:comp`. These are the CI checks.

Known problems running the checks locally:

- `test:unit` runs Cypress with `--browser chrome`. If Chrome is not installed, run the same specs without the browser flag so Cypress uses Electron:

  ```bash
  npx cypress run --component --spec "src/services/**/*.cy.ts","src/stores/**/*.cy.ts","src/composables/**/*.cy.ts","src/validation/*.cy.ts","src/utils/*.cy.ts"
  ```

- Do not use Edge as a fallback: Cypress has failed with it in this repository with an internal error ("* is not a valid URL").
- The Cypress runs take several minutes each. Write the full output to a log file rather than filtering it through `grep`, so the reason for a non-zero exit is not hidden.

## Code style

- Prettier (`.prettierrc.yml`): two spaces, single quotes, no semicolons, trailing commas, LF line endings. This differs from the tab indentation used in the Java code.
- Use `<script setup lang="ts">` and typed props and emits for new components.
- Tests are colocated with the code as `*.cy.ts` files. Add or update the matching spec when changing a service, store, validator, utility or component.
- Put user-facing message text in `constants/` (for example `message.ts`) rather than inline strings.

## Rules for changes

- Pin every version in `package.json` to an exact version (`"vite": "6.4.5"`), including `dependencies`,
  `devDependencies` and `overrides`. Do not use range prefixes such as `^` or `~`, so that a new upstream release cannot
  change or break the build without a deliberate update. `frontend/.npmrc` sets `save-exact=true`, so
  `npm install <package>@<version>` records an exact version; keep that setting. It does not cover hand edits or
  `overrides`, so check those yourself. The selector on the left of an override key may be a range (
  `"minimatch@>=7.0.0 <11.0.1"`), but the version it resolves to must be exact.
- Keep API calls in `services/` and state in stores. Components should not call Axios directly.
- `services/vdyp-api/` must match the backend API. When a backend endpoint or model changes, update this client in the same change.
- Business rules for inputs (ranges, defaults, options) live in `constants/` and `validation/`. Keep them consistent with the backend validation in `vdyp-extended-core`.
- Use Vuetify components and the existing `App*` wrappers in `components/core` before creating new base components. Follow BC Gov design guidance for visual changes.
