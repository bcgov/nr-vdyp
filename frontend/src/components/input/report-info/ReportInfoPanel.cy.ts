import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ReportInfoPanel from './ReportInfoPanel.vue'
import { useAppStore } from '@/stores/appStore'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import { setActivePinia, createPinia } from 'pinia'
import { CONSTANTS, DEFAULTS, MESSAGE } from '@/constants'

const vuetify = createVuetify({
  defaults: {
    global: {
      // Disable Vuetify transitions globally for faster tests
      transition: false,
    },
  },
})

describe('ReportInfoPanel.vue', () => {
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

  const mountComponent = (
    overrides = {},
    modelSelection: string = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
  ) => {
    const modelParameterStore = useModelParameterStore()
    const fileUploadStore = useFileUploadStore()
    const appStore = useAppStore()

    if (modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS) {
      modelParameterStore.setDefaultValues()
      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
      )
      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO,
      )
      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO,
      )

      modelParameterStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
      modelParameterStore.startingAge = '10'
      modelParameterStore.finishingAge = '100'
      modelParameterStore.ageIncrement = '5'
      modelParameterStore.startYear = '2020'
      modelParameterStore.endYear = '2030'
      modelParameterStore.yearIncrement = '2'
      modelParameterStore.isForwardGrowEnabled =
        DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED
      modelParameterStore.isBackwardGrowEnabled =
        DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED
      modelParameterStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      modelParameterStore.reportTitle = 'Sample Report'
    } else {
      fileUploadStore.setDefaultValues()
      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
      )
      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO,
      )
      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO,
      )

      fileUploadStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
      fileUploadStore.startingAge = '10'
      fileUploadStore.finishingAge = '100'
      fileUploadStore.ageIncrement = '5'
      fileUploadStore.startYear = '2020'
      fileUploadStore.endYear = '2030'
      fileUploadStore.yearIncrement = '2'
      fileUploadStore.isForwardGrowEnabled =
        DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED
      fileUploadStore.isBackwardGrowEnabled =
        DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      fileUploadStore.reportTitle = 'Sample Report'
    }

    appStore.modelSelection = modelSelection

    const isModelParametersMode =
      modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS

    mount(ReportInfoPanel, {
      global: {
        plugins: [vuetify],
      },
      props: {
        isModelParametersMode,
        ...overrides,
      },
    })

    return modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
      ? modelParameterStore
      : fileUploadStore
  }

  it('renders correctly with initial state for AGE range', () => {
    mountComponent({}, CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS)

    cy.get('.v-expansion-panel-title')
      .contains('Report Information')
      .should('exist')

    cy.get('[data-testid="starting-age"]')
      .find('input')
      .should('exist')
      .and('have.value', '10')

    cy.get('[data-testid="finishing-age"]')
      .find('input')
      .should('exist')
      .and('have.value', '100')

    cy.get('[data-testid="age-increment"]')
      .find('input')
      .should('exist')
      .and('have.value', '5')

    cy.get('[data-testid="start-year"]').should('not.exist')
    cy.get('[data-testid="end-year"]').should('not.exist')
    cy.get('[data-testid="year-increment"]').should('not.exist')

    cy.contains('.v-input', CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI)
      .find('input[type="checkbox"]')
      .should('not.be.checked')

    cy.get('.v-select')
      .find('input')
      .should('have.value', CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

    cy.get('[id="reportTitle"]').should('have.value', 'Sample Report')

    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('renders correctly with initial state for YEAR range', () => {
    mountComponent({}, CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[data-testid="start-year"]')
      .find('input')
      .should('exist')
      .and('have.value', '2020')

    cy.get('[data-testid="end-year"]')
      .find('input')
      .should('exist')
      .and('have.value', '2030')

    cy.get('[data-testid="year-increment"]')
      .find('input')
      .should('exist')
      .and('have.value', '2')

    cy.get('[data-testid="starting-age"]').should('not.exist')
    cy.get('[data-testid="finishing-age"]').should('not.exist')
    cy.get('[data-testid="age-increment"]').should('not.exist')

    cy.get('.v-select')
      .find('input')
      .should('have.value', CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

    cy.get('[id="reportTitle"]').should('have.value', 'Sample Report')

    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('renders correctly with initial state for AGE range', () => {
    mountComponent({}, CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

    cy.get('.v-expansion-panel-title')
      .contains('Report Information')
      .should('exist')

    cy.get('[data-testid="starting-age"]')
      .find('input')
      .should('exist')
      .and('have.value', '10')

    cy.get('[data-testid="finishing-age"]')
      .find('input')
      .should('exist')
      .and('have.value', '100')

    cy.get('[data-testid="age-increment"]')
      .find('input')
      .should('exist')
      .and('have.value', '5')

    cy.get('[data-testid="start-year"]').should('not.exist')
    cy.get('[data-testid="end-year"]').should('not.exist')
    cy.get('[data-testid="year-increment"]').should('not.exist')

    cy.get('.v-select')
      .find('input')
      .should('have.value', CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

    cy.get('[id="reportTitle"]').should('have.value', 'Sample Report')

    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('changes to Confirm state and renders the Edit button', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.get('button').contains('Confirm').click()

    cy.wrap(store)
      .its(
        `panelState.${CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO}.confirmed`,
      )
      .should('be.true')

    cy.get('button').contains('Edit').should('exist')
  })

  it('changes to Confirm state and renders the Edit button', () => {
    const store = mountComponent({}, CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

    cy.get('button').contains('Confirm').click()

    cy.wrap(store)
      .its(`panelState.${CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO}.confirmed`)
      .should('be.true')

    cy.get('button').contains('Edit').should('exist')
  })

  it('shows a validation error for invalid starting and finishing ages (AGE range)', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        true
    })

    cy.get('[data-testid="starting-age"]').find('input').should('exist').clear()
    cy.get('[data-testid="starting-age"]').find('input').type('200')

    cy.get('[data-testid="finishing-age"]').find('input').should('exist').clear()
    cy.get('[data-testid="finishing-age"]').find('input').type('50')

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog', { timeout: 6000 })
      .should('be.visible')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_FNSH_AGE).should(
          'exist',
        )
      })
  })

  it('shows a validation error for invalid start and end years (YEAR range)', () => {
    const store = mountComponent({}, CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        true
    })

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[data-testid="start-year"]').find('input').should('exist').clear()
    cy.get('[data-testid="start-year"]').find('input').type('2030')

    cy.get('[data-testid="end-year"]').find('input').should('exist').clear()
    cy.get('[data-testid="end-year"]').find('input').type('2020')

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog', { timeout: 6000 })
      .should('be.visible')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_END_YEAR).should(
          'exist',
        )
      })
  })

  it('shows validation error when starting age is out of range (AGE range)', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        true
    })

    cy.get('[data-testid="starting-age"]').find('input').should('exist').clear()
    cy.get('[data-testid="starting-age"]').find('input').type('-1')

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog', { timeout: 6000 })
      .should('be.visible')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_AGE_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX,
          ),
        ).should('exist')
      })
  })

  it('shows validation error when finishing age is out of range (AGE range)', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        true
    })

    cy.get('[data-testid="finishing-age"]').find('input').should('exist').clear()
    cy.get('[data-testid="finishing-age"]').find('input').type(
      (CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX + 1).toString(),
    )

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog', { timeout: 6000 })
      .should('be.visible')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_FNSH_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
          ),
        ).should('exist')
      })
  })

  it('shows validation error when age increment is out of range (AGE range)', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        true
    })

    cy.get('[data-testid="age-increment"]').find('input').should('exist').clear()
    cy.get('[data-testid="age-increment"]').find('input').type(
      (CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX + 1).toString(),
    )

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog', { timeout: 6000 })
      .should('be.visible')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_AGE_INC_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX,
          ),
        ).should('exist')
      })
  })

  it('shows validation error when start year is out of range (YEAR range)', () => {
    const store = mountComponent({}, CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        true
    })

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[data-testid="start-year"]').find('input').should('exist').clear()
    cy.get('[data-testid="start-year"]').find('input').type(
      (CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN - 1).toString(),
    )

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog', { timeout: 6000 })
      .should('be.visible')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_YEAR_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MAX,
          ),
        ).should('exist')
      })
  })

  it('shows validation error when end year is out of range (YEAR range)', () => {
    const store = mountComponent({}, CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        true
    })

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[data-testid="end-year"]').find('input').should('exist').clear()
    cy.get('[data-testid="end-year"]').find('input').type(
      (CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX + 1).toString(),
    )

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog', { timeout: 6000 })
      .should('be.visible')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_END_YEAR_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX,
          ),
        ).should('exist')
      })
  })

  it('shows validation error when year increment is out of range (YEAR range)', () => {
    const store = mountComponent({}, CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        true
    })

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[data-testid="year-increment"]').find('input').should('exist').clear()
    cy.get('[data-testid="year-increment"]').find('input').type(
      (CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX + 1).toString(),
    )

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog', { timeout: 6000 })
      .should('be.visible')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_YEAR_INC_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX,
          ),
        ).should('exist')
      })
  })

  it('clears the form when Clear is clicked', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        true
    })

    cy.get('button').contains('Clear').click()

    cy.get('[data-testid="starting-age"]').find('input').should('exist').should('have.value', '')
    cy.get('[data-testid="finishing-age"]').find('input').should('exist').should('have.value', '')
    cy.get('[data-testid="age-increment"]').find('input').should('exist').should('have.value', '')
    cy.get('[id="reportTitle"]').should('exist').should('have.value', '')
    cy.get('.v-select')
      .find('input')
      .should('have.value', DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE)
  })

  it('disables inputs and buttons when the panel is not editable', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        false
    })

    cy.get('[data-testid="starting-age"]').find('input').should('be.disabled')
    cy.get('[data-testid="finishing-age"]').find('input').should('be.disabled')
    cy.get('[data-testid="age-increment"]').find('input').should('be.disabled')
    cy.get('button').contains('Clear').should('not.be.disabled')
    cy.get('button').contains('Confirm').should('not.be.disabled')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('disables inputs and buttons when the panel is not editable', () => {
    const store = mountComponent({}, CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO].editable = false
    })

    cy.get('[data-testid="starting-age"]').find('input').should('be.disabled')
    cy.get('[data-testid="finishing-age"]').find('input').should('be.disabled')
    cy.get('[data-testid="age-increment"]').find('input').should('be.disabled')
    cy.get('button').contains('Clear').should('not.be.disabled')
    cy.get('button').contains('Confirm').should('not.be.disabled')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('renders forwardBackwardGrow checkboxes correctly', () => {
    mountComponent({}, CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS)

    cy.get('[data-testid="is-forward-grow-enabled"]').should('exist')
    cy.get('[data-testid="is-backward-grow-enabled"]').should('exist')
  })

  it('has correct initial state for forwardGrow and backwardGrow', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.wrap(store.isForwardGrowEnabled).should(
      'equal',
      DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED,
    )
    cy.wrap(store.isBackwardGrowEnabled).should(
      'equal',
      DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED,
    )

    cy.get(
      '[data-testid="is-forward-grow-enabled"] input[type="checkbox"]',
    ).should('be.checked')
    cy.get(
      '[data-testid="is-backward-grow-enabled"] input[type="checkbox"]',
    ).should('be.checked')
  })

  it('disables forwardGrow / backwardGrow checkboxes when panel is not editable', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        false
    })

    cy.get(
      '[data-testid="is-forward-grow-enabled"] input[type="checkbox"]',
    ).should('be.disabled')
    cy.get(
      '[data-testid="is-backward-grow-enabled"] input[type="checkbox"]',
    ).should('be.disabled')
  })

  it('passes isModelParametersMode prop correctly - model parameter mode', () => {
    mountComponent({}, CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS)
    cy.contains('span.min-dbh-limit-species-group-label', 'Minimum DBH Limit by Species Group').should(
      'be.visible',
    )
  })

  it('passes isModelParametersMode prop correctly - file upload mode', () => {
    mountComponent({}, CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)
    cy.contains('span.min-dbh-limit-species-group-label', 'Minimum DBH Limit by Species Group').should(
      'be.visible',
    )
  })
})
