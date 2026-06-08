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
  it('renders title and content fields when panel is open', () => {
    mountPanel()
    cy.contains('.text-h6', 'Report Settings').should('exist')
    cy.get('[data-testid="starting-age"]').should('exist')
  })

  it('hides content when panel is closed', () => {
    mountPanel((modelStore) => {
      modelStore.panelOpenStates.reportSettings = CONSTANTS.PANEL.CLOSE
    })
    cy.get('[data-testid="starting-age"]').should('not.exist')
  })

  it('shows Age fields and hides Year fields when Age range selected (default)', () => {
    mountPanel()
    cy.get('[data-testid="starting-age"]').should('exist')
    cy.get('[data-testid="finishing-age"]').should('exist')
    cy.get('[data-testid="age-increment"]').should('exist')
    cy.get('[data-testid="start-year"]').should('not.exist')
    cy.get('[data-testid="end-year"]').should('not.exist')
  })

  it('shows Year fields and hides Age fields when Year range selected', () => {
    mountPanel((modelStore) => {
      modelStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
    })
    cy.get('[data-testid="start-year"]').should('exist')
    cy.get('[data-testid="end-year"]').should('exist')
    cy.get('[data-testid="year-increment"]').should('exist')
    cy.get('[data-testid="starting-age"]').should('not.exist')
    cy.get('[data-testid="finishing-age"]').should('not.exist')
  })

  it('disables Computed MAI and By Species checkboxes when CFS Biomass is selected', () => {
    mountPanel((modelStore) => {
      modelStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
    })
    cy.get('[data-testid="is-computed-mai-enabled"] input').should('be.disabled')
    cy.get('[data-testid="is-by-species-enabled"] input').should('be.disabled')
  })

  it('disables Culmination Values when age range is not wide enough', () => {
    mountPanel((modelStore) => {
      modelStore.startingAge = '50'
      modelStore.finishingAge = '100'
    })
    cy.get('[data-testid="is-culmination-values-enabled"] input').should('be.disabled')
  })

  it('disables Secondary Height when derivedBy is Volume', () => {
    mountPanel((modelStore) => {
      modelStore.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
    })
    cy.get('[data-testid="inc-secondary-height"] input').should('be.disabled')
  })

  it('disables inputs in view/read-only mode', () => {
    mountPanel((_, appStore) => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.VIEW)
    })
    cy.get('[data-testid="starting-age"] input').should('be.disabled')
  })

  it('hides Edit button in read-only mode', () => {
    mountPanel((_, appStore) => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.VIEW)
    })
    cy.get('.edit-button-col').should('not.exist')
  })

  it('disables Edit button when projection is RUNNING', () => {
    mountPanel((modelStore, appStore) => {
      modelStore.panelState.reportSettings.confirmed = true
      modelStore.panelState.reportSettings.editable = false
      appStore.currentProjectionStatus = CONSTANTS.PROJECTION_STATUS.RUNNING
    })
    cy.contains('button', 'Edit').should('be.disabled')
  })
})
