/// <reference types="cypress" />

import { assert } from 'chai'
import {
  validateComparison,
  validateRequiredFields,
  validateAgeRange,
  validateYearRange,
  validateReportTitle,
  validateProjectionType,
} from '@/validation/reportInfoValidation'
import { CONSTANTS } from '@/constants'

describe('Report Info Validation Unit Tests', () => {
  context('validateComparison', () => {
    it('should return true when endValue is greater than or equal to startValue', () => {
      assert.isTrue(validateComparison('10', '20').isValid)
      assert.isTrue(validateComparison('50', '50').isValid)
    })

    it('should return false when endValue is less than startValue', () => {
      assert.isFalse(validateComparison('30', '20').isValid)
    })

    it('should return true when startValue or endValue is null', () => {
      assert.isTrue(validateComparison(null, '30').isValid)
      assert.isTrue(validateComparison('30', null).isValid)
      assert.isTrue(validateComparison(null, null).isValid)
    })
  })

  context('validateRequiredFields', () => {
    it('should return true when all required fields are provided', () => {
      assert.isTrue(validateRequiredFields('10', '20', '5').isValid)
    })

    it('should return false when any required field is null', () => {
      assert.isFalse(validateRequiredFields(null, '20', '5').isValid)
      assert.isFalse(validateRequiredFields('10', null, '5').isValid)
      assert.isFalse(validateRequiredFields('10', '20', null).isValid)
    })
  })

  context('validateAgeRange', () => {
    it('should return true for valid age values within range', () => {
      assert.isTrue(
        validateAgeRange(
          String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN),
          String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX),
          String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN),
        ).isValid,
      )
    })

    it('should return false and errorType "startingAge" for out of range startingAge', () => {
      const resultBelowMin = validateAgeRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN - 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN),
      )
      expect(resultBelowMin.isValid).to.be.false
      expect(resultBelowMin.errorType).to.equal('startingAge')

      const resultAboveMax = validateAgeRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX + 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN),
      )
      expect(resultAboveMax.isValid).to.be.false
      expect(resultAboveMax.errorType).to.equal('startingAge')
    })

    it('should return false and errorType "finishingAge" for out of range finishingAge', () => {
      const resultBelowMin = validateAgeRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN - 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN),
      )
      expect(resultBelowMin.isValid).to.be.false
      expect(resultBelowMin.errorType).to.equal('finishingAge')

      const resultAboveMax = validateAgeRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX + 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN),
      )
      expect(resultAboveMax.isValid).to.be.false
      expect(resultAboveMax.errorType).to.equal('finishingAge')
    })

    it('should return false and errorType "ageIncrement" for out of range ageIncrement', () => {
      const resultBelowMin = validateAgeRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN - 1),
      )
      expect(resultBelowMin.isValid).to.be.false
      expect(resultBelowMin.errorType).to.equal('ageIncrement')

      const resultAboveMax = validateAgeRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX + 1),
      )
      expect(resultAboveMax.isValid).to.be.false
      expect(resultAboveMax.errorType).to.equal('ageIncrement')
    })

    it('should return true when null values are provided', () => {
      assert.isTrue(validateAgeRange(null, null, null).isValid)
    })
  })

  context('validateYearRange', () => {
    it('should return true for valid year values within range', () => {
      assert.isTrue(
        validateYearRange(
          String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN),
          String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX),
          String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN),
        ).isValid,
      )
    })

    it('should return false and errorType "startYear" for out of range startYear', () => {
      const resultBelowMin = validateYearRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN - 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN),
      )
      expect(resultBelowMin.isValid).to.be.false
      expect(resultBelowMin.errorType).to.equal('startYear')

      const resultAboveMax = validateYearRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MAX + 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN),
      )
      expect(resultAboveMax.isValid).to.be.false
      expect(resultAboveMax.errorType).to.equal('startYear')
    })

    it('should return false and errorType "endYear" for out of range endYear', () => {
      const resultBelowMin = validateYearRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MIN - 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN),
      )
      expect(resultBelowMin.isValid).to.be.false
      expect(resultBelowMin.errorType).to.equal('endYear')

      const resultAboveMax = validateYearRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX + 1),
        String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN),
      )
      expect(resultAboveMax.isValid).to.be.false
      expect(resultAboveMax.errorType).to.equal('endYear')
    })

    it('should return false and errorType "yearIncrement" for out of range yearIncrement', () => {
      const resultBelowMin = validateYearRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN - 1),
      )
      expect(resultBelowMin.isValid).to.be.false
      expect(resultBelowMin.errorType).to.equal('yearIncrement')

      const resultAboveMax = validateYearRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN),
        String(CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX),
        String(CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX + 1),
      )
      expect(resultAboveMax.isValid).to.be.false
      expect(resultAboveMax.errorType).to.equal('yearIncrement')
    })

    it('should return true when null values are provided', () => {
      assert.isTrue(validateYearRange(null, null, null).isValid)
    })
  })

  context('validateReportTitle', () => {
    it('should return true when report title is provided', () => {
      assert.isTrue(validateReportTitle('My Report').isValid)
    })

    it('should return false when report title is null', () => {
      assert.isFalse(validateReportTitle(null).isValid)
    })

    it('should return false when report title is empty or whitespace', () => {
      assert.isFalse(validateReportTitle('').isValid)
      assert.isFalse(validateReportTitle('   ').isValid)
    })
  })

  context('validateProjectionType', () => {
    it('should return true when projection type is provided', () => {
      assert.isTrue(validateProjectionType('AGE').isValid)
    })

    it('should return false when projection type is null', () => {
      assert.isFalse(validateProjectionType(null).isValid)
    })

    it('should return false when projection type is empty or whitespace', () => {
      assert.isFalse(validateProjectionType('').isValid)
      assert.isFalse(validateProjectionType('   ').isValid)
    })
  })
})
