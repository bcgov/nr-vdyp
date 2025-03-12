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

### Run Component Tests in Specific Browser (GUI mode)

Run component tests using chrome browser with the following command:

- Chrome:

```bash
npx cypress open --component --browser chrome
```

### Run Cypress Tests in Headless Mode (CLI mode)

Execute all component tests in headless mode.

```bash
npx cypress run --component
```

Run all component tests in the src/components directory:

```bash
npx cypress run --component --spec "src/components/**/*.cy.ts"
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

### Run End-to-End (E2E) Tests (CLI mode)

Execute all end-to-end tests in headless mode.

```bash
npx cypress run --e2e
```

Execute all tests in the unit directory:

```bash
npx cypress run --e2e --spec "cypress/e2e/unit/**/*.cy.ts"
```

#### Open Cypress Test Runner for E2E in Chrome

Launches the Cypress test runner in interactive mode for E2E tests using the
Chrome browser.

```bash
npx cypress open --e2e --browser chrome
```

#### Run Specific E2E Tests

Run Cypress E2E tests for specific files or directories.

- Run all tests in the reporting folder:

```bash
npx cypress run --e2e --spec "cypress/e2e/unit/validation/*.cy.ts"
```

- Run tests for a specific component:

```bash
npx cypress run --e2e --spec "cypress/e2e/unit/validation/speciesInfoValidation.cy.ts"
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
