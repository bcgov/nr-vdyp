import { StandInfoValidator } from './standInfoValidator'

const standInfoValidator = new StandInfoValidator()

export const validateRange = (percentStockableArea: number | null) => {
  if (
    !standInfoValidator.validatePercentStockableAreaRange(percentStockableArea)
  ) {
    return { isValid: false }
  }

  return { isValid: true }
}
