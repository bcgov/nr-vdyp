import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ActionPanel from './ActionPanel.vue'
import AppButton from '@/components/core/AppButton.vue'

const vuetify = createVuetify()

interface MountProps {
  isConfirmEnabled: boolean
  isConfirmed: boolean
  hideClearButton?: boolean
  hideEditButton?: boolean
  showCancelButton?: boolean
  onClear?: () => void
  onConfirm?: () => void
  onEdit?: () => void
  onCancel?: () => void
}

const mountPanel = (props: MountProps) =>
  mount(ActionPanel, {
    global: { plugins: [vuetify], components: { AppButton } },
    props,
  })

describe('<ActionPanel />', () => {
  describe('Clear button', () => {
    it('is hidden when hideClearButton is true', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: false, hideClearButton: true })
      cy.contains('button.bcds-button', 'Clear').should('not.exist')
    })

    it('emits "clear" when clicked', () => {
      const onClear = cy.spy().as('clearSpy')
      mountPanel({ isConfirmEnabled: true, isConfirmed: false, onClear })
      cy.contains('button.bcds-button', 'Clear').click()
      cy.get('@clearSpy').should('have.been.calledOnce')
    })
  })

  describe('Cancel button', () => {
    it('is not rendered by default', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: false })
      cy.contains('button.bcds-button', 'Cancel').should('not.exist')
    })

    it('emits "cancel" when clicked', () => {
      const onCancel = cy.spy().as('cancelSpy')
      mountPanel({ isConfirmEnabled: true, isConfirmed: false, showCancelButton: true, onCancel })
      cy.contains('button.bcds-button', 'Cancel').click()
      cy.get('@cancelSpy').should('have.been.calledOnce')
    })
  })

  describe('Next / Edit button toggle', () => {
    it('shows Next and hides Edit when isConfirmed is false', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: false })
      cy.contains('button.bcds-button', 'Next').should('be.visible')
      cy.contains('button.bcds-button', 'Edit').should('not.be.visible')
    })

    it('hides Next and shows Edit when isConfirmed is true', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: true })
      cy.contains('button.bcds-button', 'Next').should('not.be.visible')
      cy.contains('button.bcds-button', 'Edit').should('be.visible')
    })

    it('disables Next when isConfirmEnabled is false', () => {
      mountPanel({ isConfirmEnabled: false, isConfirmed: false })
      cy.contains('button.bcds-button', 'Next').should('be.disabled')
    })

    it('emits "confirm" when Next is clicked', () => {
      const onConfirm = cy.spy().as('confirmSpy')
      mountPanel({ isConfirmEnabled: true, isConfirmed: false, onConfirm })
      cy.contains('button.bcds-button', 'Next').click()
      cy.get('@confirmSpy').should('have.been.calledOnce')
    })

    it('emits "edit" when Edit is clicked', () => {
      const onEdit = cy.spy().as('editSpy')
      mountPanel({ isConfirmEnabled: true, isConfirmed: true, onEdit })
      cy.contains('button.bcds-button', 'Edit').click()
      cy.get('@editSpy').should('have.been.calledOnce')
    })
  })
})
