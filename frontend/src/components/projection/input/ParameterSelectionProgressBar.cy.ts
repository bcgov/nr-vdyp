import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ParameterSelectionProgressBar from './ParameterSelectionProgressBar.vue'
import type { ProgressSection } from './ParameterSelectionProgressBar.vue'

const vuetify = createVuetify()

const defaultSections: ProgressSection[] = [
  { label: 'Details', completed: true },
  { label: 'Species', completed: true },
  { label: 'Site', completed: false },
  { label: 'Stand', completed: false },
  { label: 'Report', completed: false },
]

const mountBar = (props: {
  sections?: ProgressSection[]
  percentage?: number
  completedCount?: number
  projectionStatus?: string
}) =>
  mount(ParameterSelectionProgressBar, {
    global: { plugins: [vuetify] },
    props: {
      sections: defaultSections,
      percentage: 40,
      completedCount: 2,
      projectionStatus: 'Draft',
      ...props,
    },
  })

describe('<ParameterSelectionProgressBar />', () => {
  describe('Step count text', () => {
    it('shows completed count and total sections', () => {
      mountBar({ completedCount: 2 })
      cy.contains('Step 2 of 5 Complete').should('exist')
    })

    it('reflects updated completedCount', () => {
      mountBar({ completedCount: 5 })
      cy.contains('Step 5 of 5 Complete').should('exist')
    })
  })

  describe('Percentage display', () => {
    it('shows percentage text when status is Draft', () => {
      mountBar({ projectionStatus: 'Draft', percentage: 40 })
      cy.contains('40% Complete').should('exist')
    })

    it('hides percentage text when status is Ready', () => {
      mountBar({ projectionStatus: 'Ready', percentage: 100 })
      cy.contains('% Complete').should('not.exist')
    })

    it('hides percentage text when status is Running', () => {
      mountBar({ projectionStatus: 'Running' })
      cy.contains('% Complete').should('not.exist')
    })

    it('hides percentage text when status is Failed', () => {
      mountBar({ projectionStatus: 'Failed' })
      cy.contains('% Complete').should('not.exist')
    })
  })

  describe('Status display', () => {
    it('shows status text for Ready', () => {
      mountBar({ projectionStatus: 'Ready' })
      cy.contains('Ready').should('exist')
    })

    it('shows status text for Running', () => {
      mountBar({ projectionStatus: 'Running' })
      cy.contains('Running').should('exist')
    })

    it('shows status text for Failed', () => {
      mountBar({ projectionStatus: 'Failed' })
      cy.contains('Failed').should('exist')
    })

    it('applies correct class for Ready status', () => {
      mountBar({ projectionStatus: 'Ready' })
      cy.get('.status-text--ready').should('exist')
    })

    it('applies correct class for Running status', () => {
      mountBar({ projectionStatus: 'Running' })
      cy.get('.status-text--running').should('exist')
    })

    it('applies correct class for Failed status', () => {
      mountBar({ projectionStatus: 'Failed' })
      cy.get('.status-text--failed').should('exist')
    })
  })

  describe('Section labels', () => {
    it('renders all section labels', () => {
      mountBar({})
      defaultSections.forEach((s) => cy.contains('.section-label', s.label).should('exist'))
    })

    it('marks completed sections with section-complete class', () => {
      mountBar({})
      cy.get('.section-complete').should('have.length', 2)
    })
  })

  describe('Instruction text', () => {
    it('renders the instruction text', () => {
      mountBar({})
      cy.contains('Complete the following sections to run a projection').should('exist')
    })
  })
})
