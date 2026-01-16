import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ProjectionTable from './ProjectionTable.vue'
import type { Projection, TableHeader } from '@/interfaces/interfaces'

const vuetify = createVuetify({
  defaults: {
    global: {
      transition: false,
    },
  },
})

const mockHeaders: TableHeader[] = [
  { key: 'title', title: 'Projection Title', sortable: true },
  { key: 'description', title: 'Description', sortable: false },
  { key: 'method', title: 'Method', sortable: true },
  { key: 'projectionType', title: 'Projection Type', sortable: true },
  { key: 'lastUpdated', title: 'Last Updated', sortable: true },
  { key: 'expiration', title: 'Expiration', sortable: true },
  { key: 'status', title: 'Status', sortable: true },
]

const mockProjections: Projection[] = [
  {
    projectionGUID: '63c26de0-f6f3-42c2-bbb2-3c2b1e60d033',
    title: 'Test Projection 1',
    description: 'First test description',
    method: 'FIPSTART',
    projectionType: 'Volume',
    lastUpdated: '2024-01-15T10:30:00',
    expiration: '2024-06-15',
    status: 'Draft',
  },
  {
    projectionGUID: '63c26de0-f6f3-42c2-zzz2-3c2b1e60d033',
    title: 'Test Projection 2',
    description: 'Second test description',
    method: 'VDYP7',
    projectionType: 'Yield',
    lastUpdated: '2024-01-16T14:45:00',
    expiration: '2024-07-16',
    status: 'Ready',
  },
  {
    projectionGUID: '63c26de0-f6f3-42c2-dex2-3c2b1e60d033',
    title: 'Test Projection 3',
    description: 'Third test description',
    method: 'FIPSTART',
    projectionType: 'Volume',
    lastUpdated: '2024-01-17T09:00:00',
    expiration: '2024-08-17',
    status: 'Running',
  },
]

describe('ProjectionTable.vue', () => {
  const mountComponent = (
    projections: Projection[] = mockProjections,
    sortBy: string = 'title',
    sortOrder: 'asc' | 'desc' = 'asc',
    eventHandlers: Record<string, Cypress.Agent<sinon.SinonSpy>> = {},
  ) => {
    return mount(ProjectionTable, {
      props: {
        projections,
        headers: mockHeaders,
        sortBy,
        sortOrder,
      },
      global: {
        plugins: [vuetify],
      },
      attrs: eventHandlers,
    })
  }

  describe('rendering', () => {
    it('renders the table container', () => {
      mountComponent()

      cy.get('.table-container').should('exist')
      cy.get('.projections-table').should('exist')
    })

    it('renders all table headers', () => {
      mountComponent()

      mockHeaders.forEach((header) => {
        cy.contains('.table-header', header.title).should('exist')
      })
    })

    it('renders all projection rows', () => {
      mountComponent()

      cy.get('.table-row').should('have.length', mockProjections.length)
    })

    it('displays projection data correctly', () => {
      mountComponent()

      cy.contains('.cell-content', 'Test Projection 1').should('exist')
      cy.contains('.cell-content', 'First test description').should('exist')
      cy.contains('.cell-content', 'FIPSTART').should('exist')
    })

    it('renders empty table when no projections', () => {
      mountComponent([])

      cy.get('.table-row').should('have.length', 0)
      cy.get('.table-header').should('exist')
    })
  })

  describe('sorting', () => {
    it('shows sortable class on sortable headers', () => {
      mountComponent()

      cy.contains('.table-header', 'Projection Title').should(
        'have.class',
        'sortable',
      )
      cy.contains('.table-header', 'Description').should(
        'not.have.class',
        'sortable',
      )
    })

    it('displays sort icon for active sort column', () => {
      mountComponent(mockProjections, 'title', 'asc')

      cy.contains('.table-header', 'Projection Title')
        .find('.sort-icon')
        .should('exist')
    })

    it('emits sort event when sortable header is clicked', () => {
      const onSortSpy = cy.spy().as('sortSpy')
      mountComponent(mockProjections, 'title', 'asc', { onSort: onSortSpy })

      cy.contains('.table-header', 'Method').click()
      cy.get('@sortSpy').should('have.been.calledWith', 'method')
    })

    it('does not emit sort event when non-sortable header is clicked', () => {
      const onSortSpy = cy.spy().as('sortSpy')
      mountComponent(mockProjections, 'title', 'asc', { onSort: onSortSpy })

      cy.contains('.table-header', 'Description').click()
      cy.get('@sortSpy').should('not.have.been.called')
    })
  })

  describe('status display', () => {
    it('displays status with icon', () => {
      mountComponent()

      cy.get('.status-cell').should('have.length', mockProjections.length)
      cy.get('.status-icon').should('exist')
    })

    it('shows correct status text', () => {
      mountComponent()

      cy.contains('.status-cell', 'Draft').should('exist')
      cy.contains('.status-cell', 'Ready').should('exist')
      cy.contains('.status-cell', 'Running').should('exist')
    })
  })

  describe('actions menu', () => {
    it('renders action menu for each row', () => {
      mountComponent()

      cy.get('.actions-cell').should('have.length', mockProjections.length)
    })
  })
})
