import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import FileUpload from './FileUpload.vue'
import { CONSTANTS, MESSAGE } from '@/constants'

const vuetify = createVuetify()

describe('FileUpload.vue', () => {
  beforeEach(() => {
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
    cy.get('[id="startingAge"]').should('exist')
    cy.get('[id="finishingAge"]').should('exist')
    cy.get('[id="ageIncrement"]').should('exist')
    cy.get('input[type="file"]').should('have.length', 2) // Layer file and Polygon file
    cy.contains('Layer File').should('exist')
    cy.contains('Polygon File').should('exist')
    cy.get('button').contains('Run Model').should('exist')
  })

  it('shows a validation error for invalid starting and finishing ages', () => {
    mount(FileUpload, {
      global: {
        plugins: [vuetify],
      },
    })

    // Set invalid values for Starting Age and Finishing Age
    cy.get('[id="startingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear() // Clear the input value
        cy.wrap(input).type('200') // Type a new invalid value
      })

    cy.get('[id="finishingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear() // Clear the input value
        cy.wrap(input).type('50') // Type a new invalid value
      })

    // Click the Confirm button
    cy.get('button').contains('Run Model').click()

    // Ensure the dialog appears with an error message
    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains('Invalid Input').should('exist')
        cy.contains(MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_FNSH_AGE).should(
          'exist',
        )
      })
  })

  it('validates required fields before running the model', () => {
    mount(FileUpload, {
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('[id="startingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
      })

    // Click the Run Model button without filling any fields
    cy.get('button').contains('Run Model').click()

    // Check if a validation error message is displayed
    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(MESSAGE.FILE_UPLOAD_ERR.RPT_VLD_REQUIRED_FIELDS).should(
          'exist',
        )
      })
  })

  it('validates age range values', () => {
    mount(FileUpload, {
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('[id="finishingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('600')
      })

    cy.get('button').contains('Run Model').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT).should('exist')
        cy.contains(
          MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_FNSH_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
          ),
        ).should('exist')
      })
  })

  it('validates uploaded files', () => {
    mount(FileUpload, {
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('button').contains('Run Model').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_FILE).should('exist')
        cy.contains(MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_MISSING).should('exist')
      })
  })

  it('validates incorrect file format', () => {
    mount(FileUpload, {
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

    cy.get('button').contains('Run Model').click()

    cy.get('.v-dialog')
      .should('exist')
      .within(() => {
        cy.contains(MESSAGE.MSG_DIALOG_TITLE.INVALID_FILE).should('exist')
        cy.contains(MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_NOT_CSV_FORMAT).should(
          'exist',
        )
      })
  })

  it('validates correct form submission', () => {
    mount(FileUpload, {
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('[id="startingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('50')
      })
    cy.get('[id="finishingAge"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('100')
      })
    cy.get('[id="ageIncrement"]')
      .should('exist')
      .then((input) => {
        cy.wrap(input).clear()
        cy.wrap(input).type('10')
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

    cy.get('button').contains('Run Model').click()

    // Verify that the progress indicator is displayed
    cy.get('.centered-progress.progress-wrapper').should('exist')
    cy.get('.v-progress-circular.v-progress-circular--indeterminate').should(
      'exist',
    )
    cy.get('.message').should('contain.text', 'Running Model...')
  })
})
