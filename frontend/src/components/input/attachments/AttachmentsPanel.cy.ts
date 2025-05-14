import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import AttachmentsPanel from './AttachmentsPanel.vue'
import { setActivePinia, createPinia } from 'pinia'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import { CONSTANTS, MESSAGE } from '@/constants'

const vuetify = createVuetify()

describe('AttachmentsPanel.vue', () => {
  let fileUploadStore

  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)
    fileUploadStore = useFileUploadStore()

    fileUploadStore.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
    fileUploadStore.panelState.attachments.editable = true

    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        body {
          background-color: rgb(240, 240, 240) !important;
        }
      `
      doc.head.appendChild(style)
    })
  })

  it('renders the initial layout correctly', () => {
    mount(AttachmentsPanel, {
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-expansion-panel').should('exist')
    cy.contains('Attachments').should('exist')

    cy.get('.v-expansion-panel-text').should('exist')

    cy.get('.v-file-input').should('have.length', 2)
    cy.contains('Select Polygon File...').should('exist')
    cy.contains('Select Layer File...').should('exist')
    cy.get('button').contains('Confirm').should('exist')
    cy.get('button').contains('Clear').should('exist')
  })

  it('allows file selection and displays file names', () => {
    mount(AttachmentsPanel, {
      global: {
        plugins: [vuetify],
      },
    })

    const polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })
    const layerFile = new File(['layer content'], 'layer.csv', {
      type: 'text/csv',
    })

    cy.get('.v-file-input').first().should('not.be.disabled')
    cy.get('.v-file-input').last().should('not.be.disabled')

    cy.get('.v-file-input').first().find('input[type="file"]').selectFile({
      contents: polygonFile,
      fileName: 'polygon.csv',
      mimeType: 'text/csv',
    })
    cy.get('.v-file-input').last().find('input[type="file"]').selectFile({
      contents: layerFile,
      fileName: 'layer.csv',
      mimeType: 'text/csv',
    })

    cy.contains('polygon.csv').should('exist')
    cy.contains('layer.csv').should('exist')
  })

  it('shows validation error when confirming without files', () => {
    mount(AttachmentsPanel, {
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog').should('exist')
    cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_FILE).should('exist')
    cy.contains(MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_MISSING).should('exist')
  })

  it('validates incorrect file format', () => {
    mount(AttachmentsPanel, {
      global: {
        plugins: [vuetify],
      },
    })

    const invalidFile = new File(['invalid content'], 'invalid.txt', {
      type: 'text/plain',
    })
    cy.get('input[type="file"]').first().selectFile({
      contents: invalidFile,
      fileName: 'invalid.txt',
      mimeType: 'text/plain',
    })

    const polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })
    cy.get('input[type="file"]').last().selectFile({
      contents: polygonFile,
      fileName: 'polygon.csv',
    })

    cy.get('button').contains('Confirm').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_FILE).should('exist')
        cy.contains(MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_NOT_CSV_FORMAT).should(
          'exist',
        )
      })
  })

  it('confirms successfully with valid files', () => {
    mount(AttachmentsPanel, {
      global: {
        plugins: [vuetify],
      },
    })

    const polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })
    const layerFile = new File(['layer content'], 'layer.csv', {
      type: 'text/csv',
    })

    cy.get('.v-file-input').first().find('input[type="file"]').selectFile({
      contents: polygonFile,
      fileName: 'polygon.csv',
      mimeType: 'text/csv',
    })
    cy.get('.v-file-input').last().find('input[type="file"]').selectFile({
      contents: layerFile,
      fileName: 'layer.csv',
      mimeType: 'text/csv',
    })

    cy.get('button').contains('Confirm').click()

    cy.get('button').contains('Edit').should('exist')
  })

  it('clears files when clear button is clicked', () => {
    mount(AttachmentsPanel, {
      global: {
        plugins: [vuetify],
      },
    })

    const polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })

    cy.get('.v-file-input').first().find('input[type="file"]').selectFile({
      contents: polygonFile,
      fileName: 'polygon.csv',
      mimeType: 'text/csv',
    })

    cy.contains('polygon.csv').should('exist')

    cy.get('button').contains('Clear').click()

    cy.contains('polygon.csv').should('not.exist')
  })

  it('enables edit mode when edit button is clicked', () => {
    mount(AttachmentsPanel, {
      global: {
        plugins: [vuetify],
      },
    })

    const polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })
    const layerFile = new File(['layer content'], 'layer.csv', {
      type: 'text/csv',
    })

    cy.get('.v-file-input').first().find('input[type="file"]').selectFile({
      contents: polygonFile,
      fileName: 'polygon.csv',
      mimeType: 'text/csv',
    })
    cy.get('.v-file-input').last().find('input[type="file"]').selectFile({
      contents: layerFile,
      fileName: 'layer.csv',
      mimeType: 'text/csv',
    })

    cy.get('button').contains('Confirm').click()

    cy.get('button').contains('Edit').click()

    cy.get('.v-file-input').first().should('not.be.disabled')
  })
})
