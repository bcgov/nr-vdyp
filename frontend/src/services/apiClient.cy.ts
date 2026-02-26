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
import type { Parameters, ModelParameters } from '@/services/vdyp-api'

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

  context('createProjection', () => {
    it('should create a projection successfully', () => {
      const mockResponse = { data: { id: 'new-guid', status: 'DRAFT' } }
      const mockParameters = {} as Parameters
      cy.stub(ProjectionApi.prototype, 'createProjection').resolves(mockResponse)

      cy.wrap(apiClient.createProjection(mockParameters)).then((result: unknown) => {
        expect(ProjectionApi.prototype.createProjection).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should create a projection with optional modelParameters and reportDescription', () => {
      const mockResponse = { data: { id: 'new-guid', status: 'DRAFT' } }
      const mockParameters = {} as Parameters
      const mockModelParameters = {} as ModelParameters
      const reportDescription = 'Test report'
      cy.stub(ProjectionApi.prototype, 'createProjection').resolves(mockResponse)

      cy.wrap(
        apiClient.createProjection(mockParameters, mockModelParameters, reportDescription),
      ).then((result: unknown) => {
        expect(ProjectionApi.prototype.createProjection).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when creating a projection', () => {
      const mockError = new Error('Create projection failed')
      const mockParameters = {} as Parameters
      cy.stub(ProjectionApi.prototype, 'createProjection').rejects(mockError)

      apiClient
        .createProjection(mockParameters)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.createProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('getProjection', () => {
    it('should fetch a projection by GUID successfully', () => {
      const mockResponse = { data: { id: 'test-guid', status: 'DRAFT' } }
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'getProjection').resolves(mockResponse)

      cy.wrap(apiClient.getProjection(projectionGUID)).then((result: unknown) => {
        expect(ProjectionApi.prototype.getProjection).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when fetching a projection by GUID', () => {
      const mockError = new Error('Projection not found')
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'getProjection').rejects(mockError)

      apiClient
        .getProjection(projectionGUID)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.getProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('updateProjectionParams', () => {
    it('should update projection parameters successfully', () => {
      const mockResponse = { data: { id: 'test-guid', status: 'DRAFT' } }
      const projectionGUID = 'test-guid'
      const mockParameters = {} as Parameters
      cy.stub(ProjectionApi.prototype, 'updateProjectionParams').resolves(mockResponse)

      cy.wrap(
        apiClient.updateProjectionParams(projectionGUID, mockParameters),
      ).then((result: unknown) => {
        expect(ProjectionApi.prototype.updateProjectionParams).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should update projection parameters with optional modelParameters and reportDescription', () => {
      const mockResponse = { data: { id: 'test-guid', status: 'DRAFT' } }
      const projectionGUID = 'test-guid'
      const mockParameters = {} as Parameters
      const mockModelParameters = {} as ModelParameters
      const reportDescription = 'Updated report'
      cy.stub(ProjectionApi.prototype, 'updateProjectionParams').resolves(mockResponse)

      cy.wrap(
        apiClient.updateProjectionParams(
          projectionGUID,
          mockParameters,
          mockModelParameters,
          reportDescription,
        ),
      ).then((result: unknown) => {
        expect(ProjectionApi.prototype.updateProjectionParams).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when updating projection parameters', () => {
      const mockError = new Error('Update failed')
      const projectionGUID = 'test-guid'
      const mockParameters = {} as Parameters
      cy.stub(ProjectionApi.prototype, 'updateProjectionParams').rejects(mockError)

      apiClient
        .updateProjectionParams(projectionGUID, mockParameters)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.updateProjectionParams).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('runProjection', () => {
    it('should run a projection successfully', () => {
      const mockResponse = { data: { id: 'test-guid', status: 'RUNNING' } }
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'runProjection').resolves(mockResponse)

      cy.wrap(apiClient.runProjection(projectionGUID)).then((result: unknown) => {
        expect(ProjectionApi.prototype.runProjection).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when running a projection', () => {
      const mockError = new Error('Run projection failed')
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'runProjection').rejects(mockError)

      apiClient
        .runProjection(projectionGUID)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.runProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('cancelProjection', () => {
    it('should cancel a projection successfully', () => {
      const mockResponse = { data: { id: 'test-guid', status: 'DRAFT' } }
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'cancelProjection').resolves(mockResponse)

      cy.wrap(apiClient.cancelProjection(projectionGUID)).then((result: unknown) => {
        expect(ProjectionApi.prototype.cancelProjection).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when cancelling a projection', () => {
      const mockError = new Error('Cancel projection failed')
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'cancelProjection').rejects(mockError)

      apiClient
        .cancelProjection(projectionGUID)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.cancelProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('duplicateProjection', () => {
    it('should duplicate a projection successfully', () => {
      const mockResponse = { data: { id: 'new-guid', status: 'DRAFT' } }
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'duplicateProjection').resolves(mockResponse)

      cy.wrap(apiClient.duplicateProjection(projectionGUID)).then((result: unknown) => {
        expect(ProjectionApi.prototype.duplicateProjection).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when duplicating a projection', () => {
      const mockError = new Error('Duplicate projection failed')
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'duplicateProjection').rejects(mockError)

      apiClient
        .duplicateProjection(projectionGUID)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.duplicateProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('deleteProjection', () => {
    it('should delete a projection successfully', () => {
      const mockResponse = { status: 204 }
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'deleteProjection').resolves(mockResponse)

      cy.wrap(apiClient.deleteProjection(projectionGUID)).then((result: unknown) => {
        expect(ProjectionApi.prototype.deleteProjection).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when deleting a projection', () => {
      const mockError = new Error('Delete projection failed')
      const projectionGUID = 'test-guid'
      cy.stub(ProjectionApi.prototype, 'deleteProjection').rejects(mockError)

      apiClient
        .deleteProjection(projectionGUID)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.deleteProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('getFileSetFiles', () => {
    it('should fetch fileset files successfully', () => {
      const mockResponse = { data: [{ id: 'file-1', name: 'polygon.csv' }] }
      const projectionGUID = 'test-guid'
      const fileSetGUID = 'fileset-guid'
      cy.stub(ProjectionApi.prototype, 'getFileSetFiles').resolves(mockResponse)

      cy.wrap(apiClient.getFileSetFiles(projectionGUID, fileSetGUID)).then(
        (result: unknown) => {
          expect(ProjectionApi.prototype.getFileSetFiles).to.be.calledOnce
          expect(result).to.deep.equal(mockResponse)
        },
      )
    })

    it('should handle error when fetching fileset files', () => {
      const mockError = new Error('Fetch fileset files failed')
      const projectionGUID = 'test-guid'
      const fileSetGUID = 'fileset-guid'
      cy.stub(ProjectionApi.prototype, 'getFileSetFiles').rejects(mockError)

      apiClient
        .getFileSetFiles(projectionGUID, fileSetGUID)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.getFileSetFiles).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('uploadFileToFileSet', () => {
    it('should upload a file to a fileset successfully', () => {
      const mockResponse = { data: { id: 'test-guid', status: 'DRAFT' } }
      const projectionGUID = 'test-guid'
      const fileSetGUID = 'fileset-guid'
      const file = new File(['content'], 'polygon.csv')
      cy.stub(ProjectionApi.prototype, 'uploadFileToFileSet').resolves(mockResponse)

      cy.wrap(
        apiClient.uploadFileToFileSet(projectionGUID, fileSetGUID, file),
      ).then((result: unknown) => {
        expect(ProjectionApi.prototype.uploadFileToFileSet).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when uploading a file to a fileset', () => {
      const mockError = new Error('Upload failed')
      const projectionGUID = 'test-guid'
      const fileSetGUID = 'fileset-guid'
      const file = new File(['content'], 'polygon.csv')
      cy.stub(ProjectionApi.prototype, 'uploadFileToFileSet').rejects(mockError)

      apiClient
        .uploadFileToFileSet(projectionGUID, fileSetGUID, file)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.uploadFileToFileSet).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('getFileForDownload', () => {
    it('should get a file for download successfully', () => {
      const mockResponse = { data: { id: 'file-mapping-guid', downloadUrl: 'https://example.com/file' } }
      const projectionGUID = 'test-guid'
      const fileSetGUID = 'fileset-guid'
      const fileMappingGUID = 'file-mapping-guid'
      cy.stub(ProjectionApi.prototype, 'getFileForDownload').resolves(mockResponse)

      cy.wrap(
        apiClient.getFileForDownload(projectionGUID, fileSetGUID, fileMappingGUID),
      ).then((result: unknown) => {
        expect(ProjectionApi.prototype.getFileForDownload).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when getting a file for download', () => {
      const mockError = new Error('Get file for download failed')
      const projectionGUID = 'test-guid'
      const fileSetGUID = 'fileset-guid'
      const fileMappingGUID = 'file-mapping-guid'
      cy.stub(ProjectionApi.prototype, 'getFileForDownload').rejects(mockError)

      apiClient
        .getFileForDownload(projectionGUID, fileSetGUID, fileMappingGUID)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.getFileForDownload).to.be.calledOnce
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

  context('deleteFileFromFileSet', () => {
    it('should delete a file from a fileset successfully', () => {
      const mockResponse = { status: 204 }
      const projectionGUID = 'test-guid'
      const fileSetGUID = 'fileset-guid'
      const fileMappingGUID = 'file-mapping-guid'
      cy.stub(ProjectionApi.prototype, 'deleteFileFromFileSet').resolves(mockResponse)

      cy.wrap(
        apiClient.deleteFileFromFileSet(projectionGUID, fileSetGUID, fileMappingGUID),
      ).then((result: unknown) => {
        expect(ProjectionApi.prototype.deleteFileFromFileSet).to.be.calledOnce
        expect(result).to.deep.equal(mockResponse)
      })
    })

    it('should handle error when deleting a file from a fileset', () => {
      const mockError = new Error('Delete file failed')
      const projectionGUID = 'test-guid'
      const fileSetGUID = 'fileset-guid'
      const fileMappingGUID = 'file-mapping-guid'
      cy.stub(ProjectionApi.prototype, 'deleteFileFromFileSet').rejects(mockError)

      apiClient
        .deleteFileFromFileSet(projectionGUID, fileSetGUID, fileMappingGUID)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(ProjectionApi.prototype.deleteFileFromFileSet).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })
})
