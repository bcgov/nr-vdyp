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
    return mount(AdminProjectionTable, {
      props: {
        projections,
        sortBy: '',
        sortOrder: 'asc',
        now: Date.now(),
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
    cy.get('.status-badge').should('contain.text', 'Running')
    cy.get('.table-cell').contains('R. MacLeod').should('exist')
    cy.get('.user-type-chip').should('contain.text', 'IDIR')
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

  it('shows empty state message when projections array is empty', () => {
    mountComponent([])

    cy.get('.empty-state-row').should('exist')
    cy.get('.empty-state-message').should('contain.text', 'No running projections found.')
  })
})
