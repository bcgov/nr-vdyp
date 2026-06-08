/// <reference types="cypress" />

import {
  trimValue,
  isBlank,
  isZeroValue,
  isEmptyOrZero,
  parseNumberOrNull,
  formatDateTimeDisplay,
  formatDateDisplay,
  getStatusIcon,
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
  addExecutionOptionsFromMappings,
  normalizeNum,
  numEq,
  strEq,
} from '@/utils/util'
import JSZip from 'jszip'
import { CONSTANTS } from '@/constants'
import { ExecutionOptionsEnum } from '@/services/vdyp-api'

describe('Util Functions Unit Tests', () => {
  describe('trimValue', () => {
    it('should trim strings and pass through non-string values unchanged', () => {
      expect(trimValue('  hello  ')).to.equal('hello')
      expect(trimValue('')).to.equal('')
      expect(trimValue(123)).to.equal(123)
      expect(trimValue(null)).to.be.null
      expect(trimValue(undefined)).to.be.undefined
    })
  })

  describe('isBlank', () => {
    it('should return true for blank values and false for non-blank', () => {
      expect(isBlank(undefined)).to.be.true
      expect(isBlank(null)).to.be.true
      expect(isBlank(Number.NaN)).to.be.true
      expect(isBlank([])).to.be.true
      expect(isBlank('')).to.be.true
      expect(isBlank('text')).to.be.false
      expect(isBlank(0)).to.be.false
      expect(isBlank(false)).to.be.false
    })
  })

  describe('isZeroValue', () => {
    it('should return true for zero values and false otherwise', () => {
      expect(isZeroValue(0)).to.be.true
      expect(isZeroValue('0')).to.be.true
      expect(isZeroValue(' 0 ')).to.be.true
      expect(isZeroValue('-0')).to.be.true
      expect(isZeroValue(1)).to.be.false
      expect(isZeroValue(null)).to.be.false
      expect(isZeroValue('abc')).to.be.false
    })
  })

  describe('isEmptyOrZero', () => {
    it('should return true for empty or zero values and false otherwise', () => {
      expect(isEmptyOrZero(0)).to.be.true
      expect(isEmptyOrZero('0')).to.be.true
      expect(isEmptyOrZero('')).to.be.true
      expect(isEmptyOrZero(null)).to.be.true
      expect(isEmptyOrZero(1)).to.be.false
      expect(isEmptyOrZero('text')).to.be.false
    })
  })

  describe('parseNumberOrNull', () => {
    it('should parse valid numbers and return null for invalid inputs', () => {
      expect(parseNumberOrNull('123')).to.equal(123)
      expect(parseNumberOrNull(456)).to.equal(456)
      expect(parseNumberOrNull('-5')).to.equal(-5)
      expect(parseNumberOrNull('')).to.be.null
      expect(parseNumberOrNull(null)).to.be.null
      expect(parseNumberOrNull('abc')).to.be.null
    })
  })

  describe('formatDateTimeDisplay', () => {
    it('should format ISO date string to display format', () => {
      expect(formatDateTimeDisplay('2026-01-10T14:30:00')).to.equal('Jan 10 / 14:30')
      expect(formatDateTimeDisplay('2026-12-31T23:59:00')).to.equal('Dec 31 / 23:59')
    })
  })

  describe('formatDateDisplay', () => {
    it('should format ISO date string to short display format', () => {
      expect(formatDateDisplay('2026-01-15T12:00:00')).to.equal('Jan 15')
      expect(formatDateDisplay('2024-02-29T12:00:00')).to.equal('Feb 29')
    })
  })

  describe('getStatusIcon', () => {
    it('should return icon URL for valid statuses and empty string otherwise', () => {
      expect(getStatusIcon('Draft')).to.include('Draft_Icon_Status.png')
      expect(getStatusIcon('Failed')).to.include('Failed_Icon_Status.png')
      expect(getStatusIcon('Unknown')).to.equal('')
      expect(getStatusIcon('')).to.equal('')
    })
  })

  describe('formatUnixTimestampToDate', () => {
    it('should convert a valid timestamp to a Date and return an invalid Date for NaN', () => {
      const date = formatUnixTimestampToDate(1677655800)
      expect(date).to.be.instanceOf(Date)
      expect(date?.getTime()).to.equal(1677655800 * 1000)

      const invalidDate = formatUnixTimestampToDate(Number.NaN)
      expect(invalidDate).to.be.instanceOf(Date)
      expect(Number.isNaN(invalidDate!.getTime())).to.be.true
    })
  })

  describe('delay', () => {
    it('should resolve after the specified delay', (done) => {
      const start = Date.now()
      delay(100).then(() => {
        expect(Date.now() - start).to.be.at.least(100)
        done()
      })
    })
  })

  describe('increaseItemBySpinButton', () => {
    it('should increase the value, clamp to bounds, and reset null/invalid inputs to min', () => {
      expect(increaseItemBySpinButton('10', 20, 0, 5)).to.equal(15)
      expect(increaseItemBySpinButton('25', 20, 0, 5)).to.equal(20)
      expect(increaseItemBySpinButton('-5', 20, 0, 5)).to.equal(0)
      expect(increaseItemBySpinButton(null, 20, 0, 5)).to.equal(0)
    })

    it('should throw for non-positive step', () => {
      expect(() => increaseItemBySpinButton('10', 20, 0, 0)).to.throw('Step must be a positive number')
      expect(() => increaseItemBySpinButton('10', 20, 0, -1)).to.throw('Step must be a positive number')
    })
  })

  describe('decrementItemBySpinButton', () => {
    it('should decrease the value, clamp to bounds, and reset null/invalid inputs to min', () => {
      expect(decrementItemBySpinButton('10', 20, 0, 5)).to.equal(5)
      expect(decrementItemBySpinButton('2', 20, 0, 5)).to.equal(0)
      expect(decrementItemBySpinButton('25', 20, 0, 5)).to.equal(20)
      expect(decrementItemBySpinButton(null, 20, 0, 5)).to.equal(0)
    })

    it('should throw for non-positive step', () => {
      expect(() => decrementItemBySpinButton('10', 20, 0, 0)).to.throw('Step must be a positive number')
      expect(() => decrementItemBySpinButton('10', 20, 0, -1)).to.throw('Step must be a positive number')
    })
  })

  describe('downloadFile', () => {
    it('should trigger a file download', () => {
      const blob = new Blob(['test content'], { type: 'text/plain' })

      cy.stub(URL, 'createObjectURL').as('createObjectURL').returns('mock-url')
      cy.stub(URL, 'revokeObjectURL').as('revokeObjectURL')

      const originalCreateElement = document.createElement.bind(document)
      cy.stub(document, 'createElement').callsFake((tagName: string) => {
        const element = originalCreateElement(tagName)
        if (tagName === 'a') {
          cy.spy(element as HTMLAnchorElement, 'click').as('click')
          cy.spy(element as HTMLAnchorElement, 'remove').as('remove')
        }
        return element
      }).as('createElement')

      cy.spy(document.body, 'appendChild').as('appendChild')

      downloadFile(blob, 'test.txt')

      cy.get('@createObjectURL').should('have.been.calledWith', blob)
      cy.get('@appendChild').should('have.been.called')
      cy.get('@click').should('have.been.called')
      cy.get('@remove').should('have.been.called')
      cy.get('@revokeObjectURL').should('have.been.calledWith', 'mock-url')
    })
  })

  describe('extractZipFileName', () => {
    it('should extract filename from plain object and Headers instance, and return null if absent', () => {
      expect(extractZipFileName({ 'content-disposition': 'attachment; filename="test.zip"' })).to.equal('test.zip')

      const headers = new Headers()
      headers.set('content-disposition', 'attachment; filename="test.zip"')
      expect(extractZipFileName(headers)).to.equal('test.zip')

      expect(extractZipFileName({})).to.be.null
      expect(extractZipFileName({ 'content-disposition': 'attachment' })).to.be.null
    })
  })

  describe('checkZipForErrors', () => {
    it('should return true if Error.txt has content', async () => {
      const zip = new JSZip()
      zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, 'Error content')
      const result = await checkZipForErrors(await zip.generateAsync({ type: 'blob' }))
      expect(result).to.be.true
    })

    it('should return false if Error.txt is missing, empty, or only whitespace/null', async () => {
      const zip = new JSZip()
      expect(await checkZipForErrors(await zip.generateAsync({ type: 'blob' }))).to.be.false

      zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, '')
      expect(await checkZipForErrors(await zip.generateAsync({ type: 'blob' }))).to.be.false

      zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, '  \n  \nnull\n  ')
      expect(await checkZipForErrors(await zip.generateAsync({ type: 'blob' }))).to.be.false
    })
  })

  describe('sanitizeFileName', () => {
    it('should replace special characters with underscore and preserve valid chars', () => {
      expect(sanitizeFileName('file@name#$.zip')).to.equal('file_name.zip')
      expect(sanitizeFileName('valid.file_name-123')).to.equal('valid.file_name-123')
      expect(sanitizeFileName('')).to.equal('')
    })

    it('should collapse consecutive underscores and trim leading/trailing underscores', () => {
      expect(sanitizeFileName('test___file')).to.equal('test_file')
      expect(sanitizeFileName('__test__')).to.equal('test')
      expect(sanitizeFileName('test__file!.zip')).to.equal('test_file.zip')
    })
  })

  describe('convertToNumberSafely', () => {
    it('should convert valid numbers and return null for blank or invalid inputs', () => {
      expect(convertToNumberSafely('123')).to.equal(123)
      expect(convertToNumberSafely(-5.5)).to.equal(-5.5)
      expect(convertToNumberSafely('  10  ')).to.equal(10)
      expect(convertToNumberSafely('')).to.be.null
      expect(convertToNumberSafely(null)).to.be.null
      expect(convertToNumberSafely('abc')).to.be.null
    })
  })

  describe('extractLeadingNumber', () => {
    it('should extract a leading number and return null if absent', () => {
      expect(extractLeadingNumber('123abc')).to.equal(123)
      expect(extractLeadingNumber('-12.5test')).to.equal(-12.5)
      expect(extractLeadingNumber('  15text')).to.equal(15)
      expect(extractLeadingNumber('abc123')).to.be.null
      expect(extractLeadingNumber(null)).to.be.null
    })
  })

  describe('normalizeNum', () => {
    it('should normalize to number and return null for invalid inputs', () => {
      expect(normalizeNum('100.0')).to.equal(100)
      expect(normalizeNum(100)).to.equal(100)
      expect(normalizeNum(null)).to.be.null
      expect(normalizeNum(undefined)).to.be.null
      expect(normalizeNum('')).to.be.null
      expect(normalizeNum(Number.NaN)).to.be.null
    })
  })

  describe('numEq', () => {
    it('should compare numerically, treating null as equal to null only', () => {
      expect(numEq('100.0', 100)).to.be.true
      expect(numEq(null, null)).to.be.true
      expect(numEq('1', null)).to.be.false
      expect(numEq('1', 2)).to.be.false
    })
  })

  describe('strEq', () => {
    it('should compare strings and treat null/undefined as equivalent', () => {
      expect(strEq('AT', 'AT')).to.be.true
      expect(strEq(null, null)).to.be.true
      expect(strEq(undefined, null)).to.be.true
      expect(strEq(null, 'AT')).to.be.false
      expect(strEq('AT', 'BT')).to.be.false
    })
  })

  describe('addExecutionOptionsFromMappings', () => {
    it('should distribute multiple mappings to selected or excluded based on flag', () => {
      const selected: ExecutionOptionsEnum[] = []
      const excluded: ExecutionOptionsEnum[] = []
      addExecutionOptionsFromMappings(selected, excluded, [
        { flag: true, option: ExecutionOptionsEnum.BackGrowEnabled },
        { flag: false, option: ExecutionOptionsEnum.ForwardGrowEnabled },
        { flag: true, option: ExecutionOptionsEnum.DoSaveIntermediateFiles },
      ])
      expect(selected).to.deep.equal([
        ExecutionOptionsEnum.BackGrowEnabled,
        ExecutionOptionsEnum.DoSaveIntermediateFiles,
      ])
      expect(excluded).to.deep.equal([ExecutionOptionsEnum.ForwardGrowEnabled])
    })

    it('should not modify arrays when mappings is empty', () => {
      const selected: ExecutionOptionsEnum[] = []
      const excluded: ExecutionOptionsEnum[] = []
      addExecutionOptionsFromMappings(selected, excluded, [])
      expect(selected).to.be.empty
      expect(excluded).to.be.empty
    })
  })
})
