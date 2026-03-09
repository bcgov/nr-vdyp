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
        fu.panelOpenStates.reportInfo = CONSTANTS.PANEL.CLOSE
      })
      cy.get('#reportTitle').should('not.exist')
    })
  })

  describe('Expansion panel chevron icon', () => {
    it('shows mdi-chevron-up when the panel is open (default)', () => {
      mountPanel()
      cy.get('.expansion-panel-icon').should('have.class', 'mdi-chevron-up')
    })

    it('shows mdi-chevron-down when the panel is closed', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.reportInfo = CONSTANTS.PANEL.CLOSE
      })
      cy.get('.expansion-panel-icon').should('have.class', 'mdi-chevron-down')
    })
  })

  describe('Form fields', () => {
    it('renders "Report Title (Required)" label', () => {
      mountPanel()
      cy.contains('label', 'Report Title (Required)').should('exist')
    })

    it('renders the report title text field with placeholder', () => {
      mountPanel()
      cy.get('#reportTitle').should('have.attr', 'placeholder', 'Enter a report title...')
    })

    it('renders "Projection Type" label', () => {
      mountPanel()
      cy.contains('label', 'Projection Type').should('exist')
    })

    it('renders "Volume" and "CFS Biomass" radio options', () => {
      mountPanel()
      cy.contains('.v-label', 'Volume').should('exist')
      cy.contains('.v-label', 'CFS Biomass').should('exist')
    })

    it('renders the description textarea with placeholder', () => {
      mountPanel()
      cy.get('#reportDescription').should(
        'have.attr',
        'placeholder',
        'Provide a description of this Projection...',
      )
    })

    it('shows "0/500" counter when description is empty', () => {
      mountPanel()
      cy.contains('.counter', '0/500').should('exist')
    })

    it('updates the counter as description text is typed', () => {
      mountPanel()
      cy.get('#reportDescription').type('Hello')
      cy.contains('.counter', '5/500').should('exist')
    })

    it('renders "Numeric Range Value" label', () => {
      mountPanel()
      cy.contains('.numeric-range-value-label', 'Numeric Range Value').should('exist')
    })

    it('renders "Age" and "Year" radio options for numeric range', () => {
      mountPanel()
      cy.contains('.v-label', 'Age').should('exist')
      cy.contains('.v-label', 'Year').should('exist')
    })

    it('renders "Include following values in Report" section', () => {
      mountPanel()
      cy.contains('.include-in-report-label', 'Include following values in Report').should('exist')
    })

    it('renders all checkboxes in "Include following values in Report"', () => {
      mountPanel()
      cy.get('[data-testid="is-by-layer-enabled"]').should('exist')
      cy.get('[data-testid="is-by-species-enabled"]').should('exist')
      cy.get('[data-testid="inc-secondary-height"]').should('exist')
      cy.get('[data-testid="is-projection-mode-enabled"]').should('exist')
      cy.get('[data-testid="is-polygon-id-enabled"]').should('exist')
      cy.get('[data-testid="is-current-year-enabled"]').should('exist')
      cy.get('[data-testid="is-reference-year-enabled"]').should('exist')
    })
  })

  describe('Age range fields (default)', () => {
    it('renders Starting Age, Finishing Age, and Increment fields when Age is selected', () => {
      mountPanel()
      cy.get('[data-testid="starting-age"]').should('exist')
      cy.get('[data-testid="finishing-age"]').should('exist')
      cy.get('[data-testid="age-increment"]').should('exist')
    })

    it('does not render Year fields when Age is selected', () => {
      mountPanel()
      cy.get('[data-testid="start-year"]').should('not.exist')
      cy.get('[data-testid="end-year"]').should('not.exist')
    })
  })

  describe('Input enabled/disabled state', () => {
    it('inputs are enabled when the panel is editable (default)', () => {
      mountPanel()
      cy.get('#reportTitle').should('not.be.disabled')
      cy.get('#reportDescription').should('not.be.disabled')
    })

    it('inputs are disabled in read-only mode', () => {
      mountPanel((_, app) => {
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.get('#reportTitle').should('be.disabled')
      cy.get('#reportDescription').should('be.disabled')
    })

    it('inputs are disabled when panel is not editable', () => {
      mountPanel((fu) => {
        fu.panelState.reportInfo.editable = false
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

    it('does not show an error when the title has a value on blur', () => {
      mountPanel()
      cy.get('#reportTitle').type('My Report')
      cy.get('#reportTitle').blur()
      cy.contains('Report Title is required.').should('not.exist')
    })
  })

  describe('Store synchronization', () => {
    it('reflects the store reportTitle initial value in the text field', () => {
      mountPanel((fu) => {
        fu.reportTitle = 'Stored Title'
      })
      cy.get('#reportTitle').should('have.value', 'Stored Title')
    })

    it('updates store reportTitle when the user types in the field', () => {
      const { fileUploadStore } = mountPanel()
      cy.get('#reportTitle').type('New Title')
      cy.then(() => {
        expect(fileUploadStore.reportTitle).to.equal('New Title')
      })
    })

    it('updates store reportDescription when the user types in the textarea', () => {
      const { fileUploadStore } = mountPanel()
      cy.get('#reportDescription').type('A description')
      cy.then(() => {
        expect(fileUploadStore.reportDescription).to.equal('A description')
      })
    })

    it('disables "By Species" checkbox when CFS Biomass projection type is selected', () => {
      mountPanel((fu) => {
        fu.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      })
      cy.get('[data-testid="is-by-species-enabled"] input').should('be.disabled')
    })
  })

  describe('Edit button in header', () => {
    it('is visible in non-read-only mode', () => {
      mountPanel()
      cy.get('.edit-button-col').should('exist')
    })

    it('is not visible in read-only mode', () => {
      mountPanel((_, app) => {
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.get('.edit-button-col').should('not.exist')
    })

    it('is disabled when the panel is not yet confirmed', () => {
      mountPanel((fu) => {
        fu.panelState.reportInfo.confirmed = false
        fu.panelState.reportInfo.editable = true
      })
      cy.contains('button', 'Edit').should('be.disabled')
    })

    it('is enabled when the panel is confirmed and not editable', () => {
      mountPanel((fu) => {
        fu.panelState.reportInfo.confirmed = true
        fu.panelState.reportInfo.editable = false
      })
      cy.contains('button', 'Edit').should('not.be.disabled')
    })

    it('is disabled when projection status is RUNNING', () => {
      mountPanel((fu, app) => {
        fu.panelState.reportInfo.confirmed = true
        fu.panelState.reportInfo.editable = false
        app.setCurrentProjectionStatus(PROJECTION_STATUS.RUNNING)
      })
      cy.contains('button', 'Edit').should('be.disabled')
    })

    it('is disabled when projection status is READY', () => {
      mountPanel((fu, app) => {
        fu.panelState.reportInfo.confirmed = true
        fu.panelState.reportInfo.editable = false
        app.setCurrentProjectionStatus(PROJECTION_STATUS.READY)
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

    it('renders "Next" and "Cancel" buttons in edit mode', () => {
      mountPanel()
      cy.contains('button', 'Next').should('exist')
      cy.contains('button', 'Cancel').should('exist')
    })

    it('"Next" and "Cancel" are enabled when the panel is editable (default)', () => {
      mountPanel()
      cy.contains('button', 'Next').should('not.be.disabled')
      cy.contains('button', 'Cancel').should('not.be.disabled')
    })

    it('"Next" and "Cancel" are disabled when the panel is not editable', () => {
      mountPanel((fu) => {
        fu.panelState.reportInfo.editable = false
      })
      cy.contains('button', 'Next').should('be.disabled')
      cy.contains('button', 'Cancel').should('be.disabled')
    })

    it('does not render the "Clear" button (hideClearButton=true)', () => {
      mountPanel()
      cy.contains('button', 'Clear').should('not.exist')
    })

    it('clicking "Next" with an empty title shows a validation error', () => {
      mountPanel((fu) => {
        fu.reportTitle = null
      })
      cy.contains('button', 'Next').click()
      cy.contains('Report Title is required.').should('exist')
    })

    it('clicking "Next" with a whitespace-only title shows a validation error', () => {
      mountPanel((fu) => {
        fu.reportTitle = '   '
      })
      cy.contains('button', 'Next').click()
      cy.contains('Report Title is required.').should('exist')
    })
  })
})
