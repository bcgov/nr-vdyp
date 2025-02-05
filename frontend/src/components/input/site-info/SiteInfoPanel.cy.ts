import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import SiteInfoPanel from './SiteInfoPanel.vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { setActivePinia, createPinia } from 'pinia'
import { CONSTANTS, DEFAULTS, MESSAGE } from '@/constants'

const vuetify = createVuetify()

describe('SiteInfoPanel.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
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

    // Assume Species Info is verified
    modelParameterStore.confirmPanel(
      CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
    )

    modelParameterStore.ecoZone = '1' // 'Boreal Cordillera'

    mount(SiteInfoPanel, {
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
      .contains('Site Information')
      .should('exist')

    // Verify that BEC Zone select is rendered and has the expected value
    cy.get('label').contains('BEC Zone').should('exist')
    cy.get('.v-select__selection-text')
      .contains(DEFAULTS.DEFAULT_VALUES.BEC_ZONE)
      .should('exist')

    // Verify that Eco Zone select is rendered and has the expected value
    cy.get('label').contains('Eco Zone').should('exist')
    cy.get('.v-select__selection-text')
      .contains('Boreal Cordillera')
      .should('exist')

    // Verify that the checkbox is rendered and checked
    cy.get('label')
      .contains('Include Secondary Dominant Height in Yield Table')
      .should('exist')
    cy.get('input[type="checkbox"]').should('be.not.checked')

    // Verify that the AppPanelActions buttons are rendered
    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('changes to Confirm state and renders the Edit button', () => {
    const store = mountComponent()

    // Set valid value for BHA 50 Site Index
    cy.get('[data-testid="bha-50-site-index"] input').then((input) => {
      cy.wrap(input).clear()
      cy.wrap(input).type('19.2')
    })

    cy.get('button').contains('Confirm').click()

    // Wait for the Pinia store state to update
    cy.wrap(store)
      .its(`panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO}.confirmed`)
      .should('be.true')

    // Verify that the Edit button is now rendered
    cy.get('button').contains('Edit').should('exist')
  })

  it('should display the correct site species value', () => {
    mountComponent()

    cy.get('[data-testid="bha-50-site-index"] input').clear()

    cy.get('[data-testid="selected-site-species"] input')
      .invoke('val')
      .then((value) => {
        const siteSpeciesValue = value as string | null

        cy.get('button').contains('Confirm').click()

        cy.get('.v-dialog')
          .should('exist')
          .within(() => {
            cy.contains(MESSAGE.MSG_DIALOG_TITLE.MISSING_INFO).should('exist')
            cy.contains(
              MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_SI_VAL(
                siteSpeciesValue,
              ),
            ).should('exist')
          })
      })
  })

  it('shows a validation error for invalid BHA 50 Site Index', () => {
    mountComponent()

    // Set invalid value for BHA 50 Site Index
    cy.get('[data-testid="bha-50-site-index"] input').then((input) => {
      // Clear the input value
      cy.wrap(input).clear()

      // Type a new invalid value
      cy.wrap(input).type('200')
    })

    cy.get('button').contains('Confirm').click()

    // Ensure the dialog appears with an error message
    cy.get('.v-dialog')
      .should('exist')
      .and('be.visible') // Wait until you see the dialog
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SI_RNG).should('exist')
      })
  })

  it('clears the form when Clear is clicked', () => {
    const store = mountComponent()

    cy.get('button').contains('Clear').click()

    // Check for Pinia store updates
    cy.wrap(store).its('becZone').should('eq', 'IDF')

    // Check for UI updates
    cy.get('.v-select__selection-text')
      .first()
      .should('have.text', 'IDF - Interior Douglas Fir')
  })

  it('disables inputs and buttons when the panel is not editable', () => {
    const store = mountComponent()

    // Set panel state to non-editable
    cy.wrap(store).then(() => {
      store.panelState.siteInfo.editable = false
    })

    // Verify that all inputs and buttons are disabled
    cy.get('input').should('be.disabled')
    cy.get('button:contains("Clear")').should('be.disabled')
    cy.get('button:contains("Confirm")').should('be.disabled')

    // Verify that the Edit button is not visible
    cy.get('button').contains('Edit').should('not.be.visible')
  })
})
