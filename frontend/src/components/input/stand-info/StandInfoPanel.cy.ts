import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import StandInfoPanel from './StandInfoPanel.vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { setActivePinia, createPinia } from 'pinia'
import { DEFAULTS, CONSTANTS, MESSAGE } from '@/constants'

const vuetify = createVuetify()

describe('StandInfoPanel.vue', () => {
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

    // Assume Species Info and Site Info are verified
    modelParameterStore.confirmPanel(
      CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
    )
    modelParameterStore.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO)

    // Set conditions to enable Stand Info fields
    modelParameterStore.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
    modelParameterStore.siteSpeciesValues =
      CONSTANTS.SITE_SPECIES_VALUES.COMPUTED

    // Set default values for new fields
    modelParameterStore.percentStockableArea =
      DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA
    modelParameterStore.basalArea = DEFAULTS.DEFAULT_VALUES.BASAL_AREA
    modelParameterStore.treesPerHectare = DEFAULTS.DEFAULT_VALUES.TPH
    modelParameterStore.minDBHLimit = DEFAULTS.DEFAULT_VALUES.MIN_DBH_LIMIT
    modelParameterStore.currentDiameter =
      DEFAULTS.DEFAULT_VALUES.CURRENT_DIAMETER
    modelParameterStore.crownClosure = DEFAULTS.DEFAULT_VALUES.CROWN_CLOSURE

    mount(StandInfoPanel, {
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
    cy.get('.v-expansion-panel-title')
      .contains('Stand Information')
      .should('exist')

    // Verify % Stockable Area
    cy.get('[data-testid="percent-stockable-area"] input')
      .should('exist')
      .and('have.value', DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA)
      .and('have.attr', 'type', 'text')
      .and(
        'have.attr',
        'max',
        CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX,
      )
      .and(
        'have.attr',
        'min',
        CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN,
      )
      .and(
        'have.attr',
        'step',
        CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_STEP,
      )

    // Verify Crown Closure
    cy.get('[data-testid="crown-closure"] input')
      .should('exist')
      .and('have.value', '')
      .and('have.attr', 'type', 'text')
      .and('have.attr', 'max', CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MAX)
      .and('have.attr', 'min', CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MIN)
      .and('have.attr', 'step', CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_STEP)

    // Verify Basal Area
    cy.get('[data-testid="basal-area"] input')
      .should('exist')
      .and('have.value', DEFAULTS.DEFAULT_VALUES.BASAL_AREA)
      .and('have.attr', 'type', 'text')
      .and(
        'have.attr',
        'max',
        String(CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_MAX),
      )
      .and(
        'have.attr',
        'min',
        String(CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_MIN),
      )
      .and(
        'have.attr',
        'step',
        String(CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_STEP),
      )

    // Verify TPH
    cy.get('[data-testid="trees-per-hectare"] input')
      .should('exist')
      .and('have.value', DEFAULTS.DEFAULT_VALUES.TPH)
      .and('have.attr', 'type', 'text')
      .and('have.attr', 'max', String(CONSTANTS.NUM_INPUT_LIMITS.TPH_MAX))
      .and('have.attr', 'min', String(CONSTANTS.NUM_INPUT_LIMITS.TPH_MIN))
      .and('have.attr', 'step', String(CONSTANTS.NUM_INPUT_LIMITS.TPH_STEP))

    // Verify Min DBH Limit
    cy.get('[data-testid="min-dbh-limit"] input')
      .should('exist')
      .and('have.value', DEFAULTS.DEFAULT_VALUES.MIN_DBH_LIMIT)
      .and('be.disabled')

    // Verify Current Diameter (visible when conditions are met)
    cy.get('[data-testid="current-diameter"] input')
      .should('exist')
      .and('have.value', DEFAULTS.DEFAULT_VALUES.CURRENT_DIAMETER)
      .and('be.disabled')

    // Verify AppPanelActions buttons
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
      .its(`panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO}.confirmed`)
      .should('be.true')

    // Verify that the Edit button is now rendered
    cy.get('button').contains('Edit').should('exist')

    // Verify inputs remain with values after confirm
    cy.get('[data-testid="percent-stockable-area"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA,
    )
    cy.get('[data-testid="crown-closure"] input').should('have.value', '')
    cy.get('[data-testid="basal-area"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.BASAL_AREA,
    )
    cy.get('[data-testid="trees-per-hectare"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.TPH,
    )
    cy.get('[data-testid="min-dbh-limit"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.MIN_DBH_LIMIT,
    )
    cy.get('[data-testid="current-diameter"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.CURRENT_DIAMETER,
    )
  })

  it('shows a validation error for invalid % Stockable Area', () => {
    mountComponent()

    // Setting % Stockable Area to an invalid value
    cy.get('[data-testid="percent-stockable-area"] input').clear()
    cy.get('[data-testid="percent-stockable-area"] input').type('200')

    // Click the Confirm button
    cy.get('button').contains('Confirm').click()

    // Ensure the dialog appears and contains the error message
    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_PCT_STCB_AREA_RNG,
        ).should('exist')
      })
  })

  it('shows a validation error for invalid Basal Area', () => {
    mountComponent()

    // Setting Basal Area to an invalid value
    cy.get('[data-testid="basal-area"] input').clear()
    cy.get('[data-testid="basal-area"] input').type('1000')

    // Click the Confirm button
    cy.get('button').contains('Confirm').click()

    // Ensure the dialog appears and contains the error message
    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_BSL_AREA_RNG).should(
          'exist',
        )
      })
  })

  it('shows a validation error for invalid TPH', () => {
    mountComponent()

    // Setting TPH to an invalid value
    cy.get('[data-testid="trees-per-hectare"] input').clear()
    cy.get('[data-testid="trees-per-hectare"] input').type('10000')

    // Click the Confirm button
    cy.get('button').contains('Confirm').click()

    // Ensure the dialog appears and contains the error message
    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_TPH_RNG).should(
          'exist',
        )
      })
  })

  it('clears the form when Clear is clicked', () => {
    mountComponent()

    cy.get('button').contains('Clear').click()

    // Verify that all fields are cleared or reset
    cy.get('[data-testid="percent-stockable-area"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA,
    )
    cy.get('[data-testid="crown-closure"] input').should('have.value', '')
    cy.get('[data-testid="basal-area"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.BASAL_AREA,
    )
    cy.get('[data-testid="trees-per-hectare"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.TPH,
    )
    cy.get('[data-testid="min-dbh-limit"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.MIN_DBH_LIMIT,
    )
    cy.get('[data-testid="current-diameter"] input').should(
      'have.value',
      DEFAULTS.DEFAULT_VALUES.CURRENT_DIAMETER,
    )
  })

  it('disables inputs and buttons when the panel is not editable', () => {
    const store = mountComponent()

    // Ensure the panel state is set to non-editable
    cy.wrap(store).then(() => {
      store.panelState.standInfo.editable = false
    })

    // Verify that all input fields are disabled
    cy.get('[data-testid="percent-stockable-area"] input').should('be.disabled')
    cy.get('[data-testid="crown-closure"] input').should('be.disabled')
    cy.get('[data-testid="basal-area"] input').should('be.disabled')
    cy.get('[data-testid="trees-per-hectare"] input').should('be.disabled')
    cy.get('[data-testid="min-dbh-limit"] input').should('be.disabled')
    cy.get('[data-testid="current-diameter"] input').should('be.disabled')

    // Verify that the Clear and Confirm buttons are disabled
    cy.get('button:contains("Clear")')
      .should('have.attr', 'disabled')
    cy.get('button:contains("Clear")')
      .should('have.attr', 'data-disabled')
    cy.get('button:contains("Confirm")')
      .should('have.attr', 'disabled')
    cy.get('button:contains("Confirm")')
      .should('have.attr', 'data-disabled')

    // Verify that the Edit button is not visible
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('updates Current Diameter when Basal Area and TPH change', () => {
    mountComponent()

    // Set initial values
    cy.get('[data-testid="basal-area"] input').clear()
    cy.get('[data-testid="basal-area"] input').type('10.0')
    cy.get('[data-testid="trees-per-hectare"] input').clear()
    cy.get('[data-testid="trees-per-hectare"] input').type('100.0')

    cy.get('[data-testid="current-diameter"] input')
      .should('have.value', '35.7 cm')
      .and('be.disabled')
  })
})
