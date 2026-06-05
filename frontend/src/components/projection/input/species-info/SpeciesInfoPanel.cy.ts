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
    it('radio group is disabled when panel is not editable', () => {
      mountPanel((ms) => openPanel(ms))
      cy.get('.v-radio-group').should('have.class', 'v-input--disabled')
    })

    it('radio group is enabled when panel is editable', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
      })
      cy.get('.v-radio-group').should('not.have.class', 'v-input--disabled')
    })

    it('"Add Species" button is disabled when panel is not editable', () => {
      mountPanel((ms) => openPanel(ms))
      cy.contains('button', 'Add Species').should('be.disabled')
    })

    it('"Add Species" button is enabled when panel is editable', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
      })
      cy.contains('button', 'Add Species').should('not.be.disabled')
    })
  })

  describe('Add Species button visibility', () => {
    it('shows "Add Species" button in create mode', () => {
      mountPanel((ms, as) => {
        openPanel(ms)
        as.viewMode = PROJECTION_VIEW_MODE.CREATE
      })
      cy.contains('button', 'Add Species').should('exist')
    })

    it('hides "Add Species" button in read-only mode', () => {
      mountPanel((ms, as) => {
        openPanel(ms)
        as.viewMode = PROJECTION_VIEW_MODE.VIEW
      })
      cy.get('.add-species-btn-col').should('not.exist')
    })
  })

  describe('Total species percent display', () => {
    it('shows total species percentage when species are present', () => {
      mountPanel((ms) => {
        openPanel(ms)
        addSpecies(ms)
      })
      cy.contains('Total Species Percentage:').should('be.visible')
    })

    it('does not show total percentage row when no species are added', () => {
      mountPanel((ms) => openPanel(ms))
      cy.contains('Total Species Percentage:').should('not.exist')
    })

    it('shows percentage error when total does not equal 100', () => {
      mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
        ms.speciesList = [{ species: 'FD', percent: '50.0' }]
      })
      cy.contains('Percentages must add up to 100%').should('be.visible')
    })

    it('does not show percentage error when total equals 100', () => {
      const { modelStore } = mountPanel((ms) => {
        openPanel(ms)
        makeEditable(ms)
      })
      cy.then(() => {
        modelStore.speciesList = [
          { species: 'FD', percent: '60.0' },
          { species: 'PL', percent: '40.0' },
        ]
      })
      cy.contains('Percentages must add up to 100%').should('not.exist')
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

    it('Edit button is disabled when panel is not confirmed', () => {
      mountPanel((ms) => {
        openPanel(ms)
        ms.panelState.speciesInfo.confirmed = false
        ms.panelState.speciesInfo.editable = true
      })
      cy.get('.edit-button-col button').should('be.disabled')
    })
  })
})
