import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ReportConfiguration from './ReportConfiguration.vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import { CONSTANTS, DEFAULTS } from '@/constants'
import { useAppStore } from '@/stores/appStore'
import { createPinia, setActivePinia } from 'pinia'

describe('ReportConfiguration.vue', () => {
  const vuetify = createVuetify()

  beforeEach(() => {
    setActivePinia(createPinia())
    const appStore = useAppStore()
    const modelParameterStore = useModelParameterStore()

    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS

    // Initialize the model parameter store with default values
    modelParameterStore.setDefaultValues()
    modelParameterStore.confirmPanel(
      CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
    )
    modelParameterStore.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO)
    modelParameterStore.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO)

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

  const props = {
    selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.AGE,
    startingAge: 10,
    finishingAge: 300,
    ageIncrement: 5,
    startYear: 2020,
    endYear: 2030,
    yearIncrement: 2,
    isForwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED,
    isBackwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED,
    isComputedMAIEnabled: DEFAULTS.DEFAULT_VALUES.IS_COMPUTED_MAI_ENABLED,
    isCulminationValuesEnabled:
      DEFAULTS.DEFAULT_VALUES.IS_CULMINATION_VALUES_ENABLED,
    isBySpeciesEnabled: DEFAULTS.DEFAULT_VALUES.IS_BY_SPECIES_ENABLED,
    isByLayerEnabled: DEFAULTS.DEFAULT_VALUES.IS_BY_LAYER_ENABLED,
    isProjectionModeEnabled: DEFAULTS.DEFAULT_VALUES.IS_PROJECTION_MODE_ENABLED,
    isPolygonIDEnabled: DEFAULTS.DEFAULT_VALUES.IS_POLYGON_ID_ENABLED,
    isCurrentYearEnabled: DEFAULTS.DEFAULT_VALUES.IS_CURRENT_YEAR_ENABLED,
    isReferenceYearEnabled: DEFAULTS.DEFAULT_VALUES.IS_REFERENCE_YEAR_ENABLED,
    incSecondaryHeight: DEFAULTS.DEFAULT_VALUES.INC_SECONDARY_HEIGHT,
    specificYear: null,
    projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME,
    reportTitle: 'Test Report',
    isDisabled: false,
    isModelParametersMode: true,
  }

  it('renders correctly with initial props for AGE range', () => {
    const appStore = useAppStore()
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.FILE_UPLOAD

    mount(ReportConfiguration, {
      props: {
        ...props,
        isModelParametersMode: false,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-radio-group', { timeout: 6000 })
      .find('.v-radio', { timeout: 6000 })
      .contains('Age', { matchCase: false })
      .parent()
      .find('input[type="radio"]')
      .should('be.checked')

    cy.get('input[id="startingAge"]').should(
      'have.value',
      props.startingAge.toString(),
    )
    cy.get('input[id="finishingAge"]').should(
      'have.value',
      props.finishingAge.toString(),
    )
    cy.get('input[id="ageIncrement"]').should(
      'have.value',
      props.ageIncrement.toString(),
    )

    cy.get('input[id="startYear"]').should('not.exist')
    cy.get('input[id="endYear"]').should('not.exist')
    cy.get('input[id="yearIncrement"]').should('not.exist')
    cy.get('input[id="specificYear"]').should('exist')
    cy.get(
      '[data-testid="inc-secondary-height"] input[type="checkbox"]',
    ).should('exist')
    cy.get('.v-select').find('input').should('have.value', props.projectionType)

    cy.get('input[id="reportTitle"]').should('have.value', props.reportTitle)
    cy.get('.v-slider').should('not.exist')
  })

  it('renders correctly with initial props for YEAR range', () => {
    const appStore = useAppStore()
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.FILE_UPLOAD

    mount(ReportConfiguration, {
      props: {
        ...props,
        selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.YEAR,
        isModelParametersMode: false,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-radio-group', { timeout: 6000 })
      .find('.v-radio', { timeout: 6000 })
      .contains('Year', { matchCase: false })
      .parent()
      .find('input[type="radio"]')
      .should('be.checked')

    cy.get('input[id="startYear"]').should(
      'have.value',
      props.startYear.toString(),
    )
    cy.get('input[id="endYear"]').should('have.value', props.endYear.toString())
    cy.get('input[id="yearIncrement"]').should(
      'have.value',
      props.yearIncrement.toString(),
    )

    cy.get('input[id="startingAge"]').should('not.exist')
    cy.get('input[id="finishingAge"]').should('not.exist')
    cy.get('input[id="ageIncrement"]').should('not.exist')
    cy.get('input[id="specificYear"]').should('have.value', '')
    cy.get(
      '[data-testid="inc-secondary-height"] input[type="checkbox"]',
    ).should('exist')
    cy.get('.v-select').find('input').should('have.value', props.projectionType)

    cy.get('input[id="reportTitle"]').should('have.value', props.reportTitle)
    cy.get('.v-slider').should('not.exist')
  })

  it('emits update:isForwardGrowEnabled when Forward checkbox is toggled', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    mount(ReportConfiguration, {
      props: {
        ...props,
        isForwardGrowEnabled: true,
        'onUpdate:isForwardGrowEnabled': onUpdateSpy,
      },
      global: {
        plugins: [vuetify],
      },
    })
    cy.get(
      '[data-testid="is-forward-grow-enabled"] input[type="checkbox"]',
    ).uncheck()
    cy.get('@updateSpy').should('have.been.calledWith', false)
  })

  it('emits update:isBackwardGrowEnabled when Backward checkbox is toggled', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    mount(ReportConfiguration, {
      props: {
        ...props,
        isBackwardGrowEnabled: true,
        'onUpdate:isBackwardGrowEnabled': onUpdateSpy,
      },
      global: {
        plugins: [vuetify],
      },
    })
    cy.get(
      '[data-testid="is-backward-grow-enabled"] input[type="checkbox"]',
    ).uncheck()
    cy.get('@updateSpy').should('have.been.calledWith', false)
  })

  it('emits events when input values are changed for AGE range', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')
    const appStore = useAppStore()
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.FILE_UPLOAD

    mount(ReportConfiguration, {
      props: {
        ...props,
        isModelParametersMode: false,
        'onUpdate:selectedAgeYearRange': onUpdateSpy,
        'onUpdate:startingAge': onUpdateSpy,
        'onUpdate:finishingAge': onUpdateSpy,
        'onUpdate:ageIncrement': onUpdateSpy,
        'onUpdate:startYear': onUpdateSpy,
        'onUpdate:endYear': onUpdateSpy,
        'onUpdate:yearIncrement': onUpdateSpy,
        'onUpdate:projectionType': onUpdateSpy,
        'onUpdate:reportTitle': onUpdateSpy,
        'onUpdate:specificYear': onUpdateSpy,
        'onUpdate:incSecondaryHeight': onUpdateSpy,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('15')
    cy.get('@updateSpy').should('have.been.calledWith', 15)

    cy.get('input[id="finishingAge"]').clear()
    cy.get('input[id="finishingAge"]').type('120')
    cy.get('@updateSpy').should('have.been.calledWith', 120)

    cy.get('input[id="ageIncrement"]').clear()
    cy.get('input[id="ageIncrement"]').type('10')
    cy.get('@updateSpy').should('have.been.calledWith', 10)

    cy.get('.v-select').click()
    cy.get('.v-list-item')
      .contains(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
      .click({ force: true })
    cy.get('@updateSpy').should(
      'have.been.calledWith',
      CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS,
    )

    cy.get('input[id="reportTitle"]').clear()
    cy.get('input[id="reportTitle"]').type('Updated Report Title')
    cy.get('@updateSpy').should('have.been.calledWith', 'Updated Report Title')

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()
    cy.get('@updateSpy').should(
      'have.been.calledWith',
      CONSTANTS.AGE_YEAR_RANGE.YEAR,
    )
  })

  it('emits events when input values are changed for YEAR range', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')
    const appStore = useAppStore()
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.FILE_UPLOAD

    mount(ReportConfiguration, {
      props: {
        ...props,
        selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.YEAR,
        isModelParametersMode: false,
        'onUpdate:selectedAgeYearRange': onUpdateSpy,
        'onUpdate:startingAge': onUpdateSpy,
        'onUpdate:finishingAge': onUpdateSpy,
        'onUpdate:ageIncrement': onUpdateSpy,
        'onUpdate:startYear': onUpdateSpy,
        'onUpdate:endYear': onUpdateSpy,
        'onUpdate:yearIncrement': onUpdateSpy,
        'onUpdate:projectionType': onUpdateSpy,
        'onUpdate:reportTitle': onUpdateSpy,
        'onUpdate:specificYear': onUpdateSpy,
        'onUpdate:incSecondaryHeight': onUpdateSpy,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Age', { matchCase: false })
      .parent()
      .find('input')
      .click()
    cy.get('@updateSpy').should(
      'have.been.calledWith',
      CONSTANTS.AGE_YEAR_RANGE.AGE,
    )

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()
    cy.get('input[id="startYear"]').clear()
    cy.get('input[id="startYear"]').type('2021')
    cy.get('@updateSpy').should('have.been.calledWith', 2021)
    cy.get('input[id="endYear"]').clear()
    cy.get('input[id="endYear"]').type('2030')
    cy.get('@updateSpy').should('have.been.calledWith', 2030)
    cy.get('input[id="yearIncrement"]').clear()
    cy.get('input[id="yearIncrement"]').type('3')
    cy.get('@updateSpy').should('have.been.calledWith', 3)

    cy.get('input[id="reportTitle"]').clear()
    cy.get('input[id="reportTitle"]').type('Updated Report Title')
    cy.get('@updateSpy').should('have.been.calledWith', 'Updated Report Title')
  })

  it('enables Culmination Values checkbox when Starting Age <= 10 and Finishing Age >= 300', () => {
    mount(ReportConfiguration, {
      props: {
        ...props,
      },
      global: {
        plugins: [vuetify],
      },
    })
    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('10')
    cy.get('input[id="finishingAge"]').clear()
    cy.get('input[id="finishingAge"]').type('300')
    cy.get(
      '[data-testid="is-culmination-values-enabled"] input[type="checkbox"]',
    ).should('not.be.disabled')
  })

  it('disables Culmination Values checkbox when Starting Age > 10 or Finishing Age < 300', () => {
    mount(ReportConfiguration, {
      props: {
        ...props,
      },
      global: {
        plugins: [vuetify],
      },
    })
    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('11')
    cy.get(
      '[data-testid="is-culmination-values-enabled"] input[type="checkbox"]',
    ).should('be.disabled')
    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('10')
    cy.get('input[id="finishingAge"]').clear()
    cy.get('input[id="finishingAge"]').type('299')
    cy.get(
      '[data-testid="is-culmination-values-enabled"] input[type="checkbox"]',
    ).should('be.disabled')
  })

  it('disables all inputs when "isDisabled" is true', () => {
    const appStore = useAppStore()
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS

    mount(ReportConfiguration, {
      props: {
        ...props,
        isDisabled: true,
        isModelParametersMode: true,
      },
      global: {
        plugins: [vuetify],
      },
    })
    cy.get('.v-field__input input').should('be.disabled')
    cy.get('.v-select input').should('be.disabled')
    cy.get(
      '[data-testid="is-forward-grow-enabled"] input[type="checkbox"]',
    ).should('be.disabled')
    cy.get(
      '[data-testid="is-backward-grow-enabled"] input[type="checkbox"]',
    ).should('be.disabled')
    cy.get('.v-slider').should('have.class', 'v-slider--disabled')
  })

  it('enables all inputs when "isDisabled" is false', () => {
    const appStore = useAppStore()
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS

    mount(ReportConfiguration, {
      props: {
        ...props,
        isModelParametersMode: true,
      },
      global: {
        plugins: [vuetify],
      },
    })
    cy.get('.v-field__input input').should('not.be.disabled')
    cy.get('.v-select input').should('not.be.disabled')
    cy.get(
      '[data-testid="is-forward-grow-enabled"] input[type="checkbox"]',
    ).should('not.be.disabled')
    cy.get(
      '[data-testid="is-backward-grow-enabled"] input[type="checkbox"]',
    ).should('not.be.disabled')
    cy.get('.v-slider').should('not.have.class', 'v-slider--disabled')
  })

  it('renders Minimum DBH Limit by Species Group when isModelParametersMode is true', () => {
    const modelParameterStore = useModelParameterStore()
    const appStore = useAppStore()

    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS

    mount(ReportConfiguration, {
      props,
      global: {
        plugins: [vuetify],
      },
    })
    cy.contains('span.text-h7', 'Minimum DBH Limit by Species Group').should(
      'be.visible',
    )
    cy.get('.v-slider', { timeout: 6000 }).should(
      'have.length',
      modelParameterStore.speciesGroups.length,
    )
    cy.get('.v-slider').should('be.visible')
    cy.get('.v-slider').should('not.have.class', 'v-slider--disabled')
    cy.get('.v-slider').should(
      'have.length',
      modelParameterStore.speciesGroups.length,
    )
  })

  it('does not render Minimum DBH Limit by Species Group when isModelParametersMode is false', () => {
    mount(ReportConfiguration, {
      props: {
        ...props,
        isModelParametersMode: false,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.contains('span.text-h7', 'Minimum DBH Limit by Species Group').should(
      'not.exist',
    )
  })

  describe('File Upload Mode', () => {
    beforeEach(() => {
      const appStore = useAppStore()
      const fileUploadStore = useFileUploadStore()
      
      // Set app to file upload mode
      appStore.modelSelection = CONSTANTS.MODEL_SELECTION.FILE_UPLOAD
      
      // Initialize file upload store
      fileUploadStore.setDefaultValues()
    })

    it('renders File Upload Minimum DBH Limit by Species Group when in File Upload mode', () => {
      mount(ReportConfiguration, {
        props: {
          ...props,
          isModelParametersMode: false,
        },
        global: {
          plugins: [vuetify],
        },
      })

      cy.contains('span.text-h7', 'Minimum DBH Limit by Species Group').should(
        'be.visible',
      )
      
      // Should show 16 species groups (all species)
      cy.get('.v-slider', { timeout: 6000 }).should('have.length', 16)
      cy.get('.v-slider').should('be.visible')
      cy.get('.v-slider').should('not.have.class', 'v-slider--disabled')
    })

    it('updates sliders when projection type changes from Volume to CFO Biomass', () => {
      mount(ReportConfiguration, {
        props: {
          ...props,
          isModelParametersMode: false,
          projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME,
        },
        global: {
          plugins: [vuetify],
        },
      })

      // Initially all sliders should be enabled for Volume projection
      cy.get('.v-slider').should('not.have.class', 'v-slider--disabled')

      // Change projection type to CFO Biomass
      cy.get('.v-select').click()
      cy.get('.v-list-item')
        .contains(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
        .click({ force: true })

      // Now sliders should be disabled (grayed-out) for CFO Biomass
      cy.get('.v-slider').should('have.class', 'v-slider--disabled')
    })

    it('disables File Upload sliders when projection type is CFO Biomass', () => {
      mount(ReportConfiguration, {
        props: {
          ...props,
          isModelParametersMode: false,
          projectionType: CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS,
        },
        global: {
          plugins: [vuetify],
        },
      })

      // Sliders should be disabled for CFO Biomass projection
      cy.get('.v-slider').should('have.class', 'v-slider--disabled')
    })

    it('enables File Upload sliders when projection type is Volume', () => {
      mount(ReportConfiguration, {
        props: {
          ...props,
          isModelParametersMode: false,
          projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME,
        },
        global: {
          plugins: [vuetify],
        },
      })

      // Sliders should be enabled for Volume projection
      cy.get('.v-slider').should('not.have.class', 'v-slider--disabled')
    })

    it('does not show File Upload sliders when not in File Upload mode', () => {
      const appStore = useAppStore()
      appStore.modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS

      mount(ReportConfiguration, {
        props: {
          ...props,
          isModelParametersMode: false,
        },
        global: {
          plugins: [vuetify],
        },
      })

      // Should not show File Upload specific sliders when not in file upload mode
      cy.get('.v-slider').should('not.exist')
    })
  })
})
