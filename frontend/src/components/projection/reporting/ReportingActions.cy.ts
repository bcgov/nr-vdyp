import ReportingActions from './ReportingActions.vue'
import { REPORTING_TAB, REPORTING_ACTION } from '@/constants/constants'
import type { ReportingTab } from '@/types/types'

describe('ReportingActions.vue', () => {
  const defaultProps: { isButtonDisabled: boolean; tabname: ReportingTab } = {
    isButtonDisabled: false,
    tabname: REPORTING_TAB.MODEL_REPORT,
  }

  const mountComponent = (
    props: Partial<typeof defaultProps> = {},
    eventHandlers: Record<string, Cypress.Agent<sinon.SinonSpy>> = {},
  ) => {
    return cy.mount(ReportingActions, {
      props: { ...defaultProps, ...props, ...eventHandlers },
    })
  }

  describe('rendering', () => {
    it('renders the card wrapper', () => {
      mountComponent()
      cy.get('.bcds-reporting-actions-card').should('exist')
    })

    it('renders the actions container', () => {
      mountComponent()
      cy.get('.bcds-reporting-actions-container').should('exist')
    })

    it('renders the Print button', () => {
      mountComponent()
      cy.contains('button', 'Print').should('exist')
    })

    it('renders the Download button', () => {
      mountComponent()
      cy.contains('button', 'Download').should('exist')
    })
  })

  describe('Download button label', () => {
    it('shows "Download Yield Table" when tabname is MODEL_REPORT', () => {
      mountComponent({ tabname: REPORTING_TAB.MODEL_REPORT })
      cy.contains('button', 'Download Yield Table').should('exist')
    })

    it('shows "Download" when tabname is VIEW_ERR_MSG', () => {
      mountComponent({ tabname: REPORTING_TAB.VIEW_ERR_MSG })
      cy.contains('button', 'Download').should('exist')
      cy.contains('button', 'Download Yield Table').should('not.exist')
    })

    it('shows "Download" when tabname is VIEW_LOG_FILE', () => {
      mountComponent({ tabname: REPORTING_TAB.VIEW_LOG_FILE })
      cy.contains('button', 'Download').should('exist')
      cy.contains('button', 'Download Yield Table').should('not.exist')
    })
  })

  describe('disabled state', () => {
    it('enables both buttons when isButtonDisabled is false', () => {
      mountComponent({ isButtonDisabled: false })
      cy.contains('button', 'Print').should('not.be.disabled')
      cy.contains('button', 'Download Yield Table').should('not.be.disabled')
    })

    it('disables both buttons when isButtonDisabled is true', () => {
      mountComponent({ isButtonDisabled: true })
      cy.contains('button', 'Print').should('be.disabled')
      cy.contains('button', 'Download Yield Table').should('be.disabled')
    })
  })

  describe('Print button', () => {
    it(`emits "${REPORTING_ACTION.PRINT}" when clicked`, () => {
      const onPrintSpy = cy.spy().as('printSpy')
      mountComponent({}, { onPrint: onPrintSpy })
      cy.contains('button', 'Print').click()
      cy.get('@printSpy').should('have.been.calledOnce')
    })

    it('does not emit when disabled', () => {
      const onPrintSpy = cy.spy().as('printSpy')
      mountComponent({ isButtonDisabled: true }, { onPrint: onPrintSpy })
      cy.contains('button', 'Print').click({ force: true })
      cy.get('@printSpy').should('not.have.been.called')
    })
  })

  describe('Download button', () => {
    it(`emits "${REPORTING_ACTION.DOWNLOAD}" when clicked`, () => {
      const onDownloadSpy = cy.spy().as('downloadSpy')
      mountComponent({}, { onDownload: onDownloadSpy })
      cy.contains('button', 'Download Yield Table').click()
      cy.get('@downloadSpy').should('have.been.calledOnce')
    })

    it('does not emit when disabled', () => {
      const onDownloadSpy = cy.spy().as('downloadSpy')
      mountComponent({ isButtonDisabled: true }, { onDownload: onDownloadSpy })
      cy.contains('button', 'Download Yield Table').click({ force: true })
      cy.get('@downloadSpy').should('not.have.been.called')
    })
  })
})
