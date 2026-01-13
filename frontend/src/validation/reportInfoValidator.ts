import { ValidationBase } from './validationBase'
import { CONSTANTS } from '@/constants'

export class ReportInfoValidator extends ValidationBase {
  /**
   * Validates that all required fields are not null.
   * @param startValue - The starting value (e.g., startingAge or startYear)
   * @param endValue - The ending value (e.g., finishingAge or endYear)
   * @param incrementValue - The increment value (e.g., ageIncrement or yearIncrement)
   * @returns boolean - True if all required fields are provided, false otherwise
   */
  validateRequiredFields(
    startValue: string | null,
    endValue: string | null,
    incrementValue: string | null,
  ): boolean {
    return startValue !== null && endValue !== null && incrementValue !== null
  }

  /**
   * Validates that the end value is greater than or equal to the start value.
   * @param startValue - The starting value (e.g., startingAge or startYear)
   * @param endValue - The ending value (e.g., finishingAge or endYear)
   * @returns boolean - True if endValue >= startValue, false otherwise
   */
  validateAgeComparison(
    startValue: string | null,
    endValue: string | null,
  ): boolean {
    if (startValue !== null && endValue !== null) {
      const numStart = Number.parseFloat(startValue)
      const numEnd = Number.parseFloat(endValue)
      return numEnd >= numStart
    }
    return true
  }

  validateStartingAgeRange(startingAge: string | null): boolean {
    if (startingAge !== null) {
      const num = Number.parseFloat(startingAge)
      return (
        num >= CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN &&
        num <= CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX
      )
    }
    return true
  }

  validateFinishingAgeRange(finishingAge: string | null): boolean {
    if (finishingAge !== null) {
      const num = Number.parseFloat(finishingAge)
      return (
        num >= CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN &&
        num <= CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX
      )
    }
    return true
  }

  validateAgeIncrementRange(ageIncrement: string | null): boolean {
    if (ageIncrement !== null) {
      const num = Number.parseFloat(ageIncrement)
      return (
        num >= CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN &&
        num <= CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX
      )
    }
    return true
  }
  //
  validateStartYearRange(startYear: string | null): boolean {
    if (startYear !== null) {
      const num = Number.parseFloat(startYear)
      return (
        num >= CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN &&
        num <= CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MAX
      )
    }
    return true
  }

  validateEndYearRange(endYear: string | null): boolean {
    if (endYear !== null) {
      const num = Number.parseFloat(endYear)
      return (
        num >= CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MIN &&
        num <= CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX
      )
    }
    return true
  }

  validateYearIncrementRange(yearIncrement: string | null): boolean {
    if (yearIncrement !== null) {
      const num = Number.parseFloat(yearIncrement)
      return (
        num >= CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN &&
        num <= CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX
      )
    }
    return true
  }
}
