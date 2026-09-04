import { defineConfig } from 'cypress'
import { chmodSync, writeFileSync } from 'node:fs'

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
        writeZapAccessToken({ filePath, token }) {
          if (
            typeof filePath !== 'string' ||
            filePath.length === 0 ||
            typeof token !== 'string' ||
            token.length === 0
          ) {
            throw new Error('A token and destination path are required.')
          }

          writeFileSync(filePath, token, {
            encoding: 'utf8',
            flag: 'w',
            mode: 0o600,
          })
          chmodSync(filePath, 0o600)
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
