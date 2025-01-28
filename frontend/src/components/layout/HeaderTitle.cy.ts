import HeaderTitle from './HeaderTitle.vue'

describe('HeaderTitle.vue', () => {
  it('renders the default text', () => {
    cy.mount(HeaderTitle, {
      props: {
        text: 'VARIABLE DENSITY YIELD PROJECTION',
      },
    })

    cy.get('.header-toolbar-title')
      .should('exist')
      .and('have.text', 'VARIABLE DENSITY YIELD PROJECTION')
      .and('have.css', 'text-align', 'center')
      .and('have.css', 'font-weight', '300')
  })

  it('renders with custom text', () => {
    cy.mount(HeaderTitle, {
      props: {
        text: 'Custom Header Title',
      },
    })

    cy.get('.header-toolbar-title')
      .should('exist')
      .and('have.text', 'Custom Header Title')
  })

  it('renders with custom style', () => {
    cy.mount(HeaderTitle, {
      props: {
        text: 'Custom Styled Header Title',
        style: {
          textAlign: 'left',
          fontWeight: 'bold',
          fontSize: '24px',
          color: '#1976d2',
        },
      },
    })

    cy.get('div.v-toolbar-title')
      .should('exist')
      .and('have.text', 'Custom Styled Header Title')
      .and('have.css', 'text-align', 'left')
      .and('have.css', 'font-weight', '700')
      .and('have.css', 'font-size', '24px')
      .and('have.css', 'color', 'rgb(25, 118, 210)')
  })
})
