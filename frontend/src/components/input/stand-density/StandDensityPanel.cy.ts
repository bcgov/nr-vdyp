import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import StandDensityPanel from './StandDensityPanel.vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { setActivePinia, createPinia } from 'pinia'
import { CONSTANTS } from '@/constants'

const vuetify = createVuetify()

describe('StandDensityPanel.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia()) // Initializing the Pinia store
    cy.viewport(1024, 768)

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

  const mountComponent = (overrides = {}) => {
    const modelParameterStore = useModelParameterStore()
    modelParameterStore.panelOpenStates.standDensity = CONSTANTS.PANEL.OPEN
    modelParameterStore.panelState.standDensity = {
      editable: true,
      confirmed: false,
    }
    modelParameterStore.percentStockableArea = 50

    mount(StandDensityPanel, {
      global: {
        plugins: [vuetify],
      },
      ...overrides,
    })

    return modelParameterStore
  }

  it('renders correctly with initial state', () => {
    mountComponent()

    // Checking the Expansion panel status
    cy.get('.v-expansion-panel-title').contains('Stand Density').should('exist')

    // Verify that the % Stockable Area text field is rendered
    cy.get('label')
      .contains('% Stockable Area')
      .should('exist')
      .and('have.attr', 'for', 'input-1')

    // Verify that the input field is rendered and has the expected value
    cy.get('input#input-1')
      .should('exist')
      .and('have.value', '50')
      .and('have.attr', 'type', 'number')
      .and('have.attr', 'max', '100')
      .and('have.attr', 'min', '0')
      .and('have.attr', 'step', '5')

    // Verify that the AppPanelActions buttons are rendered
    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('changes to Confirm state and renders the Edit button', () => {
    mountComponent()

    // Click the Confirm button
    cy.get('button').contains('Confirm').click()

    // Wait for the Pinia store state to update
    cy.wrap(useModelParameterStore())
      .its(
        `panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.STAND_DENSITY}.confirmed`,
      )
      .should('be.true')

    // Verify that the Edit button is now rendered
    cy.get('button').contains('Edit').should('exist')
  })

  it('shows a validation error for invalid % Stockable Area', () => {
    mountComponent()

    // Setting % Stockable Area to an invalid value
    cy.get('input[type="number"]').then((input) => {
      // Clear the input value
      cy.wrap(input).clear()

      // Type a new invalid value
      cy.wrap(input).type('200')
    })

    // Click the Confirm button
    cy.get('button').contains('Confirm').click()

    // Ensure the dialog appears and contains the error message
    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains('Invalid Input').should('exist')
        cy.contains(
          "'Percent Stockable Area' must range from 0 and 100",
        ).should('exist')
      })
  })

  it('clears the form when Clear is clicked', () => {
    mountComponent()

    cy.get('button').contains('Clear').click()

    // Verify that % Stockable Area is initialized
    cy.get('input[type="number"]').should('have.value', '')
  })

  it('disables inputs and buttons when the panel is not editable', () => {
    const store = mountComponent()

    // Ensure the panel state is set to non-editable
    cy.wrap(store).then(() => {
      store.panelState.standDensity.editable = false
    })

    // Verify that the % Stockable Area text field is disabled
    cy.get('input[type="number"]').should('be.disabled')

    // Verify that the Clear and Confirm buttons are disabled
    cy.get('button:contains("Clear")')
      .should('have.class', 'v-btn--disabled')
      .and('have.attr', 'disabled')
    cy.get('button:contains("Confirm")')
      .should('have.class', 'v-btn--disabled')
      .and('have.attr', 'disabled')

    // Verify that the Edit button is not visible
    cy.get('button').contains('Edit').should('not.be.visible')
  })
})
