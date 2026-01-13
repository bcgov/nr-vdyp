import BCLogo from './BCLogo.vue'

describe('BCLogo', () => {
  it('renders properly', () => {
    cy.mount(BCLogo)

    cy.get('img').should('exist')
    cy.get('img').should('have.attr', 'alt', 'B.C. Government Logo')
  })

  it('applies correct styling', () => {
    cy.mount(BCLogo)

    cy.get('img').should('have.class', 'bcds-logo')
  })

  it('loads the correct image', () => {
    cy.mount(BCLogo)

    cy.get('img')
      .should('have.attr', 'src')
      .and('include', 'BCID_H_rgb_pos')
  })
})
