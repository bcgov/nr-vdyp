import { ReportInfoValidator } from '@/validation/reportInfoValidator'

const reportInfoValidator = new ReportInfoValidator()

export const validateComparison = (
  startingAge: number | null,
  finishingAge: number | null,
) => {
  if (!reportInfoValidator.validateAgeComparison(startingAge, finishingAge)) {
    return { isValid: false }
  }

  return { isValid: true }
}

export const validateRequiredFields = (
  startingAge: number | null,
  finishingAge: number | null,
  ageIncrement: number | null,
) => {
  if (
    !reportInfoValidator.validateRequiredFields(
      startingAge,
      finishingAge,
      ageIncrement,
    )
  ) {
    return { isValid: false }
  }
  return { isValid: true }
}

export const validateRange = (
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
