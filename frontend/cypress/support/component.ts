import './commands'

import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { VApp } from 'vuetify/components'
import { h, ComponentPublicInstance } from 'vue'
import { createPinia } from 'pinia'
import type { DefineComponent } from 'vue'
import { MountingOptions } from '@vue/test-utils'

import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import '@bcgov/bc-sans/css/BCSans.css'
import '../../src/styles/style.scss'

const vuetify = createVuetify({
  components,
  directives,
})

const pinia = createPinia()

// Cypress command: 'mount'
Cypress.Commands.add('mount', (component: any, options: any = {}) => {
  options.global = options.global || {}
  options.global.plugins = options.global.plugins || []
  options.global.plugins.push(vuetify)
  options.global.plugins.push(pinia)

  return mount(
    {
      render() {
        return h(VApp, {}, [h(component, options.props)])
      },
    },
    options,
  )
})

// Cypress command: 'mountWithVuetify'
function mountWithVuetify<
  T extends DefineComponent<any, any, any>,
  Props = ComponentPublicInstance<T>['$props'],
>(
  component: T,
  options?: Omit<MountingOptions<Props>, 'global'> & {
    global?: Omit<MountingOptions<Props>['global'], 'plugins'> & {
      plugins?: any[]
    }
  },
): Cypress.Chainable<ComponentPublicInstance<T>> {
  return mount(component as any, {
    ...options,
    global: {
      ...options?.global,
      plugins: [...(options?.global?.plugins ?? []), vuetify],
    },
  }) as unknown as Cypress.Chainable<ComponentPublicInstance<T>>
}

declare global {
  namespace Cypress {
    interface Chainable {
      mount: typeof mount
      mountWithVuetify: typeof mountWithVuetify
    }
  }
}

Cypress.Commands.add('mountWithVuetify', mountWithVuetify)
