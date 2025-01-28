/// <reference types="cypress" />

import * as apiClient from '@/services/apiClient'
import sinon from 'sinon'
import {
  GetHelpApi,
  GetRootApi,
  RunHCSVProjectionApi,
} from '@/services/vdyp-api'

describe('apiClient Unit Tests', () => {
  let helpGetStub: sinon.SinonStub
  let projectionHcsvPostStub: sinon.SinonStub
  let rootGetStub: sinon.SinonStub

  beforeEach(() => {
    // Stub each apiClient method individually
    helpGetStub = sinon.stub(GetHelpApi.prototype, 'helpGet')
    projectionHcsvPostStub = sinon.stub(
      RunHCSVProjectionApi.prototype,
      'projectionHcsvPostForm',
    )
    rootGetStub = sinon.stub(GetRootApi.prototype, 'rootGet')
  })

  afterEach(() => {
    sinon.restore()
  })

  context('helpGet', () => {
    it('should fetch help details successfully', async () => {
      const mockResponse = { data: [{ id: 1, name: 'Help Detail' }] }
      helpGetStub.resolves(mockResponse)

      const result = await apiClient.apiClient.helpGet()

      expect(helpGetStub.calledOnce).to.be.true
      expect(result).to.deep.equal(mockResponse)
    })

    it('should handle error when fetching help details', async () => {
      const mockError = new Error('Network error')
      helpGetStub.rejects(mockError)

      try {
        await apiClient.apiClient.helpGet()
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
      formData.append('polygonInputData', new File([], 'polygon.json'))
      formData.append('layersInputData', new File([], 'layers.json'))
      formData.append('projectionParameters', JSON.stringify({ param: 'test' }))

      const result = await apiClient.apiClient.projectionHcsvPost(
        formData,
        true,
      )

      expect(projectionHcsvPostStub.calledOnce).to.be.true

      expect(result.data).to.be.instanceOf(Blob)
      expect(result.data.size).to.equal(mockBlob.size)
      expect(result.data.type).to.equal(mockBlob.type)

      const resultText = await result.data.text()
      const mockText = await mockBlob.text()
      expect(resultText).to.equal(mockText)
    })
  })

  context('rootGet', () => {
    it('should fetch root details successfully', async () => {
      const mockResponse = {
        data: { name: 'Root Resource', description: 'Details' },
      }
      rootGetStub.resolves(mockResponse)

      const result = await apiClient.apiClient.rootGet()

      expect(rootGetStub.calledOnce).to.be.true
      expect(result).to.deep.equal(mockResponse)
    })

    it('should handle error when fetching root details', async () => {
      const mockError = new Error('Network error')
      rootGetStub.rejects(mockError)

      try {
        await apiClient.apiClient.rootGet()
      } catch (error) {
        expect(rootGetStub.calledOnce).to.be.true
        expect(error).to.equal(mockError)
      }
    })
  })
})
