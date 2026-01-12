import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import SpeciesInfoPanel from './SpeciesInfoPanel.vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { setActivePinia, createPinia } from 'pinia'
import { CONSTANTS, MESSAGE } from '@/constants'

const vuetify = createVuetify()

describe('SpeciesInfoPanel.vue', () => {
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

    // Check the second Species and Percent values
    cy.get('[data-testid="species-select"] input')
      .eq(1)
      .should('have.value', 'AC')
    cy.get('[data-testid="species-percent"] input')
      .eq(1)
      .should('have.value', '30.0')

    // Check the third Species and Percent values
    cy.get('[data-testid="species-select"] input')
      .eq(2)
      .should('have.value', 'H')
    cy.get('[data-testid="species-percent"] input')
      .eq(2)
      .should('have.value', '30.0')

    // Check the fourth Species and Percent values
    cy.get('[data-testid="species-select"] input')
      .eq(3)
      .should('have.value', 'S')
    cy.get('[data-testid="species-percent"] input')
      .eq(3)
      .should('have.value', '10.0')

    // Check the fifth Species and Percent values
    cy.get('[data-testid="species-select"] input')
      .eq(4)
      .should('have.value', '')
    cy.get('[data-testid="species-percent"] input')
      .eq(4)
      .should('have.value', '0.0')

    // Check the sixth Species and Percent values
    cy.get('[data-testid="species-select"] input')
      .eq(5)
      .should('have.value', '')
    cy.get('[data-testid="species-percent"] input')
      .eq(5)
      .should('have.value', '0.0')

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
    cy.get('button').contains('Confirm').should('exist').and('be.visible')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('changes to Confirm state and renders the Edit button, then clicks Edit button and verifies state', () => {
    const store = mountComponent()

    cy.get('button').contains('Confirm').click()

    // Wait for the Pinia store state to update
    cy.wrap(store)
      .its(
        `panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO}.confirmed`,
      )
      .should('be.true')

    // Verify that the Edit button is now visible
    cy.get('button').contains('Edit').should('be.visible')
    cy.get('button').contains('Confirm').should('not.be.visible')

    cy.get('button').contains('Edit').click()

    // Wait for the Pinia store state to update
    cy.wrap(store)
      .its(
        `panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO}.confirmed`,
      )
      .should('be.false')

    // Verify that the Edit button is not visible anymore
    cy.get('button').contains('Edit').should('not.be.visible')

    // Verify that the Confirm button is now visible again
    cy.get('button').contains('Confirm').should('be.visible')
  })

  it('shows a validation error for total species percent', () => {
    const store = mountComponent()

    // Set invalid Total Species Percent by modifying species percent values
    cy.wrap(store).then(() => {
      store.speciesList = [
        { species: 'PL', percent: '40.0' },
        { species: 'AC', percent: '40.0' },
        { species: 'H', percent: '30.0' }, // Total exceeds 100.0
      ]
    })

    cy.get('button').contains('Confirm').click()

    // Wait for and verify the validation dialog appears
    cy.get('.v-dialog', { timeout: 10000 }).should('be.visible')
    cy.get('.v-dialog').within(() => {
      cy.contains(MESSAGE.MSG_DIALOG_TITLE.DATA_INCOMPLETE).should('exist')
      cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_TOTAL_PCT).should(
        'exist',
      )
    })

    // Click the button to close dialog
    cy.get('button').contains(CONSTANTS.BUTTON_LABEL.CONT_EDIT).click()

    // Wait for dialog to close - check that it's not visible
    cy.get('.v-dialog').should('not.be.visible')

    // Reset the store to default values after clicking Continue Editing
    cy.wrap(store).then(() => {
      store.setDefaultValues()
    })

    // Verify that values have been reset to defaults
    cy.get('[data-testid="species-select"] input')
      .first()
      .should('have.value', 'PL')
    cy.get('[data-testid="species-percent"] input')
      .first()
      .should('have.value', '30.0')
    cy.get('[data-testid="total-species-percent"] input')
      .should('exist')
      .invoke('val')
      .should('equal', '100.0')
  })

  it('shows a validation error for required fields', () => {
    const store = mountComponent()

    // Clear the derivedBy value to trigger validation error
    cy.wrap(store).then(() => {
      store.derivedBy = null
    })

    cy.get('button').contains('Confirm').click()

    // Wait for and verify the validation dialog appears
    cy.get('.v-dialog', { timeout: 10000 }).should('be.visible')
    cy.get('.v-dialog').within(() => {
      cy.contains(MESSAGE.MSG_DIALOG_TITLE.MISSING_INFO).should('exist')
      cy.contains(
        MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_MISSING_DERIVED_BY,
      ).should('exist')
    })

    // Click the button to close dialog
    cy.get('button').contains(CONSTANTS.BUTTON_LABEL.CONT_EDIT).click()

    // Wait for dialog to close - check that it's not visible
    cy.get('.v-dialog').should('not.be.visible')

    // Reset the store to default values after clicking Continue Editing
    cy.wrap(store).then(() => {
      store.setDefaultValues()
    })
  })

  it('shows a validation error for duplicate species', () => {
    const store = mountComponent()

    // Introduce duplicate species
    cy.wrap(store).then(() => {
      store.speciesList.push({ species: 'PL', percent: '10.0' })
    })

    cy.get('button').contains('Confirm').click()

    // Wait for and verify the dialog appears with error message
    cy.get('.v-dialog', { timeout: 10000 }).should('be.visible')
    cy.get('.v-dialog').within(() => {
      cy.contains(
        MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_DUP_WO_LABEL(
          'PL - Lodgepole Pine',
        ),
      ).should('exist')
    })
  })

  it('clears the form when Clear is clicked', () => {
    mountComponent()

    cy.get('button').contains('Clear').click()

    // Verify that all species selects are cleared
    cy.get('[data-testid="species-select"] input').each(($input) => {
      cy.wrap($input).should('have.value', '')
    })

    // Verify that Total Species Percent is reset
    cy.get('[data-testid="total-species-percent"] input')
      .should('exist')
      .invoke('val')
      .should('equal', '0.0')
  })

  it('disables inputs and buttons when the panel is not editable', () => {
    const store = mountComponent()

    // Set panel state to non-editable
    cy.wrap(store).then(() => {
      store.panelState.speciesInfo.editable = false
    })

    // Verify that the store state has been updated
    cy.wrap(store)
      .its('panelState.speciesInfo.editable')
      .should('be.false')

    // Verify that the Clear and Confirm buttons are disabled
    cy.contains('button', 'Clear').should('be.disabled')
    cy.contains('button', 'Confirm').should('be.disabled')

    // Verify that the Edit button is not visible
    cy.get('button').contains('Edit').should('not.be.visible')
  })
})
