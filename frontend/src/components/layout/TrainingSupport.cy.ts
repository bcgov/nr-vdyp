import TrainingSupport from './TrainingSupport.vue'
import { RouterLinkStub } from '@vue/test-utils'

describe('TrainingSupport.vue', () => {
  const mountOptions = {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  }

  it('renders default text correctly', () => {
    cy.mount(TrainingSupport, mountOptions)

    cy.get('a').should('have.class', 'bcds-header-link')
    cy.get('.bcds-header-link-full')
      .should('exist')
      .and('have.text', 'Training and Support')
    cy.get('.bcds-header-link-short').should('exist').and('have.text', 'Support')
  })

  it('renders with custom text', () => {
    cy.mount(TrainingSupport, {
      ...mountOptions,
      props: {
        fullText: 'Custom Training Text',
        shortText: 'Custom',
      },
    })

    cy.get('a').should('have.class', 'bcds-header-link')
    cy.get('.bcds-header-link-full')
      .should('exist')
      .and('have.text', 'Custom Training Text')
    cy.get('.bcds-header-link-short').should('exist').and('have.text', 'Custom')
  })

  it('shows full text on large screens (>= 920px)', () => {
    cy.viewport(920, 768)
    cy.mount(TrainingSupport, mountOptions)

    cy.get('.bcds-header-link-full').should('be.visible')
    cy.get('.bcds-header-link-short').should('not.be.visible')
  })

  it('shows short text on small screens (< 920px)', () => {
    cy.viewport(919, 768)
    cy.mount(TrainingSupport, mountOptions)

    cy.get('.bcds-header-link-full').should('not.be.visible')
    cy.get('.bcds-header-link-short').should('be.visible')
  })
})
