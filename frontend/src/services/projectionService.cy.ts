/// <reference types="cypress" />

import { PROJECTION_STATUS, FILE_NAME } from '@/constants/constants'
import {
  mapProjectionStatus,
  parseProjectionParams,
  isProjectionReadOnly,
  transformProjection,
  streamResultsZip,
  createProjection,
  duplicateProjection,
  updateProjectionParams,
  updateProjectionParamsWithModel,
  fetchUserProjections,
} from '@/services/projectionService'
import apiClient from '@/services/apiClient'
import type { ProjectionModel, Parameters } from '@/services/vdyp-api'

describe('projectionService Unit Tests', () => {
  const mockProjectionModel = {
    projectionGUID: 'guid-test',
    reportTitle: 'Test Title',
    reportDescription: 'Test Description',
    projectionStatusCode: { code: 'DRAFT', description: '', displayOrder: 0 },
    lastUpdatedDate: '2024-01-01',
    expiryDate: '2024-07-01',
    projectionParameters: null,
  } as unknown as ProjectionModel

  const mockParameters = {} as Parameters

  describe('mapProjectionStatus', () => {
    it('should map known status codes correctly', () => {
      expect(mapProjectionStatus('DRAFT')).to.equal(PROJECTION_STATUS.DRAFT)
      expect(mapProjectionStatus('READY')).to.equal(PROJECTION_STATUS.READY)
      expect(mapProjectionStatus('RUNNING')).to.equal(PROJECTION_STATUS.RUNNING)
      expect(mapProjectionStatus('QUEUED')).to.equal(PROJECTION_STATUS.QUEUED)
      expect(mapProjectionStatus('FAILED')).to.equal(PROJECTION_STATUS.FAILED)
    })

    it('should default to DRAFT for unknown status codes', () => {
      expect(mapProjectionStatus('Unknown')).to.equal(PROJECTION_STATUS.DRAFT)
      expect(mapProjectionStatus('')).to.equal(PROJECTION_STATUS.DRAFT)
    })
  })

  describe('parseProjectionParams', () => {
    it('should return defaults for null or undefined input', () => {
      const result = parseProjectionParams(null)
      expect(result.selectedExecutionOptions).to.deep.equal([])
      expect(result.ageStart).to.be.null
      expect(result.outputFormat).to.be.null
      expect(result.reportTitle).to.be.null
    })

    it('should parse valid JSON string including reportTitle', () => {
      const json = JSON.stringify({
        ageStart: 10,
        ageEnd: 100,
        selectedExecutionOptions: ['DoIncludeProjectedMOFVolumes'],
        reportTitle: 'My Report Title',
      })
      const result = parseProjectionParams(json)
      expect(result.ageStart).to.equal(10)
      expect(result.ageEnd).to.equal(100)
      expect(result.selectedExecutionOptions).to.deep.equal(['DoIncludeProjectedMOFVolumes'])
      expect(result.reportTitle).to.equal('My Report Title')

      const withoutTitle = parseProjectionParams(JSON.stringify({ ageStart: 5 }))
      expect(withoutTitle.reportTitle).to.be.null
    })

    it('should return defaults for invalid JSON', () => {
      const result = parseProjectionParams('not valid json')
      expect(result.selectedExecutionOptions).to.deep.equal([])
      expect(result.ageStart).to.be.null
    })
  })

  describe('isProjectionReadOnly', () => {
    it('should return true for Ready, Running, and Queued statuses', () => {
      expect(isProjectionReadOnly(PROJECTION_STATUS.READY)).to.be.true
      expect(isProjectionReadOnly(PROJECTION_STATUS.RUNNING)).to.be.true
      expect(isProjectionReadOnly(PROJECTION_STATUS.QUEUED)).to.be.true
    })

    it('should return false for Draft and Failed statuses', () => {
      expect(isProjectionReadOnly(PROJECTION_STATUS.DRAFT)).to.be.false
      expect(isProjectionReadOnly(PROJECTION_STATUS.FAILED)).to.be.false
    })
  })

  describe('transformProjection', () => {
    it('should transform a ProjectionModel to Projection', () => {
      const model = {
        projectionGUID: 'guid-abc',
        reportTitle: 'Test Title',
        reportDescription: 'Test Desc',
        projectionStatusCode: { code: 'READY', description: '', displayOrder: 0 },
        lastUpdatedDate: '2024-01-15',
        expiryDate: '2024-06-15',
        projectionParameters: '',
      } as unknown as ProjectionModel
      const result = transformProjection(model)
      expect(result.projectionGUID).to.equal('guid-abc')
      expect(result.title).to.equal('Test Title')
      expect(result.description).to.equal('Test Desc')
      expect(result.status).to.equal(PROJECTION_STATUS.READY)
      expect(result.lastUpdated).to.equal('2024-01-15')
      expect(result.expiration).to.equal('2024-06-15')
    })

    it('should handle missing optional fields', () => {
      const model = {
        projectionGUID: 'guid-xyz',
        projectionParameters: '',
      } as unknown as ProjectionModel
      const result = transformProjection(model)
      expect(result.title).to.equal('')
      expect(result.description).to.equal('')
      expect(result.status).to.equal(PROJECTION_STATUS.DRAFT)
    })
  })

  describe('fetchUserProjections', () => {
    it('should fetch, transform and return projections', () => {
      cy.stub(apiClient, 'getUserProjections').resolves({ data: [mockProjectionModel] })

      cy.wrap(fetchUserProjections()).then((result: any) => {
        expect(apiClient.getUserProjections).to.be.calledOnce
        expect(result).to.have.length(1)
        expect(result[0].projectionGUID).to.equal('guid-test')
        expect(result[0].title).to.equal('Test Title')
        expect(result[0].description).to.equal('Test Description')
      })
    })

    it('should return empty array when no projections exist', () => {
      cy.stub(apiClient, 'getUserProjections').resolves({ data: [] })

      cy.wrap(fetchUserProjections()).then((result: any) => {
        expect(result).to.have.length(0)
      })
    })

    it('should throw error when API fails', () => {
      const mockError = new Error('Fetch failed')
      cy.stub(apiClient, 'getUserProjections').rejects(mockError)

      fetchUserProjections()
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(error).to.equal(mockError)
        })
    })
  })

  describe('createProjection', () => {
    it('should create projection and return the model', () => {
      cy.stub(apiClient, 'createProjection').resolves({ data: mockProjectionModel })

      cy.wrap(createProjection(mockParameters)).then((result: any) => {
        expect(apiClient.createProjection).to.be.calledOnce
        expect(result.projectionGUID).to.equal('guid-test')
      })
    })

    it('should pass reportDescription to apiClient', () => {
      cy.stub(apiClient, 'createProjection').resolves({ data: mockProjectionModel })

      cy.wrap(createProjection(mockParameters, undefined, 'My description')).then(() => {
        expect(apiClient.createProjection).to.be.calledWith(
          mockParameters,
          undefined,
          'My description',
        )
      })
    })

    it('should throw error when API fails', () => {
      const mockError = new Error('Create failed')
      cy.stub(apiClient, 'createProjection').rejects(mockError)

      createProjection(mockParameters)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(error).to.equal(mockError)
        })
    })
  })

  describe('duplicateProjection', () => {
    it('should duplicate projection and return the model', () => {
      const mockDuplicate = { ...mockProjectionModel, projectionGUID: 'guid-copy' }
      cy.stub(apiClient, 'duplicateProjection').resolves({ data: mockDuplicate })

      cy.wrap(duplicateProjection('guid-test')).then((result: any) => {
        expect(apiClient.duplicateProjection).to.be.calledOnceWith('guid-test')
        expect(result.projectionGUID).to.equal('guid-copy')
      })
    })

    it('should throw error when API fails', () => {
      const mockError = new Error('Duplicate failed')
      cy.stub(apiClient, 'duplicateProjection').rejects(mockError)

      duplicateProjection('guid-test')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(error).to.equal(mockError)
        })
    })
  })

  describe('updateProjectionParams', () => {
    it('should update projection and return the fetched model', () => {
      cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
      cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })

      cy.wrap(updateProjectionParams('guid-test', mockParameters)).then((result: any) => {
        expect(apiClient.updateProjectionParams).to.be.calledOnce
        expect(apiClient.getProjection).to.be.calledWith('guid-test')
        expect(result.projectionGUID).to.equal('guid-test')
      })
    })

    it('should pass reportDescription to apiClient', () => {
      cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
      cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })

      cy.wrap(updateProjectionParams('guid-test', mockParameters, 'Updated desc')).then(() => {
        expect(apiClient.updateProjectionParams).to.be.calledWith(
          'guid-test',
          mockParameters,
          undefined,
          'Updated desc',
        )
      })
    })

    it('should throw error when API fails', () => {
      const mockError = new Error('Update failed')
      cy.stub(apiClient, 'updateProjectionParams').rejects(mockError)

      updateProjectionParams('guid-test', mockParameters)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(error).to.equal(mockError)
        })
    })
  })

  describe('updateProjectionParamsWithModel', () => {
    it('should update projection with model and return the fetched model', () => {
      cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
      cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })

      cy.wrap(updateProjectionParamsWithModel('guid-test', mockParameters)).then((result: any) => {
        expect(apiClient.updateProjectionParams).to.be.calledOnce
        expect(apiClient.getProjection).to.be.calledWith('guid-test')
        expect(result.projectionGUID).to.equal('guid-test')
      })
    })

    it('should pass reportDescription to apiClient', () => {
      cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
      cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })

      cy.wrap(
        updateProjectionParamsWithModel('guid-test', mockParameters, undefined, 'Model desc'),
      ).then(() => {
        expect(apiClient.updateProjectionParams).to.be.calledWith(
          'guid-test',
          mockParameters,
          undefined,
          'Model desc',
        )
      })
    })

    it('should throw error when API fails', () => {
      const mockError = new Error('Update with model failed')
      cy.stub(apiClient, 'updateProjectionParams').rejects(mockError)

      updateProjectionParamsWithModel('guid-test', mockParameters)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(error).to.equal(mockError)
        })
    })
  })

  describe('streamResultsZip', () => {
    it('should return zip blob with filename from header', () => {
      const mockBlob = new Blob(['zip'], { type: 'application/zip' })
      const mockResponse = {
        data: mockBlob,
        headers: {
          'content-disposition': 'attachment; filename="results.zip"',
        },
      }
      cy.stub(apiClient, 'streamResultsZip').resolves(mockResponse)

      cy.wrap(streamResultsZip('test-guid')).then((result: any) => {
        expect(apiClient.streamResultsZip).to.be.calledOnceWith('test-guid')
        expect(result.zipBlob).to.equal(mockBlob)
        expect(result.zipFileName).to.equal('results.zip')
      })
    })

    it('should use default filename when header is missing', () => {
      const mockBlob = new Blob(['zip'], { type: 'application/zip' })
      const mockResponse = { data: mockBlob, headers: {} }
      cy.stub(apiClient, 'streamResultsZip').resolves(mockResponse)

      cy.wrap(streamResultsZip('test-guid')).then((result: any) => {
        expect(result.zipFileName).to.equal(FILE_NAME.PROJECTION_RESULT_ZIP)
      })
    })

    it('should handle error when streaming fails', () => {
      const mockError = new Error('Download failed')
      cy.stub(apiClient, 'streamResultsZip').rejects(mockError)

      streamResultsZip('test-guid')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.streamResultsZip).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })
})
