import { ValidationBase } from './validationBase'
import { CONSTANTS } from '@/constants'

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
    if (file.type !== validMimeType) {
      return false
    }

    return true
  }
}
