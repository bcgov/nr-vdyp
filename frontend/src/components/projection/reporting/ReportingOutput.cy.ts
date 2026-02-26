import ReportingOutput from './ReportingOutput.vue'
import { REPORTING_TAB } from '@/constants/constants'

describe('ReportingOutput.vue', () => {
  const defaultProps = {
    data: ['Line 1', 'Line 2', 'Line 3'],
  }

  const mountComponent = (props: Record<string, unknown> = {}) => {
    return cy.mount(ReportingOutput, {
      props: { ...defaultProps, ...props },
    })
  }

  describe('rendering', () => {
    it('renders the output container', () => {
      mountComponent()
      cy.get('div.ml-2.mr-2').should('exist')
    })

    it('renders joined data lines separated by newlines', () => {
      mountComponent()
      cy.get('div.ml-2.mr-2').should('contain.text', 'Line 1')
      cy.get('div.ml-2.mr-2').should('contain.text', 'Line 2')
      cy.get('div.ml-2.mr-2').should('contain.text', 'Line 3')
    })

    it('renders empty content when data is an empty array', () => {
      mountComponent({ data: [] })
      cy.get('div.ml-2.mr-2').should('have.text', '')
    })

    it('renders a single line when data has one element', () => {
      mountComponent({ data: ['Only line'] })
      cy.get('div.ml-2.mr-2').should('contain.text', 'Only line')
    })
  })

  describe('formattedData', () => {
    it('joins array elements with newline', () => {
      mountComponent({ data: ['A', 'B', 'C'] })
      cy.get('div.ml-2.mr-2').invoke('text').should('eq', 'A\nB\nC')
    })
  })

  describe('styles', () => {
    it('applies base monospace font style', () => {
      mountComponent()
      cy.get('div.ml-2.mr-2').should('have.css', 'font-family').and('include', 'Courier')
    })

    it('applies pre whitespace style', () => {
      mountComponent()
      cy.get('div.ml-2.mr-2').should('have.css', 'white-space', 'pre')
    })

    it('applies fixed height (420px) for non-MODEL_REPORT tabs', () => {
      mountComponent({ tabname: REPORTING_TAB.VIEW_ERR_MSG })
      cy.get('div.ml-2.mr-2').should('have.css', 'height', '420px')
    })

    it('applies fixed height (420px) for VIEW_LOG_FILE tab', () => {
      mountComponent({ tabname: REPORTING_TAB.VIEW_LOG_FILE })
      cy.get('div.ml-2.mr-2').should('have.css', 'height', '420px')
    })

    it('applies minHeight (420px) instead of height for MODEL_REPORT tab', () => {
      mountComponent({ tabname: REPORTING_TAB.MODEL_REPORT })
      cy.get('div.ml-2.mr-2').should('have.css', 'min-height', '420px')
    })

    it('applies fixed height when tabname is not provided', () => {
      mountComponent()
      cy.get('div.ml-2.mr-2').should('have.css', 'height', '420px')
    })
  })
})
