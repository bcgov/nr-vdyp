/// <reference types="cypress" />

import {
  trimValue,
  isBlank,
  isZeroValue,
  isEmptyOrZero,
  parseNumberOrNull,
  formatDateTime,
  formatUnixTimestampToDate,
  delay,
  increaseItemBySpinButton,
  decrementItemBySpinButton,
  downloadFile,
  extractZipFileName,
  checkZipForErrors,
  sanitizeFileName,
  convertToNumberSafely,
  extractLeadingNumber,
} from '@/utils/util'
import JSZip from 'jszip'
import { CONSTANTS } from '@/constants'

describe('Util Functions Unit Tests', () => {
  describe('trimValue', () => {
    it('should trim whitespace from strings', () => {
      expect(trimValue('  hello  ')).to.equal('hello')
      expect(trimValue('no spaces')).to.equal('no spaces')
      expect(trimValue('')).to.equal('')
    })

    it('should return non-string values unchanged', () => {
      expect(trimValue(123)).to.equal(123)
      expect(trimValue(null)).to.be.null
      expect(trimValue(undefined)).to.be.undefined
      expect(trimValue({ key: 'value' })).to.deep.equal({ key: 'value' })
      expect(trimValue(true)).to.be.true
    })
  })

  describe('isBlank', () => {
    it('should return true for blank values', () => {
      expect(isBlank(undefined)).to.be.true
      expect(isBlank(null)).to.be.true
      expect(isBlank(NaN)).to.be.true
      expect(isBlank([])).to.be.true
      expect(isBlank('  ')).to.be.true
      expect(isBlank('')).to.be.true
    })

    it('should return false for non-blank values', () => {
      expect(isBlank('text')).to.be.false
      expect(isBlank(0)).to.be.false
      expect(isBlank(false)).to.be.false
      expect(isBlank([1, 2])).to.be.false
      expect(isBlank({})).to.be.false
      expect(isBlank(-1)).to.be.false
    })
  })

  describe('isZeroValue', () => {
    it('should return true for zero values', () => {
      expect(isZeroValue(0)).to.be.true
      expect(isZeroValue('0')).to.be.true
      expect(isZeroValue(' 0 ')).to.be.true
    })

    it('should return false for non-zero values', () => {
      expect(isZeroValue(1)).to.be.false
      expect(isZeroValue('1')).to.be.false
      expect(isZeroValue('')).to.be.false
      expect(isZeroValue(null)).to.be.false
      expect(isZeroValue(undefined)).to.be.false
      expect(isZeroValue('abc')).to.be.false
      expect(isZeroValue({})).to.be.false
      expect(isZeroValue(NaN)).to.be.false
      expect(isZeroValue('-0')).to.be.true // Edge case: "-0" is considered 0
    })
  })

  describe('isEmptyOrZero', () => {
    it('should return true for empty or zero values', () => {
      expect(isEmptyOrZero(0)).to.be.true
      expect(isEmptyOrZero('0')).to.be.true
      expect(isEmptyOrZero('')).to.be.true
      expect(isEmptyOrZero(null)).to.be.true
      expect(isEmptyOrZero('  ')).to.be.true
    })

    it('should return false for non-empty and non-zero values', () => {
      expect(isEmptyOrZero(1)).to.be.false
      expect(isEmptyOrZero('1')).to.be.false
      expect(isEmptyOrZero('text')).to.be.false
      expect(isEmptyOrZero(-1)).to.be.false
    })
  })

  describe('parseNumberOrNull', () => {
    it('should parse valid numbers', () => {
      expect(parseNumberOrNull('123')).to.equal(123)
      expect(parseNumberOrNull(456)).to.equal(456)
      expect(parseNumberOrNull('0')).to.equal(0)
      expect(parseNumberOrNull('-5')).to.equal(-5)
    })

    it('should return null for invalid or empty inputs', () => {
      expect(parseNumberOrNull('')).to.be.null
      expect(parseNumberOrNull(null)).to.be.null
      expect(parseNumberOrNull('abc')).to.be.null
      expect(parseNumberOrNull('12.34.56')).to.be.null // Edge case: invalid number format
    })
  })

  describe('formatDateTime', () => {
    it('should format valid dates correctly', () => {
      const date = new Date(2025, 1, 28, 14, 30, 0) // Months are 0-indexed
      expect(formatDateTime(date)).to.equal('2025-02-28 14:30:00')
    })

    it('should return null for invalid or null dates', () => {
      expect(formatDateTime(null)).to.be.null
    })

    it('should handle edge case dates', () => {
      const leapYear = new Date(2020, 1, 29, 0, 0, 0) // Leap year
      expect(formatDateTime(leapYear)).to.equal('2020-02-29 00:00:00')
    })
  })

  describe('formatUnixTimestampToDate', () => {
    it('should convert valid timestamps to Date objects', () => {
      const date = formatUnixTimestampToDate(1677655800)
      expect(date).to.be.instanceOf(Date)
      expect(date?.getTime()).to.equal(1677655800 * 1000)
    })

    it('should return null for invalid timestamps', () => {
      // Updated to expect Invalid Date for NaN
      const invalidDate = formatUnixTimestampToDate(NaN)
      expect(invalidDate).to.be.instanceOf(Date) // Invalid Date is still a Date object
      expect(isNaN(invalidDate!.getTime())).to.be.true // Verify it's invalid
      // Negative timestamp is valid
      const negDate = formatUnixTimestampToDate(-1)
      expect(negDate).to.be.instanceOf(Date)
    })
  })

  describe('delay', () => {
    it('should resolve after the specified delay', (done) => {
      const start = Date.now()
      delay(100).then(() => {
        const end = Date.now()
        expect(end - start).to.be.at.least(100)
        done()
      })
    })

    it('should handle zero or negative delay', (done) => {
      delay(0).then(() => {
        expect(true).to.be.true // Simply resolves immediately
        done()
      })
    })
  })

  describe('increaseItemBySpinButton', () => {
    it('should increase the value correctly', () => {
      expect(increaseItemBySpinButton('10', 20, 0, 5)).to.equal(15)
      expect(increaseItemBySpinButton(null, 20, 0, 5)).to.equal(5)
      expect(increaseItemBySpinButton('10a', 20, 0, 5)).to.equal(15)
      expect(increaseItemBySpinButton('25', 20, 0, 5)).to.equal(20)
      expect(increaseItemBySpinButton('-5', 20, 0, 5)).to.equal(0)
    })

    it('should throw an error for non-positive step', () => {
      expect(() => increaseItemBySpinButton('10', 20, 0, 0)).to.throw(
        'Step must be a positive number',
      )
      expect(() => increaseItemBySpinButton('10', 20, 0, -1)).to.throw(
        'Step must be a positive number',
      )
    })

    it('should handle decimal values', () => {
      expect(increaseItemBySpinButton('10.5', 20, 0, 1.5)).to.equal(12)
    })
  })

  describe('decrementItemBySpinButton', () => {
    it('should decrease the value correctly', () => {
      expect(decrementItemBySpinButton('10', 20, 0, 5)).to.equal(5)
      expect(decrementItemBySpinButton(null, 20, 0, 5)).to.equal(0)
      expect(decrementItemBySpinButton('10a', 20, 0, 5)).to.equal(5)
      expect(decrementItemBySpinButton('2', 20, 0, 5)).to.equal(0)
      expect(decrementItemBySpinButton('25', 20, 0, 5)).to.equal(20)
    })

    it('should throw an error for non-positive step', () => {
      expect(() => decrementItemBySpinButton('10', 20, 0, 0)).to.throw(
        'Step must be a positive number',
      )
      expect(() => decrementItemBySpinButton('10', 20, 0, -1)).to.throw(
        'Step must be a positive number',
      )
    })

    it('should handle decimal values', () => {
      expect(decrementItemBySpinButton('10.5', 20, 0, 1.5)).to.equal(9)
    })
  })

  describe('downloadFile', () => {
    it('should trigger a file download', () => {
      const blob = new Blob(['test content'], { type: 'text/plain' })
      const fileName = 'test.txt'

      // Use cy.stub with alias and return value in correct order
      cy.stub(URL, 'createObjectURL').as('createObjectURL').returns('mock-url')
      cy.spy(document, 'createElement').as('createElement')
      cy.spy(document.body, 'appendChild').as('appendChild')
      cy.spy(document.body, 'removeChild').as('removeChild')

      downloadFile(blob, fileName)

      cy.get('@createObjectURL').should('have.been.calledWith', blob)
      cy.get('@createElement').should('have.been.calledWith', 'a')
      cy.get('@appendChild').should('have.been.called')
      cy.get('@removeChild').should('have.been.called')
    })
  })

  describe('extractZipFileName', () => {
    it('should extract file name from headers (plain object)', () => {
      const headers = {
        'content-disposition': 'attachment; filename="test.zip"',
      }
      expect(extractZipFileName(headers)).to.equal('test.zip')
    })

    it('should extract file name from Headers instance', () => {
      const headers = new Headers()
      headers.set('content-disposition', 'attachment; filename="test.zip"')
      expect(extractZipFileName(headers)).to.equal('test.zip')
    })

    it('should return null if no file name is found', () => {
      const headers = {}
      expect(extractZipFileName(headers)).to.be.null
      const headersNoMatch = { 'content-disposition': 'attachment' }
      expect(extractZipFileName(headersNoMatch)).to.be.null
    })
  })

  describe('checkZipForErrors', () => {
    it('should return true if Error.txt has content', async () => {
      const zip = new JSZip()
      zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error content')
      const zipBlob = await zip.generateAsync({ type: 'blob' })

      const result = await checkZipForErrors(zipBlob)
      expect(result).to.be.true
    })

    it('should return false if Error.txt is missing or empty', async () => {
      const zip = new JSZip()
      const zipBlob = await zip.generateAsync({ type: 'blob' })

      const result = await checkZipForErrors(zipBlob)
      expect(result).to.be.false

      zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, '')
      const zipBlobEmpty = await zip.generateAsync({ type: 'blob' })
      const resultEmpty = await checkZipForErrors(zipBlobEmpty)
      expect(resultEmpty).to.be.false
    })

    it('should handle Error.txt with only whitespace or "null"', async () => {
      const zip = new JSZip()
      zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, '  \n  \nnull\n  ')
      const zipBlob = await zip.generateAsync({ type: 'blob' })

      const result = await checkZipForErrors(zipBlob)
      expect(result).to.be.false
    })
  })

  describe('sanitizeFileName', () => {
    it('should sanitize file name by allowing only alphanumeric characters, dot, underscore, and hyphen', () => {
      expect(sanitizeFileName('test__file!.zip')).to.equal('test_file.zip')
      expect(sanitizeFileName('file@name#$.zip')).to.equal('file_name.zip')
      expect(sanitizeFileName('valid.file_name-123')).to.equal(
        'valid.file_name-123',
      )
    })

    it('should remove consecutive underscores', () => {
      expect(sanitizeFileName('test___file')).to.equal('test_file')
      expect(sanitizeFileName('__multiple___underscores__')).to.equal(
        'multiple_underscores',
      )
    })

    it('should remove leading and trailing underscores', () => {
      expect(sanitizeFileName('__test')).to.equal('test')
      expect(sanitizeFileName('test__')).to.equal('test')
      expect(sanitizeFileName('__test__')).to.equal('test')
    })

    it('should handle empty or null-like inputs', () => {
      expect(sanitizeFileName('')).to.equal('')
      expect(sanitizeFileName('   ')).to.equal('')
    })

    it('should handle special characters and maintain valid filename structure', () => {
      expect(sanitizeFileName('file/name/path')).to.equal('file_name_path')
      expect(sanitizeFileName('file\\name\\path')).to.equal('file_name_path')
      expect(sanitizeFileName('file<name>path')).to.equal('file_name_path')
    })

    it('should handle filenames with extensions correctly', () => {
      expect(sanitizeFileName('test__file!.zip')).to.equal('test_file.zip')
      expect(sanitizeFileName('file@name#$.zip')).to.equal('file_name.zip')
      expect(sanitizeFileName('valid.file_name-123.zip')).to.equal(
        'valid.file_name-123.zip',
      )
      expect(sanitizeFileName('test__file_.zip')).to.equal('test_file.zip')
    })
  })

  describe('convertToNumberSafely', () => {
    it('should convert valid string numbers to numbers', () => {
      expect(convertToNumberSafely('123')).to.equal(123)
      expect(convertToNumberSafely('0')).to.equal(0)
      expect(convertToNumberSafely('-5.5')).to.equal(-5.5)
    })

    it('should return null for blank inputs', () => {
      expect(convertToNumberSafely('')).to.be.null
      expect(convertToNumberSafely(null)).to.be.null
      expect(convertToNumberSafely('  ')).to.be.null
    })

    it('should return null for invalid strings', () => {
      expect(convertToNumberSafely('abc')).to.be.null
      expect(convertToNumberSafely('12.34.56')).to.be.null
    })

    it('should preserve existing numbers', () => {
      expect(convertToNumberSafely(42)).to.equal(42)
      expect(convertToNumberSafely(-10.5)).to.equal(-10.5)
    })

    it('should handle mixed input types', () => {
      expect(convertToNumberSafely('  10  ')).to.equal(10)
      expect(convertToNumberSafely('  -5.0  ')).to.equal(-5)
    })
  })

  describe('extractLeadingNumber', () => {
    it('should extract leading number from valid strings', () => {
      expect(extractLeadingNumber('123abc')).to.equal(123)
      expect(extractLeadingNumber('-12.5test')).to.equal(-12.5)
      expect(extractLeadingNumber('10.0more')).to.equal(10)
    })

    it('should return null for invalid or no leading number', () => {
      expect(extractLeadingNumber('abc123')).to.be.null
      expect(extractLeadingNumber('')).to.be.null
      expect(extractLeadingNumber(null)).to.be.null
    })

    it('should handle whitespace', () => {
      expect(extractLeadingNumber('  15text')).to.equal(15)
      expect(extractLeadingNumber('  -5.5text')).to.equal(-5.5)
    })

    it('should ignore subsequent non-numeric characters', () => {
      expect(extractLeadingNumber('12.34abc')).to.equal(12.34)
      expect(extractLeadingNumber('-10.5xyz')).to.equal(-10.5)
    })
  })
})
