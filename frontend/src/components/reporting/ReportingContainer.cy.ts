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
          cy.task('log', `Loaded ErrorLog: ${errorLog}`)
          cy.task('log', `Loaded ProgressLog: ${progressLog}`)
          cy.task('log', `Loaded YieldTable: ${yieldTable}`)

          const zip = new JSZip()
          zip.file('ErrorLog.txt', errorLog)
          zip.file('ProgressLog.txt', progressLog)
          zip.file('YieldTable.csv', yieldTable)

          cy.task('log', 'Files in ZIP archive:')
          Object.keys(zip.files).forEach((relativePath) => {
            cy.task('log', `- ${relativePath}`)
          })

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
    cy.mountWithVuetify(ReportingContainer, {
      props: {
        tabname: CONSTANTS.REPORTING_TAB.MODEL_REPORT,
      },
    }).then(() => {
      cy.get('button').contains('Print').should('exist').and('not.be.disabled')

      cy.get('button')
        .contains('Download')
        .should('exist')
        .and('not.be.disabled')

      // Verify data
      cy.get('.ml-2.mr-2')
        .invoke('text')
        .should('include', 'TABLE_NUM,FEATURE_ID,DISTRICT')
        .and('include', '2493719')
    })
  })

  it('displays view log file and verifies UI elements', () => {
    cy.mountWithVuetify(ReportingContainer, {
      props: {
        tabname: CONSTANTS.REPORTING_TAB.VIEW_LOG_FILE,
      },
    }).then(() => {
      cy.get('button').contains('Print').should('exist').and('not.be.disabled')

      cy.get('button')
        .contains('Download')
        .should('exist')
        .and('not.be.disabled')

      //  Verify data
      cy.get('.v-container')
        .find('div')
        .contains("VDYP7 Console version: '7.17d'")
        .should('exist')
    })
  })

  it('displays view error message and verifies UI elements', () => {
    cy.mountWithVuetify(ReportingContainer, {
      props: {
        tabname: CONSTANTS.REPORTING_TAB.VIEW_ERR_MSG,
      },
    }).then(() => {
      cy.get('button').contains('Print').should('exist').and('not.be.disabled')

      cy.get('button')
        .contains('Download')
        .should('exist')
        .and('not.be.disabled')

      // Verify data
      cy.get('.v-container').find('div').contains('- I SUCCESS').should('exist')
    })
  })
})
