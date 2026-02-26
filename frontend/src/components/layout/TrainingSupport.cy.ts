import TrainingSupport from './TrainingSupport.vue'

describe('TrainingSupport.vue', () => {
  it('renders default text correctly', () => {
    cy.mount(TrainingSupport)

    cy.get('.bcds-header-link').should('exist')
    cy.get('.bcds-header-link-full')
      .should('exist')
      .and('have.text', 'Training and Support')
    cy.get('.bcds-header-link-short').should('exist').and('have.text', 'Support')
  })

  it('renders with custom text', () => {
    cy.mount(TrainingSupport, {
      props: {
        fullText: 'Custom Training Text',
        shortText: 'Custom',
      },
    })

    cy.get('.bcds-header-link').should('exist')
    cy.get('.bcds-header-link-full')
      .should('exist')
      .and('have.text', 'Custom Training Text')
    cy.get('.bcds-header-link-short').should('exist').and('have.text', 'Custom')
  })

  it('shows full text on large screens (>= 920px)', () => {
    cy.viewport(920, 768)
    cy.mount(TrainingSupport)

    cy.get('.bcds-header-link-full').should('be.visible')
    cy.get('.bcds-header-link-short').should('not.be.visible')
  })

  it('shows short text on small screens (< 920px)', () => {
    cy.viewport(919, 768)
    cy.mount(TrainingSupport)

    cy.get('.bcds-header-link-full').should('not.be.visible')
    cy.get('.bcds-header-link-short').should('be.visible')
  })
})
