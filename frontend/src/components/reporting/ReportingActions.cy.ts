import ReportingActions from './ReportingActions.vue'
import { REPORTING_TAB } from '@/constants/constants'

describe('<ReportingActions />', () => {
  const defaultProps = {
    isButtonDisabled: false,
    isRawResultsButtonDisabled: false,
    tabname: REPORTING_TAB.VIEW_LOG_FILE,
  }

  it('renders with default props and buttons are enabled', () => {
    cy.mount(ReportingActions, { props: defaultProps })

    // Check if the component is rendered
    cy.get('.v-card').should('exist')

    // Check if both buttons are visible and enabled
    cy.get('button')
      .contains('Print')
      .should('be.visible')
      .and('not.be.disabled')

    cy.get('button')
      .contains('Download')
      .should('be.visible')
      .and('not.be.disabled')
  })

  it('disables both buttons when isButtonDisabled is true', () => {
    cy.mount(ReportingActions, {
      props: { ...defaultProps, isButtonDisabled: true },
    })

    // Check if both buttons are disabled
    cy.contains('button', 'Print').should('be.disabled')

    cy.contains('button', 'Download').should('be.disabled')
  })

  it('emits events correctly when buttons are clicked', () => {
    const printSpy = cy.spy().as('printSpy')
    const downloadSpy = cy.spy().as('downloadSpy')

    cy.mount(ReportingActions, {
      props: {
        ...defaultProps,
        onPrint: printSpy,
        onDownload: downloadSpy,
      },
    })

    // Click the Print button and check emitted event
    cy.get('button').contains('Print').click()
    cy.get('@printSpy').should('have.been.calledOnce')

    // Click the Download button and check emitted event
    cy.get('button').contains('Download').click()
    cy.get('@downloadSpy').should('have.been.calledOnce')
  })
})
