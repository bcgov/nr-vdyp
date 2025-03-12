/// <reference types="cypress" />

import {
  validateComparison,
  validateRange,
} from '@/validation/reportInfoValidation'
import { CONSTANTS } from '@/constants'

describe('Report Info Validation Unit Tests', () => {
  context('validateComparison', () => {
    it('should return true when finishingAge is greater than or equal to startingAge', () => {
      expect(validateComparison(10, 20).isValid).to.be.true
      expect(validateComparison(50, 50).isValid).to.be.true
    })

    it('should return false when finishingAge is less than startingAge', () => {
      expect(validateComparison(30, 20).isValid).to.be.false
    })

    it('should return true when startingAge or finishingAge is null', () => {
      expect(validateComparison(null, 30).isValid).to.be.true
      expect(validateComparison(30, null).isValid).to.be.true
      expect(validateComparison(null, null).isValid).to.be.true
    })
  })

  context('validateRange', () => {
    it('should return true for valid age values within range', () => {
      expect(
        validateRange(
          CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
          CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
          CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
        ).isValid,
      ).to.be.true
    })

    it('should return false and errorType "startingAge" for out of range startingAge', () => {
      const result = validateRange(
        CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN - 1,
        CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
        CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('startingAge')
    })

    it('should return false and errorType "finishingAge" for out of range finishingAge', () => {
      const result = validateRange(
        CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
        CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX + 1,
        CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('finishingAge')
    })

    it('should return false and errorType "ageIncrement" for out of range ageIncrement', () => {
      const result = validateRange(
        CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
        CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
        CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN - 1,
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('ageIncrement')
    })

    it('should return true when null values are provided', () => {
      expect(validateRange(null, null, null).isValid).to.be.true
    })
  })
})
