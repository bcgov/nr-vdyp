/// <reference types="cypress" />

import * as apiActions from '@/services/apiActions'
import apiClient from '@/services/apiClient'

describe('apiActions Unit Tests', () => {
  context('helpGet', () => {
    it('should fetch help details successfully', () => {
      const mockResponse = { data: [{ id: 1, name: 'Help Detail' }] }
      cy.stub(apiClient, 'helpGet').resolves(mockResponse)

      cy.wrap(apiActions.helpGet()).then((result) => {
        expect(apiClient.helpGet).to.be.calledOnce
        expect(result).to.deep.equal([{ id: 1, name: 'Help Detail' }])
      })
    })

    it('should handle error when fetching help details', () => {
      const mockError = new Error('Network error')
      cy.stub(apiClient, 'helpGet').rejects(mockError)

      apiActions
        .helpGet()
        .then(() => {
          // assert-fail block
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.helpGet).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('projectionHcsvPost', () => {
    it('should post projection data successfully', () => {
      const mockBlob = new Blob(['test data'], { type: 'application/json' })
      const mockResponse = {
        status: 200,
        headers: { 'content-type': 'application/octet-stream' },
        data: mockBlob,
      }
      const formData = new FormData()
      formData.append('file', new Blob(), 'test.csv')

      cy.stub(apiClient, 'projectionHcsvPost').resolves(mockResponse)

      apiActions
        .projectionHcsvPost(formData, true)
        .then((result: { status: number; headers: any; data: Blob }) => {
          expect(apiClient.projectionHcsvPost).to.be.calledOnceWith(
            formData,
            true,
          )
          expect(result.data).to.equal(mockBlob)
        })
    })

    it('should handle error when posting projection data', () => {
      const mockError = new Error('Server error')
      const formData = new FormData()
      formData.append('file', new Blob(), 'test.csv')

      apiActions
        .projectionHcsvPost(formData, false)
        .then(() => {
          // assert-fail block
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.projectionHcsvPost).to.be.calledOnceWith(
            formData,
            false,
          )
          expect(error).to.equal(mockError)
        })
    })
  })

  context('rootGet', () => {
    it('should fetch root details successfully', () => {
      const mockResponse = {
        data: { name: 'Root Resource', description: 'Details' },
      }
      cy.stub(apiClient, 'rootGet').resolves(mockResponse)

      cy.wrap(apiActions.rootGet()).then((result) => {
        expect(apiClient.rootGet).to.be.calledOnce
        expect(result).to.deep.equal({
          name: 'Root Resource',
          description: 'Details',
        })
      })
    })

    it('should handle error when fetching root details', () => {
      const mockError = new Error('Network error')
      cy.stub(apiClient, 'rootGet').rejects(mockError)

      apiActions
        .rootGet()
        .then(() => {
          // assert-fail block
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.rootGet).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })
})
