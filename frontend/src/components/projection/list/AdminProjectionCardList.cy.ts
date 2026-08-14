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
    startDate: '2026-01-10T14:30:00',
    status: 'Running',
    workerCount: 16,
    completedPolygonCount: 184320,
    polygonCount: 256000,
    ...overrides,
  })

  const mountComponent = (
    projections: AdminProjection[] = [createProjection()],
    eventHandlers: Record<string, Cypress.Agent<sinon.SinonSpy>> = {},
  ) => {
    return mount(AdminProjectionCardList, {
      props: { projections, sortOptions, sortValue: 'workerCount-desc', now: Date.now() },
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

  it('shows empty state message when projections array is empty', () => {
    mountComponent([])

    cy.get('.empty-state-card').should('exist')
    cy.get('.empty-state-message').should('contain.text', 'No projections found.')
  })

  it('shows the Queued badge and placeholder values for queued projections', () => {
    mountComponent([
      createProjection({ status: 'Queued', startDate: null, workerCount: 0 }),
    ])

    cy.get('.status-badge').should('contain.text', 'Queued')
    cy.get('.card-info-value').eq(2).should('contain.text', '-') // Elapsed
    cy.get('.card-info-value').eq(3).should('contain.text', '-') // Threads
    cy.get('.card-info-value').eq(4).should('contain.text', '-') // Progress
    cy.get('.card-info-value').eq(5).should('contain.text', '-') // Polygons
  })
})
