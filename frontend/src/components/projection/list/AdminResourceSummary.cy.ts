import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import AdminResourceSummary from './AdminResourceSummary.vue'

const vuetify = createVuetify()

describe('AdminResourceSummary.vue', () => {
  const mountComponent = (
    props: {
      totalRunning?: number
      threadsInUse?: number
      threadCapacity?: number
      threadUsagePercent?: number
    } = {},
  ) => {
    return mount(AdminResourceSummary, {
      props: {
        totalRunning: 5,
        threadsInUse: 3,
        threadCapacity: 10,
        threadUsagePercent: 30,
        ...props,
      },
      global: {
        plugins: [vuetify],
      },
    })
  }

  it('renders total running and thread usage values', () => {
    mountComponent()

    cy.get('.summary-pill--total .pill-value').should('contain', '5')
    cy.get('.summary-pill--threads .pill-value').should('contain', '3/ 10')
    cy.get('.thread-progress-bar').should('exist')
  })

  it('renders zero values correctly', () => {
    mountComponent({
      totalRunning: 0,
      threadsInUse: 0,
      threadCapacity: 10,
      threadUsagePercent: 0,
    })

    cy.get('.summary-pill--total .pill-value').should('contain', '0')
    cy.get('.summary-pill--threads .pill-value').should('contain', '0/ 10')
  })

  it('reflects threadUsagePercent in the progress bar', () => {
    mountComponent({ threadUsagePercent: 90 })

    cy.get('.thread-progress-bar').should(
      'have.attr',
      'aria-valuenow',
      '90',
    )
  })
})
