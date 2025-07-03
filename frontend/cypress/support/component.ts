import './commands'

import { mount as cypressMount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { VApp } from 'vuetify/components'
import { createPinia } from 'pinia'
import { h } from 'vue'
import type { MountingOptions } from '@vue/test-utils'

import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import '@bcgov/bc-sans/css/BCSans.css'
import '../../src/styles/style.scss'

const vuetify = createVuetify({
  components,
  directives,
})

const pinia = createPinia()

// Custom mount function
const customMount = (component: any, options: any = {}) => {
  options.global = options.global || {}
  options.global.plugins = options.global.plugins || []
  options.global.plugins.push(vuetify)
  options.global.plugins.push(pinia)

  return cypressMount(
    {
      render() {
        return h(VApp, {}, [h(component, options.props)])
      },
    },
    options,
  )
}

// Cypress command: 'mount'
Cypress.Commands.add('mount', customMount)

// Cypress command: 'mountWithVuetify'
function mountWithVuetify(component: any, options: any = {}) {
  options.global = options.global || {}
  options.global.plugins = options.global.plugins || []
  options.global.plugins.push(vuetify)

  return cypressMount(component, options)
}

// Type declaration for Cypress commands
declare global {
  namespace Cypress {
    interface Chainable {
      mount(
        component: any,
        options?: Partial<MountingOptions<any>> & { log?: boolean },
      ): Chainable<any>
      mountWithVuetify(
        component: any,
        options?: Partial<MountingOptions<any>> & { log?: boolean },
      ): Chainable<any>
    }
  }
}

Cypress.Commands.add('mountWithVuetify', mountWithVuetify)
