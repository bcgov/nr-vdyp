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
    selectedGUIDs: string[] = [],
    eventHandlers: Record<string, Cypress.Agent<sinon.SinonSpy>> = {},
  ) => {
    return mount(ProjectionTable, {
      props: {
        projections,
        headers: mockHeaders,
        sortBy,
        sortOrder,
        selectedGUIDs,
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

    it('shows empty state row and message when no projections', () => {
      mountComponent([])

      cy.get('.table-row').should('have.length', 0)
      cy.get('.empty-state-row').should('exist')
      cy.get('.empty-state-message').should('contain.text', 'No projections found')
      cy.get('.table-header').should('exist')
    })

    it('renders checkbox column header', () => {
      mountComponent()

      cy.get('.checkbox-header .table-checkbox').should('exist')
    })

    it('renders a checkbox for each projection row', () => {
      mountComponent()

      cy.get('.checkbox-cell .table-checkbox').should('have.length', mockProjections.length)
    })
  })

  describe('sorting', () => {
    it('shows sortable class on sortable headers', () => {
      mountComponent()

      cy.contains('.table-header', 'Projection Title').should('have.class', 'sortable')
      cy.contains('.table-header', 'Description').should('not.have.class', 'sortable')
    })

    it('displays ascending sort icon for active sort column', () => {
      mountComponent(mockProjections, 'title', 'asc')

      cy.contains('.table-header', 'Projection Title')
        .find('.sort-icon')
        .should('exist')
        .and('have.class', 'mdi-arrow-up')
    })

    it('displays descending sort icon for active sort column', () => {
      mountComponent(mockProjections, 'title', 'desc')

      cy.contains('.table-header', 'Projection Title')
        .find('.sort-icon')
        .should('exist')
        .and('have.class', 'mdi-arrow-down')
    })

    it('does not display sort icon for inactive sort column', () => {
      mountComponent(mockProjections, 'title', 'asc')

      cy.contains('.table-header', 'Method').find('.sort-icon').should('not.exist')
    })

    it('emits sort event when sortable header is clicked', () => {
      const onSortSpy = cy.spy().as('sortSpy')
      mountComponent(mockProjections, 'title', 'asc', [], { onSort: onSortSpy })

      cy.contains('.table-header', 'Method').click()
      cy.get('@sortSpy').should('have.been.calledWith', 'method')
    })

    it('does not emit sort event when non-sortable header is clicked', () => {
      const onSortSpy = cy.spy().as('sortSpy')
      mountComponent(mockProjections, 'title', 'asc', [], { onSort: onSortSpy })

      cy.contains('.table-header', 'Description').click()
      cy.get('@sortSpy').should('not.have.been.called')
    })
  })

  describe('row click', () => {
    it('emits rowClick event when a table cell is clicked', () => {
      const onRowClickSpy = cy.spy().as('rowClickSpy')
      mountComponent(mockProjections, 'title', 'asc', [], { onRowClick: onRowClickSpy })

      cy.contains('.cell-content', 'Test Projection 1').click()
      cy.get('@rowClickSpy').should('have.been.calledOnce')
    })

    it('does not emit rowClick when the checkbox cell is clicked', () => {
      const onRowClickSpy = cy.spy().as('rowClickSpy')
      mountComponent(mockProjections, 'title', 'asc', [], { onRowClick: onRowClickSpy })

      cy.get('.checkbox-cell').first().click()
      cy.get('@rowClickSpy').should('not.have.been.called')
    })

    it('does not emit rowClick when the actions cell is clicked', () => {
      const onRowClickSpy = cy.spy().as('rowClickSpy')
      mountComponent(mockProjections, 'title', 'asc', [], { onRowClick: onRowClickSpy })

      cy.get('.actions-cell').first().click({ force: true })
      cy.get('@rowClickSpy').should('not.have.been.called')
    })
  })

  describe('checkbox selection', () => {
    it('header checkbox is unchecked when no rows are selected', () => {
      mountComponent(mockProjections, 'title', 'asc', [])

      cy.get('.checkbox-header .table-checkbox').should('not.be.checked')
    })

    it('header checkbox is checked when all rows are selected', () => {
      const allGUIDs = mockProjections.map((p) => p.projectionGUID)
      mountComponent(mockProjections, 'title', 'asc', allGUIDs)

      cy.get('.checkbox-header .table-checkbox').should('be.checked')
    })

    it('header checkbox is indeterminate when some rows are selected', () => {
      mountComponent(mockProjections, 'title', 'asc', [mockProjections[0].projectionGUID])

      cy.get('.checkbox-header .table-checkbox').should(($el) => {
        expect(($el[0] as HTMLInputElement).indeterminate).to.be.true
      })
    })

    it('applies row-selected class to selected rows only', () => {
      mountComponent(mockProjections, 'title', 'asc', [mockProjections[0].projectionGUID])

      cy.get('.table-row').first().should('have.class', 'row-selected')
      cy.get('.table-row').eq(1).should('not.have.class', 'row-selected')
      cy.get('.table-row').eq(2).should('not.have.class', 'row-selected')
    })

    it('row checkbox is checked only for selected rows', () => {
      mountComponent(mockProjections, 'title', 'asc', [mockProjections[0].projectionGUID])

      cy.get('.checkbox-cell .table-checkbox').first().should('be.checked')
      cy.get('.checkbox-cell .table-checkbox').eq(1).should('not.be.checked')
    })

    it('emits selectionChange with added GUID when row checkbox is checked', () => {
      const onSelectionChangeSpy = cy.spy().as('selectionChangeSpy')
      mountComponent(mockProjections, 'title', 'asc', [], { onSelectionChange: onSelectionChangeSpy })

      cy.get('.checkbox-cell .table-checkbox').first().check()
      cy.get('@selectionChangeSpy').should('have.been.calledWith', [mockProjections[0].projectionGUID])
    })

    it('emits selectionChange with removed GUID when row checkbox is unchecked', () => {
      const onSelectionChangeSpy = cy.spy().as('selectionChangeSpy')
      const allGUIDs = mockProjections.map((p) => p.projectionGUID)
      mountComponent(mockProjections, 'title', 'asc', allGUIDs, { onSelectionChange: onSelectionChangeSpy })

      cy.get('.checkbox-cell .table-checkbox').first().uncheck()
      cy.get('@selectionChangeSpy').should('have.been.calledWith', [
        mockProjections[1].projectionGUID,
        mockProjections[2].projectionGUID,
      ])
    })

    it('emits selectionChange with all GUIDs when header checkbox is checked from none selected', () => {
      const onSelectionChangeSpy = cy.spy().as('selectionChangeSpy')
      mountComponent(mockProjections, 'title', 'asc', [], { onSelectionChange: onSelectionChangeSpy })

      cy.get('.checkbox-header .table-checkbox').check()
      cy.get('@selectionChangeSpy').should(
        'have.been.calledWith',
        mockProjections.map((p) => p.projectionGUID),
      )
    })

    it('emits selectionChange with empty array when header checkbox is unchecked from all selected', () => {
      const onSelectionChangeSpy = cy.spy().as('selectionChangeSpy')
      const allGUIDs = mockProjections.map((p) => p.projectionGUID)
      mountComponent(mockProjections, 'title', 'asc', allGUIDs, { onSelectionChange: onSelectionChangeSpy })

      cy.get('.checkbox-header .table-checkbox').uncheck()
      cy.get('@selectionChangeSpy').should('have.been.calledWith', [])
    })
  })

  describe('status display', () => {
    it('displays status badge for each row', () => {
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
