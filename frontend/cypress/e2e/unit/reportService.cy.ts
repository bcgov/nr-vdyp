/// <reference types="cypress" />

import {
  downloadTextFile,
  downloadCSVFile,
  printReport,
} from '@/services/reportService'
import * as messageHandler from '@/utils/messageHandler'
import { MESSAGE } from '@/constants'
import * as fileSaver from 'file-saver'

describe('Report Service Unit Tests', () => {
  beforeEach(() => {
    cy.spy(messageHandler, 'logWarningMessage')
    cy.stub(fileSaver, 'saveAs').as('saveAsStub')
  })

  context('downloadTextFile', () => {
    it('should call saveAs when valid data is provided', () => {
      const testData = ['line1', 'line2']
      downloadTextFile(testData, 'test.txt')

      cy.get('@saveAsStub').should('have.been.calledOnce')
    })

    it('should log a warning when data is empty', () => {
      downloadTextFile([], 'test.txt')

      cy.wrap(messageHandler.logWarningMessage).should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
    })

    it('should log a warning when all data items are empty', () => {
      downloadTextFile(['', ' '], 'test.txt')

      cy.wrap(messageHandler.logWarningMessage).should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
    })
  })

  context('downloadCSVFile', () => {
    it('should call saveAs when valid CSV data is provided', () => {
      const testData = ['header1,header2', 'value1,value2']
      downloadCSVFile(testData, 'test.csv')

      cy.get('@saveAsStub').should('have.been.calledOnce')
    })

    it('should log a warning when CSV data is empty', () => {
      downloadCSVFile([], 'test.csv')

      cy.wrap(messageHandler.logWarningMessage).should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
    })

    it('should log a warning when all CSV data items are empty', () => {
      downloadCSVFile(['', ' '], 'test.csv')

      cy.wrap(messageHandler.logWarningMessage).should(
        'be.calledWith',
        MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA,
      )
    })
  })

  context('printReport', () => {
    it('should log a warning when data is empty', () => {
      printReport([])

      cy.wrap(messageHandler.logWarningMessage).should(
        'be.calledWith',
        MESSAGE.PRINT_ERR.NO_DATA,
      )
    })

    it('should log a warning when all data items are empty', () => {
      printReport(['', ' '])

      cy.wrap(messageHandler.logWarningMessage).should(
        'be.calledWith',
        MESSAGE.PRINT_ERR.NO_DATA,
      )
    })
  })
})
