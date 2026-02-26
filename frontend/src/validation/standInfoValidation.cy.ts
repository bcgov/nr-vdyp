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
          String(CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN),
          null,
          null,
          null,
        ).isValid,
      ).to.be.true
      expect(
        validateRange(
          String(CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX),
          null,
          null,
          null,
        ).isValid,
      ).to.be.true
      expect(validateRange('50', null, null, null).isValid).to.be.true
    })

    it('should return false for percentStockableArea out of range', () => {
      const resultBelowMin = validateRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN - 1),
        null,
        null,
        null,
      )
      expect(resultBelowMin.isValid).to.be.false
      expect(resultBelowMin.errorType).to.equal('percentStockableArea')

      const resultAboveMax = validateRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX + 1),
        null,
        null,
        null,
      )
      expect(resultAboveMax.isValid).to.be.false
      expect(resultAboveMax.errorType).to.equal('percentStockableArea')
    })

    it('should return true for null value', () => {
      expect(validateRange(null, null, null, null).isValid).to.be.true
    })

    it('should return false for negative percentStockableArea', () => {
      const result = validateRange('-10', null, null, null)
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('percentStockableArea')
    })

    it('should return false for basalArea above maximum', () => {
      const result = validateRange(
        null,
        String(CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_MAX + 1),
        null,
        null,
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('basalArea')
    })

    it('should return false for basalArea below minimum', () => {
      const result = validateRange(
        null,
        String(CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_MIN / 2),
        null,
        null,
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('basalArea')
    })

    it('should return false for treesPerHectare above maximum', () => {
      const result = validateRange(
        null,
        null,
        String(CONSTANTS.NUM_INPUT_LIMITS.TPH_MAX + 1),
        null,
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('treesPerHectare')
    })

    it('should return false for treesPerHectare below minimum', () => {
      const result = validateRange(
        null,
        null,
        String(CONSTANTS.NUM_INPUT_LIMITS.TPH_MIN / 2),
        null,
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('treesPerHectare')
    })

    it('should return false for crownClosure above maximum', () => {
      const result = validateRange(
        null,
        null,
        null,
        String(CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MAX + 1),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('crownClosure')
    })
  })

  context('validateBALimits', () => {
    it('should return true for missing inputs', () => {
      expect(validateBALimits(null, null, null, null)).to.be.true
      expect(validateBALimits('H', 'CWH', null, '10.0')).to.be.true
    })

    it('should return true for basal area within limit', () => {
      expect(validateBALimits('H', 'CWH', '30', '10')).to.be.true
    })

    it('should return false for basal area exceeding limit', () => {
      expect(validateBALimits('H', 'CWH', '50', '10')).to.be.false
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

    it('should return error message for TPH above maximum', () => {
      const basalArea = '5.0'
      const tph = '4000'
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
      expect(result).to.include('Trees/ha is above a likely maximum')
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
