import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import AttachmentsPanel from './AttachmentsPanel.vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import { CONSTANTS } from '@/constants'
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
  const alertDialogStore = useAlertDialogStore()

  if (setup) setup(fileUploadStore, appStore)

  mount(AttachmentsPanel, {
    global: { plugins: [pinia, vuetify] },
  })

  return { fileUploadStore, appStore, alertDialogStore }
}

const mountViewMode = (extraSetup?: (fu: ReturnType<typeof useFileUploadStore>) => void) =>
  mountPanel((fu, app) => {
    app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
    fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
    if (extraSetup) extraSetup(fu)
  })

describe('<AttachmentsPanel />', () => {
  it('renders the "File Upload" panel title', () => {
    mountPanel()
    cy.contains('.text-h6', 'File Upload').should('exist')
  })

  it('panel content is not in the DOM when the panel is initially closed', () => {
    mountPanel()
    cy.get('.bcds-file-upload').should('not.exist')
  })

  it('renders both polygon and layer file upload components when open', () => {
    mountPanel((fu) => {
      fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
    })
    cy.contains('.bcds-file-input-label', 'Select Polygon File').should('be.visible')
    cy.contains('.bcds-file-input-label', 'Select Layer File').should('be.visible')
    cy.get('.bcds-file-upload').should('have.length', 2)
  })

  it('shows filename and delete button when file info is set', () => {
    mountPanel((fu) => {
      fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      fu.setPolygonFileInfo({ filename: 'polygon.csv', fileMappingGUID: 'poly-guid', fileSetGUID: 'set-1' })
      fu.setLayerFileInfo({ filename: 'layer.csv', fileMappingGUID: 'layer-guid', fileSetGUID: 'set-2' })
    })
    cy.get('.file-upload-col-left .uploaded-file-name').should('contain.text', 'polygon.csv')
    cy.get('.file-upload-col-right .uploaded-file-name').should('contain.text', 'layer.csv')
    cy.get('.uploaded-file-delete-btn').should('have.length', 2)
  })

  it('view mode: shows labels without "Select" and applies disabled styling', () => {
    mountViewMode()
    cy.contains('.bcds-file-input-label', 'Polygon File').should('be.visible')
    cy.contains('.bcds-file-input-label', 'Select Polygon File').should('not.exist')
    cy.contains('.bcds-file-input-label', 'Layer File').should('be.visible')
    cy.get('.bcds-file-input-label--disabled').should('have.length', 2)
  })

  it('view mode: shows file-display-containers instead of file upload controls', () => {
    mountViewMode()
    cy.get('.file-display-container').should('have.length', 2)
    cy.get('.bcds-file-upload').should('not.exist')
  })

  it('view mode: shows "No file uploaded" for both slots when no files are set', () => {
    mountViewMode()
    cy.get('.file-upload-col-left .file-display-empty').should('contain.text', 'No file uploaded')
    cy.get('.file-upload-col-right .file-display-empty').should('contain.text', 'No file uploaded')
  })

  it('view mode: shows filename with icon and no delete buttons when file info is set', () => {
    mountViewMode((fu) => {
      fu.setPolygonFileInfo({ filename: 'polygon.csv', fileMappingGUID: 'poly-guid', fileSetGUID: 'set-1' })
      fu.setLayerFileInfo({ filename: 'layer.csv', fileMappingGUID: 'layer-guid', fileSetGUID: 'set-2' })
    })
    cy.get('.file-upload-col-left .file-display-name').should('contain.text', 'polygon.csv')
    cy.get('.file-upload-col-left .file-display-icon').should('exist')
    cy.get('.file-upload-col-right .file-display-name').should('contain.text', 'layer.csv')
    cy.get('.uploaded-file-delete-btn').should('not.exist')
  })

  it('opens the confirmation dialog when a delete button is clicked', () => {
    const { alertDialogStore } = mountPanel((fu) => {
      fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      fu.setPolygonFileInfo({ filename: 'polygon.csv', fileMappingGUID: 'poly-guid', fileSetGUID: 'set-1' })
    })

    cy.get('.file-upload-col-left .uploaded-file-delete-btn').click()

    cy.then(() => {
      expect(alertDialogStore.dialog).to.be.true
      alertDialogStore.cancel()
    })
  })

  it('preserves file info when the delete dialog is cancelled', () => {
    const { fileUploadStore, alertDialogStore } = mountPanel((fu) => {
      fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      fu.setPolygonFileInfo({ filename: 'polygon.csv', fileMappingGUID: 'poly-guid', fileSetGUID: 'set-1' })
    })

    cy.get('.file-upload-col-left .uploaded-file-delete-btn').click()

    cy.then(() => alertDialogStore.cancel())

    cy.then(() => {
      expect(fileUploadStore.polygonFileInfo).to.not.be.null
      expect(fileUploadStore.polygonFileInfo?.filename).to.equal('polygon.csv')
    })
  })
})
