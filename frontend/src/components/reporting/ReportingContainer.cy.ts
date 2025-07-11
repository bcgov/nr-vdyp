import ReportingContainer from './ReportingContainer.vue'
import { createPinia, setActivePinia } from 'pinia'
import { useProjectionStore } from '@/stores/projectionStore'
import { CONSTANTS } from '@/constants'
import JSZip from 'jszip'

describe('ReportingContainer.vue', () => {
  let projectionStore: ReturnType<typeof useProjectionStore>

  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)
    projectionStore = useProjectionStore()

    cy.fixture('ErrorLog.txt').then((errorLog) => {
      cy.fixture('ProgressLog.txt').then((progressLog) => {
        cy.fixture('YieldTable.csv').then((yieldTable) => {
          const zip = new JSZip()
          zip.file('ErrorLog.txt', errorLog)
          zip.file('ProgressLog.txt', progressLog)
          zip.file('YieldTable.csv', yieldTable)
          return cy
            .wrap(
              Promise.resolve(
                zip.generateAsync({ type: 'blob' }) as Promise<Blob>,
              ),
            )
            .then((zipBlob) => {
              projectionStore.handleZipResponse(
                zipBlob as Blob,
                CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP,
              )
            })
        })
      })
    })
  })

  it('displays model report and verifies UI elements', () => {
    cy.mount(ReportingContainer, {
      props: {
        tabname: CONSTANTS.REPORTING_TAB.MODEL_REPORT,
      },
    }).then(() => {
      cy.get('button').contains('Print').should('exist').and('not.be.disabled')

      cy.get('button')
        .contains('Download')
        .should('exist')
        .and('not.be.disabled')
    })
  })

  it('displays view log file and verifies UI elements', () => {
    cy.mount(ReportingContainer, {
      props: {
        tabname: CONSTANTS.REPORTING_TAB.VIEW_LOG_FILE,
      },
    }).then(() => {
      cy.get('button').contains('Print').should('exist').and('not.be.disabled')

      cy.get('button')
        .contains('Download')
        .should('exist')
        .and('not.be.disabled')
    })
  })

  it('displays view error message and verifies UI elements', () => {
    cy.mount(ReportingContainer, {
      props: {
        tabname: CONSTANTS.REPORTING_TAB.VIEW_ERR_MSG,
      },
    }).then(() => {
      cy.get('button').contains('Print').should('exist').and('not.be.disabled')

      cy.get('button')
        .contains('Download')
        .should('exist')
        .and('not.be.disabled')
    })
  })
})
