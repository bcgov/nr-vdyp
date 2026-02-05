import { mount } from 'cypress/vue'
import ModelSelectionContainer from './ModelSelectionContainer.vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import { MODEL_SELECTION, HEADER_SELECTION } from '@/constants/constants'

describe('ModelSelectionContainer.vue', () => {
  const vuetify = createVuetify()

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

  it('renders the default model selection', () => {
    mount(ModelSelectionContainer, {
      global: {
        plugins: [vuetify],
      },
    })

    // Check that the default selection is 'File Upload'
    cy.get('.v-select').should('exist')
    cy.get('.v-select').should('contain.text', 'File Upload')

    // Verify the default text in the h3 tag
    cy.get('#modelSelectionCard h3').should(
      'contain.text',
      HEADER_SELECTION.FILE_UPLOAD,
    )
  })

  it('emits the selected model value and updates the display text when changed', () => {
    const onUpdateModelSelectionSpy = cy.spy().as('updateModelSelectionSpy')

    mount(ModelSelectionContainer, {
      global: {
        plugins: [vuetify],
      },
      props: {
        'onUpdate:modelSelection': onUpdateModelSelectionSpy,
      },
    })

    // Open the dropdown
    cy.get('.v-select').click()

    // Select "Manual Input"
    cy.get('.v-list-item').contains('Manual Input').click()

    // Verify that the selected value is updated
    cy.get('.v-select').should('contain.text', 'Manual Input')

    // Verify the emitted event
    cy.get('@updateModelSelectionSpy').should(
      'have.been.calledWith',
      MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )

    // Verify that the h3 tag text is updated accordingly
    cy.get('#modelSelectionCard h3').should(
      'contain.text',
      HEADER_SELECTION.MODEL_PARAMETER_SELECTION,
    )
  })
})
