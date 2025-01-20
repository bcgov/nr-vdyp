import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ReportInfoPanel from './ReportInfoPanel.vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { setActivePinia, createPinia } from 'pinia'
import { CONSTANTS } from '@/constants'

const vuetify = createVuetify()

describe('ReportInfoPanel.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia()) // Initialize the Pinia store
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

    modelParameterStore.setDefaultValues()

    // Assume Species Info, Site Info, Stand Density are verified
    modelParameterStore.confirmPanel(
      CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
    )
    modelParameterStore.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO)
    modelParameterStore.confirmPanel(
      CONSTANTS.MODEL_PARAMETER_PANEL.STAND_DENSITY,
    )

    modelParameterStore.startingAge = 10
    modelParameterStore.finishingAge = 100
    modelParameterStore.ageIncrement = 5
    modelParameterStore.volumeReported = [CONSTANTS.VOLUME_REPORTED.WHOLE_STEM]
    modelParameterStore.includeInReport = [
      CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI,
    ]
    modelParameterStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
    modelParameterStore.reportTitle = 'Sample Report'

    mount(ReportInfoPanel, {
      global: {
        plugins: [vuetify],
      },
      ...overrides,
    })

    return modelParameterStore
  }

  it('renders correctly with initial state', () => {
    mountComponent()

    // Check the Expansion panel title
    cy.get('.v-expansion-panel-title')
      .contains('Report Information')
      .should('exist')

    // Verify that Starting Age input is rendered and has the expected value
    cy.get('[id="startingAge"]')
      .should('exist')
      .and('have.value', '10')
      .and('have.attr', 'type', 'number')

    // Verify that Finishing Age input is rendered and has the expected value
    cy.get('[id="finishingAge"]')
      .should('exist')
      .and('have.value', '100')
      .and('have.attr', 'type', 'number')

    // Verify that Age Increment input is rendered and has the expected value
    cy.get('[id="ageIncrement"]')
      .should('exist')
      .and('have.value', '5')
      .and('have.attr', 'type', 'number')

    // Verify that the Clear and Confirm buttons are rendered
    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('changes to Confirm state and renders the Edit button', () => {
    const store = mountComponent()

    // Click the Confirm button
    cy.get('button').contains('Confirm').click()

    // Wait for the Pinia store state to update
    cy.wrap(store)
      .its(
        `panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO}.confirmed`,
      )
      .should('be.true')

    // Verify that the Edit button is now rendered
    cy.get('button').contains('Edit').should('exist')
  })

  it('shows a validation error for invalid starting and finishing ages', () => {
    mountComponent()

    // Set invalid values for Starting Age and Finishing Age
    cy.get('[id="startingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear() // Clear the input value
        cy.wrap(input).type('200') // Type a new invalid value
      })

    cy.get('[id="finishingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear() // Clear the input value
        cy.wrap(input).type('50') // Type a new invalid value
      })

    // Click the Confirm button
    cy.get('button').contains('Confirm').click()

    // Ensure the dialog appears with an error message
    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains('Invalid Input').should('exist')
        cy.contains(
          "'Finish Age' must be at least as great as the 'Start Age'",
        ).should('exist')
      })
  })

  it('clears the form when Clear is clicked', () => {
    mountComponent()

    cy.get('button').contains('Clear').click()

    // Verify that all fields are cleared
    cy.get('[id="startingAge"]').should('exist').should('have.value', '')
    cy.get('[id="finishingAge"]').should('exist').should('have.value', '')
    cy.get('[id="ageIncrement"]').should('exist').should('have.value', '')
    cy.get('[id="reportTitle"]').should('exist').should('have.value', '')
  })

  it('disables inputs and buttons when the panel is not editable', () => {
    const store = mountComponent()

    // Set panel state to non-editable
    cy.wrap(store).then(() => {
      store.panelState.reportInfo.editable = false
    })

    // Verify that all inputs are disabled
    cy.get('input[type="number"]').should('be.disabled')
    cy.get('button:contains("Clear")').should('be.disabled')
    cy.get('button:contains("Confirm")').should('be.disabled')

    // Verify that the Edit button is not visible
    cy.get('button').contains('Edit').should('not.be.visible')
  })
})
