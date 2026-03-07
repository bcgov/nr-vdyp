import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import ReportDetailsPanel from './ReportDetailsPanel.vue'
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

  mount(ReportDetailsPanel, {
    global: { plugins: [pinia, vuetify] },
  })

  return { modelStore, appStore }
}

describe('<ReportDetailsPanel />', () => {
  describe('Panel structure', () => {
    it('renders the "Report Details" panel title', () => {
      mountPanel()
      cy.contains('.text-h6', 'Report Details').should('exist')
    })

    it('panel content is in the DOM when the panel is open (default)', () => {
      mountPanel()
      cy.get('#manualReportTitle').should('exist')
    })

    it('panel content is not in the DOM when the panel is closed', () => {
      mountPanel((modelStore) => {
        modelStore.panelOpenStates.detailsInfo = CONSTANTS.PANEL.CLOSE
      })
      cy.get('#manualReportTitle').should('not.exist')
    })
  })

  describe('Expansion panel chevron icon', () => {
    it('shows mdi-chevron-up when the panel is open (default)', () => {
      mountPanel()
      cy.get('.expansion-panel-icon').should('have.class', 'mdi-chevron-up')
    })

    it('shows mdi-chevron-down when the panel is closed', () => {
      mountPanel((modelStore) => {
        modelStore.panelOpenStates.detailsInfo = CONSTANTS.PANEL.CLOSE
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
      cy.get('#manualReportTitle').should('have.attr', 'placeholder', 'Enter a report title...')
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
      cy.get('#manualReportDescription').should(
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
      cy.get('#manualReportDescription').type('Hello')
      cy.contains('.counter', '5/500').should('exist')
    })
  })

  describe('Input enabled/disabled state', () => {
    it('inputs are enabled when the panel is editable (default)', () => {
      mountPanel()
      cy.get('#manualReportTitle').should('not.be.disabled')
      cy.get('#manualReportDescription').should('not.be.disabled')
    })

    it('inputs are disabled in view/read-only mode', () => {
      mountPanel((_, app) => {
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      })
      cy.get('#manualReportTitle').should('be.disabled')
      cy.get('#manualReportDescription').should('be.disabled')
    })

    it('inputs are disabled when panel is not editable', () => {
      mountPanel((modelStore) => {
        modelStore.panelState.detailsInfo.editable = false
      })
      cy.get('#manualReportTitle').should('be.disabled')
      cy.get('#manualReportDescription').should('be.disabled')
    })
  })

  describe('Title validation', () => {
    it('shows an error message when the title is empty on blur', () => {
      mountPanel()
      cy.get('#manualReportTitle').focus()
      cy.get('#manualReportTitle').blur()
      cy.contains('Report Title is required.').should('exist')
    })

    it('does not show an error when the title has a value on blur', () => {
      mountPanel()
      cy.get('#manualReportTitle').type('My Report')
      cy.get('#manualReportTitle').blur()
      cy.contains('Report Title is required.').should('not.exist')
    })
  })

  describe('Store synchronization', () => {
    it('reflects the store reportTitle initial value in the text field', () => {
      mountPanel((modelStore) => {
        modelStore.reportTitle = 'Stored Title'
      })
      cy.get('#manualReportTitle').should('have.value', 'Stored Title')
    })

    it('updates store reportTitle when the user types in the field', () => {
      const { modelStore } = mountPanel()
      cy.get('#manualReportTitle').type('New Title')
      cy.then(() => {
        expect(modelStore.reportTitle).to.equal('New Title')
      })
    })

    it('updates store reportDescription when the user types in the textarea', () => {
      const { modelStore } = mountPanel()
      cy.get('#manualReportDescription').type('A description')
      cy.then(() => {
        expect(modelStore.reportDescription).to.equal('A description')
      })
    })
  })

  describe('ActionPanel', () => {
    it('is not rendered in view/read-only mode', () => {
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
      mountPanel((modelStore) => {
        modelStore.panelState.detailsInfo.editable = false
      })
      cy.contains('button', 'Next').should('be.disabled')
      cy.contains('button', 'Cancel').should('be.disabled')
    })

    it('does not render the "Clear" button (hideClearButton=true)', () => {
      mountPanel()
      cy.contains('button', 'Clear').should('not.exist')
    })
  })
})
