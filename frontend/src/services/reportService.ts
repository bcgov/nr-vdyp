import { saveAs } from 'file-saver'
import printJS from 'print-js'
import * as messageHandler from '@/utils/messageHandler'
import { PRINT_ERR, FILE_DOWNLOAD_ERR } from '@/constants/message'
/**
 * Download file as text.
 * @param {string[]} data - Array of strings to be saved as a text file.
 * @param {string} fileName - Name of the output file.
 */
export const downloadTextFile = (data: string[], fileName: string) => {
  if (!data || data.length === 0) {
    messageHandler.logErrorMessage(FILE_DOWNLOAD_ERR.NO_DATA)
    return
  }

  const content = data.join('\n')
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8;' })
  saveAs(blob, fileName)
}

/**
 * Download file as CSV.
 * @param {string[]} data - Array of strings to be saved as a CSV file.
 * @param {string} fileName - Name of the output file.
 */
export const downloadCSVFile = (data: string[], fileName: string) => {
  if (!data || data.length === 0) {
    messageHandler.logErrorMessage(FILE_DOWNLOAD_ERR.NO_DATA)
    return
  }

  const csvContent = data.map((row) => row.split(',').join(',')).join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  saveAs(blob, fileName)
}

/**
 * Print data with specific styles.
 * @param {string[]} data - Array of strings to be printed.
 */
export const printReport = (data: string[]) => {
  if (!data || data.length === 0) {
    messageHandler.logErrorMessage(PRINT_ERR.NO_DATA)
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
  printJS({
    printable: container.innerHTML,
    type: 'raw-html',
    style: printStyles,
  })
}
