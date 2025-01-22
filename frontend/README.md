# VDYP

Web based user interface for the Variable Density Yield Projection (VDYP).

## Getting Started

## Local Development

To start the development server, run the following command:

```bash
npm run dev
```

## Cypress Testing

Cypress is used for end-to-end, component testing, unit testing.

### Open Cypress Test Runner

```bash
npx cypress open
```

### Run Component Tests in Specific Browsers (GUI mode)

Run component tests using different browsers with the following commands:

- Chrome:

```bash
npx cypress open --component --browser chrome
```

- Firefox

```bash
npx cypress open --component --browser firefox
```

- Edge

```bash
npx cypress open --component --browser edge
```

#### Use a Specific Config File

To open Cypress with a specific configuration file:

```bash
npx cypress open --config-file cypress.config.mjs
```

### Run Cypress Tests in Headless Mode (CLI mode)

Run tests without opening the Cypress GUI in headless mode.

```bash
npx cypress run --component
```

#### Run Specific Component Tests

Run Cypress component tests for specific files or directories.

- Run all tests in the reporting folder:

```bash
npx cypress run --component --spec "src/components/reporting/*.cy.ts"
```

- Run tests for a specific component:

```bash
npx cypress run --component --spec "src/components/reporting/ReportingContainer.cy.ts"
```

- Run multiple specific tests:

```bash
npx cypress run --component --spec "src/components/reporting/ReportingContainer.cy.ts,src/components/common/AppMessageDialog.vue.cy.ts"
```

### Run End-to-End (E2E) Tests

Execute all end-to-end tests in headless mode.

```bash
npx cypress run --e2e
```

#### Open Cypress Test Runner for E2E in Chrome

Launches the Cypress test runner in interactive mode for E2E tests using the
Chrome browser.

```bash
npx cypress open --e2e --browser chrome
```

## Linting

Linting helps maintain code quality and consistency. To run lint checks, use the
following command:

```bash
npm run lint
```

## Storybook

Storybook is a frontend workshop for building UI components and pages in
isolation. To start Storybook, run the following command:

```bash
npm run storybook
```
