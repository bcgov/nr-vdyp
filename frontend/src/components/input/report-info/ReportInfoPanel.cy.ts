import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ReportInfoPanel from './ReportInfoPanel.vue'
import { useAppStore } from '@/stores/appStore'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import { setActivePinia, createPinia } from 'pinia'
import { CONSTANTS, MESSAGE } from '@/constants'

const vuetify = createVuetify()

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
    modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
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
        CONSTANTS.MODEL_PARAMETER_PANEL.STAND_DENSITY,
      )

      modelParameterStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
      modelParameterStore.startingAge = 10
      modelParameterStore.finishingAge = 100
      modelParameterStore.ageIncrement = 5
      modelParameterStore.startYear = 2020
      modelParameterStore.endYear = 2030
      modelParameterStore.yearIncrement = 2
      modelParameterStore.volumeReported = [
        CONSTANTS.VOLUME_REPORTED.WHOLE_STEM,
      ]
      modelParameterStore.includeInReport = [
        CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI,
      ]
      modelParameterStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      modelParameterStore.reportTitle = 'Sample Report'
    } else {
      fileUploadStore.setDefaultValues()
      fileUploadStore.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO)

      fileUploadStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
      fileUploadStore.startingAge = 10
      fileUploadStore.finishingAge = 100
      fileUploadStore.ageIncrement = 5
      fileUploadStore.startYear = 2020
      fileUploadStore.endYear = 2030
      fileUploadStore.yearIncrement = 2
      fileUploadStore.volumeReported = [CONSTANTS.VOLUME_REPORTED.WHOLE_STEM]
      fileUploadStore.includeInReport = [
        CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI,
      ]
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      fileUploadStore.reportTitle = 'Sample Report'
    }

    appStore.modelSelection = modelSelection

    mount(ReportInfoPanel, {
      global: {
        plugins: [vuetify],
      },
      ...overrides,
    })

    return modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
      ? modelParameterStore
      : fileUploadStore
  }

  it('renders correctly with initial state for AGE range (INPUT_MODEL_PARAMETERS)', () => {
    mountComponent({}, CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS)

    cy.get('.v-expansion-panel-title')
      .contains('Report Information')
      .should('exist')

    cy.get('[id="startingAge"]')
      .should('exist')
      .and('have.value', '10')
      .and('have.attr', 'type', 'number')

    cy.get('[id="finishingAge"]')
      .should('exist')
      .and('have.value', '100')
      .and('have.attr', 'type', 'number')

    cy.get('[id="ageIncrement"]')
      .should('exist')
      .and('have.value', '5')
      .and('have.attr', 'type', 'number')

    cy.get('[id="startYear"]').should('not.exist')
    cy.get('[id="endYear"]').should('not.exist')
    cy.get('[id="yearIncrement"]').should('not.exist')

    cy.contains('.v-input', CONSTANTS.VOLUME_REPORTED.WHOLE_STEM)
      .find('input[type="checkbox"]')
      .should('be.checked')

    cy.contains('.v-input', CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI)
      .find('input[type="checkbox"]')
      .should('be.checked')

    cy.get('.v-select')
      .find('input')
      .should('have.value', CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

    cy.get('[id="reportTitle"]').should('have.value', 'Sample Report')

    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('renders correctly with initial state for YEAR range (INPUT_MODEL_PARAMETERS)', () => {
    mountComponent({}, CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS)

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[id="startYear"]')
      .should('exist')
      .and('have.value', '2020')
      .and('have.attr', 'type', 'number')

    cy.get('[id="endYear"]')
      .should('exist')
      .and('have.value', '2030')
      .and('have.attr', 'type', 'number')

    cy.get('[id="yearIncrement"]')
      .should('exist')
      .and('have.value', '2')
      .and('have.attr', 'type', 'number')

    cy.get('[id="startingAge"]').should('not.exist')
    cy.get('[id="finishingAge"]').should('not.exist')
    cy.get('[id="ageIncrement"]').should('not.exist')

    cy.contains('.v-input', CONSTANTS.VOLUME_REPORTED.WHOLE_STEM)
      .find('input[type="checkbox"]')
      .should('be.checked')

    cy.contains('.v-input', CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI)
      .find('input[type="checkbox"]')
      .should('be.checked')

    cy.get('.v-select')
      .find('input')
      .should('have.value', CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

    cy.get('[id="reportTitle"]').should('have.value', 'Sample Report')

    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('renders correctly with initial state for AGE range (FILE_UPLOAD)', () => {
    mountComponent({})

    cy.get('.v-expansion-panel-title')
      .contains('Report Information')
      .should('exist')

    cy.get('[id="startingAge"]')
      .should('exist')
      .and('have.value', '10')
      .and('have.attr', 'type', 'number')

    cy.get('[id="finishingAge"]')
      .should('exist')
      .and('have.value', '100')
      .and('have.attr', 'type', 'number')

    cy.get('[id="ageIncrement"]')
      .should('exist')
      .and('have.value', '5')
      .and('have.attr', 'type', 'number')

    cy.get('[id="startYear"]').should('not.exist')
    cy.get('[id="endYear"]').should('not.exist')
    cy.get('[id="yearIncrement"]').should('not.exist')

    cy.contains('.v-input', CONSTANTS.VOLUME_REPORTED.WHOLE_STEM)
      .find('input[type="checkbox"]')
      .should('be.checked')

    cy.contains('.v-input', CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI)
      .find('input[type="checkbox"]')
      .should('be.checked')

    cy.get('.v-select')
      .find('input')
      .should('have.value', CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

    cy.get('[id="reportTitle"]').should('have.value', 'Sample Report')

    cy.get('button').contains('Clear').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('changes to Confirm state and renders the Edit button (INPUT_MODEL_PARAMETERS)', () => {
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

  it('changes to Confirm state and renders the Edit button (FILE_UPLOAD)', () => {
    const store = mountComponent({})

    cy.get('button').contains('Confirm').click()

    cy.wrap(store)
      .its(`panelState.${CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO}.confirmed`)
      .should('be.true')

    cy.get('button').contains('Edit').should('exist')
  })

  it('shows a validation error for invalid starting and finishing ages (AGE range)', () => {
    mountComponent()

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

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_FNSH_AGE).should(
          'exist',
        )
      })
  })

  it('shows a validation error for invalid start and end years (YEAR range)', () => {
    mountComponent()

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[id="startYear"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('2030')
      })

    cy.get('[id="endYear"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('2020')
      })

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_END_YEAR).should(
          'exist',
        )
      })
  })

  it('shows validation error when starting age is out of range (AGE range)', () => {
    mountComponent()

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

  it('shows validation error when finishing age is out of range (AGE range)', () => {
    mountComponent()

    cy.get('[id="finishingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type(
          (CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX + 1).toString(),
        )
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

  it('shows validation error when age increment is out of range (AGE range)', () => {
    mountComponent()

    cy.get('[id="ageIncrement"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type(
          (CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX + 1).toString(),
        )
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

  it('shows validation error when start year is out of range (YEAR range)', () => {
    mountComponent()

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[id="startYear"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type(
          (CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN - 1).toString(),
        )
      })

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
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
    mountComponent()

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[id="endYear"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type(
          (CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX + 1).toString(),
        )
      })

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
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
    mountComponent()

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()

    cy.get('[id="yearIncrement"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type(
          (CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX + 1).toString(),
        )
      })

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_YEAR_INC_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX,
          ),
        ).should('exist')
      })
  })

  it('clears the form when Clear is clicked (INPUT_MODEL_PARAMETERS)', () => {
    mountComponent({}, CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS)

    cy.get('button').contains('Clear').click()

    cy.get('[id="startingAge"]').should('exist').should('have.value', '')
    cy.get('[id="finishingAge"]').should('exist').should('have.value', '')
    cy.get('[id="ageIncrement"]').should('exist').should('have.value', '')
    cy.get('[id="reportTitle"]').should('exist').should('have.value', '')
    cy.get('[data-testid="volume-reported"] input[type="checkbox"]').each(
      ($el) => {
        cy.wrap($el).should('not.be.checked')
      },
    )
    cy.get('.v-select')
      .find('input')
      .should('have.value', CONSTANTS.PROJECTION_TYPE.VOLUME)
  })

  it('disables inputs and buttons when the panel is not editable (INPUT_MODEL_PARAMETERS)', () => {
    const store = mountComponent(
      {},
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO].editable =
        false
    })

    cy.get('input[type="number"]').should('be.disabled')
    cy.get('button:contains("Clear")').should('be.disabled')
    cy.get('button:contains("Confirm")').should('be.disabled')

    cy.get('button').contains('Edit').should('not.be.visible')
  })

  it('disables inputs and buttons when the panel is not editable (FILE_UPLOAD)', () => {
    const store = mountComponent({})

    cy.wrap(store).then(() => {
      store.panelState[CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO].editable = false
    })

    cy.get('input[type="number"]').should('be.disabled')
    cy.get('button:contains("Clear")').should('be.disabled')
    cy.get('button:contains("Confirm")').should('be.disabled')

    cy.get('button').contains('Edit').should('not.be.visible')
  })
})
