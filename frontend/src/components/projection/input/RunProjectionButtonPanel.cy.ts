import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import RunProjectionButtonPanel from './RunProjectionButtonPanel.vue'
import AppButton from '@/components/core/AppButton.vue'

const vuetify = createVuetify()

interface MountProps {
  isDisabled: boolean
  showCancelButton?: boolean
  cardClass?: string
  cardActionsClass?: string
  disabledText?: string
  onRunModel?: () => void
  onCancelRun?: () => void
}

const mountPanel = (props: MountProps) =>
  mount(RunProjectionButtonPanel, {
    global: { plugins: [vuetify], components: { AppButton } },
    props,
  })

describe('<RunProjectionButtonPanel />', () => {
  describe('Run Projection button (default state)', () => {
    it('renders "Run Projection" button by default', () => {
      mountPanel({ isDisabled: false })
      cy.get('button.bcds-button').should('contain.text', 'Run Projection')
    })

    it('has primary variant', () => {
      mountPanel({ isDisabled: false })
      cy.get('button.bcds-button').should('have.class', 'primary')
    })

    it('is enabled when isDisabled is false', () => {
      mountPanel({ isDisabled: false })
      cy.get('button.bcds-button').should('not.be.disabled')
    })

    it('is disabled when isDisabled is true', () => {
      mountPanel({ isDisabled: true })
      cy.get('button.bcds-button').should('be.disabled')
    })

    it('emits "runModel" when clicked', () => {
      const onRunModel = cy.spy().as('runModelSpy')
      mountPanel({ isDisabled: false, onRunModel })
      cy.get('button.bcds-button').click()
      cy.get('@runModelSpy').should('have.been.calledOnce')
    })

    it('does not emit "runModel" when disabled', () => {
      const onRunModel = cy.spy().as('runModelSpy')
      mountPanel({ isDisabled: true, onRunModel })
      cy.get('button.bcds-button').should('be.disabled')
      cy.get('@runModelSpy').should('not.have.been.called')
    })
  })

  describe('Tooltip', () => {
    it('wraps button in tooltip anchor when disabled with disabledText', () => {
      mountPanel({ isDisabled: true, disabledText: 'Please fill in required fields' })
      cy.get('.run-btn-tooltip-anchor').should('exist')
    })

    it('does not render tooltip anchor when not disabled', () => {
      mountPanel({ isDisabled: false, disabledText: 'Please fill in required fields' })
      cy.get('.run-btn-tooltip-anchor').should('not.exist')
    })

    it('does not render tooltip anchor when disabledText is empty', () => {
      mountPanel({ isDisabled: true, disabledText: '' })
      cy.get('.run-btn-tooltip-anchor').should('not.exist')
    })
  })

  describe('Cancel Run button', () => {
    it('renders "Cancel Run" button when showCancelButton is true', () => {
      mountPanel({ isDisabled: false, showCancelButton: true })
      cy.get('button.bcds-button').should('contain.text', 'Cancel Run')
    })

    it('has danger variant', () => {
      mountPanel({ isDisabled: false, showCancelButton: true })
      cy.get('button.bcds-button').should('have.class', 'danger')
    })

    it('does not render "Run Projection" when showCancelButton is true', () => {
      mountPanel({ isDisabled: false, showCancelButton: true })
      cy.get('button.bcds-button').should('not.contain.text', 'Run Projection')
    })

    it('emits "cancelRun" when clicked', () => {
      const onCancelRun = cy.spy().as('cancelRunSpy')
      mountPanel({ isDisabled: false, showCancelButton: true, onCancelRun })
      cy.get('button.bcds-button').click()
      cy.get('@cancelRunSpy').should('have.been.calledOnce')
    })
  })

  describe('Card and actions classes', () => {
    it('applies default card class when cardClass is not provided', () => {
      mountPanel({ isDisabled: false })
      cy.get('.run-projection-card').should('exist')
    })

    it('applies custom card class when cardClass is provided', () => {
      mountPanel({ isDisabled: false, cardClass: 'my-custom-card' })
      cy.get('.my-custom-card').should('exist')
    })

    it('applies default card actions class when cardActionsClass is not provided', () => {
      mountPanel({ isDisabled: false })
      cy.get('.card-actions').should('exist')
    })

    it('applies custom card actions class when cardActionsClass is provided', () => {
      mountPanel({ isDisabled: false, cardActionsClass: 'my-custom-actions' })
      cy.get('.my-custom-actions').should('exist')
    })
  })
})
