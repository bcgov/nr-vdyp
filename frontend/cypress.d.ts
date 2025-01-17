/// <reference types="cypress" />

import { MountOptions, VueWrapper } from 'cypress/vue'
import { Component } from 'vue'

declare global {
  namespace Cypress {
    interface Chainable {
      mount(component: Component, options?: MountOptions): Chainable<VueWrapper>
    }
  }
}
