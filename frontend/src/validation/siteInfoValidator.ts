import { ValidationBase } from './validationBase'
import { CONSTANTS } from '@/constants'
import { isEmptyOrZero } from '@/utils/util'

export class SiteInfoValidator extends ValidationBase {
  validateRequiredFields(
    siteSpeciesValues: string | null,
    spzAge: number | null,
    spzHeight: string | null,
    bha50SiteIndex: string | null,
  ): boolean {
    if (siteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED) {
      return !(
        isEmptyOrZero(spzAge) ||
        isEmptyOrZero(spzHeight) ||
        isEmptyOrZero(bha50SiteIndex)
      )
    } else if (siteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED) {
      return !isEmptyOrZero(bha50SiteIndex)
    }
    return true
  }

  validateAgeRange(spzAge: number | null): boolean {
    if (!spzAge) return true

    return this.validateRange(
      spzAge,
      CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MAX,
    )
  }

  validateHeightRange(spzHeight: string | null): boolean {
    if (!spzHeight) return true

    const numericHeight = parseFloat(spzHeight)

    return this.validateRange(
      numericHeight,
      CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MAX,
    )
  }

  validateBha50SiteIndexRange(bha50SiteIndex: string | null): boolean {
    if (!bha50SiteIndex) return true

    const numericBha50 = parseFloat(bha50SiteIndex)

    return this.validateRange(
      numericBha50,
      CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX,
    )
  }
}
