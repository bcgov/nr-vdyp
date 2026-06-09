import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import MinimumDBHPanel from './MinimumDBHPanel.vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS, BIZCONSTANTS } from '@/constants'
import { PROJECTION_VIEW_MODE } from '@/constants/constants'

const vuetify = createVuetify()

const mountPanel = (
  setup?: (
    fileUploadStore: ReturnType<typeof useFileUploadStore>,
    appStore: ReturnType<typeof useAppStore>,
  ) => void,
) => {
  const pinia = createPinia()
  setActivePinia(pinia)

  const fileUploadStore = useFileUploadStore()
  const appStore = useAppStore()

  if (setup) setup(fileUploadStore, appStore)

  mount(MinimumDBHPanel, {
    global: { plugins: [pinia, vuetify] },
  })

  return { fileUploadStore, appStore }
}

describe('<MinimumDBHPanel />', () => {
  describe('Panel structure', () => {
    it('renders the panel title "Minimum DBH Limit by Species Group"', () => {
      cy.viewport(1280, 800)
      mountPanel()
      cy.contains('.text-h6', 'Minimum DBH Limit by Species Group').should('exist')
    })

    it('panel content is not in the DOM when initially closed', () => {
      mountPanel()
      cy.get('.min-dbh-row').should('not.exist')
    })
  })

  describe('Header Edit button', () => {
    it('is not rendered in view mode', () => {
      mountPanel((fu, app) => {
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.get('.edit-button-col').should('not.exist')
    })

    it('is disabled when the panel has not been confirmed yet', () => {
      mountPanel()
      cy.get('.edit-button-col button').should('be.disabled')
    })

    it('is enabled when the panel is confirmed and not currently editable', () => {
      mountPanel((fu) => {
        fu.confirmPanel('reportConfig')
        fu.confirmPanel('minimumDBH')
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.edit-button-col button').should('not.be.disabled')
    })
  })

  describe('Species groups', () => {
    it('renders one row per species group', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.min-dbh-row').should('have.length', BIZCONSTANTS.SPECIES_GROUPS.length)
    })

    it('sliders are disabled when panel is not editable', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.v-slider.v-input--disabled').should('have.length', BIZCONSTANTS.SPECIES_GROUPS.length)
    })

    it('sliders are enabled when panel is editable and projection type is not CFS Biomass', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.confirmPanel('reportConfig')
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.v-slider.v-input--disabled').should('not.exist')
    })

    it('sliders are disabled when projectionType is CFS Biomass', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.confirmPanel('reportConfig')
        fu.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.v-slider.v-input--disabled').should('have.length', BIZCONSTANTS.SPECIES_GROUPS.length)
    })
  })

  describe('ActionPanel', () => {
    it('"Next" is enabled and "Cancel" is disabled (no changes yet) when the panel is editable', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.confirmPanel('reportConfig')
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.contains('button', 'Next').should('not.be.disabled')
      cy.contains('button', 'Cancel').should('be.disabled')
    })
  })
})
