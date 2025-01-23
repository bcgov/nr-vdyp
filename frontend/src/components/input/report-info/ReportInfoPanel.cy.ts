import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ReportInfoPanel from './ReportInfoPanel.vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { setActivePinia, createPinia } from 'pinia'
import { CONSTANTS, MESSAGE } from '@/constants'

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
        cy.wrap(input).clear()
        cy.wrap(input).type('200')
      })

    cy.get('[id="finishingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('50')
      })

    // Click the Confirm button
    cy.get('button').contains('Confirm').click()

    // Ensure the dialog appears with an error message
    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_FNSH_AGE).should(
          'exist',
        )
      })
  })

  it('shows validation error when starting age is out of range', () => {
    mountComponent()

    // Enter a value less than the minimum
    cy.get('[id="startingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('-1')
      })

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_AGE_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX,
          ),
        ).should('exist')
      })
  })

  it('shows validation error when finishing age is out of range', () => {
    mountComponent()

    // Enter a value greater than the maximum
    cy.get('[id="finishingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('1001')
      })

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_FNSH_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
          ),
        ).should('exist')
      })
  })

  it('shows validation error when age increment is out of range', () => {
    mountComponent()

    // Enter a value greater than the maximum
    cy.get('[id="ageIncrement"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('501')
      })

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_AGE_INC_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX,
          ),
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
