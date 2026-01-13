import { ValidationBase } from './validationBase'
import { CONSTANTS } from '@/constants'
import type { SpeciesList } from '@/interfaces/interfaces'

export class SpeciesInfoValidator extends ValidationBase {
  validatePercent(percent: string | null): boolean {
    if (percent === null || percent === '') {
      return true
    }
    const numValue = Math.floor(Number.parseFloat(percent) * 10) / 10
    return (
      numValue >= CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN &&
      numValue <= CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX
    )
  }

  validateTotalSpeciesPercent(
    totalSpeciesPercent: string,
    totalSpeciesGroupPercent: number,
  ): boolean {
    const formattedPercentLimit = (
      Math.floor(CONSTANTS.NUM_INPUT_LIMITS.TOTAL_SPECIES_PERCENT * 10) / 10
    ).toFixed(CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM)
    return (
      totalSpeciesPercent === formattedPercentLimit &&
      totalSpeciesGroupPercent ===
        CONSTANTS.NUM_INPUT_LIMITS.TOTAL_SPECIES_PERCENT
    )
  }

  validateDuplicateSpecies(speciesList: SpeciesList[]): string | null {
    const speciesCount: { [key: string]: number } = {}
    let duplicateSpecies = null

    for (const item of speciesList) {
      if (item.species) {
        if (!speciesCount[item.species]) {
          speciesCount[item.species] = 0
        }
        speciesCount[item.species] += 1

        if (speciesCount[item.species] > 1) {
          duplicateSpecies = item.species
        }
      }
    }

    return duplicateSpecies
  }
}
