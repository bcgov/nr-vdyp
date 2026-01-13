import { mount } from 'cypress/vue'
import TrainingSupport from './TrainingSupport.vue'

describe('TrainingSupport.vue', () => {
  it('renders default text correctly', () => {
    mount(TrainingSupport)

    cy.get('span')
      .should('have.class', 'bcds-header-link')
      .and('contain.text', 'Training and Support')
  })

  it('renders with custom text', () => {
    mount(TrainingSupport, {
      props: {
        text: 'Custom Training Text',
      },
    })

    cy.get('span')
      .should('contain.text', 'Custom Training Text')
      .and('have.class', 'bcds-header-link')
  })
})
