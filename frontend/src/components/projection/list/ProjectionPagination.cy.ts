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

  describe('rendering', () => {
    it('renders the pagination container', () => {
      mountComponent()

      cy.get('.pagination-container').should('exist')
    })

    it('displays correct pagination info', () => {
      mountComponent({ currentPage: 1, itemsPerPage: 10, totalItems: 100 })

      cy.get('.pagination-info-left').should(
        'contain.text',
        'Showing 1 to 10 of 100',
      )
    })

    it('displays correct pagination info for middle page', () => {
      mountComponent({ currentPage: 3, itemsPerPage: 10, totalItems: 100 })

      cy.get('.pagination-info-left').should(
        'contain.text',
        'Showing 21 to 30 of 100',
      )
    })

    it('displays correct pagination info for last page with partial items', () => {
      mountComponent({ currentPage: 5, itemsPerPage: 10, totalItems: 45 })

      cy.get('.pagination-info-left').should(
        'contain.text',
        'Showing 41 to 45 of 45',
      )
    })

    it('renders page number buttons', () => {
      mountComponent({ currentPage: 1, itemsPerPage: 10, totalItems: 50 })

      cy.get('.pagination-number').should('have.length.at.least', 1)
    })

    it('renders navigation arrows', () => {
      mountComponent()

      cy.get('.pagination-arrow').should('have.length', 2)
    })
  })

  describe('navigation', () => {
    it('disables previous arrow on first page', () => {
      mountComponent({ currentPage: 1 })

      cy.get('.pagination-arrow').first().should('be.disabled')
    })

    it('disables next arrow on last page', () => {
      mountComponent({ currentPage: 10, itemsPerPage: 10, totalItems: 100 })

      cy.get('.pagination-arrow').last().should('be.disabled')
    })

    it('disables next arrow when totalItems is 0', () => {
      mountComponent({ currentPage: 1, itemsPerPage: 10, totalItems: 0 })

      cy.get('.pagination-arrow').last().should('be.disabled')
    })

    it('disables previous arrow when totalItems is 0', () => {
      mountComponent({ currentPage: 1, itemsPerPage: 10, totalItems: 0 })

      cy.get('.pagination-arrow').first().should('be.disabled')
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

    it('emits update:currentPage when clicking page number', () => {
      const onUpdateCurrentPage = cy.spy().as('updateCurrentPageSpy')
      mountComponent(
        { currentPage: 1, itemsPerPage: 10, totalItems: 50 },
        { 'onUpdate:currentPage': onUpdateCurrentPage },
      )

      cy.get('.pagination-number').contains('3').click()
      cy.get('@updateCurrentPageSpy').should('have.been.calledWith', 3)
    })

    it('does not emit update:currentPage when clicking the current active page', () => {
      const onUpdateCurrentPage = cy.spy().as('updateCurrentPageSpy')
      mountComponent(
        { currentPage: 3, itemsPerPage: 10, totalItems: 50 },
        { 'onUpdate:currentPage': onUpdateCurrentPage },
      )

      cy.get('.pagination-number').contains('3').click()
      cy.get('@updateCurrentPageSpy').should('not.have.been.called')
    })
  })

  describe('active page highlighting', () => {
    it('highlights current page with active class', () => {
      mountComponent({ currentPage: 3, itemsPerPage: 10, totalItems: 50 })

      cy.get('.pagination-number').contains('3').should('have.class', 'active')
    })

    it('does not highlight other pages', () => {
      mountComponent({ currentPage: 3, itemsPerPage: 10, totalItems: 50 })

      cy.get('.pagination-number').contains('1').should('not.have.class', 'active')
      cy.get('.pagination-number').contains('2').should('not.have.class', 'active')
    })
  })

  describe('visiblePages', () => {
    it('shows all pages when totalPages is 7 or fewer', () => {
      mountComponent({ currentPage: 1, itemsPerPage: 10, totalItems: 70 })

      // totalPages = 7: [1, 2, 3, 4, 5, 6, 7]
      cy.get('.pagination-number').should('have.length', 7)
      cy.get('.pagination-number').first().should('contain.text', '1')
      cy.get('.pagination-number').last().should('contain.text', '7')
    })

    it('shows 5 leading pages, ellipsis, and last page when current page <= 4', () => {
      mountComponent({ currentPage: 2, itemsPerPage: 10, totalItems: 200 })

      // totalPages = 20, current <= 4: [1, 2, 3, 4, 5, -1, 20]
      cy.get('.pagination-number').should('have.length', 7)
      cy.get('.pagination-number').eq(0).should('contain.text', '1')
      cy.get('.pagination-number').eq(4).should('contain.text', '5')
      cy.get('.pagination-number').eq(5).should('contain.text', '-1')
      cy.get('.pagination-number').eq(6).should('contain.text', '20')
    })

    it('shows first page, ellipsis, and 5 trailing pages when current page is near end', () => {
      mountComponent({ currentPage: 18, itemsPerPage: 10, totalItems: 200 })

      // totalPages = 20, current >= 17: [1, -1, 16, 17, 18, 19, 20]
      cy.get('.pagination-number').should('have.length', 7)
      cy.get('.pagination-number').eq(0).should('contain.text', '1')
      cy.get('.pagination-number').eq(1).should('contain.text', '-1')
      cy.get('.pagination-number').eq(2).should('contain.text', '16')
      cy.get('.pagination-number').last().should('contain.text', '20')
    })

    it('shows first page, ellipsis, 3 middle pages, ellipsis, and last page for middle pages', () => {
      mountComponent({ currentPage: 10, itemsPerPage: 10, totalItems: 200 })

      // totalPages = 20, middle: [1, -1, 9, 10, 11, -1, 20]
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

  describe('items per page', () => {
    it('renders items per page selector', () => {
      mountComponent()

      cy.get('.items-per-page-select').should('exist')
      cy.get('.pagination-controls').should('contain.text', 'Show')
      cy.get('.pagination-controls').should('contain.text', 'entries')
    })

    it('emits update:itemsPerPage when selecting a new value', () => {
      const onUpdateItemsPerPage = cy.spy().as('updateItemsPerPageSpy')
      mountComponent(
        { itemsPerPageOptions: [10, 25, 50] },
        { 'onUpdate:itemsPerPage': onUpdateItemsPerPage },
      )

      cy.get('.items-per-page-select').click()
      cy.get('.v-list-item').contains('25').click()
      cy.get('@updateItemsPerPageSpy').should('have.been.calledWith', 25)
    })
  })
})
