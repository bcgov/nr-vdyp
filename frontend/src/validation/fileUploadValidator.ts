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
  ): Promise<{
    isValid: boolean
    details: { missing: string[]; extra: string[]; mismatches: string[] }
  }> {
    const reader = new FileReader()
    const fileContent = await new Promise<string>((resolve) => {
      reader.onload = () => resolve(reader.result as string)
      reader.readAsText(file)
    })
    const actualHeaders = fileContent
      .split('\n')[0]
      .split(',')
      .map((h) => h.trim())

    const missing: string[] = []
    const extra: string[] = []
    const mismatches: string[] = []

    // Find missing and extra regardless of length
    const expectedSet = new Set(expectedHeaders)
    const actualSet = new Set(actualHeaders)
    expectedHeaders.forEach((h) => {
      if (!actualSet.has(h)) missing.push(h)
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
    return { isValid, details: { missing, extra, mismatches } }
  }

  async validatePolygonHeader(file: File): Promise<{
    isValid: boolean
    details: { missing: string[]; extra: string[]; mismatches: string[] }
    expected: string[]
  }> {
    const { isValid, details } = await this.getHeaderValidationDetails(
      file,
      CSVHEADERS.POLYGON_HEADERS,
    )
    return { isValid, details, expected: CSVHEADERS.POLYGON_HEADERS }
  }

  async validateLayerHeader(file: File): Promise<{
    isValid: boolean
    details: { missing: string[]; extra: string[]; mismatches: string[] }
    expected: string[]
  }> {
    const { isValid, details } = await this.getHeaderValidationDetails(
      file,
      CSVHEADERS.LAYER_HEADERS,
    )
    return { isValid, details, expected: CSVHEADERS.LAYER_HEADERS }
  }
}
