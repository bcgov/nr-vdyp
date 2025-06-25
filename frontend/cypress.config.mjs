import { defineConfig } from 'cypress'

export default defineConfig({
  viewportWidth: 1024,
  viewportHeight: 768,
  e2e: {
    // eslint-disable-next-line no-unused-vars
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
    specPattern: ['cypress/e2e/**/*.cy.{js,ts}'],
    trashAssetsBeforeRuns: false,
  },
  component: {
    devServer: {
      framework: 'vue',
      bundler: 'vite',
    },
    fixturesFolder: 'cypress/fixtures',
    supportFile: 'cypress/support/component.ts',
    // defaultCommandTimeout: 10000,
    // eslint-disable-next-line no-unused-vars
    setupNodeEvents(on, config) {
      on('task', {
        log(message) {
          console.log(message)
          return null
        },
      })
    },
  },
})
