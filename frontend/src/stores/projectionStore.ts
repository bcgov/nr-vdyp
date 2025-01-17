import { defineStore } from 'pinia'
import { ref } from 'vue'
import JSZip from 'jszip'
import { messageResult } from '@/utils/messageHandler'
import { FILE_UPLOAD_ERR } from '@/constants/message'
import { FILE_NAME } from '@/constants/constants'

export const useProjectionStore = defineStore('projectionStore', () => {
  const errorMessages = ref<string[]>([])
  const logMessages = ref<string[]>([])
  const yieldTable = ref<string>('') // raw CSV
  const yieldTableArray = ref<string[]>([]) // Array of CSV lines

  const handleZipResponse = async (zipData: Blob) => {
    try {
      const zip = await JSZip.loadAsync(zipData)

      // Print all file names in the ZIP file
      console.log('Files in ZIP archive:')
      for (const relativePath of Object.keys(zip.files)) {
        console.log(`- ${relativePath}`)
      }

      const requiredFiles = {
        error: FILE_NAME.ERROR_TXT,
        log: FILE_NAME.LOG_TXT,
        yield: FILE_NAME.YIELD_TABLE_CSV,
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

      if (yieldTable.value) {
        yieldTableArray.value = yieldTable.value
          .split(/\r?\n/)
          .filter((line) => line.trim() !== '') // Remove blank lines
      } else {
        yieldTableArray.value = []
      }
    } catch (error) {
      console.error('Error processing ZIP file:', error)
      messageResult(false, '', FILE_UPLOAD_ERR.INVALID_RESPONSED_FILE)
      throw error
    }
  }

  const loadSampleData = async () => {
    try {
      const filePaths = [
        '/test-data/ErrorLog.txt',
        '/test-data/ProgressLog.txt',
        '/test-data/YieldTable.csv',
      ]

      const zip = new JSZip()

      for (const filePath of filePaths) {
        const response = await fetch(filePath)
        if (!response.ok) {
          throw new Error(`Failed to load file: ${filePath}`)
        }
        const fileContent = await response.text()
        const fileName = filePath.split('/').pop() // Extract file name
        if (fileName) {
          zip.file(fileName, fileContent)
        }
      }

      // Generate the ZIP file
      const zipBlob = await zip.generateAsync({ type: 'blob' })

      // Handle the ZIP file
      await handleZipResponse(zipBlob)
    } catch (error) {
      console.error('Error creating ZIP file:', error)
    }
  }

  return {
    errorMessages,
    logMessages,
    yieldTable,
    yieldTableArray,
    handleZipResponse,
    loadSampleData,
  }
})
