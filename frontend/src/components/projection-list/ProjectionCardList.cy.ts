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
    it('renders the sort dropdown', () => {
      mountComponent()

      cy.get('.sort-dropdown').should('exist')
      cy.get('label[for="cardSortBy"]').should('contain.text', 'Sort By:')
    })

    it('renders projection cards', () => {
      const projections = [
        createProjection({ projectionGUID: 'guid-1', title: 'Projection 1' }),
        createProjection({ projectionGUID: 'guid-2', title: 'Projection 2' }),
      ]
      mountComponent(projections)

      cy.get('.projection-card').should('have.length', 2)
    })

    it('displays projection title in card header', () => {
      mountComponent([createProjection({ title: 'My Test Projection' })])

      cy.get('.card-title').should('contain.text', 'My Test Projection')
    })

    it('displays projection status with icon', () => {
      mountComponent([createProjection({ status: 'Ready' })])

      cy.get('.status-text').should('contain.text', 'Ready')
      cy.get('.status-icon').should('exist')
    })

    it('displays last updated date', () => {
      mountComponent([createProjection({ lastUpdated: '2026-01-10T14:30:00' })])

      cy.get('.card-last-updated').should('contain.text', 'Last Updated:')
    })

    it('displays projection info fields', () => {
      mountComponent([
        createProjection({
          method: 'FIPSTART',
          projectionType: 'Year Range',
          description: 'My description',
        }),
      ])

      cy.get('.card-info-label').contains('Method').should('exist')
      cy.get('.card-info-value').contains('FIPSTART').should('exist')
      cy.get('.card-info-label').contains('Range Type').should('exist')
      cy.get('.card-info-value').contains('Year Range').should('exist')
      cy.get('.card-info-label').contains('Description').should('exist')
      cy.get('.card-info-value').contains('My description').should('exist')
    })

    it('displays expiration date', () => {
      mountComponent([createProjection({ expiration: '2026-06-15' })])

      cy.get('.card-info-label').contains('Expiration').should('exist')
    })
  })

  describe('sort functionality', () => {
    it('shows current sort value in dropdown', () => {
      mountComponent([], 'title_asc')

      cy.get('.sort-dropdown').should('contain.text', 'Title (A-Z)')
    })

    it('emits sort event when sort option changes', () => {
      const onSortSpy = cy.spy().as('sortSpy')
      mountComponent([createProjection()], 'lastUpdated_desc', {
        onSort: onSortSpy,
      })

      cy.get('.sort-dropdown .v-field__input').click()
      cy.get('.v-list').should('be.visible')
      cy.contains('.sort-option-text', 'Title (A-Z)').click()
      cy.get('@sortSpy').should('have.been.calledWith', 'title_asc')
    })
  })

  describe('status styling', () => {
    it('applies draft status styling', () => {
      mountComponent([createProjection({ status: 'Draft' })])

      cy.get('.status-text.status-draft').should('exist')
    })

    it('applies ready status styling', () => {
      mountComponent([createProjection({ status: 'Ready' })])

      cy.get('.status-text.status-ready').should('exist')
    })

    it('applies running status styling', () => {
      mountComponent([createProjection({ status: 'Running' })])

      cy.get('.status-text.status-running').should('exist')
    })

    it('applies failed status styling', () => {
      mountComponent([createProjection({ status: 'Failed' })])

      cy.get('.status-text.status-failed').should('exist')
    })
  })

  describe('Draft status actions', () => {
    it('shows Edit, Duplicate, and Delete buttons', () => {
      mountComponent([createProjection({ status: 'Draft' })])

      cy.get('.button-label').contains('Edit').should('exist')
      cy.get('.button-label').contains('Duplicate').should('exist')
      cy.get('.button-label').contains('Delete').should('exist')
      cy.get('.button-label').contains('View').should('not.exist')
      cy.get('.button-label').contains('Download').should('not.exist')
      cy.get('.button-label').contains('Cancel').should('not.exist')
    })

    it('emits edit event when Edit is clicked', () => {
      const onEditSpy = cy.spy().as('editSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid-5', status: 'Draft' })], 'lastUpdated_desc', {
        onEdit: onEditSpy,
      })

      cy.get('.button-label').contains('Edit').closest('button').click()
      cy.get('@editSpy').should('have.been.calledWith', 'test-guid-5')
    })

    it('emits duplicate event when Duplicate is clicked', () => {
      const onDuplicateSpy = cy.spy().as('duplicateSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid-5', status: 'Draft' })], 'lastUpdated_desc', {
        onDuplicate: onDuplicateSpy,
      })

      cy.get('.button-label').contains('Duplicate').closest('button').click()
      cy.get('@duplicateSpy').should('have.been.calledWith', 'test-guid-5')
    })

    it('emits delete event when Delete is clicked', () => {
      const onDeleteSpy = cy.spy().as('deleteSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid-5', status: 'Draft' })], 'lastUpdated_desc', {
        onDelete: onDeleteSpy,
      })

      cy.get('.button-label').contains('Delete').closest('button').click()
      cy.get('@deleteSpy').should('have.been.calledWith', 'test-guid-5')
    })
  })

  describe('Ready status actions', () => {
    it('shows View, Duplicate, Download, and Delete buttons', () => {
      mountComponent([createProjection({ status: 'Ready' })])

      cy.get('.button-label').contains('View').should('exist')
      cy.get('.button-label').contains('Duplicate').should('exist')
      cy.get('.button-label').contains('Download').should('exist')
      cy.get('.button-label').contains('Delete').should('exist')
      cy.get('.button-label').contains('Edit').should('not.exist')
      cy.get('.button-label').contains('Cancel').should('not.exist')
    })

    it('emits view event when View is clicked', () => {
      const onViewSpy = cy.spy().as('viewSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid-10', status: 'Ready' })], 'lastUpdated_desc', {
        onView: onViewSpy,
      })

      cy.get('.button-label').contains('View').closest('button').click()
      cy.get('@viewSpy').should('have.been.calledWith', 'test-guid-10')
    })

    it('emits download event when Download is clicked', () => {
      const onDownloadSpy = cy.spy().as('downloadSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid-10', status: 'Ready' })], 'lastUpdated_desc', {
        onDownload: onDownloadSpy,
      })

      cy.get('.button-label').contains('Download').closest('button').click()
      cy.get('@downloadSpy').should('have.been.calledWith', 'test-guid-10')
    })
  })

  describe('Running status actions', () => {
    it('shows Cancel and Delete buttons only', () => {
      mountComponent([createProjection({ status: 'Running' })])

      cy.get('.button-label').contains('Cancel').should('exist')
      cy.get('.button-label').contains('Delete').should('exist')
      cy.get('.button-label').contains('View').should('not.exist')
      cy.get('.button-label').contains('Edit').should('not.exist')
      cy.get('.button-label').contains('Duplicate').should('not.exist')
      cy.get('.button-label').contains('Download').should('not.exist')
    })

    it('emits cancel event when Cancel is clicked', () => {
      const onCancelSpy = cy.spy().as('cancelSpy')
      mountComponent([createProjection({ projectionGUID: 'test-guid-7', status: 'Running' })], 'lastUpdated_desc', {
        onCancel: onCancelSpy,
      })

      cy.get('.button-label').contains('Cancel').closest('button').click()
      cy.get('@cancelSpy').should('have.been.calledWith', 'test-guid-7')
    })
  })

  describe('Failed status actions', () => {
    it('shows Edit, Duplicate, Download, and Delete buttons', () => {
      mountComponent([createProjection({ status: 'Failed' })])

      cy.get('.button-label').contains('Edit').should('exist')
      cy.get('.button-label').contains('Duplicate').should('exist')
      cy.get('.button-label').contains('Download').should('exist')
      cy.get('.button-label').contains('Delete').should('exist')
      cy.get('.button-label').contains('View').should('not.exist')
      cy.get('.button-label').contains('Cancel').should('not.exist')
    })
  })

  describe('action button icons', () => {
    it('displays correct icons for Draft status actions', () => {
      mountComponent([createProjection({ status: 'Draft' })])

      cy.get('img[alt="Edit"]').should('exist')
      cy.get('img[alt="Duplicate"]').should('exist')
      cy.get('img[alt="Delete"]').should('exist')
    })

    it('displays correct icons for Ready status actions', () => {
      mountComponent([createProjection({ status: 'Ready' })])

      cy.get('img[alt="View"]').should('exist')
      cy.get('img[alt="Duplicate"]').should('exist')
      cy.get('img[alt="Download"]').should('exist')
      cy.get('img[alt="Delete"]').should('exist')
    })

    it('displays correct icons for Running status actions', () => {
      mountComponent([createProjection({ status: 'Running' })])

      cy.get('img[alt="Cancel"]').should('exist')
      cy.get('img[alt="Delete"]').should('exist')
    })
  })

  describe('multiple projections', () => {
    it('renders multiple cards with different statuses', () => {
      const projections = [
        createProjection({ projectionGUID: 'guid-1', title: 'Draft Projection', status: 'Draft' }),
        createProjection({ projectionGUID: 'guid-2', title: 'Ready Projection', status: 'Ready' }),
        createProjection({ projectionGUID: 'guid-3', title: 'Running Projection', status: 'Running' }),
        createProjection({ projectionGUID: 'guid-4', title: 'Failed Projection', status: 'Failed' }),
      ]
      mountComponent(projections)

      cy.get('.projection-card').should('have.length', 4)
      cy.get('.status-text.status-draft').should('exist')
      cy.get('.status-text.status-ready').should('exist')
      cy.get('.status-text.status-running').should('exist')
      cy.get('.status-text.status-failed').should('exist')
    })

    it('emits correct projectionGUID when clicking actions on different cards', () => {
      const onEditSpy = cy.spy().as('editSpy')
      const projections = [
        createProjection({ projectionGUID: 'guid-first', title: 'First', status: 'Draft' }),
        createProjection({ projectionGUID: 'guid-second', title: 'Second', status: 'Draft' }),
      ]
      mountComponent(projections, 'lastUpdated_desc', { onEdit: onEditSpy })

      cy.get('.projection-card')
        .eq(1)
        .find('.button-label')
        .contains('Edit')
        .closest('button')
        .click()
      cy.get('@editSpy').should('have.been.calledWith', 'guid-second')
    })
  })

  describe('empty state', () => {
    it('renders no cards when projections array is empty', () => {
      mountComponent([])

      cy.get('.projection-card').should('have.length', 0)
      cy.get('.sort-dropdown').should('exist')
    })
  })

  describe('accessibility', () => {
    it('action button icons have alt attributes', () => {
      mountComponent([createProjection({ status: 'Draft' })])

      cy.get('img[alt="Edit"]').should('exist')
      cy.get('img[alt="Duplicate"]').should('exist')
      cy.get('img[alt="Delete"]').should('exist')
    })
  })
})
