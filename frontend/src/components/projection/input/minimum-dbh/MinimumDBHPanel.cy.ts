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
      mountPanel()
      // Both desktop and mobile titles start with this string
      cy.contains('.text-h6', 'Minimum DBH Limit by Species Group').should('exist')
    })

    it('panel content is not in the DOM when initially closed', () => {
      // minimumDBH starts CLOSE by default in the store
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

    it('shows mdi-chevron-up when the panel is open', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.expansion-panel-icon').should('have.class', 'mdi-chevron-up')
    })
  })

  describe('Header Edit button', () => {
    it('is not rendered in view mode (isReadOnly = true)', () => {
      mountPanel((fu, app) => {
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.get('.edit-button-col').should('not.exist')
    })

    it('is rendered in non-read-only mode', () => {
      mountPanel()
      cy.get('.edit-button-col').should('exist')
    })

    it('is disabled when the panel has not been confirmed yet', () => {
      // Default state: confirmed=false, editable=false -> isHeaderEditActive=false
      mountPanel()
      cy.get('.edit-button-col button').should('be.disabled')
    })

    it('is enabled when the panel is confirmed and not currently editable', () => {
      mountPanel((fu) => {
        // Confirm both sequential panels; minimumDBH becomes confirmed+closed
        fu.confirmPanel('reportInfo')
        fu.confirmPanel('minimumDBH')
        // Re-open the panel so the title row with the Edit button is visible
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.edit-button-col button').should('not.be.disabled')
    })

    it('is disabled when projection status is RUNNING (even if confirmed)', () => {
      mountPanel((fu, app) => {
        fu.confirmPanel('reportInfo')
        fu.confirmPanel('minimumDBH')
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
        app.setCurrentProjectionStatus(PROJECTION_STATUS.RUNNING)
      })
      cy.get('.edit-button-col button').should('be.disabled')
    })

    it('is disabled when projection status is READY (even if confirmed)', () => {
      mountPanel((fu, app) => {
        fu.confirmPanel('reportInfo')
        fu.confirmPanel('minimumDBH')
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
        app.setCurrentProjectionStatus(PROJECTION_STATUS.READY)
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

    it('displays the first species group label "AC"', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.min-dbh-species-group-label').first().should('contain.text', 'AC')
    })

    it('renders a slider for each species group', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.v-slider').should('have.length', BIZCONSTANTS.SPECIES_GROUPS.length)
    })

    it('species group labels have the disabled class when panel is not editable', () => {
      // Default: editable=false -> isMinDBHDeactivated=true
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.min-dbh-species-group-label.min-dbh-disabled').should(
        'have.length',
        BIZCONSTANTS.SPECIES_GROUPS.length,
      )
    })

    it('sliders are disabled when panel is not editable', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.v-slider.v-input--disabled').should('have.length', BIZCONSTANTS.SPECIES_GROUPS.length)
    })

    it('species labels do NOT have the disabled class when panel is editable', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.confirmPanel('reportInfo') // makes minimumDBH editable
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.min-dbh-species-group-label.min-dbh-disabled').should('not.exist')
    })

    it('sliders are enabled when panel is editable and projection type is not CFS Biomass', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.confirmPanel('reportInfo') // makes minimumDBH editable
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.v-slider.v-input--disabled').should('not.exist')
    })

    it('sliders are disabled when projectionType is CFS Biomass (regardless of editable state)', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.confirmPanel('reportInfo') // makes minimumDBH editable
        fu.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.v-slider.v-input--disabled').should('have.length', BIZCONSTANTS.SPECIES_GROUPS.length)
    })

    it('species labels have disabled class when projectionType is CFS Biomass', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.confirmPanel('reportInfo') // makes minimumDBH editable
        fu.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.get('.min-dbh-species-group-label.min-dbh-disabled').should(
        'have.length',
        BIZCONSTANTS.SPECIES_GROUPS.length,
      )
    })
  })

  describe('ActionPanel', () => {
    it('is not rendered in view mode', () => {
      mountPanel((fu, app) => {
        fu.initializeSpeciesGroups()
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      // ActionPanel has v-if="!isReadOnly"
      cy.contains('button', 'Next').should('not.exist')
      cy.contains('button', 'Cancel').should('not.exist')
    })

    it('renders the "Next" button in edit mode', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.contains('button', 'Next').should('exist')
    })

    it('renders the "Cancel" button in edit mode', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.contains('button', 'Cancel').should('exist')
    })

    it('"Next" and "Cancel" are disabled when the panel is not editable', () => {
      // Default: panelState.minimumDBH.editable=false -> isConfirmEnabled=false
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
        fu.confirmPanel('reportInfo') // opens minimumDBH and makes it editable
      })
      // minimumDBH is now open (confirmPanel('reportInfo') opens it)
      cy.contains('button', 'Next').should('not.be.disabled')
      cy.contains('button', 'Cancel').should('not.be.disabled')
    })

    it('does not render the "Clear" button (hideClearButton=true)', () => {
      mountPanel((fu) => {
        fu.initializeSpeciesGroups()
        fu.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      })
      cy.contains('button', 'Clear').should('not.exist')
    })
  })
})
