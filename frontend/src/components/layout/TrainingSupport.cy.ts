import { mount } from 'cypress/vue'
import TrainingSupport from './TrainingSupport.vue'

describe('TrainingSupport.vue', () => {
  it('renders default text and style correctly', () => {
    mount(TrainingSupport)

    cy.get('span')
      .should('have.class', 'header-training-support')
      .and('have.css', 'font-size', '14px')
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
      .and('have.class', 'header-training-support')
  })

  it('applies custom styles when customStyle prop is provided', () => {
    mount(TrainingSupport, {
      props: {
        text: 'Custom Styled Text',
        customStyle: {
          fontSize: '18px',
          color: '#1976d2',
          fontWeight: 'bold',
          textDecoration: 'underline',
        },
      },
    })

    cy.get('span')
      .should('have.css', 'font-size', '18px')
      .and('have.css', 'color', 'rgb(25, 118, 210)')
      .and('have.css', 'font-weight', '700')
      .and('have.css', 'text-decoration-line', 'underline')
      .and('not.have.class', 'header-training-support')
      .and('contain.text', 'Custom Styled Text')
  })
})
