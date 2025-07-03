/// <reference types="cypress" />

import { validateRange } from '@/validation/standDensityValidation'
import { CONSTANTS } from '@/constants'

describe('Stand Density Validation Unit Tests', () => {
  context('validateRange', () => {
    it('should return true for valid percentStockableArea within range', () => {
      expect(
        validateRange(CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN)
          .isValid,
      ).to.be.true
      expect(
        validateRange(CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX)
          .isValid,
      ).to.be.true
      expect(validateRange(50).isValid).to.be.true
    })

    it('should return false for percentStockableArea out of range', () => {
      expect(
        validateRange(CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN - 1)
          .isValid,
      ).to.be.false
      expect(
        validateRange(CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX + 1)
          .isValid,
      ).to.be.false
    })

    it('should return true for null value', () => {
      expect(validateRange(null).isValid).to.be.true
    })

    it('should return false for negative values', () => {
      expect(validateRange(-10).isValid).to.be.false
    })
  })
})
