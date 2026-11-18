import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import AdminProjectionCardList from './AdminProjectionCardList.vue'
import type { AdminProjection, SortOption } from '@/interfaces/interfaces'

const vuetify = createVuetify({
  defaults: {
    global: {
      transition: false,
    },
  },
})

const sortOptions: SortOption[] = [
  { title: 'Threads (Highest First)', value: 'workerCount-desc' },
  { title: 'Threads (Lowest First)', value: 'workerCount-asc' },
  { title: 'Progress (Highest First)', value: 'progress-desc' },
  { title: 'Progress (Lowest First)', value: 'progress-asc' },
  { title: 'Polygons (Highest First)', value: 'completedPolygonCount-desc' },
  { title: 'Polygons (Lowest First)', value: 'completedPolygonCount-asc' },
]

describe('AdminProjectionCardList.vue', () => {
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
    return mount(AdminProjectionCardList, {
      props: { projections, sortOptions, sortValue: 'workerCount-desc', now },
      global: { plugins: [vuetify] },
      attrs: eventHandlers,
    })
  }

  it('renders a card for each projection', () => {
    mountComponent([createProjection({ projectionGUID: 'guid-1' }), createProjection({ projectionGUID: 'guid-2' })])

    cy.get('.admin-projection-card').should('have.length', 2)
  })

  it('displays projection info fields', () => {
    mountComponent()

    cy.get('.card-title').should('contain.text', 'Test Projection')
    cy.get('.status-badge').should('contain.text', 'Running')
    cy.get('.card-info-value').contains('R. MacLeod').should('exist')
    cy.get('.card-info-value').contains('IDIR').should('exist')
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

  it('shows the High Priority badge when isPrioritized is true', () => {
    mountComponent([createProjection({ isPrioritized: true })])

    cy.get('.status-badge.status-priority').should('contain.text', 'High Priority')
  })

  it('shows empty state message when projections array is empty', () => {
    mountComponent([])

    cy.get('.empty-state-card').should('exist')
    cy.get('.empty-state-message').should('contain.text', 'No projections found.')
  })

  it('shows the Queued badge and placeholder values for queued projections', () => {
    mountComponent([
      createProjection({ status: 'Queued', queuedDate: null, startDate: null, workerCount: 0 }),
    ])

    cy.get('.status-badge').should('contain.text', 'Queued')
    cy.get('.card-info-value').eq(2).should('contain.text', '-') // Elapsed
    cy.get('.card-info-value').eq(3).should('contain.text', '-') // Threads
    cy.get('.card-info-value').eq(4).should('contain.text', '-') // Progress
    cy.get('.card-info-value').eq(5).should('contain.text', '-') // Polygons
    cy.get('.polygons-estimate').should('not.exist')
  })

  it('shows the estimated polygon count for queued projections with an upload-time count', () => {
    mountComponent([
      createProjection({ status: 'Queued', startDate: null, workerCount: 0, estimatedPolygonCount: 12345 }),
    ])

    cy.get('.card-info-value')
      .eq(5)
      .should('have.class', 'polygons-estimate')
      .find('.polygons-total')
      .should('contain.text', `/ ${(12345).toLocaleString()}`)
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

    cy.get('.card-info-value')
      .eq(2)
      .should('have.class', 'elapsed-queue-wait')
      .and('contain.text', '02:05:09')
      .trigger('mouseenter')
    cy.get('.v-tooltip').should('contain.text', 'waiting in the queue prior to resource allocation')
  })
})
