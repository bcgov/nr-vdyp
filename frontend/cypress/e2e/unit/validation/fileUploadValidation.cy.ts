/// <reference types="cypress" />

import {
  validateComparison,
  validateRequiredFields,
  validateRange,
  validateFiles,
} from '@/validation/fileUploadValidation'
import { CONSTANTS } from '@/constants'

describe('File Upload Validation Unit Tests', () => {
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

  context('validateRequiredFields', () => {
    it('should return true when all fields are provided', () => {
      expect(validateRequiredFields(10, 20, 5).isValid).to.be.true
    })

    it('should return false when any field is null', () => {
      expect(validateRequiredFields(null, 20, 5).isValid).to.be.false
      expect(validateRequiredFields(10, null, 5).isValid).to.be.false
      expect(validateRequiredFields(10, 20, null).isValid).to.be.false
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
  })

  context('validateFiles', () => {
    it('should return false if layer file is missing', async () => {
      const result = await validateFiles(
        null,
        new File([''], 'polygon.csv', { type: 'text/csv' }),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('layerFileMissing')
    })

    it('should return false if polygon file is missing', async () => {
      const result = await validateFiles(
        new File([''], 'layer.csv', { type: 'text/csv' }),
        null,
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('polygonFileMissing')
    })

    it('should return false if layer file is not in CSV format', async () => {
      const result = await validateFiles(
        new File([''], 'layer.txt', { type: 'text/plain' }),
        new File([''], 'polygon.csv', { type: 'text/csv' }),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('layerFileNotCSVFormat')
    })

    it('should return false if polygon file is not in CSV format', async () => {
      const result = await validateFiles(
        new File([''], 'layer.csv', { type: 'text/csv' }),
        new File([''], 'polygon.txt', { type: 'text/plain' }),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('polygonFileNotCSVFormat')
    })

    it('should return true if both files are valid CSV files', async () => {
      const result = await validateFiles(
        new File([''], 'layer.csv', { type: 'text/csv' }),
        new File([''], 'polygon.csv', { type: 'text/csv' }),
      )
      expect(result.isValid).to.be.true
    })
  })
})
