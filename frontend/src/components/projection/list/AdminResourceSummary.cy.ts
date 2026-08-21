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
      stuckCount?: number
      queuedCount?: number
      storagePercent?: number
      storageUsedBytes?: number
      storageTotalBytes?: number
      storageOutOfSpec?: boolean
    } = {},
  ) => {
    return mount(AdminResourceSummary, {
      props: {
        totalRunning: 5,
        threadsInUse: 3,
        threadCapacity: 10,
        threadUsagePercent: 30,
        stuckCount: 1,
        queuedCount: 2,
        storagePercent: 20,
        storageUsedBytes: 20 * 1024 ** 3,
        storageTotalBytes: 100 * 1024 ** 3,
        storageOutOfSpec: false,
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
    cy.get('.summary-pill--threads .thread-progress-bar').should('exist')
  })

  it('renders the stuck projections count', () => {
    mountComponent({ stuckCount: 4 })

    cy.get('.summary-pill--stuck .pill-value').should('contain', '4')
  })

  it('renders the queued projections count', () => {
    mountComponent({ queuedCount: 7 })

    cy.get('.summary-pill--queued .pill-value').should('contain', '7')
  })

  it('renders zero values correctly', () => {
    mountComponent({
      totalRunning: 0,
      threadsInUse: 0,
      threadCapacity: 10,
      threadUsagePercent: 0,
      stuckCount: 0,
    })

    cy.get('.summary-pill--total .pill-value').should('contain', '0')
    cy.get('.summary-pill--threads .pill-value').should('contain', '0/ 10')
    cy.get('.summary-pill--stuck .pill-value').should('contain', '0')
  })

  it('reflects threadUsagePercent in the progress bar', () => {
    mountComponent({ threadUsagePercent: 90 })

    cy.get('.summary-pill--threads .thread-progress-bar').should(
      'have.attr',
      'aria-valuenow',
      '90',
    )
  })

  it('renders the system storage percentage and progress bar', () => {
    mountComponent({ storagePercent: 42 })

    cy.get('.summary-pill--storage .pill-value').should('contain', '42%')
    cy.get('.summary-pill--storage .thread-progress-bar').should(
      'have.attr',
      'aria-valuenow',
      '42',
    )
  })

  it('flags the storage pill as an alert when out of spec', () => {
    mountComponent({ storageOutOfSpec: true })

    cy.get('.summary-pill--storage-alert').should('exist')
  })

  it('does not flag the storage pill as an alert when within spec', () => {
    mountComponent({ storageOutOfSpec: false })

    cy.get('.summary-pill--storage-alert').should('not.exist')
  })
})
