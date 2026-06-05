import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ProjectionCardList from './ProjectionCardList.vue'
import type { Projection, SortOption } from '@/interfaces/interfaces'

const vuetify = createVuetify({
  defaults: {
    global: {
      transition: false,
    },
  },
})

describe('ProjectionCardList.vue', () => {
  const sortOptions: SortOption[] = [
    { title: 'Last Updated (Newest)', value: 'lastUpdated_desc' },
    { title: 'Last Updated (Oldest)', value: 'lastUpdated_asc' },
    { title: 'Title (A-Z)', value: 'title_asc' },
    { title: 'Title (Z-A)', value: 'title_desc' },
  ]

  const createProjection = (
    overrides: Partial<Projection> = {},
  ): Projection => ({
    projectionGUID: '63c26de0-f6f3-42c2-bbb2-3c2b1e60d033',
    title: 'Test Projection',
    description: 'Test description',
    method: 'FIPSTART',
    projectionType: 'Single Year',
    lastUpdated: '2026-01-10T14:30:00',
    expiration: '2026-06-15',
    status: 'Draft',
    ...overrides,
  })

  const mountComponent = (
    projections: Projection[] = [createProjection()],
    sortValue: string = 'lastUpdated_desc',
    eventHandlers: Record<string, Cypress.Agent<sinon.SinonSpy>> = {},
  ) => {
    return mount(ProjectionCardList, {
      props: {
        projections,
        sortOptions,
        sortValue,
      },
      global: {
        plugins: [vuetify],
      },
      attrs: eventHandlers,
    })
  }

  beforeEach(() => {
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        body {
          background-color: rgb(240, 240, 240) !important;
        }
      `
      doc.head.appendChild(style)
    })
  })

  describe('rendering', () => {
    it('renders projection cards with correct count', () => {
      const projections = [
        createProjection({ projectionGUID: 'guid-1', title: 'Projection 1' }),
        createProjection({ projectionGUID: 'guid-2', title: 'Projection 2' }),
      ]
      mountComponent(projections)

      cy.get('.projection-card').should('have.length', 2)
    })

    it('displays projection info fields', () => {
      mountComponent([
        createProjection({
          title: 'My Test Projection',
          method: 'FIPSTART',
          projectionType: 'Year Range',
          description: 'My description',
          status: 'Ready',
          lastUpdated: '2026-01-10T14:30:00',
          expiration: '2026-06-15',
        }),
      ])

      cy.get('.card-title').should('contain.text', 'My Test Projection')
      cy.get('.status-text').should('contain.text', 'Ready')
      cy.get('.card-last-updated').should('contain.text', 'Last Updated:')
      cy.get('.card-info-label').contains('Method').should('exist')
      cy.get('.card-info-value').contains('FIPSTART').should('exist')
      cy.get('.card-info-label').contains('Range Type').should('exist')
      cy.get('.card-info-value').contains('Year Range').should('exist')
      cy.get('.card-info-label').contains('Description').should('exist')
      cy.get('.card-info-value').contains('My description').should('exist')
      cy.get('.card-info-label').contains('Expiration').should('exist')
    })

    it('shows current sort value in dropdown', () => {
      mountComponent([], 'title_asc')

      cy.get('.sort-dropdown').should('contain.text', 'Title (A-Z)')
    })
  })

  describe('Draft status actions', () => {
    it('shows Edit, Duplicate, and Delete buttons only', () => {
      mountComponent([createProjection({ status: 'Draft' })])

      cy.get('.button-label').contains('Edit').should('exist')
      cy.get('.button-label').contains('Duplicate').should('exist')
      cy.get('.button-label').contains('Delete').should('exist')
      cy.get('.button-label').contains('View').should('not.exist')
      cy.get('.button-label').contains('Download').should('not.exist')
      cy.get('.button-label').contains('Cancel').should('not.exist')
    })

    it('emits edit event with correct GUID when Edit is clicked', () => {
      const onEditSpy = cy.spy().as('editSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid', status: 'Draft' })], 'lastUpdated_desc', {
        onEdit: onEditSpy,
      })

      cy.get('.button-label').contains('Edit').closest('button').click()
      cy.get('@editSpy').should('have.been.calledWith', 'test-guid')
    })

    it('emits delete event with correct GUID when Delete is clicked', () => {
      const onDeleteSpy = cy.spy().as('deleteSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid', status: 'Draft' })], 'lastUpdated_desc', {
        onDelete: onDeleteSpy,
      })

      cy.get('.button-label').contains('Delete').closest('button').click()
      cy.get('@deleteSpy').should('have.been.calledWith', 'test-guid')
    })
  })

  describe('Ready status actions', () => {
    it('shows View, Duplicate, Download, and Delete buttons only', () => {
      mountComponent([createProjection({ status: 'Ready' })])

      cy.get('.button-label').contains('View').should('exist')
      cy.get('.button-label').contains('Duplicate').should('exist')
      cy.get('.button-label').contains('Download').should('exist')
      cy.get('.button-label').contains('Delete').should('exist')
      cy.get('.button-label').contains('Edit').should('not.exist')
      cy.get('.button-label').contains('Cancel').should('not.exist')
    })

    it('emits view event with correct GUID when View is clicked', () => {
      const onViewSpy = cy.spy().as('viewSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid', status: 'Ready' })], 'lastUpdated_desc', {
        onView: onViewSpy,
      })

      cy.get('.button-label').contains('View').closest('button').click()
      cy.get('@viewSpy').should('have.been.calledWith', 'test-guid')
    })
  })

  describe('Running status actions', () => {
    it('shows Cancel button only', () => {
      mountComponent([createProjection({ status: 'Running' })])

      cy.get('.button-label').contains('Cancel').should('exist')
      cy.get('.button-label').contains('Delete').should('not.exist')
      cy.get('.button-label').contains('View').should('not.exist')
      cy.get('.button-label').contains('Edit').should('not.exist')
      cy.get('.button-label').contains('Duplicate').should('not.exist')
      cy.get('.button-label').contains('Download').should('not.exist')
    })

    it('emits cancel event with correct GUID when Cancel is clicked', () => {
      const onCancelSpy = cy.spy().as('cancelSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid', status: 'Running' })], 'lastUpdated_desc', {
        onCancel: onCancelSpy,
      })

      cy.get('.button-label').contains('Cancel').closest('button').click()
      cy.get('@cancelSpy').should('have.been.calledWith', 'test-guid')
    })
  })

  describe('Failed status actions', () => {
    it('shows Edit, Duplicate, Download, and Delete buttons only', () => {
      mountComponent([createProjection({ status: 'Failed' })])

      cy.get('.button-label').contains('Edit').should('exist')
      cy.get('.button-label').contains('Duplicate').should('exist')
      cy.get('.button-label').contains('Download').should('exist')
      cy.get('.button-label').contains('Delete').should('exist')
      cy.get('.button-label').contains('View').should('not.exist')
      cy.get('.button-label').contains('Cancel').should('not.exist')
    })
  })

  describe('multiple projections', () => {
    it('renders multiple cards with different status styles', () => {
      const projections = [
        createProjection({ projectionGUID: 'guid-1', status: 'Draft' }),
        createProjection({ projectionGUID: 'guid-2', status: 'Ready' }),
        createProjection({ projectionGUID: 'guid-3', status: 'Running' }),
        createProjection({ projectionGUID: 'guid-4', status: 'Failed' }),
      ]
      mountComponent(projections)

      cy.get('.projection-card').should('have.length', 4)
      cy.get('.status-text.status-draft').should('exist')
      cy.get('.status-text.status-ready').should('exist')
      cy.get('.status-text.status-running').should('exist')
      cy.get('.status-text.status-failed').should('exist')
    })
  })

  describe('card click (rowClick)', () => {
    it('emits rowClick when clicking on card body', () => {
      const onRowClickSpy = cy.spy().as('rowClickSpy')
      const projection = createProjection({ projectionGUID: 'guid-click', title: 'Clickable Card' })
      mountComponent([projection], 'lastUpdated_desc', { onRowClick: onRowClickSpy })

      cy.get('.card-header-section').click()
      cy.get('@rowClickSpy').should('have.been.calledOnce')
      cy.get('@rowClickSpy').should('have.been.calledWith', projection)
    })

    it('does not emit rowClick when clicking on action buttons area', () => {
      const onRowClickSpy = cy.spy().as('rowClickSpy')
      mountComponent([createProjection({ status: 'Draft' })], 'lastUpdated_desc', {
        onRowClick: onRowClickSpy,
      })

      cy.get('.card-actions').click({ force: true })
      cy.get('@rowClickSpy').should('not.have.been.called')
    })
  })

  describe('empty state', () => {
    it('shows empty state card with message when projections array is empty', () => {
      mountComponent([])

      cy.get('.empty-state-card').should('exist')
      cy.get('.empty-state-message').should(
        'contain.text',
        'No projections found. Create a new projection to build your history.',
      )
    })

    it('does not show empty state card when projections exist', () => {
      mountComponent([createProjection()])

      cy.get('.empty-state-card').should('not.exist')
    })
  })
})
