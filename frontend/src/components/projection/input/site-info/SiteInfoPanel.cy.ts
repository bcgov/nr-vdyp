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
  it('renders the "Site Information" heading', () => {
    mountPanel()
    cy.contains('.text-h6', 'Site Information').should('exist')
  })

  it('shows panel content when open', () => {
    mountPanel((ms) => openPanel(ms))
    cy.get('.v-expansion-panel-text').should('be.visible')
  })

  it('hides panel content when closed', () => {
    mountPanel()
    cy.get('.v-expansion-panel-text').should('not.be.visible')
  })

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

  it('Site Index radio group is disabled when panel is not editable', () => {
    mountPanel((ms) => openPanel(ms))
    cy.get('.site-index-container .v-radio-group').should('have.class', 'v-input--disabled')
  })

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

  it('does not render Edit button in read-only mode', () => {
    mountPanel((ms, as) => {
      openPanel(ms)
      as.viewMode = PROJECTION_VIEW_MODE.VIEW
    })
    cy.get('.edit-button-col').should('not.exist')
  })
})
