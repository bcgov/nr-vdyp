import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import SiteInfoPanel from './SiteInfoPanel.vue'
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
  if (setup) setup(modelStore, appStore)
  mount(SiteInfoPanel, { global: { plugins: [pinia, vuetify] } })
  return { modelStore, appStore }
}

const openPanel = (modelStore: ReturnType<typeof useModelParameterStore>) => {
  modelStore.panelOpenStates.siteInfo = CONSTANTS.PANEL.OPEN
}

const makeEditable = (modelStore: ReturnType<typeof useModelParameterStore>) => {
  modelStore.panelState.siteInfo.editable = true
  modelStore.panelState.siteInfo.confirmed = false
}

describe('<SiteInfoPanel />', () => {
  describe('Panel structure', () => {
    it('renders the "Site Information" heading', () => {
      mountPanel()
      cy.contains('.text-h6', 'Site Information').should('exist')
    })

    it('renders BEC Zone label and select', () => {
      mountPanel((ms) => openPanel(ms))
      cy.contains('label', 'BEC Zone').should('exist')
      cy.get('#bec-zone-select').should('exist')
    })

    it('renders Eco Zone label and select', () => {
      mountPanel((ms) => openPanel(ms))
      cy.contains('label', 'Eco Zone').should('exist')
      cy.get('#eco-zone-select').should('exist')
    })

    it('renders Site Index radio group', () => {
      mountPanel((ms) => openPanel(ms))
      cy.contains('label', 'Site Index:').should('exist')
      cy.get('.site-index-container .v-radio-group').should('exist')
    })
  })

  describe('Expansion panel toggle', () => {
    it('shows panel content when open', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('.v-expansion-panel-text').should('be.visible')
    })

    it('hides panel content when closed', () => {
      mountPanel()
      cy.get('.v-expansion-panel-text').should('not.be.visible')
    })

    it('shows chevron-down icon when panel is closed', () => {
      mountPanel()
      cy.get('i.expansion-panel-icon').should('have.class', 'mdi-chevron-down')
    })

    it('shows chevron-up icon when panel is open', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('i.expansion-panel-icon').should('have.class', 'mdi-chevron-up')
    })
  })

  describe('Input disabled states', () => {
    it('BEC Zone is disabled when panel is not editable', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('#bec-zone-select').closest('.v-select').should('have.class', 'v-input--disabled')
    })

    it('BEC Zone is enabled when panel is editable', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
      })
      cy.get('#bec-zone-select').closest('.v-select').should('not.have.class', 'v-input--disabled')
    })

    it('Eco Zone is disabled when panel is not editable', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('#eco-zone-select').closest('.v-select').should('have.class', 'v-input--disabled')
    })

    it('Site Index radio group is disabled when panel is not editable', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('.site-index-container .v-radio-group').should('have.class', 'v-input--disabled')
    })

    it('Site Index radio group is enabled when panel is editable', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
        ms.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.get('.site-index-container .v-radio-group').should('not.have.class', 'v-input--disabled')
    })
  })

  describe('ActionPanel visibility', () => {
    it('hides ActionPanel buttons in read-only mode', () => {
      mountPanel((ms, as) => {
        openPanel(ms)
        as.viewMode = PROJECTION_VIEW_MODE.VIEW
      })
      cy.contains('button', 'Cancel').should('not.exist')
      cy.contains('button', 'Next').should('not.exist')
    })

    it('shows ActionPanel buttons in create mode', () => {
      mountPanel((ms, as) => {
        openPanel(ms)
        makeEditable(ms)
        as.viewMode = PROJECTION_VIEW_MODE.CREATE
      })
      cy.contains('button', 'Cancel').should('exist')
      cy.contains('button', 'Next').should('exist')
    })
  })

  describe('SiteIndicesTable (feature flag on)', () => {
    it('renders SiteIndicesTable subtitle when VITE_SITE_INDICES_TABLE_ENABLED is true', () => {
      mountPanel((ms) => openPanel(ms))
      cy.contains('.text-subtitle-2', 'Site Indices').should('exist')
    })

    it('renders the site indices table', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('table').should('exist')
    })

    it('does not render Site Species select in new layout', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('[data-testid="selected-site-species"]').should('not.exist')
    })
  })
})
