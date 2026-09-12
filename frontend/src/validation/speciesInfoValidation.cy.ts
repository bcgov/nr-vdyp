/// <reference types="cypress" />

import { assert } from 'chai'
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
    assert.isTrue(result.isValid)
  })

  it('should not treat null species as duplicates', () => {
    const speciesList = [
      { species: null, percent: '50' },
      { species: null, percent: '50' },
    ]

    const result = validateDuplicateSpecies(speciesList)
    assert.isTrue(result.isValid)
  })

  it('should validate total species percent correctly', () => {
    const result = validateTotalSpeciesPercent(
      '100.0',
      CONSTANTS.NUM_INPUT_LIMITS.TOTAL_SPECIES_PERCENT,
    )
    assert.isTrue(result.isValid)

    const resultInvalid = validateTotalSpeciesPercent(
      '90.0',
      CONSTANTS.NUM_INPUT_LIMITS.TOTAL_SPECIES_PERCENT,
    )
    assert.isFalse(resultInvalid.isValid)
  })

  it('should fail when totalSpeciesGroupPercent does not match', () => {
    const result = validateTotalSpeciesPercent(
      '100.0',
      CONSTANTS.NUM_INPUT_LIMITS.TOTAL_SPECIES_PERCENT - 1,
    )
    assert.isFalse(result.isValid)
  })

  it('should validate required fields', () => {
    assert.isTrue(validateRequired('SomeValue').isValid)
    assert.isFalse(validateRequired(null).isValid)
    assert.isFalse(validateRequired('').isValid)
  })

  it('should validate percent range correctly', () => {
    assert.isTrue(validatePercent('50').isValid)
    assert.isTrue(validatePercent('0').isValid)
    assert.isTrue(validatePercent('100').isValid)
    assert.isTrue(validatePercent(null).isValid)
    assert.isTrue(validatePercent('').isValid)
    assert.isFalse(validatePercent('-1').isValid)
    assert.isFalse(validatePercent('150').isValid)
  })
})
