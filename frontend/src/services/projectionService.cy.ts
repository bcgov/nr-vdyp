/// <reference types="cypress" />

import type { Projection } from '@/interfaces/interfaces'
import { SORT_ORDER, PROJECTION_LIST_HEADER_KEY, PROJECTION_STATUS, FILE_NAME } from '@/constants/constants'
import {
  mapProjectionStatus,
  parseProjectionParams,
  isProjectionReadOnly,
  transformProjection,
  streamResultsZip,
  createProjection,
  updateProjectionParams,
  updateProjectionParamsWithModel,
  fetchUserProjections,
} from '@/services/projectionService'
import apiClient from '@/services/apiClient'
import type { ProjectionModel, Parameters } from '@/services/vdyp-api'

// Local implementations for testing
const sortProjections = (
  projections: Projection[],
  sortKey: string,
  sortOrder: string,
): Projection[] => {
  return [...projections].sort((a, b) => {
    const aValue = a[sortKey as keyof Projection] ?? ''
    const bValue = b[sortKey as keyof Projection] ?? ''

    let comparison = 0
    if (aValue < bValue) {
      comparison = -1
    } else if (aValue > bValue) {
      comparison = 1
    }

    return sortOrder === 'desc' ? -comparison : comparison
  })
}

const paginateProjections = (
  projections: Projection[],
  page: number,
  itemsPerPage: number,
): Projection[] => {
  const startIndex = (page - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  return projections.slice(startIndex, endIndex)
}

const calculateTotalPages = (
  totalItems: number,
  itemsPerPage: number,
): number => {
  if (totalItems === 0) return 0
  return Math.ceil(totalItems / itemsPerPage)
}

describe('ProjectionListService Unit Tests', () => {
  const mockProjections: Projection[] = [
    {
      projectionGUID: 'guid-1',
      title: 'Alpha Project',
      description: 'First project',
      method: 'File Upload',
      projectionType: 'Volume',
      lastUpdated: '2024-01-15T10:00:00',
      expiration: '2024-06-15T10:00:00',
      status: 'Draft',
    },
    {
      projectionGUID: 'guid-2',
      title: 'Beta Project',
      description: 'Second project',
      method: 'Manual Input',
      projectionType: 'CFS Biomass',
      lastUpdated: '2024-02-20T14:30:00',
      expiration: '2024-07-20T14:30:00',
      status: 'Ready',
    },
    {
      projectionGUID: 'guid-3',
      title: 'Gamma Project',
      description: 'Third project',
      method: 'File Upload',
      projectionType: 'Volume',
      lastUpdated: '2024-01-10T08:00:00',
      expiration: '2024-05-10T08:00:00',
      status: 'Running',
    },
    {
      projectionGUID: 'guid-4',
      title: 'Delta Project',
      description: 'Fourth project',
      method: 'Manual Input',
      projectionType: 'CFS Biomass',
      lastUpdated: '2024-03-01T16:45:00',
      expiration: '2024-08-01T16:45:00',
      status: 'Failed',
    },
  ]

  describe('sortProjections', () => {
    it('should sort by title in ascending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.ASC,
      )
      expect(sorted[0].title).to.equal('Alpha Project')
      expect(sorted[1].title).to.equal('Beta Project')
      expect(sorted[2].title).to.equal('Delta Project')
      expect(sorted[3].title).to.equal('Gamma Project')
    })

    it('should sort by title in descending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.DESC,
      )
      expect(sorted[0].title).to.equal('Gamma Project')
      expect(sorted[1].title).to.equal('Delta Project')
      expect(sorted[2].title).to.equal('Beta Project')
      expect(sorted[3].title).to.equal('Alpha Project')
    })

    it('should sort by lastUpdated in ascending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.LAST_UPDATED,
        SORT_ORDER.ASC,
      )
      expect(sorted[0].projectionGUID).to.equal('guid-3') // Jan 10
      expect(sorted[1].projectionGUID).to.equal('guid-1') // Jan 15
      expect(sorted[2].projectionGUID).to.equal('guid-2') // Feb 20
      expect(sorted[3].projectionGUID).to.equal('guid-4') // Mar 01
    })

    it('should sort by lastUpdated in descending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.LAST_UPDATED,
        SORT_ORDER.DESC,
      )
      expect(sorted[0].projectionGUID).to.equal('guid-4') // Mar 01
      expect(sorted[1].projectionGUID).to.equal('guid-2') // Feb 20
      expect(sorted[2].projectionGUID).to.equal('guid-1') // Jan 15
      expect(sorted[3].projectionGUID).to.equal('guid-3') // Jan 10
    })

    it('should sort by expiration in ascending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.EXPIRATION,
        SORT_ORDER.ASC,
      )
      expect(sorted[0].projectionGUID).to.equal('guid-3') // May 10
      expect(sorted[1].projectionGUID).to.equal('guid-1') // Jun 15
      expect(sorted[2].projectionGUID).to.equal('guid-2') // Jul 20
      expect(sorted[3].projectionGUID).to.equal('guid-4') // Aug 01
    })

    it('should sort by status in ascending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.STATUS,
        SORT_ORDER.ASC,
      )
      expect(sorted[0].status).to.equal('Draft')
      expect(sorted[1].status).to.equal('Failed')
      expect(sorted[2].status).to.equal('Ready')
      expect(sorted[3].status).to.equal('Running')
    })

    it('should not mutate the original array', () => {
      const original = [...mockProjections]
      sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.ASC,
      )
      expect(mockProjections).to.deep.equal(original)
    })

    it('should handle empty array', () => {
      const sorted = sortProjections(
        [],
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.ASC,
      )
      expect(sorted).to.deep.equal([])
    })

    it('should handle single item array', () => {
      const singleItem = [mockProjections[0]]
      const sorted = sortProjections(
        singleItem,
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.ASC,
      )
      expect(sorted).to.have.length(1)
      expect(sorted[0].title).to.equal('Alpha Project')
    })
  })

  describe('paginateProjections', () => {
    it('should return correct items for first page', () => {
      const paginated = paginateProjections(mockProjections, 1, 2)
      expect(paginated).to.have.length(2)
      expect(paginated[0].projectionGUID).to.equal('guid-1')
      expect(paginated[1].projectionGUID).to.equal('guid-2')
    })

    it('should return correct items for second page', () => {
      const paginated = paginateProjections(mockProjections, 2, 2)
      expect(paginated).to.have.length(2)
      expect(paginated[0].projectionGUID).to.equal('guid-3')
      expect(paginated[1].projectionGUID).to.equal('guid-4')
    })

    it('should return remaining items for last page', () => {
      const paginated = paginateProjections(mockProjections, 2, 3)
      expect(paginated).to.have.length(1)
      expect(paginated[0].projectionGUID).to.equal('guid-4')
    })

    it('should return empty array for page beyond available items', () => {
      const paginated = paginateProjections(mockProjections, 5, 2)
      expect(paginated).to.have.length(0)
    })

    it('should return all items when itemsPerPage exceeds total', () => {
      const paginated = paginateProjections(mockProjections, 1, 10)
      expect(paginated).to.have.length(4)
    })

    it('should handle empty array', () => {
      const paginated = paginateProjections([], 1, 5)
      expect(paginated).to.have.length(0)
    })

    it('should handle page 1 with itemsPerPage of 1', () => {
      const paginated = paginateProjections(mockProjections, 1, 1)
      expect(paginated).to.have.length(1)
      expect(paginated[0].projectionGUID).to.equal('guid-1')
    })
  })

  describe('calculateTotalPages', () => {
    it('should calculate correct total pages when evenly divisible', () => {
      expect(calculateTotalPages(10, 5)).to.equal(2)
      expect(calculateTotalPages(20, 10)).to.equal(2)
      expect(calculateTotalPages(100, 25)).to.equal(4)
    })

    it('should round up when not evenly divisible', () => {
      expect(calculateTotalPages(11, 5)).to.equal(3)
      expect(calculateTotalPages(7, 3)).to.equal(3)
      expect(calculateTotalPages(1, 10)).to.equal(1)
    })

    it('should return 0 for zero items', () => {
      expect(calculateTotalPages(0, 5)).to.equal(0)
    })

    it('should handle single item', () => {
      expect(calculateTotalPages(1, 5)).to.equal(1)
    })

    it('should handle items equal to itemsPerPage', () => {
      expect(calculateTotalPages(5, 5)).to.equal(1)
    })
  })
})

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
    })

    it('should return null reportTitle in defaults', () => {
      const result = parseProjectionParams(null)
      expect(result.reportTitle).to.be.null
    })

    it('should parse valid JSON string', () => {
      const json = JSON.stringify({
        ageStart: 10,
        ageEnd: 100,
        selectedExecutionOptions: ['DoIncludeProjectedMOFVolumes'],
      })
      const result = parseProjectionParams(json)
      expect(result.ageStart).to.equal(10)
      expect(result.ageEnd).to.equal(100)
      expect(result.selectedExecutionOptions).to.deep.equal([
        'DoIncludeProjectedMOFVolumes',
      ])
    })

    it('should parse reportTitle from valid JSON', () => {
      const json = JSON.stringify({ reportTitle: 'My Report Title' })
      const result = parseProjectionParams(json)
      expect(result.reportTitle).to.equal('My Report Title')
    })

    it('should return null for missing reportTitle in JSON', () => {
      const json = JSON.stringify({ ageStart: 10 })
      const result = parseProjectionParams(json)
      expect(result.reportTitle).to.be.null
    })

    it('should return defaults for invalid JSON', () => {
      const result = parseProjectionParams('not valid json')
      expect(result.selectedExecutionOptions).to.deep.equal([])
      expect(result.ageStart).to.be.null
    })
  })

  describe('isProjectionReadOnly', () => {
    it('should return true for Ready and Running statuses', () => {
      expect(isProjectionReadOnly(PROJECTION_STATUS.READY)).to.be.true
      expect(isProjectionReadOnly(PROJECTION_STATUS.RUNNING)).to.be.true
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

    it('should pass null reportDescription when explicitly provided as null', () => {
      cy.stub(apiClient, 'createProjection').resolves({ data: mockProjectionModel })

      cy.wrap(createProjection(mockParameters, undefined, null)).then(() => {
        expect(apiClient.createProjection).to.be.calledWith(mockParameters, undefined, null)
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

    it('should call getProjection after updateProjectionParams', () => {
      cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
      cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })

      cy.wrap(updateProjectionParams('guid-test', mockParameters, 'desc')).then(() => {
        expect(apiClient.getProjection).to.be.calledWith('guid-test')
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

    it('should call getProjection after updateProjectionParams', () => {
      cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
      cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })

      cy.wrap(updateProjectionParamsWithModel('guid-test', mockParameters)).then(() => {
        expect(apiClient.getProjection).to.be.calledWith('guid-test')
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
