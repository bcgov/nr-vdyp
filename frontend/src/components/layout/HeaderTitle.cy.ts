import HeaderTitle from './HeaderTitle.vue'

describe('HeaderTitle.vue', () => {
  it('renders the default text', () => {
    cy.mount(HeaderTitle, {
      props: {
        text: 'VARIABLE DENSITY YIELD PROJECTION',
      },
    })

    cy.get('.bcds-header--title')
      .should('exist')
      .and('have.text', 'VARIABLE DENSITY YIELD PROJECTION')
  })

  it('renders with custom text', () => {
    cy.mount(HeaderTitle, {
      props: {
        text: 'Custom Header Title',
      },
    })

    cy.get('.bcds-header--title')
      .should('exist')
      .and('have.text', 'Custom Header Title')
  })
})
