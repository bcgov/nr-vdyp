import { ValidationBase } from './validationBase'
import { NUM_INPUT_LIMITS } from '@/constants/constants'
import type { SpeciesList } from '@/interfaces/interfaces'

export class SpeciesInfoValidation extends ValidationBase {
  validatePercent(percent: any): boolean {
    if (percent === null || percent === '') {
      return true
    }
    const numValue = Math.floor(parseFloat(percent) * 10) / 10
    return (
      numValue >= NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN &&
      numValue <= NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX
    )
  }

  validateTotalSpeciesPercent(
    totalSpeciesPercent: string,
    totalSpeciesGroupPercent: number,
  ): boolean {
    const formattedPercentLimit = (
      Math.floor(NUM_INPUT_LIMITS.TOTAL_SPECIES_PERCENT * 10) / 10
    ).toFixed(NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM)
    return (
      totalSpeciesPercent === formattedPercentLimit &&
      totalSpeciesGroupPercent === NUM_INPUT_LIMITS.TOTAL_SPECIES_PERCENT
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
