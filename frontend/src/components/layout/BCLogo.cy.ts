import BCLogo from './BCLogo.vue'

describe('BCLogo', () => {
  beforeEach(() => {
    // Add custom styles to set background color
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        #header-bc-logo-img {
          background-color: rgb(0, 51, 102) !important;
        }
      `
      doc.head.appendChild(style)
    })
  })

  it('renders properly', () => {
    cy.mount(BCLogo)

    cy.get('#header-bc-logo-img').should('exist')
    cy.get('img').should('have.attr', 'alt', 'B.C. Government Logo')
  })

  it('applies default props correctly', () => {
    cy.mount(BCLogo)

    cy.get('#header-bc-logo-img')
      .should('have.attr', 'style')
      .and('include', 'max-height: 50px')
    cy.get('#header-bc-logo-img')
      .should('have.attr', 'style')
      .and('include', 'max-width: 150px')
    cy.get('#header-bc-logo-img')
      .should('have.attr', 'style')
      .and('include', 'margin-left: 15px')
  })

  it('applies custom props correctly', () => {
    cy.mount(BCLogo, {
      props: {
        maxHeight: 100,
        maxWidth: 300,
        marginLeft: 30,
      },
    })

    cy.get('#header-bc-logo-img')
      .should('have.attr', 'style')
      .and('include', 'max-height: 100px')
    cy.get('#header-bc-logo-img')
      .should('have.attr', 'style')
      .and('include', 'max-width: 300px')
    cy.get('#header-bc-logo-img')
      .should('have.attr', 'style')
      .and('include', 'margin-left: 30px')
  })

  it('loads the correct image', () => {
    cy.mount(BCLogo)

    cy.get('#header-bc-logo-img')
      .should('exist')
      .find('img')
      .should('have.attr', 'src')
      .and('include', 'gov-bc-logo-horiz')
  })
})
