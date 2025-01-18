import { SiteInfoValidator } from './siteInfoValidator'

const siteInfoValidator = new SiteInfoValidator()

export const validateRequiredFields = (bha50SiteIndex: string | null) => {
  if (!siteInfoValidator.validateRequiredFields(bha50SiteIndex)) {
    return { isValid: false }
  }

  return { isValid: true }
}

export const validateRange = (bha50SiteIndex: string | null) => {
  if (!siteInfoValidator.validateBha50SiteIndexRange(bha50SiteIndex)) {
    return { isValid: false }
  }

  return { isValid: true }
}
