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

### Run Component Tests in Specific Browsers

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

### Use a Specific Config File

To open Cypress with a specific configuration file:

```bash
npx cypress open --config-file cypress.config.mjs
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
