/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import JSZip from 'jszip'
import { useProjectionStore } from '@/stores/projection/projectionStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'

const makeValidZip = async (overrides: Record<string, string> = {}): Promise<Blob> => {
  const zip = new JSZip()
  const defaults: Record<string, string> = {
    [CONSTANTS.FILE_NAME.ERROR_TXT]: 'error line 1\nerror line 2',
    [CONSTANTS.FILE_NAME.LOG_TXT]: 'log line 1\nlog line 2',
    [CONSTANTS.FILE_NAME.YIELD_TABLE_CSV]: 'col1,col2\nval1,val2',
  }
  const files = { ...defaults, ...overrides }
  for (const [name, content] of Object.entries(files)) {
    zip.file(name, content)
  }
  return zip.generateAsync({ type: 'blob' })
}

describe('Projection Store Unit Tests', () => {
  let store: ReturnType<typeof useProjectionStore>
  let appStore: ReturnType<typeof useAppStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useProjectionStore()
    appStore = useAppStore()
  })

  describe('Initial State', () => {
    it('should initialize errorMessages as empty array', () => {
      expect(store.errorMessages).to.deep.equal([])
    })

    it('should initialize logMessages as empty array', () => {
      expect(store.logMessages).to.deep.equal([])
    })

    it('should initialize debugMessages as empty array', () => {
      expect(store.debugMessages).to.deep.equal([])
    })

    it('should initialize rawYieldData as empty string', () => {
      expect(store.rawYieldData).to.equal('')
    })

    it('should initialize csvYieldLines as empty array', () => {
      expect(store.csvYieldLines).to.deep.equal([])
    })

    it('should initialize txtYieldLines as empty array', () => {
      expect(store.txtYieldLines).to.deep.equal([])
    })

    it('should initialize rawResultZipFile as null', () => {
      expect(store.rawResultZipFile).to.be.null
    })

    it('should initialize rawResultZipFileName as empty string', () => {
      expect(store.rawResultZipFileName).to.equal('')
    })
  })

  describe('handleZipResponse - valid ZIP', () => {
    it('should populate errorMessages from ErrorLog.txt', async () => {
      const blob = await makeValidZip({ [CONSTANTS.FILE_NAME.ERROR_TXT]: 'err1\nerr2' })
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.errorMessages).to.deep.equal(['err1', 'err2'])
    })

    it('should populate logMessages from ProgressLog.txt', async () => {
      const blob = await makeValidZip({ [CONSTANTS.FILE_NAME.LOG_TXT]: 'log1\nlog2\nlog3' })
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.logMessages).to.deep.equal(['log1', 'log2', 'log3'])
    })

    it('should populate rawYieldData from YieldTable.csv', async () => {
      const csv = 'h1,h2\nv1,v2'
      const blob = await makeValidZip({ [CONSTANTS.FILE_NAME.YIELD_TABLE_CSV]: csv })
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.rawYieldData).to.equal(csv)
    })

    it('should populate csvYieldLines and exclude blank lines', async () => {
      const blob = await makeValidZip({
        [CONSTANTS.FILE_NAME.YIELD_TABLE_CSV]: 'h1,h2\nv1,v2\n\nv3,v4\n',
      })
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.csvYieldLines).to.deep.equal(['h1,h2', 'v1,v2', 'v3,v4'])
    })

    it('should store rawResultZipFile as a Blob', async () => {
      const blob = await makeValidZip()
      await store.handleZipResponse(blob, 'result.zip')
      expect(store.rawResultZipFile).to.be.instanceOf(Blob)
    })

    it('should store rawResultZipFileName', async () => {
      const blob = await makeValidZip()
      await store.handleZipResponse(blob, 'my-output.zip')
      expect(store.rawResultZipFileName).to.equal('my-output.zip')
    })
  })

  describe('handleZipResponse - resets state before processing', () => {
    it('should replace errorMessages on subsequent calls', async () => {
      const blob1 = await makeValidZip({ [CONSTANTS.FILE_NAME.ERROR_TXT]: 'old error' })
      await store.handleZipResponse(blob1, 'first.zip')
      expect(store.errorMessages).to.deep.equal(['old error'])

      const blob2 = await makeValidZip({ [CONSTANTS.FILE_NAME.ERROR_TXT]: 'new error' })
      await store.handleZipResponse(blob2, 'second.zip')
      expect(store.errorMessages).to.deep.equal(['new error'])
    })

    it('should replace csvYieldLines on subsequent calls', async () => {
      const blob1 = await makeValidZip({ [CONSTANTS.FILE_NAME.YIELD_TABLE_CSV]: 'old,data' })
      await store.handleZipResponse(blob1, 'first.zip')

      const blob2 = await makeValidZip({ [CONSTANTS.FILE_NAME.YIELD_TABLE_CSV]: 'new,data' })
      await store.handleZipResponse(blob2, 'second.zip')
      expect(store.csvYieldLines).to.deep.equal(['new,data'])
    })

    it('should clear debugMessages when second ZIP has no DebugLog.txt', async () => {
      const blob1 = await makeValidZip({ [CONSTANTS.FILE_NAME.DEBUG_TXT]: 'debug line' })
      await store.handleZipResponse(blob1, 'first.zip')
      expect(store.debugMessages).to.have.length.greaterThan(0)

      const blob2 = await makeValidZip()
      await store.handleZipResponse(blob2, 'second.zip')
      expect(store.debugMessages).to.deep.equal([])
    })
  })

  describe('handleZipResponse - optional DebugLog.txt', () => {
    it('should populate debugMessages when DebugLog.txt is present', async () => {
      const blob = await makeValidZip({
        [CONSTANTS.FILE_NAME.DEBUG_TXT]: 'debug line 1\ndebug line 2',
      })
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.debugMessages).to.deep.equal(['debug line 1', 'debug line 2'])
    })

    it('should leave debugMessages empty when DebugLog.txt is absent', async () => {
      const blob = await makeValidZip()
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.debugMessages).to.deep.equal([])
    })
  })

  describe('handleZipResponse - YieldTable.txt (INPUT_MODEL_PARAMETERS only)', () => {
    it('should populate txtYieldLines when modelSelection is INPUT_MODEL_PARAMETERS', async () => {
      appStore.setModelSelection(CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS)
      const blob = await makeValidZip({
        [CONSTANTS.FILE_NAME.YIELD_TABLE_TXT]: 'txt line 1\ntxt line 2',
      })
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.txtYieldLines).to.deep.equal(['txt line 1', 'txt line 2'])
    })

    it('should NOT populate txtYieldLines when modelSelection is FILE_UPLOAD', async () => {
      appStore.setModelSelection(CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)
      const blob = await makeValidZip({
        [CONSTANTS.FILE_NAME.YIELD_TABLE_TXT]: 'should be ignored',
      })
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.txtYieldLines).to.deep.equal([])
    })

    it('should leave txtYieldLines empty when YieldTable.txt is absent (INPUT_MODEL_PARAMETERS)', async () => {
      appStore.setModelSelection(CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS)
      const blob = await makeValidZip()
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.txtYieldLines).to.deep.equal([])
    })
  })

  describe('handleZipResponse - missing required files', () => {
    const makeZipWithout = async (missingFile: string): Promise<Blob> => {
      const zip = new JSZip()
      const required: Record<string, string> = {
        [CONSTANTS.FILE_NAME.ERROR_TXT]: 'err',
        [CONSTANTS.FILE_NAME.LOG_TXT]: 'log',
        [CONSTANTS.FILE_NAME.YIELD_TABLE_CSV]: 'csv',
      }
      for (const [name, content] of Object.entries(required)) {
        if (name !== missingFile) zip.file(name, content)
      }
      return zip.generateAsync({ type: 'blob' })
    }

    it('should throw when ErrorLog.txt is missing', async () => {
      const blob = await makeZipWithout(CONSTANTS.FILE_NAME.ERROR_TXT)
      let threw = false
      try {
        await store.handleZipResponse(blob, 'output.zip')
      } catch {
        threw = true
      }
      expect(threw).to.be.true
    })

    it('should throw when ProgressLog.txt is missing', async () => {
      const blob = await makeZipWithout(CONSTANTS.FILE_NAME.LOG_TXT)
      let threw = false
      try {
        await store.handleZipResponse(blob, 'output.zip')
      } catch {
        threw = true
      }
      expect(threw).to.be.true
    })

    it('should throw when YieldTable.csv is missing', async () => {
      const blob = await makeZipWithout(CONSTANTS.FILE_NAME.YIELD_TABLE_CSV)
      let threw = false
      try {
        await store.handleZipResponse(blob, 'output.zip')
      } catch {
        threw = true
      }
      expect(threw).to.be.true
    })

    it('should throw when ZIP is completely empty', async () => {
      const zip = new JSZip()
      const blob = await zip.generateAsync({ type: 'blob' })
      let threw = false
      try {
        await store.handleZipResponse(blob, 'empty.zip')
      } catch {
        threw = true
      }
      expect(threw).to.be.true
    })
  })

  describe('handleZipResponse - CRLF line endings', () => {
    it('should split errorMessages correctly with CRLF', async () => {
      const blob = await makeValidZip({
        [CONSTANTS.FILE_NAME.ERROR_TXT]: 'err1\r\nerr2',
      })
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.errorMessages).to.deep.equal(['err1', 'err2'])
    })

    it('should split csvYieldLines correctly with CRLF and exclude blank lines', async () => {
      const blob = await makeValidZip({
        [CONSTANTS.FILE_NAME.YIELD_TABLE_CSV]: 'h1,h2\r\nv1,v2\r\n\r\nv3,v4\r\n',
      })
      await store.handleZipResponse(blob, 'output.zip')
      expect(store.csvYieldLines).to.deep.equal(['h1,h2', 'v1,v2', 'v3,v4'])
    })
  })
})
