import { ReportInfoValidator } from '@/validation/reportInfoValidator'

const reportInfoValidator = new ReportInfoValidator()

/**
 * Validates that the end value is greater than or equal to the start value.
 * @param startValue - The starting value (e.g., startingAge or startYear)
 * @param endValue - The ending value (e.g., finishingAge or endYear)
 * @returns Object - Validation result
 */
export const validateComparison = (
  startValue: number | null,
  endValue: number | null,
) => {
  if (!reportInfoValidator.validateAgeComparison(startValue, endValue)) {
    return { isValid: false }
  }
  return { isValid: true }
}

/**
 * Validates that all required fields are provided.
 * @param startValue - The starting value (e.g., startingAge or startYear)
 * @param endValue - The ending value (e.g., finishingAge or endYear)
 * @param incrementValue - The increment value (e.g., ageIncrement or yearIncrement)
 * @returns Object - Validation result
 */
export const validateRequiredFields = (
  startValue: number | null,
  endValue: number | null,
  incrementValue: number | null,
) => {
  if (
    !reportInfoValidator.validateRequiredFields(
      startValue,
      endValue,
      incrementValue,
    )
  ) {
    return { isValid: false }
  }
  return { isValid: true }
}

export const validateAgeRange = (
  startingAge: number | null,
  finishingAge: number | null,
  ageIncrement: number | null,
) => {
  if (!reportInfoValidator.validateStartingAgeRange(startingAge)) {
    return {
      isValid: false,
      errorType: 'startingAge',
    }
  }

  if (!reportInfoValidator.validateFinishingAgeRange(finishingAge)) {
    return {
      isValid: false,
      errorType: 'finishingAge',
    }
  }

  if (!reportInfoValidator.validateAgeIncrementRange(ageIncrement)) {
    return {
      isValid: false,
      errorType: 'ageIncrement',
    }
  }

  return { isValid: true }
}

export const validateYearRange = (
  startYear: number | null,
  endYear: number | null,
  yearIncrement: number | null,
) => {
  if (!reportInfoValidator.validateStartYearRange(startYear)) {
    return {
      isValid: false,
      errorType: 'startYear',
    }
  }

  if (!reportInfoValidator.validateEndYearRange(endYear)) {
    return {
      isValid: false,
      errorType: 'endYear',
    }
  }

  if (!reportInfoValidator.validateYearIncrementRange(yearIncrement)) {
    return {
      isValid: false,
      errorType: 'yearIncrement',
    }
  }

  return { isValid: true }
}
