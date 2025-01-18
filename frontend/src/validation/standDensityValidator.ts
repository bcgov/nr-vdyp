import { ValidationBase } from './validationBase'
import { NUM_INPUT_LIMITS } from '@/constants/constants'

export class StandDensityValidator extends ValidationBase {
  validatePercentStockableAreaRange(psa: number | null): boolean {
    if (!psa) return true

    return this.validateRange(
      psa,
      NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN,
      NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX,
    )
  }
}
