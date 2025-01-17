import { defineConfig } from 'cypress'

export default defineConfig({
  // e2e: {
  //   setupNodeEvents(on, config) {
  //     // implement node event listeners here
  //   },
  // },
  fixturesFolder: 'cypress/fixtures',
  component: {
    devServer: {
      framework: 'vue',
      bundler: 'vite',
    },
    // supportFile: 'cypress/support/component.js',
    supportFile: 'cypress/support/component.ts',
  },
})
