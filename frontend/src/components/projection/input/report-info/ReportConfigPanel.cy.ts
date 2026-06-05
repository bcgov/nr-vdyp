import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import ReportConfigPanel from './ReportConfigPanel.vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'
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

  mount(ReportConfigPanel, {
    global: { plugins: [pinia, vuetify] },
  })

  return { fileUploadStore, appStore }
}

describe('<ReportConfigPanel />', () => {
  describe('Panel structure', () => {
    it('renders the "Report Details" panel title', () => {
      mountPanel()
      cy.contains('.text-h6', 'Report Details').should('exist')
    })

    it('panel content is in the DOM when the panel is open (default)', () => {
      mountPanel()
      cy.get('#reportTitle').should('exist')
    })

    it('panel content is not in the DOM when the panel is closed', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.reportConfig = CONSTANTS.PANEL.CLOSE
      })
      cy.get('#reportTitle').should('not.exist')
    })
  })

  describe('Form fields', () => {
    it('renders "Volume" and "CFS Biomass" radio options for projection type', () => {
      mountPanel()
      cy.contains('.v-label', 'Volume').should('exist')
      cy.contains('.v-label', 'CFS Biomass').should('exist')
    })

    it('renders all checkboxes in "Include following values in Report"', () => {
      mountPanel()
      cy.get('[data-testid="is-by-species-enabled"]').should('exist')
      cy.get('[data-testid="inc-secondary-height"]').should('exist')
      cy.get('[data-testid="is-projection-mode-enabled"]').should('exist')
      cy.get('[data-testid="is-polygon-id-enabled"]').should('exist')
      cy.get('[data-testid="is-current-year-enabled"]').should('exist')
      cy.get('[data-testid="is-reference-year-enabled"]').should('exist')
    })

    it('renders Starting Age, Finishing Age, and Increment fields when Age is selected (default)', () => {
      mountPanel()
      cy.get('[data-testid="starting-age"]').should('exist')
      cy.get('[data-testid="finishing-age"]').should('exist')
      cy.get('[data-testid="age-increment"]').should('exist')
    })
  })

  describe('Input enabled/disabled state', () => {
    it('inputs are disabled in read-only mode', () => {
      mountPanel((_, app) => {
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.get('#reportTitle').should('be.disabled')
      cy.get('#reportDescription').should('be.disabled')
    })

    it('inputs are disabled when panel is not editable', () => {
      mountPanel((fu) => {
        fu.panelState.reportConfig.editable = false
      })
      cy.get('#reportTitle').should('be.disabled')
      cy.get('#reportDescription').should('be.disabled')
    })
  })

  describe('Title validation', () => {
    it('shows an error message when the title is empty on blur', () => {
      mountPanel()
      cy.get('#reportTitle').focus()
      cy.get('#reportTitle').blur()
      cy.contains('Report Title is required.').should('exist')
    })
  })

  describe('Store synchronization', () => {
    it('reflects the store reportTitle initial value in the text field', () => {
      mountPanel((fu) => {
        fu.reportTitle = 'Stored Title'
      })
      cy.get('#reportTitle').should('have.value', 'Stored Title')
    })

    it('disables "By Species" checkbox when CFS Biomass projection type is selected', () => {
      mountPanel((fu) => {
        fu.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      })
      cy.get('[data-testid="is-by-species-enabled"] input').should('be.disabled')
    })
  })

  describe('Edit button in header', () => {
    it('is not visible in read-only mode', () => {
      mountPanel((_, app) => {
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.get('.edit-button-col').should('not.exist')
    })

    it('is disabled when the panel is not yet confirmed', () => {
      mountPanel((fu) => {
        fu.panelState.reportConfig.confirmed = false
        fu.panelState.reportConfig.editable = true
      })
      cy.contains('button', 'Edit').should('be.disabled')
    })

    it('is disabled when projection is running or ready', () => {
      mountPanel((fu, app) => {
        fu.panelState.reportConfig.confirmed = true
        fu.panelState.reportConfig.editable = false
        app.setCurrentProjectionStatus(PROJECTION_STATUS.RUNNING)
      })
      cy.contains('button', 'Edit').should('be.disabled')
    })
  })

  describe('ActionPanel', () => {
    it('is not rendered in read-only mode', () => {
      mountPanel((_, app) => {
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.contains('button', 'Next').should('not.exist')
      cy.contains('button', 'Cancel').should('not.exist')
    })

    it('"Next" and "Cancel" are disabled when the panel is not editable', () => {
      mountPanel((fu) => {
        fu.panelState.reportConfig.editable = false
      })
      cy.contains('button', 'Next').should('be.disabled')
      cy.contains('button', 'Cancel').should('be.disabled')
    })
  })
})
