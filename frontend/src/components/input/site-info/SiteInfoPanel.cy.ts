import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import SiteInfoPanel from './SiteInfoPanel.vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { setActivePinia, createPinia } from 'pinia'
import { CONSTANTS } from '@/constants'

const vuetify = createVuetify()

describe('SiteInfoPanel.vue', () => {
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
    modelParameterStore.panelOpenStates.siteInfo = CONSTANTS.PANEL.OPEN
    modelParameterStore.panelState.siteInfo = {
      editable: true,
      confirmed: false,
    }
    modelParameterStore.becZone = 'Zone A'
    modelParameterStore.ecoZone = 'Eco A'
    modelParameterStore.incSecondaryHeight = true
    modelParameterStore.selectedSiteSpecies = 'Species A'
    modelParameterStore.siteSpeciesValues = 'Value A'
    modelParameterStore.bha50SiteIndex = '10'

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
    cy.get('.v-select__selection-text').contains('Zone A').should('exist')

    // Verify that Eco Zone select is rendered and has the expected value
    cy.get('label').contains('Eco Zone').should('exist')
    cy.get('.v-select__selection-text').contains('Eco A').should('exist')

    // Verify that the checkbox is rendered and checked
    cy.get('label')
      .contains('Include Secondary Dominant Height in Yield Table')
      .should('exist')
    cy.get('input[type="checkbox"]').should('be.checked')

    // Verify that the AppPanelActions buttons are rendered
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
      .its(`panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO}.confirmed`)
      .should('be.true')

    // Verify that the Edit button is now rendered
    cy.get('button').contains('Edit').should('exist')
  })

  it('shows a validation error for invalid BHA 50 Site Index', () => {
    mountComponent()

    // Set invalid value for BHA 50 Site Index
    cy.get('input#input-17').then((input) => {
      // Clear the input value
      cy.wrap(input).clear()

      // Type a new invalid value
      cy.wrap(input).type('200')
    })

    // Click the Confirm button
    cy.get('button').contains('Confirm').click()

    // Ensure the dialog appears with an error message
    cy.get('.v-dialog')
      .should('exist')
      .and('be.visible') // Wait until you see the dialog
      .within(() => {
        cy.contains('Invalid Input').should('exist')
        cy.contains("'Site Index' must range from 0.00 and 60.00").should(
          'exist',
        )
      })
  })

  // cy.get('body').then(($body) => {
  //   cy.log($body.html())
  // })

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
