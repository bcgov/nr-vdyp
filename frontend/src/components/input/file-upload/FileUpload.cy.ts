import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import FileUpload from './FileUpload.vue'

const vuetify = createVuetify()

describe('FileUpload.vue', () => {
  beforeEach(() => {
    cy.viewport(1024, 768)

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
    mount(FileUpload, {
      global: {
        plugins: [vuetify],
      },
    })

    // Check if the form and inputs are rendered
    cy.get('form').should('exist')
    cy.get('input[type="file"]').should('have.length', 2) // Layer file and Polygon file
    cy.contains('Layer File').should('exist')
    cy.contains('Polygon File').should('exist')
    cy.get('button').contains('Run Model').should('exist')
  })

  it('uploads layer and polygon files', () => {
    mount(FileUpload, {
      global: {
        plugins: [vuetify],
      },
    })

    // Simulate file upload for Layer File
    const layerFile = new File(['layer content'], 'layer.csv', {
      type: 'text/csv',
    })
    cy.get('input[type="file"]').first().selectFile({
      contents: layerFile,
      fileName: 'layer.csv',
      mimeType: 'text/csv',
    })

    // Simulate file upload for Polygon File
    const polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })
    cy.get('input[type="file"]').last().selectFile({
      contents: polygonFile,
      fileName: 'polygon.csv',
      mimeType: 'text/csv',
    })

    // Verify that files are uploaded
    cy.get('input[type="file"]')
      .first()
      .should('have.value', 'C:\\fakepath\\layer.csv')
    cy.get('input[type="file"]')
      .last()
      .should('have.value', 'C:\\fakepath\\polygon.csv')
  })

  it('validates required fields before running the model', () => {
    mount(FileUpload, {
      global: {
        plugins: [vuetify],
      },
    })

    // Click the Run Model button without filling any fields
    cy.get('button').contains('Run Model').click()

    // Check if a validation error message is displayed
    cy.get('.v-card-title.popup-header')
      .should('exist')
      .and('contain.text', 'Invalid File!')
  })

  it('shows progress indicator when running the model', () => {
    mount(FileUpload, {
      global: {
        plugins: [vuetify],
      },
    })

    // Mock file uploads
    const layerFile = new File(['layer content'], 'layer.csv', {
      type: 'text/csv',
    })
    const polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })

    cy.get('input[type="file"]').first().selectFile({
      contents: layerFile,
      fileName: 'layer.csv',
      mimeType: 'text/csv',
    })

    cy.get('input[type="file"]').last().selectFile({
      contents: polygonFile,
      fileName: 'polygon.csv',
    })

    // Click the Run Model button
    cy.get('button').contains('Run Model').click()

    // Verify that the progress indicator is displayed
    cy.get('.centered-progress.progress-wrapper').should('exist') // Check the progress wrapper
    cy.get('.v-progress-circular.v-progress-circular--indeterminate').should(
      'exist',
    ) // Check the circular progress
    cy.get('.message').should('contain.text', 'Running Model...') // Check the progress message
  })
})
