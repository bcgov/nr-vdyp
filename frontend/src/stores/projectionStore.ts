import { defineStore } from 'pinia'
import { ref } from 'vue'
import JSZip from 'jszip'
import { messageResult } from '@/utils/messageHandler'
import { CONSTANTS, MESSAGE } from '@/constants'

export const useProjectionStore = defineStore('projectionStore', () => {
  const errorMessages = ref<string[]>([])
  const logMessages = ref<string[]>([])
  const debugMessages = ref<string[]>([])
  const yieldTable = ref<string>('') // raw CSV
  const yieldTableArray = ref<string[]>([]) // Array of CSV lines
  const rawResultZipFile = ref<Blob | null>(null) // zip file
  const rawResultZipFileName = ref<string>('')

  const handleZipResponse = async (zipFile: Blob, zipFileName: string) => {
    try {
      rawResultZipFile.value = zipFile
      rawResultZipFileName.value = zipFileName

      const zip = await JSZip.loadAsync(zipFile)

      // Print all file names in the ZIP file
      console.log('Files in ZIP archive:')
      for (const relativePath of Object.keys(zip.files)) {
        console.log(`- ${relativePath}`)
      }

      const requiredFiles = {
        error: CONSTANTS.FILE_NAME.ERROR_TXT,
        log: CONSTANTS.FILE_NAME.LOG_TXT,
        yield: CONSTANTS.FILE_NAME.YIELD_TABLE_CSV,
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
          `${MESSAGE.FILE_UPLOAD_ERR.MISSING_RESPONSED_FILE}: ${missingFiles.join(', ')}`,
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

      // Optional
      const debugFile = zip.file(CONSTANTS.FILE_NAME.DEBUG_TXT)
      if (debugFile) {
        debugMessages.value = (await debugFile.async('string')).split(/\r?\n/)
      }
    } catch (error) {
      console.error('Error processing ZIP file:', error)
      messageResult(false, '', MESSAGE.FILE_UPLOAD_ERR.INVALID_RESPONSED_FILE)
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
      await handleZipResponse(
        zipBlob,
        CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP,
      )
    } catch (error) {
      console.error('Error creating ZIP file:', error)
    }
  }

  const loadSampleRawData = async (): Promise<Blob> => {
    try {
      const filePath = '/test-data/test-data.zip'
      const response = await fetch(filePath)
      if (!response.ok) {
        throw new Error(`Failed to load file: ${filePath}`)
      }
      const zipBlob = await response.blob()

      return zipBlob
    } catch (error) {
      console.error('Error loading raw ZIP file:', error)
      throw error
    }
  }

  return {
    errorMessages,
    logMessages,
    yieldTable,
    yieldTableArray,
    rawResultZipFile,
    rawResultZipFileName,
    handleZipResponse,
    loadSampleData,
    loadSampleRawData,
  }
})
