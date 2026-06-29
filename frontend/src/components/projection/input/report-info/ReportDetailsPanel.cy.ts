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
  it('shows panel content when open (default)', () => {
    mountPanel()
    cy.get('#manualReportTitle').should('exist')
  })

  it('hides panel content when closed', () => {
    mountPanel((modelStore) => {
      modelStore.panelOpenStates.reportDetails = CONSTANTS.PANEL.CLOSE
    })
    cy.get('#manualReportTitle').should('not.exist')
  })

  it('disables inputs in view mode and when panel is not editable', () => {
    mountPanel((_, app) => {
      app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
    })
    cy.get('#manualReportTitle').should('be.disabled')
    cy.get('#manualReportDescription').should('be.disabled')

    mountPanel((modelStore) => {
      modelStore.panelState.reportDetails.editable = false
    })
    cy.get('#manualReportTitle').should('be.disabled')
    cy.get('#manualReportDescription').should('be.disabled')
  })

  it('shows validation error when title is empty on blur', () => {
    mountPanel()
    cy.get('#manualReportTitle').focus()
    cy.get('#manualReportTitle').blur()
    cy.contains('Report Title is required.').should('exist')
  })

  it('reflects the store reportTitle value in the text field', () => {
    mountPanel((modelStore) => {
      modelStore.reportTitle = 'Stored Title'
    })
    cy.get('#manualReportTitle').should('have.value', 'Stored Title')
  })

  it('hides ActionPanel in view mode and shows Next/Cancel in edit mode', () => {
    mountPanel((_, app) => {
      app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
    })
    cy.contains('button', 'Next').should('not.exist')
    cy.contains('button', 'Cancel').should('not.exist')

    mountPanel()
    cy.contains('button', 'Next').should('exist')
    cy.contains('button', 'Cancel').should('exist')
  })

  it('disables Next and Cancel when panel is not editable', () => {
    mountPanel((modelStore) => {
      modelStore.panelState.reportDetails.editable = false
    })
    cy.contains('button', 'Next').should('be.disabled')
    cy.contains('button', 'Cancel').should('be.disabled')
  })

  it('clicking Edit button makes the panel editable', () => {
    const { modelStore } = mountPanel((modelStore) => {
      modelStore.panelState.reportDetails.confirmed = true
      modelStore.panelState.reportDetails.editable = false
    })
    cy.contains('button', 'Edit').click({ force: true })
    cy.then(() => {
      expect(modelStore.panelState.reportDetails.editable).to.be.true
    })
  })

  it('disables Edit button when projection is RUNNING', () => {
    mountPanel((modelStore, appStore) => {
      modelStore.panelState.reportDetails.confirmed = true
      modelStore.panelState.reportDetails.editable = false
      appStore.currentProjectionStatus = CONSTANTS.PROJECTION_STATUS.RUNNING
    })
    cy.contains('button', 'Edit').should('be.disabled')
  })

  it('disables Edit button when projection is QUEUED', () => {
    mountPanel((modelStore, appStore) => {
      modelStore.panelState.reportDetails.confirmed = true
      modelStore.panelState.reportDetails.editable = false
      appStore.currentProjectionStatus = CONSTANTS.PROJECTION_STATUS.QUEUED
    })
    cy.contains('button', 'Edit').should('be.disabled')
  })
})
