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
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadTextFile(testData, 'test.txt', saveAsStub)
      cy.get('@saveAsStub').should('have.been.calledOnce')
    })

    it('should log a warning when data is empty', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadTextFile([], 'test.txt', saveAsStub)
      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
      cy.get('@saveAsStub').should('not.have.been.called')
    })

    it('should log a warning when all data items are empty', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadTextFile(['', ' '], 'test.txt', saveAsStub)
      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
      cy.get('@saveAsStub').should('not.have.been.called')
    })
  })

  context('downloadCSVFile', () => {
    it('check behavior with valid CSV data', () => {
      const testData = ['header1,header2', 'value1,value2']
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadCSVFile(testData, 'test.csv', saveAsStub)
      cy.get('@saveAsStub').should('have.been.calledOnce')
    })

    it('should log a warning when CSV data is empty', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadCSVFile([], 'test.csv', saveAsStub)
      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
      cy.get('@saveAsStub').should('not.have.been.called')
    })

    it('should log a warning when all CSV data items are empty', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadCSVFile(['', ' '], 'test.csv', saveAsStub)
      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
      cy.get('@saveAsStub').should('not.have.been.called')
    })
  })

  context('printReport', () => {
    it('check behavior with valid data', () => {
      const testData = ['line1', 'line2']
      const printJSStub = cy.stub().as('printJSStub')
      printReport(testData, printJSStub)
      cy.get('@printJSStub').should('have.been.calledOnce')
    })

    it('should log a warning when data is empty', () => {
      const printJSStub = cy.stub().as('printJSStub')
      printReport([], printJSStub)
      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.PRINT_ERR.NO_DATA,
      )
      cy.get('@printJSStub').should('not.have.been.called')
    })

    it('should log a warning when all data items are empty', () => {
      const printJSStub = cy.stub().as('printJSStub')
      printReport(['', ' '], printJSStub)
      cy.get('@logWarningMessageSpy').should(
        'be.calledWith',
        MESSAGE.PRINT_ERR.NO_DATA,
      )
      cy.get('@printJSStub').should('not.have.been.called')
    })
  })
})
