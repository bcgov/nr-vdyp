import { StandDensityValidator } from './standDensityValidator'

const standDensityValidator = new StandDensityValidator()

export const validateRange = (percentStockableArea: number | null) => {
  if (
    !standDensityValidator.validatePercentStockableAreaRange(
      percentStockableArea,
    )
  ) {
    return { isValid: false }
  }

  return { isValid: true }
}
