import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import AdminProjectionTable from './AdminProjectionTable.vue'
import type { AdminProjection } from '@/interfaces/interfaces'

const vuetify = createVuetify({
  defaults: {
    global: {
      transition: false,
    },
  },
})

describe('AdminProjectionTable.vue', () => {
  const createProjection = (
    overrides: Partial<AdminProjection> = {},
  ): AdminProjection => ({
    projectionGUID: 'guid-1',
    title: 'Test Projection',
    ownerDisplayName: 'R. MacLeod',
    userType: 'IDIR',
    queuedDate: '2026-01-10T14:00:00Z',
    startDate: '2026-01-10T14:30:00Z',
    status: 'Running',
    workerCount: 16,
    completedPolygonCount: 184320,
    polygonCount: 256000,
    estimatedPolygonCount: null,
    isPrioritized: false,
    ...overrides,
  })

  const mountComponent = (
    projections: AdminProjection[] = [createProjection()],
    eventHandlers: Record<string, Cypress.Agent<sinon.SinonSpy>> = {},
    now: number = Date.now(),
  ) => {
    return mount(AdminProjectionTable, {
      props: {
        projections,
        sortBy: '',
        sortOrder: 'asc',
        now,
      },
      global: { plugins: [vuetify] },
      attrs: eventHandlers,
    })
  }

  it('renders a row for each projection', () => {
    mountComponent([createProjection({ projectionGUID: 'guid-1' }), createProjection({ projectionGUID: 'guid-2' })])

    cy.get('.table-row').should('have.length', 2)
  })

  it('displays projection info fields', () => {
    mountComponent()

    cy.get('.projection-title').should('contain.text', 'Test Projection')
    cy.get('.status-text').should('contain.text', 'Running')
    cy.get('.table-cell').contains('R. MacLeod').should('exist')
    cy.get('.table-cell').contains('IDIR').should('exist')
  })

  it('emits sort event with correct key when a sortable header is clicked', () => {
    const onSortSpy = cy.spy().as('sortSpy')
    mountComponent([createProjection()], { onSort: onSortSpy })

    cy.get('.table-header.sortable').contains('Threads').click()
    cy.get('@sortSpy').should('have.been.calledWith', 'workerCount')
  })

  it('emits cancel event with correct GUID when Cancel is clicked', () => {
    const onCancelSpy = cy.spy().as('cancelSpy')
    mountComponent([createProjection({ projectionGUID: 'test-guid' })], { onCancel: onCancelSpy })

    cy.get('.cancel-button').click()
    cy.get('@cancelSpy').should('have.been.calledWith', 'test-guid')
  })

  it('emits prioritize event with correct GUID when Prioritize is clicked', () => {
    const onPrioritizeSpy = cy.spy().as('prioritizeSpy')
    mountComponent([createProjection({ projectionGUID: 'test-guid' })], { onPrioritize: onPrioritizeSpy })

    cy.get('.prioritize-button').click()
    cy.get('@prioritizeSpy').should('have.been.calledWith', 'test-guid')
  })

  it('enables the Prioritize button only for Running projections', () => {
    mountComponent([createProjection({ status: 'Running' })])

    cy.get('.prioritize-button').should('not.have.attr', 'data-disabled')
  })

  it('disables the Prioritize button for Stuck projections', () => {
    mountComponent([createProjection({ status: 'Stuck' })])

    cy.get('.prioritize-button').should('have.attr', 'data-disabled')
  })

  it('disables the Prioritize button for Queued projections', () => {
    mountComponent([createProjection({ status: 'Queued' })])

    cy.get('.prioritize-button').should('have.attr', 'data-disabled')
  })

  it('shows the High Priority badge when isPrioritized is true', () => {
    mountComponent([createProjection({ isPrioritized: true })])

    cy.get('.status-text.status-text-priority').should('contain.text', 'High Priority')
  })

  it('does not show the High Priority badge when isPrioritized is false', () => {
    mountComponent([createProjection({ isPrioritized: false })])

    cy.get('.status-text.status-text-priority').should('not.exist')
  })

  it('hides the Prioritize button once a projection is already prioritized', () => {
    mountComponent([createProjection({ isPrioritized: true })])

    cy.get('.prioritize-button').should('not.exist')
    cy.get('.cancel-button').should('exist')
  })

  it('shows empty state message when projections array is empty', () => {
    mountComponent([])

    cy.get('.empty-state-row').should('exist')
    cy.get('.empty-state-message').should('contain.text', 'No projections found.')
  })

  it('shows the Queued badge and placeholder values for queued projections', () => {
    mountComponent([
      createProjection({ status: 'Queued', queuedDate: null, startDate: null, workerCount: 0 }),
    ])

    cy.get('.status-text').should('contain.text', 'Queued')
    cy.get('.table-cell').eq(4).should('contain.text', '-') // Elapsed
    cy.get('.table-cell').eq(5).should('contain.text', '-') // Threads
    cy.get('.table-cell').eq(6).should('contain.text', '-') // Progress
    cy.get('.table-cell').eq(7).should('contain.text', '-') // Polygons
    cy.get('.polygons-estimate').should('not.exist')
  })

  it('shows the estimated polygon count for queued projections with an upload-time count', () => {
    mountComponent([
      createProjection({
        status: 'Queued',
        startDate: null,
        workerCount: 0,
        completedPolygonCount: 0,
        polygonCount: 0,
        estimatedPolygonCount: 12345,
      }),
    ])

    cy.get('.table-cell').eq(6).should('contain.text', '-')
    cy.get('.table-cell')
      .eq(7)
      .find('.polygons-estimate')
      .should('exist')
      .within(() => {
        cy.get('.polygons-completed').should('have.text', '-')
        cy.get('.polygons-total').should('contain.text', `/ ${(12345).toLocaleString()}`)
      })
  })

  it('shows the pre-execution estimate tooltip on hover', () => {
    mountComponent([
      createProjection({ status: 'Queued', startDate: null, estimatedPolygonCount: 500 }),
    ])

    cy.get('.polygons-estimate').trigger('mouseenter')
    cy.get('.v-tooltip').should(
      'contain.text',
      'pre-execution estimates based on the uploaded input dataset',
    )
  })

  it('shows the queue wait time and its tooltip for queued projections', () => {
    mountComponent(
      [
        createProjection({
          status: 'Queued',
          queuedDate: '2026-01-10T14:00:00Z',
          startDate: '2026-01-01T00:00:00Z',
          workerCount: 0,
        }),
      ],
      {},
      Date.parse('2026-01-10T16:05:09Z'),
    )

    cy.get('.table-cell')
      .eq(4)
      .find('.elapsed-queue-wait')
      .should('contain.text', '02:05:09')
      .trigger('mouseenter')
    cy.get('.v-tooltip').should('contain.text', 'waiting in the queue prior to resource allocation')
  })
})
