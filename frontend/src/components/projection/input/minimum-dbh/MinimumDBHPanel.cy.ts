import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import MinimumDBHPanel from './MinimumDBHPanel.vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS, BIZCONSTANTS } from '@/constants'
import { PROJECTION_VIEW_MODE, PROJECTION_STATUS } from '@/constants/constants'

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

  describe('Expansion panel chevron icon', () => {
    it('shows mdi-chevron-down when the panel is closed', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.CLOSE
      })
      cy.get('.expansion-panel-icon').should('have.class', 'mdi-chevron-down')
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

    it('is disabled when projection status is RUNNING', () => {
      mountPanel((fu, app) => {
        fu.confirmPanel('reportConfig')
        fu.confirmPanel('minimumDBH')
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
        app.setCurrentProjectionStatus(PROJECTION_STATUS.RUNNING)
      })
      cy.get('.edit-button-col button').should('be.disabled')
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

    it('renders a slider for each species group', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.v-slider').should('have.length', BIZCONSTANTS.SPECIES_GROUPS.length)
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
    it('is not rendered in view mode', () => {
      mountPanel((fu, app) => {
        fu.initializeSpeciesGroups()
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.contains('button', 'Next').should('not.exist')
      cy.contains('button', 'Cancel').should('not.exist')
    })

    it('renders "Next" and "Cancel" buttons in edit mode', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.contains('button', 'Next').should('exist')
      cy.contains('button', 'Cancel').should('exist')
    })

    it('"Next" and "Cancel" are disabled when the panel is not editable', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.contains('button', 'Next').should('be.disabled')
      cy.contains('button', 'Cancel').should('be.disabled')
    })

    it('"Next" and "Cancel" are enabled when the panel is editable', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.confirmPanel('reportConfig')
      })
      cy.contains('button', 'Next').should('not.be.disabled')
      cy.contains('button', 'Cancel').should('not.be.disabled')
    })
  })
})
