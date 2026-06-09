import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import SpeciesInfoPanel from './SpeciesInfoPanel.vue'
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
  mount(SpeciesInfoPanel, { global: { plugins: [pinia, vuetify] } })
  return { modelStore, appStore }
}

const openPanel = (modelStore: ReturnType<typeof useModelParameterStore>) => {
  modelStore.panelOpenStates.speciesInfo = CONSTANTS.PANEL.OPEN
}

const makeEditable = (modelStore: ReturnType<typeof useModelParameterStore>) => {
  modelStore.panelState.speciesInfo.editable = true
  modelStore.panelState.speciesInfo.confirmed = false
}

const addSpecies = (modelStore: ReturnType<typeof useModelParameterStore>) => {
  modelStore.speciesList = [
    { species: 'FD', percent: '60.0' },
    { species: 'PL', percent: '40.0' },
  ]
}

describe('<SpeciesInfoPanel />', () => {
  describe('Input disabled states', () => {
    it('inputs are disabled when panel is not editable', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('.v-radio-group').should('have.class', 'v-input--disabled')
      cy.contains('button', 'Add Species').should('be.disabled')
    })

    it('inputs are enabled when panel is editable', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
      })
      cy.get('.v-radio-group').should('not.have.class', 'v-input--disabled')
      cy.contains('button', 'Add Species').should('not.be.disabled')
    })
  })

  describe('Add Species and Edit button visibility', () => {
    it('shows "Add Species" and Edit buttons in create mode', () => {
      mountPanel((ms, as) => {
        openPanel(ms)
        as.viewMode = PROJECTION_VIEW_MODE.CREATE
      })
      cy.contains('button', 'Add Species').should('exist')
      cy.get('.edit-button-col').should('exist')
    })

    it('hides "Add Species" and Edit buttons in read-only mode', () => {
      mountPanel((ms, as) => {
        openPanel(ms)
        as.viewMode = PROJECTION_VIEW_MODE.VIEW
      })
      cy.get('.add-species-btn-col').should('not.exist')
      cy.get('.edit-button-col').should('not.exist')
    })

    it('Edit button is disabled when panel is not confirmed', () => {
      mountPanel((ms) => {
        openPanel(ms)
        ms.panelState.speciesInfo.confirmed = false
        ms.panelState.speciesInfo.editable = true
      })
      cy.get('.edit-button-col button').should('be.disabled')
    })
  })

  describe('Total species percent display', () => {
    it('shows total percentage when species are present, hides when none', () => {
      const { modelStore } = mountPanel((ms) => openPanel(ms))
      cy.contains('Total Species Percentage:').should('not.exist')
      cy.then(() => addSpecies(modelStore))
      cy.contains('Total Species Percentage:').should('be.visible')
    })

    it('shows percentage error when total does not equal 100', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
        ms.speciesList = [{ species: 'FD', percent: '50.0' }]
      })
      cy.contains('Percentages must add up to 100%').should('be.visible')
    })
  })

  describe('ActionPanel visibility', () => {
    it('hides ActionPanel in read-only mode', () => {
      mountPanel((ms, as) => {
        openPanel(ms)
        as.viewMode = PROJECTION_VIEW_MODE.VIEW
      })
      cy.contains('button', 'Cancel').should('not.exist')
      cy.contains('button', 'Next').should('not.exist')
    })

    it('shows ActionPanel in create mode when editable', () => {
      mountPanel((ms, as) => {
        openPanel(ms)
        makeEditable(ms)
        as.viewMode = PROJECTION_VIEW_MODE.CREATE
      })
      cy.contains('button', 'Cancel').should('exist')
      cy.contains('button', 'Next').should('exist')
    })
  })
})
