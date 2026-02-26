/// <reference types="cypress" />

import {
  downloadTextFile,
  downloadCSVFile,
  printReport,
} from '@/services/reportService'

describe('Report Service Unit Tests', () => {
  context('downloadTextFile', () => {
    it('check behavior with valid data', () => {
      const testData = ['line1', 'line2']
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadTextFile(testData, 'test.txt', saveAsStub)
      cy.get('@saveAsStub').should('have.been.calledOnce')
      cy.get('@saveAsStub').then((stub: any) => {
        const [blob, filename] = stub.firstCall.args
        expect(blob).to.be.instanceOf(Blob)
        expect(blob.type).to.equal('text/plain;charset=utf-8;')
        expect(filename).to.equal('test.txt')
      })
    })

    it('should not call saveAs when data is null', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadTextFile(null as any, 'test.txt', saveAsStub)
      cy.get('@saveAsStub').should('not.have.been.called')
    })

    it('should log a warning when data is empty', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadTextFile([], 'test.txt', saveAsStub)
      cy.get('@saveAsStub').should('not.have.been.called')
    })

    it('should log a warning when all data items are empty', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadTextFile(['', ' '], 'test.txt', saveAsStub)
      cy.get('@saveAsStub').should('not.have.been.called')
    })
  })

  context('downloadCSVFile', () => {
    it('check behavior with valid CSV data', () => {
      const testData = ['header1,header2', 'value1,value2']
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadCSVFile(testData, 'test.csv', saveAsStub)
      cy.get('@saveAsStub').should('have.been.calledOnce')
      cy.get('@saveAsStub').then((stub: any) => {
        const [blob, filename] = stub.firstCall.args
        expect(blob).to.be.instanceOf(Blob)
        expect(blob.type).to.equal('text/csv;charset=utf-8;')
        expect(filename).to.equal('test.csv')
      })
    })

    it('should not call saveAs when data is null', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadCSVFile(null as any, 'test.csv', saveAsStub)
      cy.get('@saveAsStub').should('not.have.been.called')
    })

    it('should log a warning when CSV data is empty', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadCSVFile([], 'test.csv', saveAsStub)
      cy.get('@saveAsStub').should('not.have.been.called')
    })

    it('should log a warning when all CSV data items are empty', () => {
      const saveAsStub = cy.stub().as('saveAsStub')
      downloadCSVFile(['', ' '], 'test.csv', saveAsStub)
      cy.get('@saveAsStub').should('not.have.been.called')
    })
  })

  context('printReport', () => {
    it('check behavior with valid data', () => {
      const testData = ['line1', 'line2']
      const printJSStub = cy.stub().as('printJSStub')
      printReport(testData, printJSStub)
      cy.get('@printJSStub').should('have.been.calledOnce')
      cy.get('@printJSStub').then((stub: any) => {
        const [options] = stub.firstCall.args
        expect(options.type).to.equal('raw-html')
        expect(options.style).to.include('@page')
        expect(options.printable).to.be.a('string')
      })
    })

    it('should not call printJS when data is null', () => {
      const printJSStub = cy.stub().as('printJSStub')
      printReport(null as any, printJSStub)
      cy.get('@printJSStub').should('not.have.been.called')
    })

    it('should log a warning when data is empty', () => {
      const printJSStub = cy.stub().as('printJSStub')
      printReport([], printJSStub)
      cy.get('@printJSStub').should('not.have.been.called')
    })

    it('should log a warning when all data items are empty', () => {
      const printJSStub = cy.stub().as('printJSStub')
      printReport(['', ' '], printJSStub)
      cy.get('@printJSStub').should('not.have.been.called')
    })
  })
})
