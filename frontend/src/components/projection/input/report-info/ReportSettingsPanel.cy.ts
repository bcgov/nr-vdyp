import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import ReportSettingsPanel from './ReportSettingsPanel.vue'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'
import { PROJECTION_VIEW_MODE } from '@/constants/constants'

const vuetify = createVuetify()

const mountPanel = (
  setup?: (
    modelStore: ReturnType<typeof useModelParameterStore>,
    appStore: ReturnType<typeof useAppStore>,
  ) => void,
) => {
  const pinia = createPinia()
  setActivePinia(pinia)

  const modelStore = useModelParameterStore()
  const appStore = useAppStore()

  modelStore.panelOpenStates.reportSettings = CONSTANTS.PANEL.OPEN
  modelStore.panelState.reportSettings.editable = true

  if (setup) setup(modelStore, appStore)

  mount(ReportSettingsPanel, {
    global: { plugins: [pinia, vuetify] },
  })

  return { modelStore, appStore }
}

describe('<ReportSettingsPanel />', () => {
  describe('Panel structure', () => {
    it('renders the "Report Settings" panel title', () => {
      mountPanel()
      cy.contains('.text-h6', 'Report Settings').should('exist')
    })

    it('panel content is in the DOM when the panel is open', () => {
      mountPanel()
      cy.get('[data-testid="starting-age"]').should('exist')
    })

    it('panel content is not in the DOM when the panel is closed', () => {
      mountPanel((modelStore) => {
        modelStore.panelOpenStates.reportSettings = CONSTANTS.PANEL.CLOSE
      })
      cy.get('[data-testid="starting-age"]').should('not.exist')
    })
  })

  describe('Expansion panel chevron icon', () => {
    it('shows mdi-chevron-up when the panel is open', () => {
      mountPanel()
      cy.get('.expansion-panel-icon').should('have.class', 'mdi-chevron-up')
    })

    it('shows mdi-chevron-down when the panel is closed', () => {
      mountPanel((modelStore) => {
        modelStore.panelOpenStates.reportSettings = CONSTANTS.PANEL.CLOSE
      })
      cy.get('.expansion-panel-icon').should('have.class', 'mdi-chevron-down')
    })
  })

  describe('Age range fields (default)', () => {
    it('renders Starting Age, Finishing Age, and Increment fields when Age is selected', () => {
      mountPanel()
      cy.get('[data-testid="starting-age"]').should('exist')
      cy.get('[data-testid="finishing-age"]').should('exist')
      cy.get('[data-testid="age-increment"]').should('exist')
    })

    it('does not render Year fields when Age is selected', () => {
      mountPanel()
      cy.get('[data-testid="start-year"]').should('not.exist')
      cy.get('[data-testid="end-year"]').should('not.exist')
      cy.get('[data-testid="year-increment"]').should('not.exist')
    })
  })

  describe('Year range fields', () => {
    it('renders Start Year, End Year, and Increment fields when Year is selected', () => {
      mountPanel((modelStore) => {
        modelStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
      })
      cy.get('[data-testid="start-year"]').should('exist')
      cy.get('[data-testid="end-year"]').should('exist')
      cy.get('[data-testid="year-increment"]').should('exist')
    })

    it('does not render Age fields when Year is selected', () => {
      mountPanel((modelStore) => {
        modelStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
      })
      cy.get('[data-testid="starting-age"]').should('not.exist')
      cy.get('[data-testid="finishing-age"]').should('not.exist')
    })
  })

  describe('Include in Report checkboxes', () => {
    it('renders all checkboxes', () => {
      mountPanel()
      cy.get('[data-testid="is-computed-mai-enabled"]').should('exist')
      cy.get('[data-testid="is-culmination-values-enabled"]').should('exist')
      cy.get('[data-testid="is-by-species-enabled"]').should('exist')
      cy.get('[data-testid="inc-secondary-height"]').should('exist')
    })

    it('disables Computed MAI checkbox when CFS Biomass is selected', () => {
      mountPanel((modelStore) => {
        modelStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      })
      cy.get('[data-testid="is-computed-mai-enabled"] input').should('be.disabled')
    })

    it('disables By Species checkbox when CFS Biomass is selected', () => {
      mountPanel((modelStore) => {
        modelStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      })
      cy.get('[data-testid="is-by-species-enabled"] input').should('be.disabled')
    })

    it('disables Secondary Height checkbox when derivedBy is Volume', () => {
      mountPanel((modelStore) => {
        modelStore.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
      })
      cy.get('[data-testid="inc-secondary-height"] input').should('be.disabled')
    })

    it('disables Culmination Values checkbox when age range is not wide enough', () => {
      mountPanel((modelStore) => {
        modelStore.startingAge = '50'
        modelStore.finishingAge = '100'
      })
      cy.get('[data-testid="is-culmination-values-enabled"] input').should('be.disabled')
    })

    it('enables Culmination Values when startingAge <= 10 and finishingAge >= 300', () => {
      mountPanel((modelStore) => {
        modelStore.startingAge = '10'
        modelStore.finishingAge = '300'
      })
      cy.get('[data-testid="is-culmination-values-enabled"] input').should('not.be.disabled')
    })
  })

  describe('Input enabled/disabled state', () => {
    it('inputs are enabled when the panel is editable (default)', () => {
      mountPanel()
      cy.get('[data-testid="starting-age"] input').should('not.be.disabled')
    })

    it('inputs are disabled in view/read-only mode', () => {
      mountPanel((_, appStore) => {
        appStore.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.get('[data-testid="starting-age"] input').should('be.disabled')
    })

    it('inputs are disabled when panel is not editable', () => {
      mountPanel((modelStore) => {
        modelStore.panelState.reportSettings.editable = false
      })
      cy.get('[data-testid="starting-age"] input').should('be.disabled')
    })
  })

  describe('Edit button in header', () => {
    it('is visible in non-read-only mode', () => {
      mountPanel()
      cy.get('.edit-button-col').should('exist')
    })

    it('is not visible in read-only mode', () => {
      mountPanel((_, appStore) => {
        appStore.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.get('.edit-button-col').should('not.exist')
    })

    it('is disabled when the panel is not yet confirmed', () => {
      mountPanel((modelStore) => {
        modelStore.panelState.reportSettings.confirmed = false
        modelStore.panelState.reportSettings.editable = true
      })
      cy.contains('button', 'Edit').should('be.disabled')
    })

    it('is enabled when the panel is confirmed and not editable', () => {
      mountPanel((modelStore) => {
        modelStore.panelState.reportSettings.confirmed = true
        modelStore.panelState.reportSettings.editable = false
      })
      cy.contains('button', 'Edit').should('not.be.disabled')
    })

    it('is disabled when projection status is RUNNING', () => {
      mountPanel((modelStore, appStore) => {
        modelStore.panelState.reportSettings.confirmed = true
        modelStore.panelState.reportSettings.editable = false
        appStore.currentProjectionStatus = CONSTANTS.PROJECTION_STATUS.RUNNING
      })
      cy.contains('button', 'Edit').should('be.disabled')
    })

    it('is disabled when projection status is READY', () => {
      mountPanel((modelStore, appStore) => {
        modelStore.panelState.reportSettings.confirmed = true
        modelStore.panelState.reportSettings.editable = false
        appStore.currentProjectionStatus = CONSTANTS.PROJECTION_STATUS.READY
      })
      cy.contains('button', 'Edit').should('be.disabled')
    })
  })

  describe('Store synchronization', () => {
    it('reflects the store startingAge initial value', () => {
      mountPanel((modelStore) => {
        modelStore.startingAge = '20'
      })
      cy.get('[data-testid="starting-age"] input').should('have.value', '20')
    })

    it('reflects the store startYear initial value when Year range is selected', () => {
      mountPanel((modelStore) => {
        modelStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
        modelStore.startYear = '2020'
      })
      cy.get('[data-testid="start-year"] input').should('have.value', '2020')
    })

    it('reflects isComputedMAIEnabled checkbox state from store', () => {
      mountPanel((modelStore) => {
        modelStore.isComputedMAIEnabled = true
      })
      cy.get('[data-testid="is-computed-mai-enabled"] input').should('be.checked')
    })

    it('reflects isBySpeciesEnabled checkbox state from store', () => {
      mountPanel((modelStore) => {
        modelStore.isBySpeciesEnabled = false
      })
      cy.get('[data-testid="is-by-species-enabled"] input').should('not.be.checked')
    })
  })

  describe('Minimum DBH section', () => {
    it('renders the Min DBH label', () => {
      mountPanel()
      cy.get('.min-dbh-limit-species-group-label').should('exist')
    })
  })
})
