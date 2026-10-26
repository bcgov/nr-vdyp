/// <reference types="cypress" />

import { assert } from 'chai'
import {
  trimValue,
  isBlank,
  isZeroValue,
  isEmptyOrZero,
  parseNumberOrNull,
  getDateTimeParts,
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
      assert.isTrue(isBlank(undefined))
      assert.isTrue(isBlank(null))
      assert.isTrue(isBlank(Number.NaN))
      assert.isTrue(isBlank([]))
      assert.isTrue(isBlank(''))
      assert.isFalse(isBlank('text'))
      assert.isFalse(isBlank(0))
      assert.isFalse(isBlank(false))
    })
  })

  describe('isZeroValue', () => {
    it('should return true for zero values and false otherwise', () => {
      assert.isTrue(isZeroValue(0))
      assert.isTrue(isZeroValue('0'))
      assert.isTrue(isZeroValue(' 0 '))
      assert.isTrue(isZeroValue('-0'))
      assert.isFalse(isZeroValue(1))
      assert.isFalse(isZeroValue(null))
      assert.isFalse(isZeroValue('abc'))
    })
  })

  describe('isEmptyOrZero', () => {
    it('should return true for empty or zero values and false otherwise', () => {
      assert.isTrue(isEmptyOrZero(0))
      assert.isTrue(isEmptyOrZero('0'))
      assert.isTrue(isEmptyOrZero(''))
      assert.isTrue(isEmptyOrZero(null))
      assert.isFalse(isEmptyOrZero(1))
      assert.isFalse(isEmptyOrZero('text'))
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

  // Expected values for BC follow the browser's tzdata. BC's permanent Pacific Time (UTC-7 from
  // 2026-11-01) needs tzdata 2026b or later, which is why these specs run in Chrome, not Electron.
  const BC_TIME_ZONE = 'America/Vancouver'

  describe('getDateTimeParts', () => {
    it('should split a UTC timestamp into local parts', () => {
      expect(getDateTimeParts('2026-01-10T22:30:00Z', BC_TIME_ZONE)).to.deep.equal({
        month: 'Jan',
        day: '10',
        hour: '14',
        minute: '30',
      })
    })

    it('should use 00 for the midnight hour', () => {
      expect(getDateTimeParts('2026-01-10T08:05:00Z', BC_TIME_ZONE)!.hour).to.equal('00')
    })

    it('should return null for an invalid timestamp', () => {
      expect(getDateTimeParts('')).to.be.null
      expect(getDateTimeParts('not a date')).to.be.null
    })
  })

  describe('formatDateTimeDisplay', () => {
    it('should format a UTC timestamp in the given time zone', () => {
      expect(formatDateTimeDisplay('2026-07-10T21:30:00Z', 'UTC')).to.equal('Jul 10 / 21:30')
      expect(formatDateTimeDisplay('2026-01-10T22:30:00Z', BC_TIME_ZONE)).to.equal('Jan 10 / 14:30')
    })

    it('should use the browser time zone when no time zone is given', () => {
      const browserTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone
      const timestamp = '2026-07-10T21:30:00Z'
      expect(formatDateTimeDisplay(timestamp)).to.equal(formatDateTimeDisplay(timestamp, browserTimeZone))
    })

    it('should show historical BC timestamps with the rules of their date', () => {
      // Winter 2025-2026: PST (UTC-8)
      expect(formatDateTimeDisplay('2025-12-15T20:00:00Z', BC_TIME_ZONE)).to.equal('Dec 15 / 12:00')
      // Summer 2026: PDT (UTC-7)
      expect(formatDateTimeDisplay('2026-07-15T20:00:00Z', BC_TIME_ZONE)).to.equal('Jul 15 / 13:00')
    })

    it('should show the last BC fall-back transition in November 2025', () => {
      // 2025-11-02 02:00 PDT fell back to 01:00 PST
      expect(formatDateTimeDisplay('2025-11-02T08:30:00Z', BC_TIME_ZONE)).to.equal('Nov 02 / 01:30')
      expect(formatDateTimeDisplay('2025-11-02T09:30:00Z', BC_TIME_ZONE)).to.equal('Nov 02 / 01:30')
      expect(formatDateTimeDisplay('2025-11-02T10:30:00Z', BC_TIME_ZONE)).to.equal('Nov 02 / 02:30')
    })

    it('should not fall back one hour in BC after November 2026', () => {
      // No fall-back on 2026-11-01: BC stays at UTC-7
      expect(formatDateTimeDisplay('2026-11-01T08:30:00Z', BC_TIME_ZONE)).to.equal('Nov 01 / 01:30')
      expect(formatDateTimeDisplay('2026-11-01T09:30:00Z', BC_TIME_ZONE)).to.equal('Nov 01 / 02:30')
      expect(formatDateTimeDisplay('2026-11-15T20:00:00Z', BC_TIME_ZONE)).to.equal('Nov 15 / 13:00')
      expect(formatDateTimeDisplay('2027-01-15T20:00:00Z', BC_TIME_ZONE)).to.equal('Jan 15 / 13:00')
      expect(formatDateTimeDisplay('2027-07-15T20:00:00Z', BC_TIME_ZONE)).to.equal('Jul 15 / 13:00')
    })

    it('should return an empty string for an invalid timestamp', () => {
      expect(formatDateTimeDisplay('')).to.equal('')
    })
  })

  describe('formatDateDisplay', () => {
    it('should format a UTC timestamp to a short date in the given time zone', () => {
      expect(formatDateDisplay('2026-01-15T22:30:00Z', BC_TIME_ZONE)).to.equal('Jan 15')
      expect(formatDateDisplay('2024-02-29T20:00:00Z', BC_TIME_ZONE)).to.equal('Feb 29')
    })

    it('should use the local date when it differs from the UTC date', () => {
      expect(formatDateDisplay('2026-11-16T06:30:00Z', 'UTC')).to.equal('Nov 16')
      expect(formatDateDisplay('2026-11-16T06:30:00Z', BC_TIME_ZONE)).to.equal('Nov 15')
    })

    it('should return an empty string for an invalid timestamp', () => {
      expect(formatDateDisplay('')).to.equal('')
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
      cy.stub(document, 'createElement').as('createElement').callsFake((tagName: string) => {
        const element = originalCreateElement(tagName)
        if (tagName === 'a') {
          // Stub instead of spy so the browser does not actually download the file
          cy.stub(element as HTMLAnchorElement, 'click').as('click')
          cy.spy(element as HTMLAnchorElement, 'remove').as('remove')
        }
        return element
      })

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
      assert.isTrue(result)
    })

    it('should return false if Error.txt is missing, empty, or only whitespace/null', async () => {
      const zip = new JSZip()
      assert.isFalse(await checkZipForErrors(await zip.generateAsync({ type: 'blob' })))

      zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, '')
      assert.isFalse(await checkZipForErrors(await zip.generateAsync({ type: 'blob' })))

      zip.file(CONSTANTS.FILE_NAME.ERROR_TXT, '  \n  \nnull\n  ')
      assert.isFalse(await checkZipForErrors(await zip.generateAsync({ type: 'blob' })))
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
      assert.isTrue(numEq('100.0', 100))
      assert.isTrue(numEq(null, null))
      assert.isFalse(numEq('1', null))
      assert.isFalse(numEq('1', 2))
    })
  })

  describe('strEq', () => {
    it('should compare strings and treat null/undefined as equivalent', () => {
      assert.isTrue(strEq('AT', 'AT'))
      assert.isTrue(strEq(null, null))
      assert.isTrue(strEq(undefined, null))
      assert.isFalse(strEq(null, 'AT'))
      assert.isFalse(strEq('AT', 'BT'))
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
      assert.isEmpty(selected)
      assert.isEmpty(excluded)
    })
  })
})
