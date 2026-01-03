# VDYP Frontend

This is the VDYP frontend. It implements a Vue frontend with Keycloak
authentication support.

## Getting Started

### 1. Local Development

Start the development server to work on the project locally.

Start Development Server:

```bash
npm run dev
```

Start Development Server in Debug Mode:

```bash
npm run dev-debug
```

### 2. Code Quality & Validation

Ensure code quality and type safety before building.

Run Type Checking:

```bash
npm run type-check
```

Lint and Fix Files:

```sh
npm run lint
```

### 3. Building the Project

Prepare the project for production or preview.

Build the Project (with Type Checking):

```bash
npm run build
```

Build the Project Only (without Type Checking):

```bash
npm run build-only
```

Preview the Built Project:

```bash
npm run preview
```

### 4. Testing

Run various tests to validate the application.

Open Component Tests (Interactive Mode):

```bash
npm run test:comp-open
```

Run Component Tests:

```bash
npm run test:comp
```

Run Unit Tests:

```bash
npm run test:unit
```

To run only a specific test file:

```bash
npx cypress run --component --spec "src/utils/util.cy.ts"
```

Open E2E Tests (Interactive Mode):

```bash
npm run test:e2e-open
```

### 5. Storybook

Develop and document UI components using Storybook.

Run Storybook Development Server:

```bash
npm run storybook
```
