import { MESSAGE } from '@/constants'
import * as messageHandler from '@/utils/messageHandler'
import { saveAs } from 'file-saver'
import printJS from 'print-js'

/**
 * Download file as text.
 * @param {string[]} data - Array of strings to be saved as a text file.
 * @param {string} fileName - Name of the output file.
 * @param {Function} saveAsFunc - Optional function to handle file saving (defaults to saveAs).
 */
export const downloadTextFile = (
  data: string[],
  fileName: string,
  saveAsFunc = saveAs,
) => {
  if (!data || data.length === 0 || (data.length > 0 && data.every((item) => item.trim() === ''))) {
    messageHandler.logWarningMessage(MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA, null, false, false, MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA_TITLE)
    return
  }

  const content = data.join('\n')
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8;' })
  saveAsFunc(blob, fileName)
}

/**
 * Download file as CSV.
 * @param {string[]} data - Array of strings to be saved as a CSV file.
 * @param {string} fileName - Name of the output file.
 * @param {Function} saveAsFunc - Optional function to handle file saving (defaults to saveAs).
 */
export const downloadCSVFile = (
  data: string[],
  fileName: string,
  saveAsFunc = saveAs,
) => {
  if (!data || data.length === 0 || (data.length > 0 && data.every((item) => item.trim() === ''))) {
    messageHandler.logWarningMessage(MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA, null, false, false, MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA_TITLE)
    return
  }

  const csvContent = data.map((row) => row.split(',').join(',')).join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  saveAsFunc(blob, fileName)
}

/**
 * Print data with specific styles.
 * @param {string[]} data - Array of strings to be printed.
 * @param {Function} printJSFunc - Optional function to handle printing (defaults to printJS).
 */
export const printReport = (data: string[], printJSFunc = printJS) => {
  if (!data || data.length === 0 || (data.length > 0 && data.every((item) => item.trim() === ''))) {
    messageHandler.logWarningMessage(MESSAGE.PRINT_ERR.NO_DATA, null, false, false, MESSAGE.PRINT_ERR.NO_DATA_TITLE)
    return
  }

  // Combine data into a formatted text block
  const content = data.join('\n')

  // Create a container to apply styles and include the content
  const container = document.createElement('div')
  container.style.fontFamily = "'Courier New', Courier, monospace"
  container.style.fontSize = '10px'
  container.style.whiteSpace = 'pre'
  container.style.lineHeight = '1.5'
  container.textContent = content

  // Define print styles
  const printStyles = `
    @page {
      size: A4 landscape;
      margin: 10mm;
    }
    body {
      font-family: 'Courier New', Courier, monospace;
      font-size: 10px;
      white-space: pre;
      line-height: 1.5;
    }
  `

  // Use printJS to print the container content
  printJSFunc({
    printable: container.innerHTML,
    type: 'raw-html',
    style: printStyles,
  })
}
