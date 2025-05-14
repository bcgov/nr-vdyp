/// <reference types="cypress" />
import { setActivePinia, createPinia } from 'pinia'
import { useProjectionStore } from '@/stores/projectionStore'
import { CONSTANTS } from '@/constants'
import JSZip from 'jszip'

describe('Projection Store Unit Tests', () => {
  let store: ReturnType<typeof useProjectionStore>

  beforeEach(() => {
    // Create and activate Pinia instance
    setActivePinia(createPinia())

    store = useProjectionStore()
  })

  it('should initialize with default values', () => {
    expect(store.errorMessages).to.be.empty
    expect(store.logMessages).to.be.empty
    expect(store.yieldTableArray).to.be.empty
  })

  it('should handle ZIP response and populate store state', async () => {
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error log content')
    zip.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log content')
    zip.file(
      CONSTANTS.FILE_NAME.YIELD_TABLE_CSV,
      'Header1,Header2\nValue1,Value2',
    )
    const zipBlob = await zip.generateAsync({ type: 'blob' })

    await store.handleZipResponse(
      zipBlob,
      CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP,
    )

    expect(store.errorMessages.length).to.be.greaterThan(0)
    expect(store.logMessages.length).to.be.greaterThan(0)
    expect(store.yieldTableArray.length).to.be.greaterThan(0)
  })
})
