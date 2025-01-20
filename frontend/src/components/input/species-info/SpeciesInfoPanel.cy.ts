import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import SpeciesInfoPanel from './SpeciesInfoPanel.vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { setActivePinia, createPinia } from 'pinia'
import { CONSTANTS, DEFAULTS } from '@/constants'

const vuetify = createVuetify()

describe('SpeciesInfoPanel.vue', () => {
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

    modelParameterStore.panelOpenStates.speciesInfo = CONSTANTS.PANEL.OPEN
    modelParameterStore.panelState.speciesInfo = {
      editable: true,
      confirmed: false,
    }

    modelParameterStore.setDefaultValues()
    // modelParameterStore.speciesList = [
    //   { species: 'PL', percent: '30.0' },
    //   { species: 'AC', percent: '30.0' },
    //   { species: 'H', percent: '30.0' },
    //   { species: 'S', percent: '10.0' },
    //   { species: null, percent: '0.0' },
    //   { species: null, percent: '0.0' },
    // ]
    // modelParameterStore.derivedBy = DEFAULTS.DEFAULT_VALUES.DERIVED_BY
    // modelParameterStore.totalSpeciesPercent = 100

    mount(SpeciesInfoPanel, {
      global: {
        plugins: [vuetify],
      },
      ...overrides,
    })

    return modelParameterStore
  }

  it('renders correctly with initial state', () => {
    mountComponent()

    // cy.get('body').then(($body) => {
    //   cy.log($body.html())
    // })

    // Check the Expansion panel title
    cy.get('.v-expansion-panel-title')
      .contains('Species Information')
      .should('exist')

    // Verify that Species % derived by radio group is rendered
    cy.get('div').contains('Species % derived by:').should('exist')
    cy.get('input[type="radio"]').should('have.length', 2)

    // Check the first Species and Percent values
    cy.get('[data-testid="species-select"] input')
      .first()
      .should('have.value', 'PL')
    cy.get('[data-testid="species-percent"] input')
      .first()
      .should('have.value', '30.0')

    // Verify that Total Species Percent text field is rendered and has the expected value
    cy.get('[data-testid="total-species-percent"] input')
      .should('exist')
      .invoke('val')
      .should('equal', '100.0')

    // Verify that Total Species Percent text field is disabled
    cy.get('[data-testid="total-species-percent"] input')
      .should('exist')
      .and('be.disabled')

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
      .its(
        `panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO}.confirmed`,
      )
      .should('be.true')

    // Verify that the Edit button is now rendered
    cy.get('button').contains('Edit').should('exist')
  })

  // it('shows a validation error for duplicate species', () => {
  //   const store = mountComponent()

  //   // Introduce duplicate species
  //   cy.wrap(store).then(() => {
  //     store.speciesList.push({ species: 'SP1', percent: 10 })
  //   })

  //   // Click the Confirm button
  //   cy.get('button').contains('Confirm').click()

  //   // Ensure the dialog appears with an error message
  //   cy.get('.v-dialog')
  //     .should('exist')
  //     .within(() => {
  //       cy.contains('Duplicate Species Found').should('exist')
  //     })
  // })

  // it('clears the form when Clear is clicked', () => {
  //   const store = mountComponent()

  //   cy.get('button').contains('Clear').click()

  //   // Verify that speciesList is cleared in the store
  //   cy.wrap(store)
  //     .its('speciesList')
  //     .should('deep.equal', [
  //       { species: null, percent: null },
  //       { species: null, percent: null },
  //     ])

  //   // Verify that Total Species Percent is reset
  //   cy.get('input[aria-label="Total Species Percent"]').should('have.value', '')
  // })

  // it('disables inputs and buttons when the panel is not editable', () => {
  //   const store = mountComponent()

  //   // Set panel state to non-editable
  //   cy.wrap(store).then(() => {
  //     store.panelState.speciesInfo.editable = false
  //   })

  //   // Verify that all inputs are disabled
  //   cy.get('input').should('be.disabled')

  //   // Verify that the Clear and Confirm buttons are disabled
  //   cy.get('button:contains("Clear")').should('be.disabled')
  //   cy.get('button:contains("Confirm")').should('be.disabled')

  //   // Verify that the Edit button is not visible
  //   cy.get('button').contains('Edit').should('not.be.visible')
  // })
})
