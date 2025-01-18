import { ValidationBase } from './validationBase'
import { NUM_INPUT_LIMITS } from '@/constants/constants'
import { Util } from '@/utils/util'

export class SiteInfoValidator extends ValidationBase {
  validateRequiredFields(bha50SiteIndex: string | null): boolean {
    return !Util.isEmptyOrZero(bha50SiteIndex)
  }

  validateBha50SiteIndexRange(bha50SiteIndex: string | null): boolean {
    if (!bha50SiteIndex) return true

    const numericBha50 = parseFloat(bha50SiteIndex)

    return this.validateRange(
      numericBha50,
      NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN,
      NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX,
    )
  }
}
