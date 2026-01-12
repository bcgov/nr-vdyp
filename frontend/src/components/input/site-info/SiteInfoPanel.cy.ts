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

    modelParameterStore.ecoZone = '12' // 'Boreal Cordillera'

    // Explicitly set initial panel state to unconfirmed for initial state test
    modelParameterStore.panelState[
      CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO
    ].confirmed = false
    modelParameterStore.panelState[
      CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO
    ].editable = true

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

    // Check the Expansion panel title and icon
    cy.get('.v-expansion-panel-title')
      .contains('Site Information')
      .should('exist')
    cy.get('.expansion-panel-icon').should('exist')

    // Verify BEC Zone and Eco Zone selects
    cy.get('label').contains('BEC Zone').should('exist')
    cy.get('.v-select__selection-text')
      .contains(DEFAULTS.DEFAULT_VALUES.BEC_ZONE)
      .should('exist')
    cy.get('label').contains('Eco Zone').should('exist')
    cy.get('.v-select__selection-text')
      .contains('Boreal Cordillera')
      .should('exist')

    // Verify Site Species select (disabled)
    cy.get('label').contains('Site Species').should('exist')
    cy.get('[data-testid="selected-site-species"] .v-field').should(
      'have.class',
      'v-field--disabled',
    )

    // Verify Site Index radio group (default: Computed)
    cy.contains('Site Index:').should('exist')
    cy.get('.v-radio-group')
      .first()
      .within(() => {
        cy.get('.v-radio').should('have.length', 2)
        cy.get('.v-selection-control--dirty .v-label')
          .contains('Computed')
          .should('exist') // Default selected
      })

    // Verify Age Years radio group (default: Total)
    cy.contains('Age Years:').should('exist')
    cy.get('.v-radio-group')
      .eq(1)
      .within(() => {
        cy.get('.v-radio').should('have.length', 2)
        cy.get('.v-selection-control--dirty .v-label')
          .contains('Total')
          .should('exist') // Default selected
      })

    // Verify input fields
    cy.contains('Years').should('exist')
    cy.get('[data-testid="spz-age"] input').should('have.value', '60')
    cy.contains('Height in Meters').should('exist')
    cy.get('[data-testid="spz-height"] input').should('have.value', '17.00')
    cy.contains('BHA 50 Site Index').should('exist')
    cy.get('[data-testid="bha-50-site-index"] input').should('have.value', '')
    // Verify AppPanelActions buttons (Edit should not be visible in initial state)
    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('changes to Confirm state and renders the Edit button in Computed mode', () => {
    const store = mountComponent()

    // Default is Computed mode, set valid values for spzAge and spzHeight
    cy.get('[data-testid="spz-age"] input').clear()
    cy.get('[data-testid="spz-age"] input').type('50')

    cy.get('[data-testid="spz-height"] input').clear()
    cy.get('[data-testid="spz-height"] input').type('15.5')

    cy.get('button').contains('Confirm').click()

    // Wait for the Pinia store state to update
    cy.wrap(store)
      .its(`panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO}.confirmed`)
      .should('be.true')

    // Verify that the Edit button is now rendered
    cy.get('button').contains('Edit').should('exist')
    // Verify values are stored
    cy.wrap(store).its('spzAge').should('eq', '50')
    cy.wrap(store).its('spzHeight').should('eq', '15.5')
  })

  it('changes to Confirm state in Supplied mode', () => {
    const store = mountComponent()

    // Switch to Supplied mode
    cy.get('.v-radio-group')
      .first()
      .within(() => {
        cy.contains('Supplied').click()
      })

    // Set valid value for BHA 50 Site Index
    cy.get('[data-testid="bha-50-site-index"] input').clear()
    cy.get('[data-testid="bha-50-site-index"] input').type('19.2')

    cy.get('button').contains('Confirm').click()

    // Wait for the Pinia store state to update
    cy.wrap(store)
      .its(`panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO}.confirmed`)
      .should('be.true')
    // Verify value is stored
    cy.wrap(store).its('bha50SiteIndex').should('eq', '19.20')
  })

  it('shows validation error for missing required fields in Computed mode', () => {
    mountComponent()

    cy.get('[data-testid="spz-age"] input').clear()
    cy.get('[data-testid="spz-height"] input').clear()

    cy.get('[data-testid="selected-site-species"] input')
      .invoke('val')
      .then((siteSpecies) => {
        cy.get('button').contains('Confirm').click()

        cy.get('.v-dialog')
          .should('exist')
          .within(() => {
            cy.contains(MESSAGE.MSG_DIALOG_TITLE.MISSING_INFO).should('exist')
            cy.contains(
              MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_VALS_SUP(
                siteSpecies as string | null,
              ),
            ).should('exist')
          })
      })
  })

  it('shows validation error for missing required fields in Supplied mode', () => {
    mountComponent()

    // Switch to Supplied mode
    cy.get('.v-radio-group')
      .first()
      .within(() => {
        cy.contains('Supplied').click()
      })

    cy.get('[data-testid="selected-site-species"] input')
      .invoke('val')
      .then((siteSpecies) => {
        cy.get('[data-testid="bha-50-site-index"] input').clear()

        cy.get('button').contains('Confirm').click()

        cy.get('.v-dialog')
          .should('exist')
          .within(() => {
            cy.contains(MESSAGE.MSG_DIALOG_TITLE.MISSING_INFO).should('exist')
            cy.contains(
              MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_SI_VAL(
                siteSpecies as string | null,
              ),
            ).should('exist')
          })
      })
  })

  it('shows a validation error for invalid range in spzHeight', () => {
    mountComponent()

    // Set invalid value for spzHeight (exceeds max)
    cy.get('[data-testid="spz-height"] input').clear()
    cy.get('[data-testid="spz-height"] input').type('100')

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_HIGHT_RNG).should(
          'exist',
        )
      })
  })

  it('shows a validation error for invalid range in BHA 50 Site Index', () => {
    mountComponent()

    // Switch to Supplied mode
    cy.get('.v-radio-group')
      .first()
      .within(() => {
        cy.contains('Supplied').click()
      })

    // Set invalid value for BHA 50 Site Index
    cy.get('[data-testid="bha-50-site-index"] input').clear()
    cy.get('[data-testid="bha-50-site-index"] input').type('200')

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SI_RNG).should('exist')
      })
  })

  it('disables fields correctly when switching to Supplied mode', () => {
    mountComponent()

    // Switch to Supplied mode
    cy.get('.v-radio-group')
      .first()
      .within(() => {
        cy.contains('Supplied').click()
      })

    // Verify disabled fields
    cy.get('.v-radio-group').eq(1).should('have.class', 'v-input--disabled') // Age Years disabled
    cy.get('[data-testid="spz-age"] input').should('be.disabled')
    cy.get('[data-testid="spz-height"] .v-field').should(
      'have.class',
      'v-field--disabled',
    )
    cy.get('[data-testid="bha-50-site-index"] .v-field').should(
      'not.have.class',
      'v-field--disabled',
    )
  })

  it('clears the form when Clear is clicked', () => {
    const store = mountComponent()

    // Modify some values first
    cy.get('[data-testid="spz-age"] input').clear()
    cy.get('[data-testid="spz-age"] input').type('100')
    cy.get('.v-radio-group')
      .first()
      .within(() => {
        cy.contains('Supplied').click()
      })

    cy.get('button').contains('Clear').click()

    // Check for Pinia store updates
    cy.wrap(store).its('becZone').should('eq', DEFAULTS.DEFAULT_VALUES.BEC_ZONE)
    cy.wrap(store)
      .its('siteSpeciesValues')
      .should('eq', DEFAULTS.DEFAULT_VALUES.SITE_SPECIES_VALUES)
    cy.wrap(store).its('ageType').should('eq', DEFAULTS.DEFAULT_VALUES.AGE_TYPE)
    cy.wrap(store).its('spzAge').should('eq', DEFAULTS.DEFAULT_VALUES.SPZ_AGE)
    cy.wrap(store)
      .its('spzHeight')
      .should('eq', DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT)
    cy.wrap(store).its('bha50SiteIndex').should('eq', '')
    // Check for UI updates
    cy.get('.v-select__selection-text').first().should('contain.text', 'IDF') // BEC Zone reset (partial match for displayed label)
    cy.get('.v-radio-group')
      .first()
      .within(() => {
        cy.get('.v-selection-control--dirty .v-label')
          .contains('Computed')
          .should('exist')
      })
    cy.get('.v-radio-group')
      .eq(1)
      .within(() => {
        cy.get('.v-selection-control--dirty .v-label')
          .contains('Total')
          .should('exist')
      })
  })

  it('disables inputs and buttons when the panel is not editable', () => {
    const store = mountComponent()

    // Set panel state to non-editable
    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO].editable =
        false
    })

    // Verify that all inputs, selects, radios, and buttons are disabled
    cy.get('input').each(($input) => {
      cy.wrap($input).should('be.disabled')
    })
    cy.get('.v-select .v-field').each(($field) => {
      cy.wrap($field).should('have.class', 'v-field--disabled')
    })
    cy.get('.v-radio-group').each(($radioGroup) => {
      cy.wrap($radioGroup).should('have.class', 'v-input--disabled')
    })

    cy.get('button:contains("Clear")').should('be.disabled')
    cy.get('button:contains("Confirm")').should('be.disabled')

    // Verify that the Edit button is not visible
    cy.get('button').contains('Edit').should('not.be.visible')
  })
})
