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
    it('renders Clear button by default', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: false })
      cy.contains('button.bcds-button', 'Clear').should('exist')
    })

    it('is hidden when hideClearButton is true', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: false, hideClearButton: true })
      cy.contains('button.bcds-button', 'Clear').should('not.exist')
    })

    it('is disabled when isConfirmEnabled is false', () => {
      mountPanel({ isConfirmEnabled: false, isConfirmed: false })
      cy.contains('button.bcds-button', 'Clear').should('be.disabled')
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

    it('renders when showCancelButton is true', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: false, showCancelButton: true })
      cy.contains('button.bcds-button', 'Cancel').should('exist')
    })

    it('is disabled when isConfirmEnabled is false', () => {
      mountPanel({ isConfirmEnabled: false, isConfirmed: false, showCancelButton: true })
      cy.contains('button.bcds-button', 'Cancel').should('be.disabled')
    })

    it('emits "cancel" when clicked', () => {
      const onCancel = cy.spy().as('cancelSpy')
      mountPanel({ isConfirmEnabled: true, isConfirmed: false, showCancelButton: true, onCancel })
      cy.contains('button.bcds-button', 'Cancel').click()
      cy.get('@cancelSpy').should('have.been.calledOnce')
    })
  })

  describe('Next button', () => {
    it('is visible when isConfirmed is false', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: false })
      cy.contains('button.bcds-button', 'Next').should('be.visible')
    })

    it('is hidden when isConfirmed is true and hideEditButton is false', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: true })
      cy.contains('button.bcds-button', 'Next').should('not.be.visible')
    })

    it('is visible when isConfirmed is true but hideEditButton is true', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: true, hideEditButton: true })
      cy.contains('button.bcds-button', 'Next').should('be.visible')
    })

    it('is disabled when isConfirmEnabled is false', () => {
      mountPanel({ isConfirmEnabled: false, isConfirmed: false })
      cy.contains('button.bcds-button', 'Next').should('be.disabled')
    })

    it('emits "confirm" when clicked', () => {
      const onConfirm = cy.spy().as('confirmSpy')
      mountPanel({ isConfirmEnabled: true, isConfirmed: false, onConfirm })
      cy.contains('button.bcds-button', 'Next').click()
      cy.get('@confirmSpy').should('have.been.calledOnce')
    })
  })

  describe('Edit button', () => {
    it('is hidden when isConfirmed is false', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: false })
      cy.contains('button.bcds-button', 'Edit').should('not.be.visible')
    })

    it('is visible when isConfirmed is true', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: true })
      cy.contains('button.bcds-button', 'Edit').should('be.visible')
    })

    it('is hidden when isConfirmed is true but hideEditButton is true', () => {
      mountPanel({ isConfirmEnabled: true, isConfirmed: true, hideEditButton: true })
      cy.contains('button.bcds-button', 'Edit').should('not.be.visible')
    })

    it('emits "edit" when clicked', () => {
      const onEdit = cy.spy().as('editSpy')
      mountPanel({ isConfirmEnabled: true, isConfirmed: true, onEdit })
      cy.contains('button.bcds-button', 'Edit').click()
      cy.get('@editSpy').should('have.been.calledOnce')
    })
  })
})
