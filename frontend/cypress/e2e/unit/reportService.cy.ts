/// <reference types="cypress" />

import {
  downloadTextFile,
  downloadCSVFile,
  printReport,
} from '@/services/reportService'
import * as messageHandler from '@/utils/messageHandler'
import { MESSAGE } from '@/constants'

describe('Report Service Unit Tests', () => {
  beforeEach(() => {
    cy.spy(messageHandler, 'logWarningMessage').as('logWarningMessageSpy')
  })

  context('downloadTextFile', () => {
    it('check behavior with valid data', () => {
      const testData = ['line1', 'line2']
      downloadTextFile(testData, 'test.txt')
    })

    it('should log a warning when data is empty', () => {
      downloadTextFile([], 'test.txt')

      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
    })

    it('should log a warning when all data items are empty', () => {
      downloadTextFile(['', ' '], 'test.txt')

      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
    })
  })

  context('downloadCSVFile', () => {
    it('check behavior with valid CSV data', () => {
      const testData = ['header1,header2', 'value1,value2']

      downloadCSVFile(testData, 'test.csv')
    })

    it('should log a warning when CSV data is empty', () => {
      downloadCSVFile([], 'test.csv')

      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
    })

    it('should log a warning when all CSV data items are empty', () => {
      downloadCSVFile(['', ' '], 'test.csv')

      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
    })
  })

  context('printReport', () => {
    it('should log a warning when data is empty', () => {
      printReport([])

      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.PRINT_ERR.NO_DATA,
      )
    })

    it('should log a warning when all data items are empty', () => {
      printReport(['', ' '])

      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.PRINT_ERR.NO_DATA,
      )
    })
  })
})
