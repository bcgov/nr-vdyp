import { SiteInfoValidator } from './siteInfoValidator'

const siteInfoValidator = new SiteInfoValidator()

export const validateRequiredFields = (
  siteSpeciesValues: string | null,
  spzAge: number | null,
  spzHeight: string | null,
  bha50SiteIndex: string | null,
) => {
  if (
    !siteInfoValidator.validateRequiredFields(
      siteSpeciesValues,
      spzAge,
      spzHeight,
      bha50SiteIndex,
    )
  ) {
    return { isValid: false }
  }

  return { isValid: true }
}

export const validateRange = (
  spzAge: number | null,
  spzHeight: string | null,
  bha50SiteIndex: string | null,
) => {
  if (!siteInfoValidator.validateAgeRange(spzAge)) {
    return {
      isValid: false,
      errorType: 'spzAge',
    }
  }

  if (!siteInfoValidator.validateHeightRange(spzHeight)) {
    return {
      isValid: false,
      errorType: 'spzHeight',
    }
  }

  if (!siteInfoValidator.validateBha50SiteIndexRange(bha50SiteIndex)) {
    return {
      isValid: false,
      errorType: 'bha50SiteIndex',
    }
  }

  return { isValid: true }
}
