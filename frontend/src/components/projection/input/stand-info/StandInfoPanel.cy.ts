import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import StandInfoPanel from './StandInfoPanel.vue'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS, MESSAGE } from '@/constants'
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
  mount(StandInfoPanel, { global: { plugins: [pinia, vuetify] } })
  return { modelStore, appStore }
}

const openPanel = (modelStore: ReturnType<typeof useModelParameterStore>) => {
  modelStore.panelOpenStates.standInfo = CONSTANTS.PANEL.OPEN
}

const makeEditable = (modelStore: ReturnType<typeof useModelParameterStore>) => {
  modelStore.panelState.standInfo.editable = true
  modelStore.panelState.standInfo.confirmed = false
}

describe('<StandInfoPanel />', () => {
  describe('Panel structure', () => {
    it('renders the "Stand Information" heading', () => {
      mountPanel()
      cy.contains('.text-h6', 'Stand Information').should('exist')
    })

    it('renders all four input fields when panel is open', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('[data-testid="percent-stockable-area"]').should('exist')
      cy.get('[data-testid="crown-closure"]').should('exist')
      cy.get('[data-testid="basal-area"]').should('exist')
      cy.get('[data-testid="trees-per-hectare"]').should('exist')
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
    it('inputs are disabled when panel is not editable', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('[data-testid="percent-stockable-area"]')
        .find('.v-text-field')
        .should('have.class', 'v-input--disabled')
    })

    it('Percent Stockable Area is enabled when panel is editable', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
      })
      cy.get('[data-testid="percent-stockable-area"]')
        .find('.v-text-field')
        .should('not.have.class', 'v-input--disabled')
    })

    it('Basal Area shows N/A placeholder when derivedBy is VOLUME', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
        ms.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
        ms.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.get('[data-testid="basal-area"]')
        .find('input')
        .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
    })

    it('Crown Closure shows N/A placeholder when derivedBy is BASAL_AREA', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
        ms.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
        ms.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.get('[data-testid="crown-closure"]')
        .find('input')
        .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
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

  describe('Header Edit button', () => {
    it('renders Edit button in the header when not in read-only mode', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('.edit-button-col').should('exist')
    })

    it('does not render Edit button in read-only mode', () => {
      mountPanel((ms, as) => {
        openPanel(ms)
        as.viewMode = PROJECTION_VIEW_MODE.VIEW
      })
      cy.get('.edit-button-col').should('not.exist')
    })
  })

  describe('Confirm validation errors', () => {
    it('shows required error when Percent Stockable Area is empty', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
        ms.percentStockableArea = null
        ms.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
        ms.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.contains('button', 'Next').click()
      cy.get('[data-testid="percent-stockable-area"]')
        .find('.v-messages__message')
        .should('contain.text', MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_PCT_STCB_AREA_REQ)
    })

    it('shows range error when Percent Stockable Area is out of range', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
        ms.percentStockableArea = '150'
        ms.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
        ms.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.contains('button', 'Next').click()
      cy.get('[data-testid="percent-stockable-area"]')
        .find('.v-messages__message')
        .should('contain.text', MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_PCT_STCB_AREA_RNG)
    })
  })

  describe('Min DBH Limit display', () => {
    it('does not render min-dbh-limit block when minDBHLimit is null', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('[data-testid="min-dbh-limit"]').should('not.exist')
    })

    it('renders min-dbh-limit block when minDBHLimit is set', () => {
      const { modelStore } = mountPanel((ms) => openPanel(ms))
      cy.then(() => { modelStore.minDBHLimit = '12.5 cm' })
      cy.get('[data-testid="min-dbh-limit"]').should('exist')
      cy.get('.min-dbh-value').should('contain', '12.5 cm')
    })
  })
})
