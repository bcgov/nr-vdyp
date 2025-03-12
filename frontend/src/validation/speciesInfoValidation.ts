import { SpeciesInfoValidator } from './speciesInfoValidator'
import type { SpeciesList } from '@/interfaces/interfaces'

const speciesInfoValidator = new SpeciesInfoValidator()

export const validateDuplicateSpecies = (speciesList: SpeciesList[]) => {
  const duplicateSpecies =
    speciesInfoValidator.validateDuplicateSpecies(speciesList)
  if (duplicateSpecies) {
    return {
      isValid: false,
      duplicateSpecies,
    }
  }
  return { isValid: true }
}

export const validateTotalSpeciesPercent = (
  totalSpeciesPercent: string,
  totalSpeciesGroupPercent: number,
) => {
  const isValid = speciesInfoValidator.validateTotalSpeciesPercent(
    totalSpeciesPercent,
    totalSpeciesGroupPercent,
  )
  return { isValid }
}

export const validateRequired = (derivedBy: string | null) => {
  const isValid = speciesInfoValidator.validateRequired(derivedBy)
  return { isValid }
}

export const validatePercent = (percent: string | null) => {
  const isValid = speciesInfoValidator.validatePercent(percent)
  return { isValid }
}
