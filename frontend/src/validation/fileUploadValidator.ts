import { ValidationBase } from './validationBase'
import { CONSTANTS, CSVHEADERS } from '@/constants'

export class FileUploadValidator extends ValidationBase {
  validateRequiredFields(
    startingAge: number | null,
    finishingAge: number | null,
    ageIncrement: number | null,
  ): boolean {
    return (
      startingAge !== null && finishingAge !== null && ageIncrement !== null
    )
  }

  validateAgeComparison(
    startingAge: number | null,
    finishingAge: number | null,
  ): boolean {
    if (startingAge !== null && finishingAge !== null) {
      return finishingAge >= startingAge
    }
    return true
  }

  validateStartingAgeRange(startingAge: number | null): boolean {
    if (startingAge !== null) {
      return (
        startingAge >= CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN &&
        startingAge <= CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX
      )
    }
    return true
  }

  validateFinishingAgeRange(finishingAge: number | null): boolean {
    if (finishingAge !== null) {
      return (
        finishingAge >= CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN &&
        finishingAge <= CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX
      )
    }
    return true
  }

  validateAgeIncrementRange(ageIncrement: number | null): boolean {
    if (ageIncrement !== null) {
      return (
        ageIncrement >= CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN &&
        ageIncrement <= CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX
      )
    }
    return true
  }

  async isCSVFile(file: File): Promise<boolean> {
    // Check file extension
    const fileExtension = file.name.split('.').pop()?.toLowerCase()
    if (fileExtension !== 'csv') {
      return false
    }

    // Check MIME type
    const validMimeType = 'text/csv'
    if (
      file.type !== validMimeType &&
      file.type !== 'application/vnd.ms-excel' /* firefox */
    ) {
      return false
    }

    return true
  }

  private async getHeaderValidationDetails(
    file: File,
    expectedHeaders: string[],
    optionalHeaders: Set<string> = new Set(),
    otherTypeHeaders?: string[],
  ): Promise<{
    isValid: boolean
    isEmpty: boolean
    isWrongFileType: boolean
    details: { missing: string[]; extra: string[]; mismatches: string[] }
  }> {
    const actualHeaders = await this.getFileHeaders(file)

    // Empty file - fail with isEmpty flag so callers can show a specific message.
    if (actualHeaders.length === 0) {
      return { isValid: false, isEmpty: true, isWrongFileType: false, details: { missing: [], extra: [], mismatches: [] } }
    }

    // If the first field doesn't match the expected first header, treat the file
    // as headerless and skip validation.
    if (actualHeaders[0] !== expectedHeaders[0]) {
      return { isValid: true, isEmpty: false, isWrongFileType: false, details: { missing: [], extra: [], mismatches: [] } }
    }

    const missing: string[] = []
    const extra: string[] = []
    const mismatches: string[] = []

    // Find missing and extra regardless of length
    const expectedSet = new Set(expectedHeaders)
    const actualSet = new Set(actualHeaders)
    expectedHeaders.forEach((h) => {
      if (!actualSet.has(h) && !optionalHeaders.has(h)) missing.push(h)
    })
    actualHeaders.forEach((h) => {
      if (!expectedSet.has(h)) extra.push(h)
    })

    // Only check mismatches if lengths are equal (to avoid unnecessary details)
    if (actualHeaders.length === expectedHeaders.length) {
      for (let i = 0; i < expectedHeaders.length; i++) {
        if (actualHeaders[i] !== expectedHeaders[i]) {
          mismatches.push(
            `Position ${i + 1}: Expected '${expectedHeaders[i]}', found '${actualHeaders[i]}'`,
          )
        }
      }
    }

    const isValid =
      missing.length === 0 && extra.length === 0 && mismatches.length === 0

    // Detect if the file looks like the other file type by comparing the second header.
    const isWrongFileType =
      !isValid &&
      otherTypeHeaders !== undefined &&
      actualHeaders.length > 1 &&
      actualHeaders[1] === otherTypeHeaders[1]

    return { isValid, isEmpty: false, isWrongFileType, details: { missing, extra, mismatches } }
  }

  async validatePolygonHeader(file: File): Promise<{
    isValid: boolean
    isEmpty: boolean
    isWrongFileType: boolean
    details: { missing: string[]; extra: string[]; mismatches: string[] }
    expected: string[]
  }> {
    const { isValid, isEmpty, isWrongFileType, details } =
      await this.getHeaderValidationDetails(
        file,
        CSVHEADERS.POLYGON_HEADERS,
        new Set(),
        CSVHEADERS.LAYER_HEADERS,
      )
    return { isValid, isEmpty, isWrongFileType, details, expected: CSVHEADERS.POLYGON_HEADERS }
  }

  async validateLayerHeader(file: File): Promise<{
    isValid: boolean
    isEmpty: boolean
    isWrongFileType: boolean
    details: { missing: string[]; extra: string[]; mismatches: string[] }
    expected: string[]
  }> {
    const { isValid, isEmpty, isWrongFileType, details } =
      await this.getHeaderValidationDetails(
        file,
        CSVHEADERS.LAYER_HEADERS,
        CSVHEADERS.OPTIONAL_LAYER_HEADERS,
        CSVHEADERS.POLYGON_HEADERS,
      )
    return { isValid, isEmpty, isWrongFileType, details, expected: CSVHEADERS.LAYER_HEADERS }
  }

  private async getFileHeaders(file: File): Promise<string[]> {
    const headerLine = await this.readFirstLine(file)
    return this.parseCSVHeader(headerLine)
  }

  // Streams the file until the first newline, then cancels - avoids loading
  // large files into memory entirely.
  private async readFirstLine(file: File): Promise<string> {
    const reader = file.stream().getReader()
    const decoder = new TextDecoder()
    let line = ''

    try {
      outer: while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const chunk = decoder.decode(value, { stream: true })
        for (const char of chunk) {
          if (char === '\n') break outer
          line += char
        }
      }
    } finally {
      await reader.cancel()
    }

    return line.replace(/\r$/, '')
  }

  private parseCSVHeader(headerLine: string): string[] {
    const headers: string[] = []
    let current = ''
    let inQuotes = false
    let quoteChar = ''

    for (const char of headerLine) {
      if (!inQuotes && (char === '"' || char === "'")) {
        // Start of quoted field
        inQuotes = true
        quoteChar = char
      } else if (inQuotes && char === quoteChar) {
        // End of quoted field
        inQuotes = false
        quoteChar = ''
      } else if (!inQuotes && char === ',') {
        // Field separator
        headers.push(current.trim())
        current = ''
      } else {
        // Regular character
        current += char
      }
    }

    // Add the last field
    if (current.length > 0 || headers.length > 0) {
      headers.push(current.trim())
    }

    return headers
  }

  async validateDuplicateColumns(
    file: File,
    expectedFirstHeader: string,
    expectedHeaders: string[],
    optionalHeaders: Set<string> = new Set(),
  ): Promise<{
    isValid: boolean
    isColumnCountInvalid: boolean
    duplicates: string[]
  }> {
    const headers = await this.getFileHeaders(file)

    if (headers[0] !== expectedFirstHeader) {
      // Headerless file: validate column count is within expected range.
      const minColumns = expectedHeaders.length - optionalHeaders.size
      const maxColumns = expectedHeaders.length
      const validCount = headers.length >= minColumns && headers.length <= maxColumns
      return { isValid: validCount, isColumnCountInvalid: !validCount, duplicates: [] }
    }

    const seen = new Set<string>()
    const duplicates: string[] = []
    const duplicateTracker = new Set<string>()

    headers.forEach((header) => {
      const lowerCaseHeader = header.toLowerCase()
      if (seen.has(lowerCaseHeader)) {
        if (!duplicateTracker.has(lowerCaseHeader)) {
          duplicates.push(header)
          duplicateTracker.add(lowerCaseHeader)
        }
      } else {
        seen.add(lowerCaseHeader)
      }
    })

    return {
      isValid: duplicates.length === 0,
      isColumnCountInvalid: false,
      duplicates,
    }
  }
}
