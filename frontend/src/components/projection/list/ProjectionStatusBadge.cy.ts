import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ProjectionStatusBadge from './ProjectionStatusBadge.vue'

const vuetify = createVuetify()

describe('ProjectionStatusBadge.vue', () => {
  const mountComponent = (status: string) => {
    return mount(ProjectionStatusBadge, {
      props: { status },
      global: {
        plugins: [vuetify],
      },
    })
  }

  describe('rendering', () => {
    it('renders status badge with icon and text', () => {
      mountComponent('Draft')

      cy.get('.status-badge').should('exist')
      cy.get('.status-icon').should('exist')
      cy.get('.status-text').should('contain', 'Draft')
    })

    it('renders Draft status correctly', () => {
      mountComponent('Draft')

      cy.get('.status-text')
        .should('contain', 'Draft')
        .and('have.class', 'status-draft')
      cy.get('.status-icon').should('have.attr', 'alt', 'Draft')
    })

    it('renders Ready status correctly', () => {
      mountComponent('Ready')

      cy.get('.status-text')
        .should('contain', 'Ready')
        .and('have.class', 'status-ready')
      cy.get('.status-icon').should('have.attr', 'alt', 'Ready')
    })

    it('renders Running status correctly', () => {
      mountComponent('Running')

      cy.get('.status-text')
        .should('contain', 'Running')
        .and('have.class', 'status-running')
      cy.get('.status-icon').should('have.attr', 'alt', 'Running')
    })

    it('renders Failed status correctly', () => {
      mountComponent('Failed')

      cy.get('.status-text')
        .should('contain', 'Failed')
        .and('have.class', 'status-failed')
      cy.get('.status-icon').should('have.attr', 'alt', 'Failed')
    })
  })

  describe('icon display', () => {
    it('displays status icon with correct src attribute', () => {
      mountComponent('Draft')

      cy.get('.status-icon')
        .should('have.attr', 'src')
        .and('include', 'Draft_Icon')
    })

    it('displays Ready icon for Ready status', () => {
      mountComponent('Ready')

      cy.get('.status-icon')
        .should('have.attr', 'src')
        .and('include', 'Ready_Icon')
    })

    it('displays Running icon for Running status', () => {
      mountComponent('Running')

      cy.get('.status-icon')
        .should('have.attr', 'src')
        .and('include', 'Running_Icon')
    })

    it('displays Failed icon for Failed status', () => {
      mountComponent('Failed')

      cy.get('.status-icon')
        .should('have.attr', 'src')
        .and('include', 'Failed_Icon')
    })
  })
})
