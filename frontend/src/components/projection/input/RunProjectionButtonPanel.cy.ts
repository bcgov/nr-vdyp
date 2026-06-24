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
  describe('Run Projection button', () => {
    it('renders enabled Run Projection button with primary variant', () => {
      mountPanel({ isDisabled: false })
      cy.get('button.bcds-button')
        .should('contain.text', 'Run Projection')
        .and('have.class', 'primary')
        .and('not.be.disabled')
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
  })

  describe('Tooltip', () => {
    it('shows tooltip anchor when disabled with disabledText', () => {
      mountPanel({ isDisabled: true, disabledText: 'Please fill in required fields' })
      cy.get('.run-btn-tooltip-anchor').should('exist')
    })

    it('hides tooltip anchor when enabled or disabledText is empty', () => {
      mountPanel({ isDisabled: false, disabledText: 'Please fill in required fields' })
      cy.get('.run-btn-tooltip-anchor').should('not.exist')

      mountPanel({ isDisabled: true, disabledText: '' })
      cy.get('.run-btn-tooltip-anchor').should('not.exist')
    })
  })

  describe('Cancel Run button', () => {
    it('renders Cancel Run button with danger variant and hides Run Projection', () => {
      mountPanel({ isDisabled: false, showCancelButton: true })
      cy.get('button.bcds-button')
        .should('contain.text', 'Cancel Run')
        .and('have.class', 'danger')
        .and('not.contain.text', 'Run Projection')
    })

    it('emits "cancelRun" when clicked', () => {
      const onCancelRun = cy.spy().as('cancelRunSpy')
      mountPanel({ isDisabled: false, showCancelButton: true, onCancelRun })
      cy.get('button.bcds-button').click()
      cy.get('@cancelRunSpy').should('have.been.calledOnce')
    })
  })

  describe('Card and actions classes', () => {
    it('applies default card and actions classes', () => {
      mountPanel({ isDisabled: false })
      cy.get('.run-projection-card').should('exist')
      cy.get('.card-actions').should('exist')
    })

    it('applies custom card and actions classes', () => {
      mountPanel({ isDisabled: false, cardClass: 'my-custom-card', cardActionsClass: 'my-custom-actions' })
      cy.get('.my-custom-card').should('exist')
      cy.get('.my-custom-actions').should('exist')
    })
  })
})
