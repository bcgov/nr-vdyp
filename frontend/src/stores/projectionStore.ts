import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as JSZip from 'jszip'
import { messageResult } from '@/utils/messageHandler'
import { FILE_UPLOAD_ERR } from '@/constants/message'

export const useProjectionStore = defineStore('projectionStore', () => {
  const errorMessages = ref<string[]>([])
  const logMessages = ref<string[]>([])
  const yieldTable = ref<string>('')

  const handleZipResponse = async (zipData: Blob) => {
    try {
      const zip = await JSZip.loadAsync(zipData)
      const requiredFiles = {
        error: 'Output_Error.txt',
        log: 'Output_Log.txt',
        yield: 'Output_YldTbl.csv',
      }

      const errorFile = zip.file(requiredFiles.error)
      const logFile = zip.file(requiredFiles.log)
      const yieldFile = zip.file(requiredFiles.yield)

      if (!errorFile || !logFile || !yieldFile) {
        const missingFiles = Object.values(requiredFiles).filter(
          (file) => !zip.file(file),
        )
        messageResult(
          false,
          '',
          `${FILE_UPLOAD_ERR.MISSING_RESPONSED_FILE}: ${missingFiles.join(', ')}`,
        )
        throw new Error(`Missing files: ${missingFiles.join(', ')}`)
      }

      errorMessages.value = (await errorFile.async('string')).split(/\r?\n/)
      logMessages.value = (await logFile.async('string')).split(/\r?\n/)
      yieldTable.value = await yieldFile.async('string')
    } catch (error) {
      console.error('Error processing ZIP file:', error)
      messageResult(false, '', FILE_UPLOAD_ERR.INVALID_RESPONSED_FILE)
      throw error
    }
  }

  return {
    errorMessages,
    logMessages,
    yieldTable,
    handleZipResponse,
  }
})
