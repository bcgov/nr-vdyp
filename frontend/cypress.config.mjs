import { defineConfig } from 'cypress'

export default defineConfig({
  viewportWidth: 1024,
  viewportHeight: 768,
  e2e: {
    setupNodeEvents(on, config) {
      on('task', {
        log(message) {
          console.log(message)
          return null
        },
      })
      return config
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
    setupNodeEvents(on, config) {
      on('task', {
        log(message) {
          console.log(message)
          return null
        },
      })
      return config
    },
  },
})
