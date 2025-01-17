import ReportingContainer from '@/components/reporting/ReportingContainer.vue'
import { createPinia, setActivePinia } from 'pinia'
import { useProjectionStore } from '@/stores/projectionStore'
import { REPORTING_TAB } from '@/constants/constants'
import JSZip from 'jszip'

describe('ReportingContainer.vue', () => {
  let projectionStore: ReturnType<typeof useProjectionStore>

  beforeEach(() => {
    cy.viewport(1024, 768)

    const pinia = createPinia()
    setActivePinia(pinia)
    projectionStore = useProjectionStore()

    const loadAndProcessZip = async () => {
      try {
        const [errorLog, progressLog, yieldTable] = await Promise.all([
          cy.fixture('ErrorLog.txt'),
          cy.fixture('ProgressLog.txt'),
          cy.fixture('YieldTable.csv'),
        ])

        const zip = new JSZip()

        console.log('Loaded ErrorLog:', errorLog)
        console.log('Loaded ProgressLog:', progressLog)
        console.log('Loaded YieldTable:', yieldTable)

        // Add files to ZIP
        zip.file('ErrorLog.txt', errorLog)
        zip.file('ProgressLog.txt', progressLog)
        zip.file('YieldTable.csv', yieldTable)

        // Print all file names in the ZIP file
        console.log('Files in ZIP archive:')
        for (const relativePath of Object.keys(zip.files)) {
          console.log(`- ${relativePath}`)
        }

        // Generate ZIP and process it
        return zip.generateAsync({ type: 'blob' }).then((zipBlob) => {
          return projectionStore.handleZipResponse(zipBlob)
        })
      } catch (error) {
        console.error('Error loading fixtures or processing ZIP:', error)
      }
    }

    loadAndProcessZip()
  })

  it('displays model report and verifies UI elements', () => {
    cy.mountWithVuetify(ReportingContainer, {
      props: {
        tabname: REPORTING_TAB.MODEL_REPORT,
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
        tabname: REPORTING_TAB.VIEW_LOG_FILE,
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
        tabname: REPORTING_TAB.VIEW_ERR_MSG,
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
