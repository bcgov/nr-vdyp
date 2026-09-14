import { mount } from 'cypress/vue'
import AdminCancellationBanner from './AdminCancellationBanner.vue'

describe('AdminCancellationBanner.vue', () => {
  it('renders the given reason text prefixed with "Cancel Reason:"', () => {
    mount(AdminCancellationBanner, {
      props: { reason: 'Cancelled by admin for maintenance' },
    })

    cy.get('.admin-cancellation-banner').should('exist')
    cy.get('.admin-cancellation-banner--text').should(
      'contain',
      'Cancel Reason: Cancelled by admin for maintenance',
    )
  })
})
