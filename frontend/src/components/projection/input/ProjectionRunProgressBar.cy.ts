import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ProjectionRunProgressBar from './ProjectionRunProgressBar.vue'

const vuetify = createVuetify()

const mountBar = (props: {
  status?: string
  polygonCount?: number | null
  completedPolygonCount?: number | null
  errorCount?: number | null
  startDate?: string | null
  endDate?: string | null
}) =>
  mount(ProjectionRunProgressBar, {
    global: { plugins: [vuetify] },
    props: {
      status: 'Running',
      polygonCount: 1000,
      completedPolygonCount: 500,
      errorCount: 3,
      startDate: null,
      endDate: null,
      ...props,
    },
  })

describe('<ProjectionRunProgressBar />', () => {
  describe('Status tile', () => {
    it('displays Running status text and color class', () => {
      mountBar({ status: 'Running' })
      cy.contains('.tile-value', 'Running').should('exist')
      cy.get('.tile-value--running').should('exist')
    })

    it('displays Ready status text and color class', () => {
      mountBar({ status: 'Ready' })
      cy.contains('.tile-value', 'Ready').should('exist')
      cy.get('.tile-value--ready').should('exist')
    })

    it('displays Failed status text and color class', () => {
      mountBar({ status: 'Failed' })
      cy.contains('.tile-value', 'Failed').should('exist')
      cy.get('.tile-value--failed').should('exist')
    })
  })

  describe('Time elapsed tile', () => {
    it('shows dash when startDate is null', () => {
      mountBar({ startDate: null })
      cy.get('.tile--time .tile-value').should('have.text', '-')
    })

    it('shows elapsed time when startDate is provided', () => {
      const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
      mountBar({ startDate: twoHoursAgo })
      cy.get('.tile--time .tile-value').should('contain', 'h')
    })
  })

  describe('Polygons processed tile', () => {
    it('shows completed/total polygon counts', () => {
      mountBar({ polygonCount: 1000, completedPolygonCount: 500 })
      cy.get('.tile--polygons .tile-value').should('contain', '500')
      cy.get('.tile--polygons .tile-value').should('contain', '1,000')
    })

    it('shows 0/0 when both are null', () => {
      mountBar({ polygonCount: null, completedPolygonCount: null })
      cy.get('.tile--polygons .tile-value').should('have.text', '0/0')
    })
  })

  describe('Error count tile', () => {
    it('shows error count', () => {
      mountBar({ errorCount: 42 })
      cy.get('.tile--errors .tile-value').should('have.text', '42')
    })

    it('shows 0 when errorCount is null', () => {
      mountBar({ errorCount: null })
      cy.get('.tile--errors .tile-value').should('have.text', '0')
    })
  })

  describe('Progress bar', () => {
    it('shows correct progress percentage text for Running', () => {
      mountBar({ status: 'Running', polygonCount: 1000, completedPolygonCount: 250 })
      cy.contains('.progress-right-text', '25% Complete').should('exist')
    })

    it('shows Done text for Ready status', () => {
      mountBar({ status: 'Ready', polygonCount: 1000, completedPolygonCount: 1000 })
      cy.contains('.progress-right-text', 'Done').should('exist')
    })

    it('shows green fill for Running and red fill for Failed', () => {
      mountBar({ status: 'Running' })
      cy.get('.progress-fill--green').should('exist')

      mountBar({ status: 'Failed' })
      cy.get('.progress-fill--red').should('exist')
    })

    it('shows correct left labels for each status', () => {
      mountBar({ status: 'Running' })
      cy.contains('.progress-left-text', 'Running Projection Model...').should('exist')

      mountBar({ status: 'Ready' })
      cy.contains('.progress-left-text', 'Projection Complete').should('exist')

      mountBar({ status: 'Failed' })
      cy.contains('.progress-left-text', 'Projection Run Failed').should('exist')
    })

    it('shows 0% when polygonCount is null', () => {
      mountBar({ status: 'Running', polygonCount: null, completedPolygonCount: null })
      cy.contains('.progress-right-text', '0% Complete').should('exist')
    })
  })

  describe('Tile labels', () => {
    it('renders all four tile labels', () => {
      mountBar({})
      cy.contains('.tile-label', 'Status').should('exist')
      cy.contains('.tile-label', 'Time Elapsed').should('exist')
      cy.contains('.tile-label', 'Polygons Processed').should('exist')
      cy.contains('.tile-label', 'Error Count').should('exist')
    })
  })
})
