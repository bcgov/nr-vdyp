import HeaderTitle from './HeaderTitle.vue'

describe('HeaderTitle.vue', () => {
  it('renders the default text', () => {
    cy.mount(HeaderTitle)

    cy.get('.bcds-header--title').should('exist')
    cy.get('.bcds-header--title-full')
      .should('exist')
      .and('have.text', 'Variable Density Yield Projection')
    cy.get('.bcds-header--title-short').should('exist').and('have.text', 'VDYP')
  })

  it('renders with custom text', () => {
    cy.mount(HeaderTitle, {
      props: {
        fullText: 'Custom Header Title',
        shortText: 'CHT',
      },
    })

    cy.get('.bcds-header--title').should('exist')
    cy.get('.bcds-header--title-full')
      .should('exist')
      .and('have.text', 'Custom Header Title')
    cy.get('.bcds-header--title-short').should('exist').and('have.text', 'CHT')
  })

  it('shows full text on large screens (>= 920px)', () => {
    cy.viewport(920, 768)
    cy.mount(HeaderTitle)

    cy.get('.bcds-header--title-full').should('be.visible')
    cy.get('.bcds-header--title-short').should('not.be.visible')
  })

  it('shows short text on small screens (< 920px)', () => {
    cy.viewport(919, 768)
    cy.mount(HeaderTitle)

    cy.get('.bcds-header--title-full').should('not.be.visible')
    cy.get('.bcds-header--title-short').should('be.visible')
  })
})
