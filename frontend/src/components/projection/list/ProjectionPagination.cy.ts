import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ProjectionPagination from './ProjectionPagination.vue'

const vuetify = createVuetify({
  defaults: {
    global: {
      transition: false,
    },
  },
})

describe('ProjectionPagination.vue', () => {
  const mountComponent = (
    props: {
      currentPage?: number
      itemsPerPage?: number
      totalItems?: number
      itemsPerPageOptions?: number[]
    } = {},
    eventHandlers: Record<string, Cypress.Agent<sinon.SinonSpy>> = {},
  ) => {
    return mount(ProjectionPagination, {
      props: {
        currentPage: 1,
        itemsPerPage: 10,
        totalItems: 100,
        ...props,
      },
      global: {
        plugins: [vuetify],
      },
      attrs: eventHandlers,
    })
  }

  it('displays correct pagination info', () => {
    mountComponent({ currentPage: 1, itemsPerPage: 10, totalItems: 100 })

    cy.get('.pagination-info-left').should('contain.text', 'Showing 1 to 10 of 100')
  })

  it('displays correct pagination info for last page with partial items', () => {
    mountComponent({ currentPage: 5, itemsPerPage: 10, totalItems: 45 })

    cy.get('.pagination-info-left').should('contain.text', 'Showing 41 to 45 of 45')
  })

  it('disables previous arrow on first page and next arrow on last page', () => {
    mountComponent({ currentPage: 1, itemsPerPage: 10, totalItems: 100 })
    cy.get('.pagination-arrow').first().should('be.disabled')

    mountComponent({ currentPage: 10, itemsPerPage: 10, totalItems: 100 })
    cy.get('.pagination-arrow').last().should('be.disabled')
  })

  it('disables both arrows when totalItems is 0', () => {
    mountComponent({ currentPage: 1, itemsPerPage: 10, totalItems: 0 })

    cy.get('.pagination-arrow').first().should('be.disabled')
    cy.get('.pagination-arrow').last().should('be.disabled')
  })

  it('enables both arrows on middle page', () => {
    mountComponent({ currentPage: 5, itemsPerPage: 10, totalItems: 100 })

    cy.get('.pagination-arrow').first().should('not.be.disabled')
    cy.get('.pagination-arrow').last().should('not.be.disabled')
  })

  it('emits update:currentPage when clicking previous arrow', () => {
    const onUpdateCurrentPage = cy.spy().as('updateCurrentPageSpy')
    mountComponent({ currentPage: 5 }, { 'onUpdate:currentPage': onUpdateCurrentPage })

    cy.get('.pagination-arrow').first().click()
    cy.get('@updateCurrentPageSpy').should('have.been.calledWith', 4)
  })

  it('emits update:currentPage when clicking next arrow', () => {
    const onUpdateCurrentPage = cy.spy().as('updateCurrentPageSpy')
    mountComponent({ currentPage: 5 }, { 'onUpdate:currentPage': onUpdateCurrentPage })

    cy.get('.pagination-arrow').last().click()
    cy.get('@updateCurrentPageSpy').should('have.been.calledWith', 6)
  })

  it('emits update:currentPage when clicking page number, not when clicking current page', () => {
    const onUpdateCurrentPage = cy.spy().as('updateCurrentPageSpy')
    mountComponent(
      { currentPage: 3, itemsPerPage: 10, totalItems: 50 },
      { 'onUpdate:currentPage': onUpdateCurrentPage },
    )

    cy.get('.pagination-number').contains('3').click()
    cy.get('@updateCurrentPageSpy').should('not.have.been.called')

    cy.get('.pagination-number').contains('1').click()
    cy.get('@updateCurrentPageSpy').should('have.been.calledWith', 1)
  })

  it('highlights current page with active class', () => {
    mountComponent({ currentPage: 3, itemsPerPage: 10, totalItems: 50 })

    cy.get('.pagination-number').contains('3').should('have.class', 'active')
    cy.get('.pagination-number').contains('1').should('not.have.class', 'active')
  })

  it('shows first, ellipsis, 3 middle pages, ellipsis, and last for middle page', () => {
    mountComponent({ currentPage: 10, itemsPerPage: 10, totalItems: 200 })

    cy.get('.pagination-number').should('have.length', 7)
    cy.get('.pagination-number').eq(0).should('contain.text', '1')
    cy.get('.pagination-number').eq(1).should('contain.text', '-1')
    cy.get('.pagination-number').eq(2).should('contain.text', '9')
    cy.get('.pagination-number').eq(3).should('contain.text', '10')
    cy.get('.pagination-number').eq(4).should('contain.text', '11')
    cy.get('.pagination-number').eq(5).should('contain.text', '-1')
    cy.get('.pagination-number').eq(6).should('contain.text', '20')
  })
})
