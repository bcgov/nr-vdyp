/// <reference types="cypress" />

import * as apiActions from '@/services/apiActions'
import apiClient from '@/services/apiClient'
import sinon from 'sinon'

describe('apiActions Unit Tests', () => {
  let helpGetStub: sinon.SinonStub
  let projectionHcsvPostStub: sinon.SinonStub
  let rootGetStub: sinon.SinonStub

  beforeEach(() => {
    // Stub each apiClient method individually
    helpGetStub = sinon.stub(apiClient, 'helpGet')
    projectionHcsvPostStub = sinon.stub(apiClient, 'projectionHcsvPost')
    rootGetStub = sinon.stub(apiClient, 'rootGet')
  })

  afterEach(() => {
    sinon.restore()
  })

  context('helpGet', () => {
    it('should fetch help details successfully', async () => {
      const mockResponse = [{ id: 1, name: 'Help Detail' }]
      helpGetStub.resolves({ data: mockResponse })

      const result = await apiActions.helpGet()

      expect(helpGetStub.calledOnce).to.be.true
      expect(result).to.deep.equal(mockResponse)
    })

    it('should handle error when fetching help details', async () => {
      const mockError = new Error('Network error')
      helpGetStub.rejects(mockError)

      try {
        await apiActions.helpGet()
      } catch (error) {
        expect(helpGetStub.calledOnce).to.be.true
        expect(error).to.equal(mockError)
      }
    })
  })

  context('projectionHcsvPost', () => {
    it('should post projection data successfully', async () => {
      const mockBlob = new Blob(['test data'], { type: 'application/json' })
      projectionHcsvPostStub.resolves({ data: mockBlob })

      const formData = new FormData()
      formData.append('file', new Blob(), 'test.csv')
      const result = await apiActions.projectionHcsvPost(formData, true)

      expect(projectionHcsvPostStub.calledOnceWith(formData, true)).to.be.true
      expect(result).to.equal(mockBlob)
    })

    it('should handle error when posting projection data', async () => {
      const mockError = new Error('Server error')
      const formData = new FormData()
      formData.append('file', new Blob(), 'test.csv')
      projectionHcsvPostStub.rejects(mockError)

      try {
        await apiActions.projectionHcsvPost(formData, false)
      } catch (error) {
        expect(projectionHcsvPostStub.calledOnceWith(formData, false)).to.be
          .true
        expect(error).to.equal(mockError)
      }
    })
  })

  context('rootGet', () => {
    it('should fetch root details successfully', async () => {
      const mockResponse = { name: 'Root Resource', description: 'Details' }
      rootGetStub.resolves({ data: mockResponse })

      const result = await apiActions.rootGet()

      expect(rootGetStub.calledOnce).to.be.true
      expect(result).to.deep.equal(mockResponse)
    })

    it('should handle error when fetching root details', async () => {
      const mockError = new Error('Network error')
      rootGetStub.rejects(mockError)

      try {
        await apiActions.rootGet()
      } catch (error) {
        expect(rootGetStub.calledOnce).to.be.true
        expect(error).to.equal(mockError)
      }
    })
  })
})
