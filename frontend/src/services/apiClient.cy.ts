/// <reference types="cypress" />

import { createPinia, setActivePinia } from 'pinia'
import apiClient from '@/services/apiClient'
import {
  GetHelpApi,
  GetRootApi,
  ProjectionApi,
  RunHCSVProjectionApi,
  ParameterNamesEnum,
} from '@/services/vdyp-api'

describe('apiClient Unit Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  context('helpGet', () => {
    it('should fetch help details successfully', () => {
      const mockResponse = { data: [{ id: 1, name: 'Help Detail' }] }
      cy.stub(GetHelpApi.prototype, 'helpGet').resolves(mockResponse)

      cy.wrap(apiClient.helpGet()).then((result) => {
        expect(GetHelpApi.prototype.helpGet).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when fetching help details', () => {
      const mockError = new Error('Network error')
      cy.stub(GetHelpApi.prototype, 'helpGet').rejects(mockError)

      apiClient.helpGet().then(
        () => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        },
        (error) => {
          expect(GetHelpApi.prototype.helpGet).to.be.calledOnce
          expect(error).to.equal(mockError)
        },
      )
    })
  })

  context('projectionHcsvPost', () => {
    it('should post projection data successfully', () => {
      const mockBlob = new Blob(['test data'], { type: 'application/json' })
      const mockResponse = { data: mockBlob }
      cy.stub(
        RunHCSVProjectionApi.prototype,
        'projectionHcsvPostForm',
      ).resolves(mockResponse)

      const formData = new FormData()
      formData.append(
        ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA,
        new File([], 'polygon.json'),
      )
      formData.append(
        ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA,
        new File([], 'layers.json'),
      )
      formData.append(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
        JSON.stringify({ param: 'test' }),
      )

      apiClient.projectionHcsvPost(formData, true).then((result) => {
        expect(RunHCSVProjectionApi.prototype.projectionHcsvPostForm).to.be
          .calledOnce
        expect(result.data).to.be.instanceOf(Blob)
        expect(result.data.size).to.equal(mockBlob.size)
        expect(result.data.type).to.equal(mockBlob.type)
      })
    })

    it('should handle error when posting projection data', () => {
      const mockError = new Error('Projection failed')
      const formData = new FormData()
      formData.append(
        ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA,
        new File([], 'polygon.json'),
      )
      formData.append(
        ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA,
        new File([], 'layers.json'),
      )
      formData.append(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
        JSON.stringify({ param: 'test' }),
      )

      cy.stub(RunHCSVProjectionApi.prototype, 'projectionHcsvPostForm').rejects(
        mockError,
      )

      apiClient
        .projectionHcsvPost(formData, false)
        .then(() => {
          // assert-fail block
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(RunHCSVProjectionApi.prototype.projectionHcsvPostForm).to.be
            .calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('rootGet', () => {
    it('should fetch root details successfully', () => {
      const mockResponse = {
        data: { name: 'Root Resource', description: 'Details' },
      }
      cy.stub(GetRootApi.prototype, 'rootGet').resolves(mockResponse)

      cy.wrap(apiClient.rootGet()).then((result) => {
        expect(GetRootApi.prototype.rootGet).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when fetching root details', () => {
      const mockError = new Error('Network error')
      cy.stub(GetRootApi.prototype, 'rootGet').rejects(mockError)

      apiClient.rootGet().then(
        () => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        },
        (error) => {
          expect(GetRootApi.prototype.rootGet).to.be.calledOnce
          expect(error).to.equal(mockError)
        },
      )
    })
  })

  context('getUserProjections', () => {
    it('should fetch user projections successfully', () => {
      const mockResponse = {
        data: [{ id: 1, name: 'API Projection' }],
      }
      cy.stub(ProjectionApi.prototype, 'getUserProjections').resolves(
        mockResponse,
      )

      cy.wrap(apiClient.getUserProjections()).then((result: unknown) => {
        const response = result as { data: unknown }
        expect(ProjectionApi.prototype.getUserProjections).to.be.calledOnce
        expect(response).to.have.property('data')
        expect(response.data).to.deep.equal([{ id: 1, name: 'API Projection' }])
      })
    })

    it('should handle error when fetching user projections', () => {
      const mockError = new Error('Network error')
      cy.stub(ProjectionApi.prototype, 'getUserProjections').rejects(mockError)

      apiClient
        .getUserProjections()
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.getUserProjections).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('streamResultsZip', () => {
    it('should stream results zip successfully', () => {
      const mockBlob = new Blob(['zip content'], { type: 'application/zip' })
      const mockResponse = { data: mockBlob }
      cy.stub(ProjectionApi.prototype, 'streamResultsZip').resolves(
        mockResponse,
      )

      const projectionGUID = 'test-guid-123'

      cy.wrap(apiClient.streamResultsZip(projectionGUID)).then(
        (result: unknown) => {
          const response = result as { data: Blob }
          expect(ProjectionApi.prototype.streamResultsZip).to.be.calledOnce
          expect(response.data).to.be.instanceOf(Blob)
          expect(response.data.size).to.equal(mockBlob.size)
          expect(response.data.type).to.equal(mockBlob.type)
        },
      )
    })

    it('should handle error when streaming results zip', () => {
      const mockError = new Error('Download failed')
      cy.stub(ProjectionApi.prototype, 'streamResultsZip').rejects(mockError)

      apiClient
        .streamResultsZip('test-guid-123')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.streamResultsZip).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })
})
