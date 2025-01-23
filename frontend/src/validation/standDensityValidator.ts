import { ValidationBase } from './validationBase'
import { CONSTANTS } from '@/constants'

export class StandDensityValidator extends ValidationBase {
  validatePercentStockableAreaRange(psa: number | null): boolean {
    if (!psa) return true

    return this.validateRange(
      psa,
      CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX,
    )
  }
}
