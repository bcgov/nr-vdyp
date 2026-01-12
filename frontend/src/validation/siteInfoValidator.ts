import { ValidationBase } from './validationBase'
import { CONSTANTS } from '@/constants'
import { isEmptyOrZero } from '@/utils/util'

export class SiteInfoValidator extends ValidationBase {
  validateRequiredFields(
    siteSpeciesValues: string | null,
    spzAge: string | null,
    spzHeight: string | null,
    bha50SiteIndex: string | null,
  ): boolean {
    if (siteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED) {
      return !(isEmptyOrZero(spzAge) || isEmptyOrZero(spzHeight))
    } else if (siteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED) {
      return !isEmptyOrZero(bha50SiteIndex)
    }
    return true
  }

  validateAgeRange(spzAge: string | null): boolean {
    if (!spzAge) return true

    const numericAge = Number.parseFloat(spzAge)

    return this.validateRange(
      numericAge,
      CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MAX,
    )
  }

  validateHeightRange(spzHeight: string | null): boolean {
    if (!spzHeight) return true

    const numericHeight = Number.parseFloat(spzHeight)

    return this.validateRange(
      numericHeight,
      CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MAX,
    )
  }

  validateBha50SiteIndexRange(bha50SiteIndex: string | null): boolean {
    if (!bha50SiteIndex) return true

    const numericBha50 = Number.parseFloat(bha50SiteIndex)

    return this.validateRange(
      numericBha50,
      CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX,
    )
  }
}
