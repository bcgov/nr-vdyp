/// <reference types="cypress" />

import ReportingContainer from './ReportingContainer.vue'
import { useProjectionStore } from '@/stores/projection/projectionStore'
import { REPORTING_TAB } from '@/constants/constants'
import type { ReportingTab } from '@/types/types'

describe('ReportingContainer.vue', () => {
  type StoreState = {
    txtYieldLines?: string[]
    csvYieldLines?: string[]
    errorMessages?: string[]
    logMessages?: string[]
  }

  const mountComponent = (tabname: ReportingTab, storeState: StoreState = {}) => {
    cy.mount(ReportingContainer, { props: { tabname } })
    cy.then(() => {
      const store = useProjectionStore()
      store.txtYieldLines = storeState.txtYieldLines ?? []
      store.csvYieldLines = storeState.csvYieldLines ?? []
      store.errorMessages = storeState.errorMessages ?? []
      store.logMessages = storeState.logMessages ?? []
    })
  }

  describe('rendering', () => {
    it('renders the container', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT)
      cy.get('.bcds-reporting-container').should('exist')
    })
  })

  describe('output content', () => {
    it('displays txtYieldLines in output for MODEL_REPORT', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        txtYieldLines: ['Yield Report Header', 'Species: Fir'],
        csvYieldLines: ['csv,header,col'],
      })
      cy.get('.ml-2').should('contain.text', 'Yield Report Header')
      cy.get('.ml-2').should('contain.text', 'Species: Fir')
      cy.get('.ml-2').should('not.contain.text', 'csv,header,col')
    })

    it('displays errorMessages in output for VIEW_ERR_MSG', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG, {
        errorMessages: ['ERROR: Missing polygon', 'WARNING: Low density value'],
      })
      cy.get('.ml-2').should('contain.text', 'ERROR: Missing polygon')
      cy.get('.ml-2').should('contain.text', 'WARNING: Low density value')
    })

    it('displays logMessages in output for VIEW_LOG_FILE', () => {
      mountComponent(REPORTING_TAB.VIEW_LOG_FILE, {
        logMessages: ['Batch job started', 'Batch job completed'],
      })
      cy.get('.ml-2').should('contain.text', 'Batch job started')
      cy.get('.ml-2').should('contain.text', 'Batch job completed')
    })

    it('output updates reactively when store data changes after mount', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG, { errorMessages: [] })
      cy.get('.ml-2').should('have.text', '')
      cy.then(() => {
        useProjectionStore().errorMessages = ['Late-arriving error message']
      })
      cy.get('.ml-2').should('contain.text', 'Late-arriving error message')
    })
  })
})
