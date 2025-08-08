/// <reference types="cypress" />
import { setActivePinia, createPinia } from 'pinia'
import { useProjectionStore } from '@/stores/projectionStore'
import { CONSTANTS } from '@/constants'
import JSZip from 'jszip'
import { useAppStore } from '@/stores/appStore'

describe('Projection Store Unit Tests', () => {
  let store: ReturnType<typeof useProjectionStore>
  let appStore: ReturnType<typeof useAppStore>

  beforeEach(() => {
    // Create and activate Pinia instance
    setActivePinia(createPinia())

    store = useProjectionStore()
    appStore = useAppStore()
  })

  it('should initialize with default values', () => {
    expect(store.errorMessages).to.be.empty
    expect(store.logMessages).to.be.empty
    expect(store.debugMessages).to.be.empty
    expect(store.rawYieldData).to.equal('')
    expect(store.csvYieldLines).to.be.empty
    expect(store.txtYieldLines).to.be.empty
    expect(store.rawResultZipFile).to.be.null
    expect(store.rawResultZipFileName).to.equal('')
  })

  it('should handle ZIP response with INPUT_MODEL_PARAMETERS and populate txtYieldLines', async () => {
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error log content')
    zip.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log content')
    zip.file(CONSTANTS.FILE_NAME.YIELD_TABLE_TXT, 'Line1\n\nLine2\nLine3')
    const zipBlob = await zip.generateAsync({ type: 'blob' })

    await store.handleZipResponse(
      zipBlob,
      CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP,
    )
    expect(store.errorMessages).to.deep.equal(['Error log content'])
    expect(store.logMessages).to.deep.equal(['Log content'])
    expect(store.rawYieldData).to.equal('Line1\n\nLine2\nLine3')
    expect(store.txtYieldLines).to.deep.equal(['Line1', '', 'Line2', 'Line3'])
    expect(store.csvYieldLines).to.be.empty
    expect(store.rawResultZipFile).to.equal(zipBlob)
    expect(store.rawResultZipFileName).to.equal(
      CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP,
    )
  })

  it('should handle ZIP response with FILE_UPLOAD and populate csvYieldLines', async () => {
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.FILE_UPLOAD
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error log content')
    zip.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log content')
    zip.file(
      CONSTANTS.FILE_NAME.YIELD_TABLE_CSV,
      'Header1,Header2\nValue1,Value2\n\nValue3,Value4',
    )
    const zipBlob = await zip.generateAsync({ type: 'blob' })

    await store.handleZipResponse(
      zipBlob,
      CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP,
    )

    expect(store.errorMessages).to.deep.equal(['Error log content'])
    expect(store.logMessages).to.deep.equal(['Log content'])
    expect(store.rawYieldData).to.equal(
      'Header1,Header2\nValue1,Value2\n\nValue3,Value4',
    )
    expect(store.csvYieldLines).to.deep.equal([
      'Header1,Header2',
      'Value1,Value2',
      'Value3,Value4',
    ])
    expect(store.txtYieldLines).to.be.empty
    expect(store.rawResultZipFile).to.equal(zipBlob)
    expect(store.rawResultZipFileName).to.equal(
      CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP,
    )
  })

  it('should handle missing files in ZIP', async () => {
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error log content')
    // Missing LOG_TXT and YIELD_TABLE_CSV/TXT
    const zipBlob = await zip.generateAsync({ type: 'blob' })

    try {
      await store.handleZipResponse(zipBlob, 'test.zip')
      expect.fail('Expected handleZipResponse to throw an error')
    } catch (error) {
      const typedError = error as Error
      expect(typedError.message).to.include('Missing files')
    }
  })

  it('should handle empty files in ZIP', async () => {
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.FILE_UPLOAD
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, '')
    zip.file(CONSTANTS.FILE_NAME.LOG_TXT, '')
    zip.file(CONSTANTS.FILE_NAME.YIELD_TABLE_CSV, '')
    const zipBlob = await zip.generateAsync({ type: 'blob' })

    await store.handleZipResponse(zipBlob, 'empty.zip')

    expect(store.errorMessages).to.deep.equal([''])
    expect(store.logMessages).to.deep.equal([''])
    expect(store.csvYieldLines).to.be.empty
    expect(store.txtYieldLines).to.be.empty
  })

  it('should handle errors during ZIP processing', async () => {
    const invalidBlob = new Blob(['invalid zip content'], {
      type: 'application/zip',
    })

    try {
      await store.handleZipResponse(invalidBlob, 'invalid.zip')
      expect.fail('Expected handleZipResponse to throw an error')
    } catch (error) {
      const typedError = error as Error
      expect(typedError.message).to.include(
        "Can't find end of central directory",
      )
    }
  })

  it('should reset state correctly on multiple ZIP uploads with different model selections', async () => {
    // First ZIP with INPUT_MODEL_PARAMETERS
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
    const zip1 = new JSZip()
    zip1.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error1')
    zip1.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log1')
    zip1.file(CONSTANTS.FILE_NAME.YIELD_TABLE_TXT, 'Line1\nLine2')
    const zipBlob1 = await zip1.generateAsync({ type: 'blob' })

    await store.handleZipResponse(zipBlob1, 'first.zip')

    expect(store.errorMessages).to.deep.equal(['Error1'])
    expect(store.logMessages).to.deep.equal(['Log1'])
    expect(store.txtYieldLines).to.deep.equal(['Line1', 'Line2'])
    expect(store.csvYieldLines).to.be.empty

    // Second ZIP with FILE_UPLOAD
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.FILE_UPLOAD
    const zip2 = new JSZip()
    zip2.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error2')
    zip2.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log2')
    zip2.file(CONSTANTS.FILE_NAME.YIELD_TABLE_CSV, 'Header\nValue1\nValue2')
    const zipBlob2 = await zip2.generateAsync({ type: 'blob' })

    await store.handleZipResponse(zipBlob2, 'second.zip')

    expect(store.errorMessages).to.deep.equal(['Error2'])
    expect(store.logMessages).to.deep.equal(['Log2'])
    expect(store.csvYieldLines).to.deep.equal(['Header', 'Value1', 'Value2'])
    expect(store.txtYieldLines).to.be.empty
  })
})
