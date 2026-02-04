import ModelSelection from './ModelSelection.vue'
import { createVuetify } from 'vuetify'
import { MODEL_SELECTION } from '@/constants/constants'

describe('ModelSelection.vue', () => {
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
    cy.mount(ModelSelection, {
      global: {
        plugins: [vuetify],
      },
    })

    // Check that the default selection is 'File Upload'
    cy.get('.v-select').should('exist')
    cy.get('.v-select').should('contain.text', 'File Upload')
  })

  it('emits the selected model value when changed', () => {
    const onUpdateModelSelectionSpy = cy.spy().as('updateModelSelectionSpy')

    cy.mount(ModelSelection, {
      global: {
        plugins: [vuetify],
      },
      props: {
        'onUpdate:modelSelection': onUpdateModelSelectionSpy,
      },
    })

    // Open the dropdown
    cy.get('.v-select').click()

    // Select a new option
    cy.get('.v-list-item').contains('Manual Input').click()

    // Verify that the selected value is updated
    cy.get('.v-select').should('contain.text', 'Manual Input')

    // Verify that the emit event is triggered
    cy.get('@updateModelSelectionSpy').should(
      'have.been.calledWith',
      MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
    )
  })
})
