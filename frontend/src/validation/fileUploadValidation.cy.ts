/// <reference types="cypress" />

import {
  validateComparison,
  validateRequiredFields,
  validateRange,
  validateFiles,
  validatePolygonHeader,
  validateLayerHeader,
} from '@/validation/fileUploadValidation'
import { CONSTANTS, CSVHEADERS } from '@/constants'

// Helper function to create a File object from content
const createFile = (content: any, fileName: any, mimeType: any) => {
  return new File([content], fileName, { type: mimeType })
}

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
    it('should return false if polygon file is missing', async () => {
      const result = await validateFiles(
        null,
        createFile('', 'layer.csv', 'text/csv'),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('polygonFileMissing')
    })

    it('should return false if layer file is missing', async () => {
      const result = await validateFiles(
        createFile('', 'polygon.csv', 'text/csv'),
        null,
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('layerFileMissing')
    })

    it('should return false if polygon file is not in CSV format', async () => {
      const result = await validateFiles(
        createFile('', 'polygon.txt', 'text/plain'),
        createFile('', 'layer.csv', 'text/csv'),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('polygonFileNotCSVFormat')
    })

    it('should return false if layer file is not in CSV format', async () => {
      const result = await validateFiles(
        createFile('', 'polygon.csv', 'text/csv'),
        createFile('', 'layer.txt', 'text/plain'),
      )
      expect(result.isValid).to.be.false
      expect(result.errorType).to.equal('layerFileNotCSVFormat')
    })

    it('should return true if both files are valid CSV files', async () => {
      const result = await validateFiles(
        createFile('', 'polygon.csv', 'text/csv'),
        createFile('', 'layer.csv', 'text/csv'),
      )
      expect(result.isValid).to.be.true
    })
  })

  context('validatePolygonHeader', () => {
    it('should return true for a valid polygon header', async () => {
      const validHeader = CSVHEADERS.POLYGON_HEADERS.join(',')
      const file = createFile(validHeader, 'polygon.csv', 'text/csv')
      const result = await validatePolygonHeader(file)
      expect(result.isValid).to.be.true
      expect(result.details.missing).to.deep.equal([])
      expect(result.details.extra).to.deep.equal([])
      expect(result.details.mismatches).to.deep.equal([])
    })

    it('should return false for a polygon header with missing columns', async () => {
      const incompleteHeader = CSVHEADERS.POLYGON_HEADERS.slice(1).join(',')
      const file = createFile(incompleteHeader, 'polygon.csv', 'text/csv')
      const result = await validatePolygonHeader(file)
      expect(result.isValid).to.be.false
      expect(result.details.missing).to.include(CSVHEADERS.POLYGON_HEADERS[0])
    })

    it('should return false for a polygon header with extra columns', async () => {
      const extraHeader = `${CSVHEADERS.POLYGON_HEADERS.join(',')},EXTRA_COLUMN`
      const file = createFile(extraHeader, 'polygon.csv', 'text/csv')
      const result = await validatePolygonHeader(file)
      expect(result.isValid).to.be.false
      expect(result.details.extra).to.include('EXTRA_COLUMN')
    })

    it('should return false for a polygon header with mismatched columns', async () => {
      const mismatchedHeader = CSVHEADERS.POLYGON_HEADERS.map((h, i) =>
        i === 0 ? 'WRONG_COLUMN' : h,
      ).join(',')
      const file = createFile(mismatchedHeader, 'polygon.csv', 'text/csv')
      const result = await validatePolygonHeader(file)
      expect(result.isValid).to.be.false
      expect(result.details.mismatches.length).to.be.greaterThan(0)
    })
  })

  context('validateLayerHeader', () => {
    it('should return true for a valid layer header', async () => {
      const validHeader = CSVHEADERS.LAYER_HEADERS.join(',')
      const file = createFile(validHeader, 'layer.csv', 'text/csv')
      const result = await validateLayerHeader(file)
      expect(result.isValid).to.be.true
      expect(result.details.missing).to.deep.equal([])
      expect(result.details.extra).to.deep.equal([])
      expect(result.details.mismatches).to.deep.equal([])
    })

    it('should return false for a layer header with missing columns', async () => {
      const incompleteHeader = CSVHEADERS.LAYER_HEADERS.slice(1).join(',')
      const file = createFile(incompleteHeader, 'layer.csv', 'text/csv')
      const result = await validateLayerHeader(file)
      expect(result.isValid).to.be.false
      expect(result.details.missing).to.include(CSVHEADERS.LAYER_HEADERS[0])
    })

    it('should return false for a layer header with extra columns', async () => {
      const extraHeader = `${CSVHEADERS.LAYER_HEADERS.join(',')},EXTRA_COLUMN`
      const file = createFile(extraHeader, 'layer.csv', 'text/csv')
      const result = await validateLayerHeader(file)
      expect(result.isValid).to.be.false
      expect(result.details.extra).to.include('EXTRA_COLUMN')
    })

    it('should return false for a layer header with mismatched columns', async () => {
      const mismatchedHeader = CSVHEADERS.LAYER_HEADERS.map((h, i) =>
        i === 0 ? 'WRONG_COLUMN' : h,
      ).join(',')
      const file = createFile(mismatchedHeader, 'layer.csv', 'text/csv')
      const result = await validateLayerHeader(file)
      expect(result.isValid).to.be.false
      expect(result.details.mismatches.length).to.be.greaterThan(0)
    })
  })
})
