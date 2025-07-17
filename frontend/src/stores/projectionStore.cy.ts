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
    expect(store.debugMessages).to.be.empty
    expect(store.yieldTable).to.equal('')
    expect(store.yieldTableArray).to.be.empty
    expect(store.rawResultZipFile).to.be.null
    expect(store.rawResultZipFileName).to.equal('')
  })

  it('should handle ZIP response and populate store state', async () => {
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error log content')
    zip.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log content')
    zip.file(
      CONSTANTS.FILE_NAME.YIELD_TABLE_CSV,
      'Header1,Header2\nValue1,Value2',
    )
    zip.file(CONSTANTS.FILE_NAME.DEBUG_TXT, 'Debug content')
    const zipBlob = await zip.generateAsync({ type: 'blob' })

    await store.handleZipResponse(
      zipBlob,
      CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP,
    )

    expect(store.errorMessages).to.deep.equal(['Error log content'])
    expect(store.logMessages).to.deep.equal(['Log content'])
    expect(store.debugMessages).to.deep.equal(['Debug content'])
    expect(store.yieldTable).to.equal('Header1,Header2\nValue1,Value2')
    expect(store.yieldTableArray).to.deep.equal([
      'Header1,Header2',
      'Value1,Value2',
    ])
    expect(store.rawResultZipFile).to.equal(zipBlob)
    expect(store.rawResultZipFileName).to.equal(
      CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP,
    )
  })

  it('should handle missing files in ZIP', async () => {
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error log content')
    // Missing LOG_TXT and YIELD_TABLE_CSV
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
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, '')
    zip.file(CONSTANTS.FILE_NAME.LOG_TXT, '')
    zip.file(CONSTANTS.FILE_NAME.YIELD_TABLE_CSV, '')
    const zipBlob = await zip.generateAsync({ type: 'blob' })

    await store.handleZipResponse(zipBlob, 'empty.zip')

    expect(store.errorMessages).to.deep.equal([''])
    expect(store.logMessages).to.deep.equal([''])
    expect(store.yieldTableArray).to.be.empty // Empty lines are filtered out
  })

  it('should handle malformed CSV in ZIP', async () => {
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error log content')
    zip.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log content')
    zip.file(CONSTANTS.FILE_NAME.YIELD_TABLE_CSV, 'Malformed,CSV,content')
    const zipBlob = await zip.generateAsync({ type: 'blob' })

    await store.handleZipResponse(zipBlob, 'malformed.zip')

    expect(store.yieldTableArray).to.deep.equal(['Malformed,CSV,content'])
  })

  it('should ignore additional files in ZIP', async () => {
    const zip = new JSZip()
    zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error log content')
    zip.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log content')
    zip.file(
      CONSTANTS.FILE_NAME.YIELD_TABLE_CSV,
      'Header1,Header2\nValue1,Value2',
    )
    zip.file('extra.txt', 'Extra content')
    const zipBlob = await zip.generateAsync({ type: 'blob' })

    await store.handleZipResponse(zipBlob, 'extra.zip')

    expect(store.errorMessages).to.deep.equal(['Error log content'])
    expect(store.logMessages).to.deep.equal(['Log content'])
    expect(store.yieldTableArray).to.deep.equal([
      'Header1,Header2',
      'Value1,Value2',
    ])
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

  it('should reset state correctly on multiple ZIP uploads', async () => {
    const zip1 = new JSZip()
    zip1.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error1')
    zip1.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log1')
    zip1.file(CONSTANTS.FILE_NAME.YIELD_TABLE_CSV, 'Yield1')
    const zipBlob1 = await zip1.generateAsync({ type: 'blob' })

    await store.handleZipResponse(zipBlob1, 'first.zip')

    expect(store.errorMessages).to.deep.equal(['Error1'])
    expect(store.logMessages).to.deep.equal(['Log1'])
    expect(store.yieldTableArray).to.deep.equal(['Yield1'])

    const zip2 = new JSZip()
    zip2.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error2')
    zip2.file(CONSTANTS.FILE_NAME.LOG_TXT, 'Log2')
    zip2.file(CONSTANTS.FILE_NAME.YIELD_TABLE_CSV, 'Yield2')
    const zipBlob2 = await zip2.generateAsync({ type: 'blob' })

    await store.handleZipResponse(zipBlob2, 'second.zip')

    expect(store.errorMessages).to.deep.equal(['Error2'])
    expect(store.logMessages).to.deep.equal(['Log2'])
    expect(store.yieldTableArray).to.deep.equal(['Yield2'])
  })
})
