/// <reference types="cypress" />

import {
  validateRange,
  validateBALimits,
  validateTPHLimits,
  validateQuadDiameter,
} from '@/validation/standInfoValidation'
import { CONSTANTS } from '@/constants'

describe('Stand Information Validation Unit Tests', () => {
  context('validateRange', () => {
    it('should return true for valid percentStockableArea within range', () => {
      expect(
        validateRange(
          CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN,
          null,
          null,
          null,
        ).isValid,
      ).to.be.true
      expect(
        validateRange(
          CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX,
          null,
          null,
          null,
        ).isValid,
      ).to.be.true
      expect(validateRange(50, null, null, null).isValid).to.be.true
    })

    it('should return false for percentStockableArea out of range', () => {
      expect(
        validateRange(
          CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN - 1,
          null,
          null,
          null,
        ).isValid,
      ).to.be.false
      expect(
        validateRange(
          CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX + 1,
          null,
          null,
          null,
        ).isValid,
      ).to.be.false
    })

    it('should return true for null value', () => {
      expect(validateRange(null, null, null, null).isValid).to.be.true
    })

    it('should return false for negative percentStockableArea', () => {
      expect(validateRange(-10, null, null, null).isValid).to.be.false
    })

    it('should return false for invalid basalArea', () => {
      expect(
        validateRange(
          null,
          String(CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_MAX + 1),
          null,
          null,
        ).isValid,
      ).to.be.false
    })

    it('should return false for invalid treesPerHectare', () => {
      expect(
        validateRange(
          null,
          null,
          String(CONSTANTS.NUM_INPUT_LIMITS.TPH_MAX + 1),
          null,
        ).isValid,
      ).to.be.false
    })

    it('should return false for invalid crownClosure', () => {
      expect(
        validateRange(
          null,
          null,
          null,
          CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MAX + 1,
        ).isValid,
      ).to.be.false
    })
  })

  context('validateBALimits', () => {
    it('should return true for invalid inputs', () => {
      expect(validateBALimits(null, null, null, null)).to.be.true
      expect(validateBALimits('H', 'CWH', null, '10.0')).to.be.true
    })
  })

  context('validateTPHLimits', () => {
    it('should return null for valid TPH within limits', () => {
      const basalArea = '25.0'
      const tph = '1000'
      const spzHeight = '10.0'
      const species = 'AC'
      const becZone = 'CWH'

      expect(validateTPHLimits(basalArea, tph, spzHeight, species, becZone)).to
        .be.null
    })

    it('should return error message for TPH below minimum', () => {
      const basalArea = '25.0'
      const tph = '50'
      const spzHeight = '10.0'
      const species = 'AC'
      const becZone = 'CWH'

      const result = validateTPHLimits(
        basalArea,
        tph,
        spzHeight,
        species,
        becZone,
      )
      expect(result).to.include('Trees/ha is less than a likely minimum')
    })

    it('should return null for invalid inputs', () => {
      expect(validateTPHLimits(null, null, null, null, null)).to.be.null
    })
  })

  context('validateQuadDiameter', () => {
    it('should return null for valid quadratic diameter', () => {
      const basalArea = '10.0'
      const tph = '1000'
      const minDBHLimit = '7.5'

      expect(validateQuadDiameter(basalArea, tph, minDBHLimit)).to.be.null
    })

    it('should return error message for quadratic diameter below minDBHLimit', () => {
      const basalArea = '1.0' // Low basal area
      const tph = '1000'
      const minDBHLimit = '10.0'

      const result = validateQuadDiameter(basalArea, tph, minDBHLimit)
      expect(result).to.include('Quadratic Mean Diameter')
      expect(result).to.include('less than the required diameter')
    })

    it('should return null for invalid inputs', () => {
      expect(validateQuadDiameter(null, null, null)).to.be.null
      expect(validateQuadDiameter('10.0', null, '7.5')).to.be.null
    })
  })
})
