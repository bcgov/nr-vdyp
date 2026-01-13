import TheFooter from './TheFooter.vue'

describe('TheFooter.vue', () => {
  it('renders properly with default content', () => {
    cy.mount(TheFooter)

    // Check if footer element exists
    cy.get('.bcds-footer').should('exist')

    // Check if land acknowledgement section is visible
    cy.get('.bcds-footer--acknowledgement').should('exist')
    cy.get('.bcds-footer--acknowledgement-text')
      .should('contain.text', 'The B.C. Public Service acknowledges')

    // Check if BC Government logo is rendered
    cy.get('#bcgov-logo-footer').should('exist')

    // Check if default links section exists
    cy.get('.bcds-footer--links').should('exist')
    cy.get('.bcds-footer--links-title').should('contain.text', 'MORE INFO')

    // Check if copyright section is visible
    cy.get('.bcds-footer--copyright').should('exist')
    cy.get('.bcds-footer--copyright').should('contain.text', 'Government of British Columbia')
  })

  it('hides land acknowledgement when hideAcknowledgement is true', () => {
    cy.mount(TheFooter, {
      props: {
        hideAcknowledgement: true,
      },
    })

    cy.get('.bcds-footer--acknowledgement').should('not.exist')
  })

  it('hides logo and links when hideLogoAndLinks is true', () => {
    cy.mount(TheFooter, {
      props: {
        hideLogoAndLinks: true,
      },
    })

    cy.get('.bcds-footer--logo-links').should('not.exist')
  })

  it('hides copyright when hideCopyright is true', () => {
    cy.mount(TheFooter, {
      props: {
        hideCopyright: true,
      },
    })

    cy.get('.bcds-footer--copyright').should('not.exist')
  })

  it('renders default footer links correctly', () => {
    cy.mount(TheFooter)

    // Check for presence of default footer links
    const expectedLinks = [
      'Home',
      'Accessibility',
      'About gov.bc.ca',
      'Copyright',
      'Disclaimer',
      'Contact us',
      'Privacy',
    ]

    expectedLinks.forEach((linkText) => {
      cy.get('.bcds-footer--links').should('contain.text', linkText)
    })
  })

  it('displays current year in copyright', () => {
    cy.mount(TheFooter)

    const currentYear = new Date().getUTCFullYear()
    cy.get('.bcds-footer--copyright').should('contain.text', `© ${currentYear}`)
  })
})
