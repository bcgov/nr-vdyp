import { mount } from 'cypress/vue'
import AppRunModelButton from './AppRunModelButton.vue'
import AppButton from '../core/AppButton.vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'

describe('AppRunModelButton.vue', () => {
  const vuetify = createVuetify()

  beforeEach(() => {
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        body {
          background-color: rgb(240, 240, 240) !important;
        }
      `
      doc.head.appendChild(style)
    })
  })

  it('renders the button with the correct label', () => {
    mount(AppRunModelButton, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
      },
    })

    // Check if the button is rendered with the correct label
    cy.get('.blue-btn').should('contain.text', 'Run Model')
  })

  it('emits the "runModel" event when clicked', () => {
    const onRunModelSpy = cy.spy().as('runModelSpy')

    mount(AppRunModelButton, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
      },
      attrs: {
        onRunModel: onRunModelSpy,
      },
    })

    // Click the button
    cy.get('.blue-btn').click()

    // Verify that the "runModel" event is emitted
    cy.get('@runModelSpy').should('have.been.calledOnce')
  })

  it('disables the button when "isDisabled" is true', () => {
    mount(AppRunModelButton, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: true,
      },
    })

    // Check if the button is disabled
    cy.get('.blue-btn').should('be.disabled')
  })

  it('enables the button when "isDisabled" is false', () => {
    mount(AppRunModelButton, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
      },
    })

    // Check if the button is enabled
    cy.get('.blue-btn').should('not.be.disabled')
  })
})
