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

describe('<AttachmentsPanel />', () => {
  describe('Panel structure', () => {
    it('renders the "File Upload" panel title', () => {
      mountPanel()
      cy.contains('.text-h6', 'File Upload').should('exist')
    })

    it('panel content is not in the DOM when the panel is initially closed', () => {
      // attachments panel defaults to CLOSE in the store
      mountPanel()
      cy.get('.bcds-file-upload').should('not.exist')
    })
  })

  describe('Expansion panel chevron icon', () => {
    it('shows mdi-chevron-down when the panel is closed', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.CLOSE
      })
      // Vuetify renders <v-icon> as <i> with the icon name as a CSS class
      cy.get('.expansion-panel-icon').should('have.class', 'mdi-chevron-down')
    })

    it('shows mdi-chevron-up when the panel is open', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      })
      cy.get('.expansion-panel-icon').should('have.class', 'mdi-chevron-up')
    })
  })

  describe('Edit mode – file upload (panel open)', () => {
    it('shows "Select Polygon File" label', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      })
      cy.contains('.bcds-file-input-label', 'Select Polygon File').should('be.visible')
    })

    it('shows "Select Layer File" label', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      })
      cy.contains('.bcds-file-input-label', 'Select Layer File').should('be.visible')
    })

    it('renders both file upload components (polygon and layer)', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      })
      cy.get('.bcds-file-upload').should('have.length', 2)
    })

    it('does not show uploaded-file-info when no files have been uploaded', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      })
      cy.get('.uploaded-file-info').should('not.exist')
    })
  })

  describe('Edit mode - uploaded file info', () => {
    it('shows polygon filename and delete button when polygonFileInfo is set', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
        fu.setPolygonFileInfo({
          filename: 'polygon_data.csv',
          fileMappingGUID: 'poly-guid',
          fileSetGUID: 'set-1',
        })
      })

      cy.get('.file-upload-col-left .uploaded-file-name').should('contain.text', 'polygon_data.csv')
      cy.get('.file-upload-col-left .uploaded-file-delete-btn').should('exist')
    })

    it('shows layer filename and delete button when layerFileInfo is set', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
        fu.setLayerFileInfo({
          filename: 'layer_data.csv',
          fileMappingGUID: 'layer-guid',
          fileSetGUID: 'set-2',
        })
      })

      cy.get('.file-upload-col-right .uploaded-file-name').should('contain.text', 'layer_data.csv')
      cy.get('.file-upload-col-right .uploaded-file-delete-btn').should('exist')
    })

    it('delete button is enabled in edit mode', () => {
      mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
        fu.setPolygonFileInfo({
          filename: 'polygon.csv',
          fileMappingGUID: 'poly-guid',
          fileSetGUID: 'set-1',
        })
      })

      cy.get('.uploaded-file-delete-btn').first().should('not.be.disabled')
    })

    it('updates polygon filename reactively when polygonFileInfo changes after mount', () => {
      const { fileUploadStore } = mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      })

      cy.then(() => {
        fileUploadStore.setPolygonFileInfo({
          filename: 'updated_polygon.csv',
          fileMappingGUID: 'poly-guid',
          fileSetGUID: 'set-1',
        })
      })

      cy.get('.uploaded-file-name').first().should('contain.text', 'updated_polygon.csv')
    })
  })

  describe('View mode (isReadOnly = true)', () => {
    const mountViewMode = (
      extraSetup?: (fu: ReturnType<typeof useFileUploadStore>) => void,
    ) =>
      mountPanel((fu, app) => {
        app.setViewMode(PROJECTION_VIEW_MODE.VIEW)
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
        if (extraSetup) extraSetup(fu)
      })

    it('shows "Polygon File" label (without "Select") in view mode', () => {
      mountViewMode()
      cy.contains('.bcds-file-input-label', 'Polygon File').should('be.visible')
      cy.contains('.bcds-file-input-label', 'Select Polygon File').should('not.exist')
    })

    it('shows "Layer File" label (without "Select") in view mode', () => {
      mountViewMode()
      cy.contains('.bcds-file-input-label', 'Layer File').should('be.visible')
      cy.contains('.bcds-file-input-label', 'Select Layer File').should('not.exist')
    })

    it('applies disabled CSS class to both labels in view mode', () => {
      mountViewMode()
      cy.get('.bcds-file-input-label--disabled').should('have.length', 2)
    })

    it('shows file-display-container elements instead of v-file-upload in view mode', () => {
      mountViewMode()
      cy.get('.file-display-container').should('have.length', 2)
      cy.get('.bcds-file-upload').should('not.exist')
    })

    it('shows "No file uploaded" for polygon when no file info is available', () => {
      mountViewMode()
      cy.get('.file-upload-col-left .file-display-empty').should('contain.text', 'No file uploaded')
    })

    it('shows "No file uploaded" for layer when no file info is available', () => {
      mountViewMode()
      cy.get('.file-upload-col-right .file-display-empty').should('contain.text', 'No file uploaded')
    })

    it('shows polygon filename with paperclip icon when polygonFileInfo is set', () => {
      mountViewMode((fu) => {
        fu.setPolygonFileInfo({
          filename: 'polygon.csv',
          fileMappingGUID: 'poly-guid',
          fileSetGUID: 'set-1',
        })
      })

      cy.get('.file-upload-col-left .file-display-name').should('contain.text', 'polygon.csv')
      cy.get('.file-upload-col-left .file-display-icon').should('exist')
    })

    it('shows layer filename with paperclip icon when layerFileInfo is set', () => {
      mountViewMode((fu) => {
        fu.setLayerFileInfo({
          filename: 'layer.csv',
          fileMappingGUID: 'layer-guid',
          fileSetGUID: 'set-2',
        })
      })

      cy.get('.file-upload-col-right .file-display-name').should('contain.text', 'layer.csv')
      cy.get('.file-upload-col-right .file-display-icon').should('exist')
    })

    it('does not show delete buttons in view mode', () => {
      mountViewMode((fu) => {
        fu.setPolygonFileInfo({
          filename: 'polygon.csv',
          fileMappingGUID: 'poly-guid',
          fileSetGUID: 'set-1',
        })
      })

      cy.get('.uploaded-file-delete-btn').should('not.exist')
    })
  })

  describe('Delete button interaction', () => {
    it('opens the confirmation dialog when the polygon delete button is clicked', () => {
      const { alertDialogStore } = mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
        fu.setPolygonFileInfo({
          filename: 'polygon.csv',
          fileMappingGUID: 'poly-guid',
          fileSetGUID: 'set-1',
        })
      })

      cy.get('.file-upload-col-left .uploaded-file-delete-btn').click()

      cy.then(() => {
        expect(alertDialogStore.dialog).to.be.true
        // Cancel to avoid a dangling promise
        alertDialogStore.cancel()
      })
    })

    it('preserves polygon file info when the delete dialog is cancelled', () => {
      const { fileUploadStore, alertDialogStore } = mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
        fu.setPolygonFileInfo({
          filename: 'polygon.csv',
          fileMappingGUID: 'poly-guid',
          fileSetGUID: 'set-1',
        })
      })

      cy.get('.file-upload-col-left .uploaded-file-delete-btn').click()

      cy.then(() => alertDialogStore.cancel())

      cy.then(() => {
        expect(fileUploadStore.polygonFileInfo).to.not.be.null
        expect(fileUploadStore.polygonFileInfo?.filename).to.equal('polygon.csv')
      })
    })

    it('opens the confirmation dialog when the layer delete button is clicked', () => {
      const { alertDialogStore } = mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
        fu.setLayerFileInfo({
          filename: 'layer.csv',
          fileMappingGUID: 'layer-guid',
          fileSetGUID: 'set-2',
        })
      })

      cy.get('.file-upload-col-right .uploaded-file-delete-btn').click()

      cy.then(() => {
        expect(alertDialogStore.dialog).to.be.true
        alertDialogStore.cancel()
      })
    })

    it('preserves layer file info when the delete dialog is cancelled', () => {
      const { fileUploadStore, alertDialogStore } = mountPanel((fu) => {
        fu.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
        fu.setLayerFileInfo({
          filename: 'layer.csv',
          fileMappingGUID: 'layer-guid',
          fileSetGUID: 'set-2',
        })
      })

      cy.get('.file-upload-col-right .uploaded-file-delete-btn').click()

      cy.then(() => alertDialogStore.cancel())

      cy.then(() => {
        expect(fileUploadStore.layerFileInfo).to.not.be.null
        expect(fileUploadStore.layerFileInfo?.filename).to.equal('layer.csv')
      })
    })
  })
})
