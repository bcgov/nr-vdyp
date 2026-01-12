/// <reference types="cypress" />

import {
  validateComparison,
  validateRequiredFields,
  validateAgeRange,
  validateYearRange,
} from '@/validation/reportInfoValidation'
import { CONSTANTS } from '@/constants'

describe('Report Info Validation Unit Tests', () => {
  context('validateComparison', () => {
    it('should return true when endValue is greater than or equal to startValue', () => {
      expect(validateComparison('10', '20').isValid).to.be.true
      expect(validateComparison('50', '50').isValid).to.be.true
    })

    it('should return false when endValue is less than startValue', () => {
      expect(validateComparison('30', '20').isValid).to.be.false
    })

    it('should return true when startValue or endValue is null', () => {
      expect(validateComparison(null, '30').isValid).to.be.true
      expect(validateComparison('30', null).isValid).to.be.true
      expect(validateComparison(null, null).isValid).to.be.true
    })
  })

  context('validateRequiredFields', () => {
    it('should return true when all required fields are provided', () => {
      expect(validateRequiredFields('10', '20','5').isValid).to.be.true
    })

    it('should return false when any required field is null', () => {
      expect(validateRequiredFields(null, '20', '5').isValid).to.be.false
      expect(validateRequiredFields('10', null, '5').isValid).to.be.false
      expect(validateRequiredFields('10', '20', null).isValid).to.be.false
    })
  })

  context('validateAgeRange', () => {
    it('should return true for valid age values within range', () => {
      expect(
        validateAgeRange(
          String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN),
          String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX),
          String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN),
        ).isValid,
      ).to.be.true
    })

    it('should return false and errorType "startingAge" for out of range startingAge', () => {
      const result = validateAgeRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN - 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('startingAge')
    })

    it('should return false and errorType "finishingAge" for out of range finishingAge', () => {
      const result = validateAgeRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX + 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('finishingAge')
    })

    it('should return false and errorType "ageIncrement" for out of range ageIncrement', () => {
      const result = validateAgeRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN - 1),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('ageIncrement')
    })

    it('should return true when null values are provided', () => {
      expect(validateAgeRange(null, null, null).isValid).to.be.true
    })
  })

  context('validateYearRange', () => {
    it('should return true for valid year values within range', () => {
      expect(
        validateYearRange(
          String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN),
          String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX),
          String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN),
        ).isValid,
      ).to.be.true
    })

    it('should return false and errorType "startYear" for out of range startYear', () => {
      const result = validateYearRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN - 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('startYear')
    })

    it('should return false and errorType "endYear" for out of range endYear', () => {
      const result = validateYearRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX + 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('endYear')
    })

    it('should return false and errorType "yearIncrement" for out of range yearIncrement', () => {
      const result = validateYearRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN - 1),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('yearIncrement')
    })

    it('should return true when null values are provided', () => {
      expect(validateYearRange(null, null, null).isValid).to.be.true
    })
  })
})
