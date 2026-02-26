/// <reference types="cypress" />

import {
  validateRequiredFields,
  validateRange,
  validatePreConfirmFields,
} from '@/validation/siteInfoValidation'
import { CONSTANTS } from '@/constants'

describe('Site Info Validation Unit Tests', () => {
  context('validateRequiredFields', () => {
    it('should return false when siteSpeciesValues is Computed and any field is empty or zero', () => {
      // All fields empty
      expect(
        validateRequiredFields(
          CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
          null,
          null,
          null,
        ).isValid,
      ).to.be.false

      // spzAge zero, others valid
      expect(
        validateRequiredFields(
          CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
          '0',
          '15.0',
          '20.0',
        ).isValid,
      ).to.be.false

      // spzHeight empty, others valid
      expect(
        validateRequiredFields(
          CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
          '50',
          null,
          '20.0',
        ).isValid,
      ).to.be.false
    })

    it('should return true when siteSpeciesValues is Computed and all fields are valid', () => {
      expect(
        validateRequiredFields(
          CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
          '50',
          '15.0',
          '20.0',
        ).isValid,
      ).to.be.true
    })

    it('should return false when siteSpeciesValues is Supplied and bha50SiteIndex is empty or zero', () => {
      // bha50SiteIndex empty
      expect(
        validateRequiredFields(
          CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED,
          null,
          null,
          null,
        ).isValid,
      ).to.be.false

      // bha50SiteIndex zero
      expect(
        validateRequiredFields(
          CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED,
          null,
          null,
          '0',
        ).isValid,
      ).to.be.false
    })

    it('should return true when siteSpeciesValues is Supplied and bha50SiteIndex is valid', () => {
      expect(
        validateRequiredFields(
          CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED,
          null,
          null,
          '20.0',
        ).isValid,
      ).to.be.true
    })

    it('should return true for unknown siteSpeciesValues', () => {
      expect(validateRequiredFields('Unknown', null, null, null).isValid).to.be
        .true
    })
  })

  context('validateRange', () => {
    it('should return true for empty values', () => {
      expect(validateRange(null, null, null).isValid).to.be.true
    })

    it('should validate spzAge range correctly', () => {
      // Valid range
      expect(
        validateRange(
          String(CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MIN),
          null,
          null,
        ).isValid,
      ).to.be.true
      expect(
        validateRange(
          String(CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MAX),
          null,
          null,
        ).isValid,
      ).to.be.true

      // Out of range
      const belowMinResult = validateRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MIN - 1),
        null,
        null,
      )
      expect(belowMinResult.isValid).to.be.false
      expect(belowMinResult.errorType).to.equal('spzAge')

      const aboveMaxResult = validateRange(
        String(CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MAX + 1),
        null,
        null,
      )
      expect(aboveMaxResult.isValid).to.be.false
      expect(aboveMaxResult.errorType).to.equal('spzAge')

      // Non-numeric
      const nonNumericResult = validateRange('abc', null, null)
      expect(nonNumericResult.isValid).to.be.false
      expect(nonNumericResult.errorType).to.equal('spzAge')
    })

    it('should validate spzHeight range correctly', () => {
      // Valid range
      expect(
        validateRange(
          null,
          CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MIN.toString(),
          null,
        ).isValid,
      ).to.be.true
      expect(
        validateRange(
          null,
          CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MAX.toString(),
          null,
        ).isValid,
      ).to.be.true

      // Out of range
      const belowMinResult = validateRange(
        null,
        (CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MIN - 0.1).toString(),
        null,
      )
      expect(belowMinResult.isValid).to.be.false
      expect(belowMinResult.errorType).to.equal('spzHeight')

      const aboveMaxResult = validateRange(
        null,
        (CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MAX + 0.1).toString(),
        null,
      )
      expect(aboveMaxResult.isValid).to.be.false
      expect(aboveMaxResult.errorType).to.equal('spzHeight')

      // Non-numeric
      const nonNumericResult = validateRange(null, 'abc', null)
      expect(nonNumericResult.isValid).to.be.false
      expect(nonNumericResult.errorType).to.equal('spzHeight')
    })

    it('should validate bha50SiteIndex range correctly', () => {
      // Valid range
      expect(
        validateRange(
          null,
          null,
          CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN.toString(),
        ).isValid,
      ).to.be.true
      expect(
        validateRange(
          null,
          null,
          CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX.toString(),
        ).isValid,
      ).to.be.true

      // Out of range
      const belowMinResult = validateRange(
        null,
        null,
        (CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN - 0.1).toString(),
      )
      expect(belowMinResult.isValid).to.be.false
      expect(belowMinResult.errorType).to.equal('bha50SiteIndex')

      const aboveMaxResult = validateRange(
        null,
        null,
        (CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX + 0.1).toString(),
      )
      expect(aboveMaxResult.isValid).to.be.false
      expect(aboveMaxResult.errorType).to.equal('bha50SiteIndex')

      // Non-numeric
      const nonNumericResult = validateRange(null, null, 'abc')
      expect(nonNumericResult.isValid).to.be.false
      expect(nonNumericResult.errorType).to.equal('bha50SiteIndex')
    })
  })

  context('validatePreConfirmFields', () => {
    it('should return false with errorType siteIndex when siteSpeciesValues is not Supplied or Computed', () => {
      const nullResult = validatePreConfirmFields(null, 'SBS')
      expect(nullResult.isValid).to.be.false
      expect(nullResult.errorType).to.equal('siteIndex')

      const unknownResult = validatePreConfirmFields('Unknown', 'SBS')
      expect(unknownResult.isValid).to.be.false
      expect(unknownResult.errorType).to.equal('siteIndex')
    })

    it('should return false with errorType becZone when siteSpeciesValues is valid but becZone is null or empty', () => {
      const nullBecResult = validatePreConfirmFields(
        CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED,
        null,
      )
      expect(nullBecResult.isValid).to.be.false
      expect(nullBecResult.errorType).to.equal('becZone')

      const emptyBecResult = validatePreConfirmFields(
        CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
        '',
      )
      expect(emptyBecResult.isValid).to.be.false
      expect(emptyBecResult.errorType).to.equal('becZone')
    })

    it('should return true when siteSpeciesValues and becZone are both valid', () => {
      expect(
        validatePreConfirmFields(CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED, 'SBS')
          .isValid,
      ).to.be.true
      expect(
        validatePreConfirmFields(CONSTANTS.SITE_SPECIES_VALUES.COMPUTED, 'CWH')
          .isValid,
      ).to.be.true
    })
  })
})
