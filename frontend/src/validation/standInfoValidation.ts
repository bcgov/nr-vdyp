import { StandInfoValidator } from './standInfoValidator'

const standInfoValidator = new StandInfoValidator()

export const validateRange = (
  percentStockableArea: string | null,
  basalArea: string | null,
  treesPerHectare: string | null,
  crownClosure: string | null,
) => {
  if (
    !standInfoValidator.validatePercentStockableAreaRange(percentStockableArea)
  ) {
    return {
      isValid: false,
      errorType: 'percentStockableArea',
    }
  }

  if (!standInfoValidator.validateBasalAreaRange(basalArea)) {
    return {
      isValid: false,
      errorType: 'basalArea',
    }
  }

  if (!standInfoValidator.validateTreesPerHectareRange(treesPerHectare)) {
    return {
      isValid: false,
      errorType: 'treesPerHectare',
    }
  }

  if (!standInfoValidator.validatePercentCrownClosureRange(crownClosure)) {
    return {
      isValid: false,
      errorType: 'crownClosure',
    }
  }

  return { isValid: true }
}

export const validateBALimits = (
  selectedSiteSpecies: string | null,
  becZone: string | null,
  basalArea: string | null,
  height: string | null,
): boolean => {
  return standInfoValidator.validateBALimits(
    selectedSiteSpecies,
    becZone,
    basalArea,
    height,
  )
}

export const validateTPHLimits = (
  basalArea: string | null,
  treesPerHectare: string | null,
  spzHeight: string | null,
  selectedSiteSpecies: string | null,
  becZone: string | null,
): string | null => {
  return standInfoValidator.validateTPHLimits(
    basalArea,
    treesPerHectare,
    spzHeight,
    selectedSiteSpecies,
    becZone,
  )
}

export const validateQuadDiameter = (
  basalArea: string | null,
  treesPerHectare: string | null,
  minimumDBHLimit: string | null,
): string | null => {
  return standInfoValidator.validateQuadDiameter(
    basalArea,
    treesPerHectare,
    minimumDBHLimit,
  )
}
