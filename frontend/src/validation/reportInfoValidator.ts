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
    startValue: number | null,
    endValue: number | null,
    incrementValue: number | null,
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
    startValue: number | null,
    endValue: number | null,
  ): boolean {
    if (startValue !== null && endValue !== null) {
      return endValue >= startValue
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
  //
  validateStartYearRange(startYear: number | null): boolean {
    if (startYear !== null) {
      return (
        startYear >= CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN &&
        startYear <= CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MAX
      )
    }
    return true
  }

  validateEndYearRange(endYear: number | null): boolean {
    if (endYear !== null) {
      return (
        endYear >= CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MIN &&
        endYear <= CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX
      )
    }
    return true
  }

  validateYearIncrementRange(yearIncrement: number | null): boolean {
    if (yearIncrement !== null) {
      return (
        yearIncrement >= CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN &&
        yearIncrement <= CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX
      )
    }
    return true
  }
}
