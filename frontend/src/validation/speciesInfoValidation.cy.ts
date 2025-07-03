/// <reference types="cypress" />

import {
  validateDuplicateSpecies,
  validateTotalSpeciesPercent,
  validateRequired,
  validatePercent,
} from '@/validation/speciesInfoValidation'
import { CONSTANTS } from '@/constants'

describe('Species Info Validation Unit Tests', () => {
  it('should validate duplicate species', () => {
    const speciesList = [
      { species: 'Pine', percent: '30' },
      { species: 'Spruce', percent: '40' },
      { species: 'Pine', percent: '30' }, // duplicate
    ]

    const result = validateDuplicateSpecies(speciesList)
    expect(result.isValid).to.be.false
    expect(result.duplicateSpecies).to.equal('Pine')
  })

  it('should pass when there are no duplicate species', () => {
    const speciesList = [
      { species: 'Pine', percent: '30' },
      { species: 'Spruce', percent: '40' },
    ]

    const result = validateDuplicateSpecies(speciesList)
    expect(result.isValid).to.be.true
  })

  it('should validate total species percent correctly', () => {
    const result = validateTotalSpeciesPercent(
      '100.0',
      CONSTANTS.NUM_INPUT_LIMITS.TOTAL_SPECIES_PERCENT,
    )
    expect(result.isValid).to.be.true

    const resultInvalid = validateTotalSpeciesPercent(
      '90.0',
      CONSTANTS.NUM_INPUT_LIMITS.TOTAL_SPECIES_PERCENT,
    )
    expect(resultInvalid.isValid).to.be.false
  })

  it('should validate required fields', () => {
    expect(validateRequired('SomeValue').isValid).to.be.true
    expect(validateRequired(null).isValid).to.be.false
  })

  it('should validate percent range correctly', () => {
    expect(validatePercent('50').isValid).to.be.true
    expect(validatePercent(null).isValid).to.be.true
    expect(validatePercent('150').isValid).to.be.false
  })
})
