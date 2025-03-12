/// <reference types="cypress" />

import {
  validateRequiredFields,
  validateRange,
} from '@/validation/siteInfoValidation'
import { CONSTANTS } from '@/constants'

describe('Site Info Validation Unit Tests', () => {
  context('validateRequiredFields', () => {
    it('should return false for null or empty values', () => {
      expect(validateRequiredFields(null).isValid).to.be.false
      expect(validateRequiredFields('').isValid).to.be.false
      expect(validateRequiredFields('0').isValid).to.be.false
    })

    it('should return true for valid values', () => {
      expect(validateRequiredFields('50').isValid).to.be.true
      expect(validateRequiredFields('10').isValid).to.be.true
    })
  })

  context('validateRange', () => {
    it('should return true for valid bha50SiteIndex values within range', () => {
      expect(validateRange('30').isValid).to.be.true
      expect(
        validateRange(
          CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN.toString(),
        ).isValid,
      ).to.be.true
      expect(
        validateRange(
          CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX.toString(),
        ).isValid,
      ).to.be.true
    })

    it('should return false for out of range bha50SiteIndex values', () => {
      expect(
        validateRange(
          (CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN - 1).toString(),
        ).isValid,
      ).to.be.false
      expect(
        validateRange(
          (CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX + 1).toString(),
        ).isValid,
      ).to.be.false
    })

    it('should return true for empty values', () => {
      expect(validateRange(null).isValid).to.be.true
      expect(validateRange('').isValid).to.be.true
    })

    it('should return false for non-numeric values', () => {
      expect(validateRange('abc').isValid).to.be.false
    })
  })
})
