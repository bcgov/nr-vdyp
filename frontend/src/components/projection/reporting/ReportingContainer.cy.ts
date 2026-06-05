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
    rawResultZipFile?: Blob | null
    rawResultZipFileName?: string
  }

  const mountComponent = (tabname: ReportingTab, storeState: StoreState = {}) => {
    cy.mount(ReportingContainer, { props: { tabname } })
    cy.then(() => {
      const store = useProjectionStore()
      store.txtYieldLines = storeState.txtYieldLines ?? []
      store.csvYieldLines = storeState.csvYieldLines ?? []
      store.errorMessages = storeState.errorMessages ?? []
      store.logMessages = storeState.logMessages ?? []
      store.rawResultZipFile = storeState.rawResultZipFile ?? null
      store.rawResultZipFileName = storeState.rawResultZipFileName ?? ''
    })
  }

  describe('rendering', () => {
    it('renders the container and actions panel', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT)
      cy.get('.bcds-reporting-container').should('exist')
      cy.get('.bcds-reporting-actions-card').should('exist')
    })

    it('renders Print and Download buttons', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT)
      cy.contains('button', 'Print').should('exist')
      cy.contains('button', 'Download Yield Table').should('exist')
    })
  })

  describe('Download button label', () => {
    it('shows "Download Yield Table" for MODEL_REPORT', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT)
      cy.contains('button', 'Download Yield Table').should('exist')
    })

    it('shows "Download" for non-MODEL tabs', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG)
      cy.contains('button', 'Download').should('exist')
      cy.contains('button', 'Download Yield Table').should('not.exist')
    })
  })

  describe('button enabled/disabled state', () => {
    it('disables both buttons when MODEL_REPORT csvYieldLines is empty', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, { csvYieldLines: [] })
      cy.contains('button', 'Print').should('be.disabled')
      cy.contains('button', 'Download Yield Table').should('be.disabled')
    })

    it('enables both buttons when MODEL_REPORT has data', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        csvYieldLines: ['header,col1', 'row1,val1'],
        txtYieldLines: ['Text report line'],
      })
      cy.contains('button', 'Print').should('not.be.disabled')
      cy.contains('button', 'Download Yield Table').should('not.be.disabled')
    })

    it('keeps Download disabled when only txtYieldLines has data but csvYieldLines is empty', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        txtYieldLines: ['Text report line'],
        csvYieldLines: [],
      })
      cy.contains('button', 'Download Yield Table').should('be.disabled')
    })

    it('enables both buttons when VIEW_ERR_MSG has errorMessages data', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG, { errorMessages: ['ERROR: Polygon data invalid'] })
      cy.contains('button', 'Print').should('not.be.disabled')
      cy.contains('button', 'Download').should('not.be.disabled')
    })

    it('enables both buttons when VIEW_LOG_FILE has logMessages data', () => {
      mountComponent(REPORTING_TAB.VIEW_LOG_FILE, { logMessages: ['Processing batch: started'] })
      cy.contains('button', 'Print').should('not.be.disabled')
      cy.contains('button', 'Download').should('not.be.disabled')
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

  describe('button click actions', () => {
    beforeEach(() => {
      cy.window().then((win) => {
        cy.stub(win.HTMLAnchorElement.prototype, 'click')
      })
    })

    it('clicking Download Yield Table with data does not throw', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        csvYieldLines: ['header,col1', 'row1,val1'],
        txtYieldLines: ['Text line'],
      })
      cy.contains('button', 'Download Yield Table').click()
    })

    it('clicking Print with data does not throw', () => {
      // printJS appends an <iframe id="printJS"> and calls iframe.contentWindow.print().
      // Intercept via MutationObserver before the iframe's onload fires.
      cy.window().then((win) => {
        const observer = new win.MutationObserver((mutations) => {
          for (const mutation of mutations) {
            for (const node of Array.from(mutation.addedNodes)) {
              const el = node as HTMLElement
              if (el.id === 'printJS') {
                el.addEventListener(
                  'load',
                  () => {
                    try {
                      const cw = (el as HTMLIFrameElement).contentWindow as any
                      cw.print = () => {}
                      cw.focus = () => {}
                    } catch {
                      // ignore cross-origin errors
                    }
                  },
                  true,
                )
              }
            }
          }
        })
        observer.observe(win.document.body, { childList: true, subtree: true })
      })

      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        txtYieldLines: ['Yield line 1'],
        csvYieldLines: ['csv,data'],
      })
      cy.contains('button', 'Print').click()
    })
  })
})
