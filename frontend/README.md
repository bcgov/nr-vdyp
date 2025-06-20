# VDYP Frontend

This is the VDYP frontend. It implements a Vue frontend with Keycloak
authentication support.

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

```bash
npm run storybook
```

### Lints and fixes files

```sh
npm run lint
```
