import { mount } from 'cypress/vue'
import AppRunModelButtonPanel from './AppRunModelButtonPanel.vue'
import AppButton from '../core/AppButton.vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'

describe('AppRunModelButtonPanel.vue', () => {
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
    mount(AppRunModelButtonPanel, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
      },
    })

    // Check if the button is rendered with the correct label
    cy.get('button.bcds-button').should('contain.text', 'Run Model')
  })

  it('renders the button with primary variant', () => {
    mount(AppRunModelButtonPanel, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
      },
    })

    // Check if the button has the primary variant class
    cy.get('button.bcds-button').should('have.class', 'primary')
  })

  it('emits the "runModel" event when clicked', () => {
    const onRunModelSpy = cy.spy().as('runModelSpy')

    mount(AppRunModelButtonPanel, {
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
    cy.get('button.bcds-button').click()

    // Verify that the "runModel" event is emitted
    cy.get('@runModelSpy').should('have.been.calledOnce')
  })

  it('disables the button when "isDisabled" is true', () => {
    mount(AppRunModelButtonPanel, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: true,
      },
    })

    // Check if the button is disabled
    cy.get('button.bcds-button').should('be.disabled')
  })

  it('enables the button when "isDisabled" is false', () => {
    mount(AppRunModelButtonPanel, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
      },
    })

    // Check if the button is enabled
    cy.get('button.bcds-button').should('not.be.disabled')
  })

  it('applies custom card class when provided', () => {
    mount(AppRunModelButtonPanel, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
        cardClass: 'custom-card-class',
      },
    })

    // Check if the custom card class is applied
    cy.get('.custom-card-class').should('exist')
  })

  it('applies default card class when not provided', () => {
    mount(AppRunModelButtonPanel, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
      },
    })

    // Check if the default card class is applied
    cy.get('.file-upload-run-model-card').should('exist')
  })

  it('applies custom card actions class when provided', () => {
    mount(AppRunModelButtonPanel, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
        cardActionsClass: 'custom-actions-class',
      },
    })

    // Check if the custom card actions class is applied
    cy.get('.custom-actions-class').should('exist')
  })

  it('applies default card actions class when not provided', () => {
    mount(AppRunModelButtonPanel, {
      global: {
        plugins: [vuetify],
        components: { AppButton },
      },
      props: {
        isDisabled: false,
      },
    })

    // Check if the default card actions class is applied
    cy.get('.card-actions').should('exist')
  })
})
