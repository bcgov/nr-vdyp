# VDYP

Web based user interface for the Variable Density Yield Projection (VDYP).

## Getting Started

## Local Development

To start the development server, run the following command:

```bash
npm run dev
```

To start the development server in debug mode, run the following command:

```bash
npm run dev-debug
```

## Cypress Testing

Cypress is used for end-to-end, component testing, unit testing.

### Run Component Tests

```bash
npx cypress open --component --browser chrome
```

```bash
npx cypress run --component --spec "src/components/**/*.cy.ts"
```

### Run E2E Tests

```bash
npx cypress open --e2e --browser chrome
```

```bash
npx cypress run --e2e --spec "cypress/e2e/unit/**/*.cy.ts"
```

## Storybook

Storybook is a frontend workshop for building UI components and pages in
isolation. To start Storybook, run the following command:

```bash
npm run storybook
```
