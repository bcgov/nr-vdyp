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

  /**
   * Mount the component with the given tabname, then reset the shared pinia
   * store and apply any desired overrides. Using cy.then() ensures the store
   * is accessed only after cy.mount() has installed the pinia plugin and made
   * it the active pinia (via pinia.install -> setActivePinia).
   */
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
    it('renders the container element', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT)
      cy.get('.bcds-reporting-container').should('exist')
    })

    it('renders the ReportingActions panel', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT)
      cy.get('.bcds-reporting-actions-card').should('exist')
    })

    it('renders the Print button', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT)
      cy.contains('button', 'Print').should('exist')
    })

    it('renders the ReportingOutput panel', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT)
      cy.get('.ml-2.mr-2').should('exist')
    })
  })

  describe('Download button label', () => {
    it('shows "Download Yield Table" for MODEL_REPORT', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT)
      cy.contains('button', 'Download Yield Table').should('exist')
    })

    it('shows "Download" for VIEW_ERR_MSG', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG)
      cy.contains('button', 'Download').should('exist')
      cy.contains('button', 'Download Yield Table').should('not.exist')
    })

    it('shows "Download" for VIEW_LOG_FILE', () => {
      mountComponent(REPORTING_TAB.VIEW_LOG_FILE)
      cy.contains('button', 'Download').should('exist')
      cy.contains('button', 'Download Yield Table').should('not.exist')
    })
  })

  describe('isButtonDisabled - MODEL_REPORT tab', () => {
    it('disables both buttons when csvYieldLines is empty', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, { csvYieldLines: [] })
      cy.contains('button', 'Print').should('be.disabled')
      cy.contains('button', 'Download Yield Table').should('be.disabled')
    })

    it('enables both buttons when csvYieldLines has data', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        csvYieldLines: ['header,col1', 'row1,val1'],
        txtYieldLines: ['Text report line'],
      })
      cy.contains('button', 'Print').should('not.be.disabled')
      cy.contains('button', 'Download Yield Table').should('not.be.disabled')
    })

    it('remains disabled when only txtYieldLines has data but csvYieldLines is empty', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        txtYieldLines: ['Text report line'],
        csvYieldLines: [],
      })
      cy.contains('button', 'Download Yield Table').should('be.disabled')
    })
  })

  describe('isButtonDisabled - VIEW_ERR_MSG tab', () => {
    it('disables both buttons when errorMessages is empty', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG, { errorMessages: [] })
      cy.contains('button', 'Print').should('be.disabled')
      cy.contains('button', 'Download').should('be.disabled')
    })

    it('enables both buttons when errorMessages has data', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG, {
        errorMessages: ['ERROR: Polygon data invalid'],
      })
      cy.contains('button', 'Print').should('not.be.disabled')
      cy.contains('button', 'Download').should('not.be.disabled')
    })
  })

  describe('isButtonDisabled - VIEW_LOG_FILE tab', () => {
    it('disables both buttons when logMessages is empty', () => {
      mountComponent(REPORTING_TAB.VIEW_LOG_FILE, { logMessages: [] })
      cy.contains('button', 'Print').should('be.disabled')
      cy.contains('button', 'Download').should('be.disabled')
    })

    it('enables both buttons when logMessages has data', () => {
      mountComponent(REPORTING_TAB.VIEW_LOG_FILE, {
        logMessages: ['Processing batch: started'],
      })
      cy.contains('button', 'Print').should('not.be.disabled')
      cy.contains('button', 'Download').should('not.be.disabled')
    })
  })

  describe('output data by tabname', () => {
    it('displays txtYieldLines in output for MODEL_REPORT', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        txtYieldLines: ['Yield Report Header', 'Species: Fir'],
        csvYieldLines: ['csv,header,col'],
      })
      cy.get('.ml-2.mr-2').should('contain.text', 'Yield Report Header')
      cy.get('.ml-2.mr-2').should('contain.text', 'Species: Fir')
    })

    it('does NOT show csvYieldLines in output for MODEL_REPORT (display uses txtYieldLines)', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        txtYieldLines: ['Text report line'],
        csvYieldLines: ['csv,only,data'],
      })
      cy.get('.ml-2.mr-2').should('contain.text', 'Text report line')
      cy.get('.ml-2.mr-2').should('not.contain.text', 'csv,only,data')
    })

    it('displays errorMessages in output for VIEW_ERR_MSG', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG, {
        errorMessages: ['ERROR: Missing polygon', 'WARNING: Low density value'],
      })
      cy.get('.ml-2.mr-2').should('contain.text', 'ERROR: Missing polygon')
      cy.get('.ml-2.mr-2').should('contain.text', 'WARNING: Low density value')
    })

    it('displays logMessages in output for VIEW_LOG_FILE', () => {
      mountComponent(REPORTING_TAB.VIEW_LOG_FILE, {
        logMessages: ['Batch job started', 'Batch job completed'],
      })
      cy.get('.ml-2.mr-2').should('contain.text', 'Batch job started')
      cy.get('.ml-2.mr-2').should('contain.text', 'Batch job completed')
    })

    it('shows empty output when store has no data', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, { txtYieldLines: [] })
      cy.get('.ml-2.mr-2').should('have.text', '')
    })

    it('output updates reactively when store data changes after mount', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG, { errorMessages: [] })
      cy.get('.ml-2.mr-2').should('have.text', '')

      cy.then(() => {
        useProjectionStore().errorMessages = ['Late-arriving error message']
      })
      cy.get('.ml-2.mr-2').should('contain.text', 'Late-arriving error message')
    })
  })

  describe('download button click - with data', () => {
    beforeEach(() => {
      cy.window().then((win) => {
        // Prevent file-saver from triggering an actual browser download
        cy.stub(win.HTMLAnchorElement.prototype, 'click')
      })
    })

    it('clicking Download Yield Table with csvYieldLines data does not throw', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        csvYieldLines: ['header,col1', 'row1,val1'],
        txtYieldLines: ['Text line'],
      })
      cy.contains('button', 'Download Yield Table').click()
    })

    it('clicking Download with errorMessages data does not throw', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG, {
        errorMessages: ['Error: something failed'],
      })
      cy.contains('button', 'Download').click()
    })

    it('clicking Download with logMessages data does not throw', () => {
      mountComponent(REPORTING_TAB.VIEW_LOG_FILE, {
        logMessages: ['Log entry 1'],
      })
      cy.contains('button', 'Download').click()
    })
  })

  describe('print button click - with data', () => {
    // printJS creates <iframe id="printJS"> and calls iframe.contentWindow.print()
    // inside its onload callback - not window.print() directly. To prevent the
    // native print dialog from blocking the test, we use a MutationObserver to
    // detect when printJS appends its iframe to the DOM, then attach a capture-
    // phase load listener that replaces contentWindow.print with a no-op before
    // printJS's own onload fires (capture runs before target/bubble handlers).
    let printObserver: MutationObserver | null = null

    beforeEach(() => {
      printObserver = null
      cy.window().then((win) => {
        printObserver = new win.MutationObserver((mutations) => {
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
                  true, // capture phase - fires before printJS's onload
                )
              }
            }
          }
        })
        printObserver.observe(win.document.body, { childList: true, subtree: true })
      })
    })

    afterEach(() => {
      printObserver?.disconnect()
      printObserver = null
    })

    it('clicking Print with txtYieldLines data does not throw for MODEL_REPORT', () => {
      mountComponent(REPORTING_TAB.MODEL_REPORT, {
        txtYieldLines: ['Yield line 1', 'Yield line 2'],
        csvYieldLines: ['csv,data'],
      })
      cy.contains('button', 'Print').click()
    })

    it('clicking Print with errorMessages data does not throw for VIEW_ERR_MSG', () => {
      mountComponent(REPORTING_TAB.VIEW_ERR_MSG, {
        errorMessages: ['Error entry'],
      })
      cy.contains('button', 'Print').click()
    })

    it('clicking Print with logMessages data does not throw for VIEW_LOG_FILE', () => {
      mountComponent(REPORTING_TAB.VIEW_LOG_FILE, {
        logMessages: ['Log entry'],
      })
      cy.contains('button', 'Print').click()
    })
  })
})
