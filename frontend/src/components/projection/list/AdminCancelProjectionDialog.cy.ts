import AdminCancelProjectionDialog from './AdminCancelProjectionDialog.vue'
import { ADMIN_CANCEL_REASON } from '@/constants/constants'

describe('AdminCancelProjectionDialog.vue', () => {
  const defaultProps = {
    modelValue: true,
    projectionTitle: 'My Test Projection',
  }

  it('renders the modal title with the projection name', () => {
    cy.mountWithVuetify(AdminCancelProjectionDialog, { props: defaultProps })

    cy.contains('Cancel Projection: My Test Projection').should('be.visible')
  })

  it('pre-fills the reason textarea with the default justification text', () => {
    cy.mountWithVuetify(AdminCancelProjectionDialog, { props: defaultProps })

    cy.get('#adminCancelReason').should('have.value', ADMIN_CANCEL_REASON.DEFAULT_TEXT)
  })

  it('disables Confirm Cancellation when the reason is below the minimum length', () => {
    cy.mountWithVuetify(AdminCancelProjectionDialog, { props: defaultProps })

    cy.get('#adminCancelReason').clear()
    cy.get('#adminCancelReason').type('Hi')
    cy.contains('button', 'Confirm Cancellation').should('be.disabled')
  })

  it('enables Confirm Cancellation once a valid reason is typed', () => {
    cy.mountWithVuetify(AdminCancelProjectionDialog, { props: defaultProps })

    cy.get('#adminCancelReason').clear()
    cy.get('#adminCancelReason').type('System maintenance window.')
    cy.contains('button', 'Confirm Cancellation').should('not.be.disabled')
  })

  it('emits confirm with the trimmed reason and closes the dialog when Confirm Cancellation is clicked', () => {
    const onConfirmSpy = cy.spy().as('confirmSpy')
    const onUpdateSpy = cy.spy().as('updateSpy')

    cy.mountWithVuetify(AdminCancelProjectionDialog, {
      props: { ...defaultProps, onConfirm: onConfirmSpy, 'onUpdate:modelValue': onUpdateSpy },
    })

    cy.get('#adminCancelReason').clear()
    cy.get('#adminCancelReason').type('System maintenance window.')
    cy.contains('button', 'Confirm Cancellation').click()
    cy.get('@confirmSpy').should('have.been.calledOnceWith', 'System maintenance window.')
    cy.get('@updateSpy').should('have.been.calledOnceWith', false)
  })

  it('emits update:modelValue false without emitting confirm when Keep Running is clicked', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')
    const onConfirmSpy = cy.spy().as('confirmSpy')

    cy.mountWithVuetify(AdminCancelProjectionDialog, {
      props: { ...defaultProps, 'onUpdate:modelValue': onUpdateSpy, onConfirm: onConfirmSpy },
    })

    cy.contains('button', 'Keep Running').click()
    cy.get('@updateSpy').should('have.been.calledOnceWith', false)
    cy.get('@confirmSpy').should('not.have.been.called')
  })
})
