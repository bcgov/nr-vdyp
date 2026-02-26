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
    it('should post projection data successfully with status 200', () => {
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

    it('should post projection data successfully with status 201', () => {
      const mockBlob = new Blob(['test data'], { type: 'application/octet-stream' })
      const mockResponse = {
        status: 201,
        headers: { 'content-type': 'application/octet-stream' },
        data: mockBlob,
      }
      const formData = new FormData()
      formData.append('file', new Blob(), 'test.csv')

      cy.stub(apiClient, 'projectionHcsvPost').resolves(mockResponse)

      apiActions
        .projectionHcsvPost(formData, false)
        .then((result: { status: number; headers: any; data: Blob }) => {
          expect(apiClient.projectionHcsvPost).to.be.calledOnceWith(
            formData,
            false,
          )
          expect(result.data).to.equal(mockBlob)
        })
    })

    it('should throw error on status 400 with application/json response', () => {
      const errorText = JSON.stringify({ message: 'Validation failed' })
      const mockResponse = {
        status: 400,
        headers: { 'content-type': 'application/json' },
        data: { text: () => Promise.resolve(errorText) },
      }
      const formData = new FormData()
      formData.append('file', new Blob(), 'test.csv')

      cy.stub(apiClient, 'projectionHcsvPost').resolves(mockResponse)

      apiActions
        .projectionHcsvPost(formData, false)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.projectionHcsvPost).to.be.calledOnceWith(
            formData,
            false,
          )
          expect(error.message).to.equal(errorText)
        })
    })

    it('should throw error on unexpected response status', () => {
      const mockResponse = {
        status: 500,
        headers: { 'content-type': 'text/plain' },
        data: null,
      }
      const formData = new FormData()
      formData.append('file', new Blob(), 'test.csv')

      cy.stub(apiClient, 'projectionHcsvPost').resolves(mockResponse)

      apiActions
        .projectionHcsvPost(formData, false)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.projectionHcsvPost).to.be.calledOnceWith(
            formData,
            false,
          )
          expect(error.message).to.equal('Unexpected response: 500')
        })
    })

    it('should handle error when posting projection data', () => {
      const mockError = new Error('Server error')
      const formData = new FormData()
      formData.append('file', new Blob(), 'test.csv')

      cy.stub(apiClient, 'projectionHcsvPost').rejects(mockError)

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

  context('getUserProjections', () => {
    it('should fetch user projections successfully', () => {
      const mockResponse = {
        data: [
          { id: '1', name: 'Projection 1', status: 'READY' },
          { id: '2', name: 'Projection 2', status: 'DRAFT' },
        ],
      }
      cy.stub(apiClient, 'getUserProjections').resolves(mockResponse)

      cy.wrap(apiActions.getUserProjections()).then((result) => {
        expect(apiClient.getUserProjections).to.be.calledOnce
        expect(result).to.deep.equal([
          { id: '1', name: 'Projection 1', status: 'READY' },
          { id: '2', name: 'Projection 2', status: 'DRAFT' },
        ])
      })
    })

    it('should handle error when fetching user projections', () => {
      const mockError = new Error('Network error')
      cy.stub(apiClient, 'getUserProjections').rejects(mockError)

      apiActions
        .getUserProjections()
        .then(() => {
          // assert-fail block
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.getUserProjections).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('createProjection', () => {
    it('should create a projection successfully with all parameters', () => {
      const parameters = { projectionYear: 2024 }
      const modelParameters = { model: 'FIP' }
      const reportDescription = 'Test report'
      const mockProjection = { projectionGUID: 'guid-123', status: 'DRAFT' }
      const mockResponse = { data: mockProjection }

      cy.stub(apiClient, 'createProjection').resolves(mockResponse)

      cy.wrap(
        apiActions.createProjection(
          parameters as any,
          modelParameters as any,
          reportDescription,
        ),
      ).then((result) => {
        expect(apiClient.createProjection).to.be.calledOnceWith(
          parameters,
          modelParameters,
          reportDescription,
        )
        expect(result).to.deep.equal(mockProjection)
      })
    })

    it('should create a projection successfully with only required parameters', () => {
      const parameters = { projectionYear: 2024 }
      const mockProjection = { projectionGUID: 'guid-456', status: 'DRAFT' }
      const mockResponse = { data: mockProjection }

      cy.stub(apiClient, 'createProjection').resolves(mockResponse)

      cy.wrap(apiActions.createProjection(parameters as any)).then((result) => {
        expect(apiClient.createProjection).to.be.calledOnceWith(
          parameters,
          undefined,
          undefined,
        )
        expect(result).to.deep.equal(mockProjection)
      })
    })

    it('should handle error when creating a projection', () => {
      const mockError = new Error('Creation failed')
      cy.stub(apiClient, 'createProjection').rejects(mockError)

      apiActions
        .createProjection({} as any)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.createProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('runProjection', () => {
    it('should run a projection successfully', () => {
      const projectionGUID = 'test-guid-123'
      const mockProjection = { projectionGUID, status: 'RUNNING' }
      const mockResponse = { data: mockProjection }

      cy.stub(apiClient, 'runProjection').resolves(mockResponse)

      cy.wrap(apiActions.runProjection(projectionGUID)).then((result) => {
        expect(apiClient.runProjection).to.be.calledOnceWith(projectionGUID)
        expect(result).to.deep.equal(mockProjection)
      })
    })

    it('should handle error when running a projection', () => {
      const mockError = new Error('Run failed')
      cy.stub(apiClient, 'runProjection').rejects(mockError)

      apiActions
        .runProjection('test-guid-123')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.runProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('cancelProjection', () => {
    it('should cancel a projection successfully', () => {
      const projectionGUID = 'test-guid-123'
      const mockProjection = { projectionGUID, status: 'DRAFT' }
      const mockResponse = { data: mockProjection }

      cy.stub(apiClient, 'cancelProjection').resolves(mockResponse)

      cy.wrap(apiActions.cancelProjection(projectionGUID)).then((result) => {
        expect(apiClient.cancelProjection).to.be.calledOnceWith(projectionGUID)
        expect(result).to.deep.equal(mockProjection)
      })
    })

    it('should handle error when cancelling a projection', () => {
      const mockError = new Error('Cancel failed')
      cy.stub(apiClient, 'cancelProjection').rejects(mockError)

      apiActions
        .cancelProjection('test-guid-123')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.cancelProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('duplicateProjection', () => {
    it('should duplicate a projection successfully', () => {
      const projectionGUID = 'test-guid-123'
      const mockProjection = { projectionGUID: 'new-guid-456', status: 'DRAFT' }
      const mockResponse = { data: mockProjection }

      cy.stub(apiClient, 'duplicateProjection').resolves(mockResponse)

      cy.wrap(apiActions.duplicateProjection(projectionGUID)).then((result) => {
        expect(apiClient.duplicateProjection).to.be.calledOnceWith(projectionGUID)
        expect(result).to.deep.equal(mockProjection)
      })
    })

    it('should handle error when duplicating a projection', () => {
      const mockError = new Error('Duplicate failed')
      cy.stub(apiClient, 'duplicateProjection').rejects(mockError)

      apiActions
        .duplicateProjection('test-guid-123')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.duplicateProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('getProjection', () => {
    it('should fetch a projection successfully', () => {
      const projectionGUID = 'test-guid-123'
      const mockProjection = { projectionGUID, status: 'READY' }
      const mockResponse = { data: mockProjection }

      cy.stub(apiClient, 'getProjection').resolves(mockResponse)

      cy.wrap(apiActions.getProjection(projectionGUID)).then((result) => {
        expect(apiClient.getProjection).to.be.calledOnceWith(projectionGUID)
        expect(result).to.deep.equal(mockProjection)
      })
    })

    it('should handle error when fetching a projection', () => {
      const mockError = new Error('Fetch failed')
      cy.stub(apiClient, 'getProjection').rejects(mockError)

      apiActions
        .getProjection('test-guid-123')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.getProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('updateProjectionParams', () => {
    it('should update projection parameters successfully with all parameters', () => {
      const projectionGUID = 'test-guid-123'
      const parameters = { projectionYear: 2025 }
      const modelParameters = { model: 'FIP' }
      const reportDescription = 'Updated report'
      const mockProjection = { projectionGUID, status: 'DRAFT' }
      const mockResponse = { data: mockProjection }

      cy.stub(apiClient, 'updateProjectionParams').resolves(mockResponse)

      cy.wrap(
        apiActions.updateProjectionParams(
          projectionGUID,
          parameters as any,
          modelParameters as any,
          reportDescription,
        ),
      ).then((result) => {
        expect(apiClient.updateProjectionParams).to.be.calledOnceWith(
          projectionGUID,
          parameters,
          modelParameters,
          reportDescription,
        )
        expect(result).to.deep.equal(mockProjection)
      })
    })

    it('should update projection parameters successfully with only required parameters', () => {
      const projectionGUID = 'test-guid-123'
      const parameters = { projectionYear: 2025 }
      const mockProjection = { projectionGUID, status: 'DRAFT' }
      const mockResponse = { data: mockProjection }

      cy.stub(apiClient, 'updateProjectionParams').resolves(mockResponse)

      cy.wrap(
        apiActions.updateProjectionParams(projectionGUID, parameters as any),
      ).then((result) => {
        expect(apiClient.updateProjectionParams).to.be.calledOnceWith(
          projectionGUID,
          parameters,
          undefined,
          undefined,
        )
        expect(result).to.deep.equal(mockProjection)
      })
    })

    it('should handle error when updating projection parameters', () => {
      const mockError = new Error('Update failed')
      cy.stub(apiClient, 'updateProjectionParams').rejects(mockError)

      apiActions
        .updateProjectionParams('test-guid-123', {} as any)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.updateProjectionParams).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('deleteProjection', () => {
    it('should delete a projection successfully', () => {
      const projectionGUID = 'test-guid-123'
      cy.stub(apiClient, 'deleteProjection').resolves()

      cy.wrap(apiActions.deleteProjection(projectionGUID)).then(() => {
        expect(apiClient.deleteProjection).to.be.calledOnceWith(projectionGUID)
      })
    })

    it('should handle error when deleting a projection', () => {
      const mockError = new Error('Delete failed')
      cy.stub(apiClient, 'deleteProjection').rejects(mockError)

      apiActions
        .deleteProjection('test-guid-123')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.deleteProjection).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('getFileSetFiles', () => {
    it('should fetch fileset files successfully', () => {
      const projectionGUID = 'proj-guid-123'
      const fileSetGUID = 'fileset-guid-456'
      const mockFiles = [
        { fileMappingGUID: 'file-1', fileName: 'test.csv' },
        { fileMappingGUID: 'file-2', fileName: 'data.csv' },
      ]
      const mockResponse = { data: mockFiles }

      cy.stub(apiClient, 'getFileSetFiles').resolves(mockResponse)

      cy.wrap(
        apiActions.getFileSetFiles(projectionGUID, fileSetGUID),
      ).then((result) => {
        expect(apiClient.getFileSetFiles).to.be.calledOnceWith(
          projectionGUID,
          fileSetGUID,
        )
        expect(result).to.deep.equal(mockFiles)
      })
    })

    it('should handle error when fetching fileset files', () => {
      const mockError = new Error('Fetch files failed')
      cy.stub(apiClient, 'getFileSetFiles').rejects(mockError)

      apiActions
        .getFileSetFiles('proj-guid-123', 'fileset-guid-456')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.getFileSetFiles).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('uploadFileToFileSet', () => {
    it('should upload a file to fileset successfully', () => {
      const projectionGUID = 'proj-guid-123'
      const fileSetGUID = 'fileset-guid-456'
      const file = new File(['content'], 'test.csv', { type: 'text/csv' })
      const mockProjection = { projectionGUID, status: 'DRAFT' }
      const mockResponse = { data: mockProjection }

      cy.stub(apiClient, 'uploadFileToFileSet').resolves(mockResponse)

      cy.wrap(
        apiActions.uploadFileToFileSet(projectionGUID, fileSetGUID, file),
      ).then((result) => {
        expect(apiClient.uploadFileToFileSet).to.be.calledOnceWith(
          projectionGUID,
          fileSetGUID,
          file,
        )
        expect(result).to.deep.equal(mockProjection)
      })
    })

    it('should handle error when uploading a file to fileset', () => {
      const mockError = new Error('Upload failed')
      const file = new File(['content'], 'test.csv', { type: 'text/csv' })
      cy.stub(apiClient, 'uploadFileToFileSet').rejects(mockError)

      apiActions
        .uploadFileToFileSet('proj-guid-123', 'fileset-guid-456', file)
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.uploadFileToFileSet).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('getFileForDownload', () => {
    it('should get file for download successfully', () => {
      const projectionGUID = 'proj-guid-123'
      const fileSetGUID = 'fileset-guid-456'
      const fileMappingGUID = 'file-guid-789'
      const mockFileMapping = {
        fileMappingGUID,
        downloadUrl: 'https://example.com/file.csv',
      }
      const mockResponse = { data: mockFileMapping }

      cy.stub(apiClient, 'getFileForDownload').resolves(mockResponse)

      cy.wrap(
        apiActions.getFileForDownload(
          projectionGUID,
          fileSetGUID,
          fileMappingGUID,
        ),
      ).then((result) => {
        expect(apiClient.getFileForDownload).to.be.calledOnceWith(
          projectionGUID,
          fileSetGUID,
          fileMappingGUID,
        )
        expect(result).to.deep.equal(mockFileMapping)
      })
    })

    it('should handle error when getting file for download', () => {
      const mockError = new Error('Download URL failed')
      cy.stub(apiClient, 'getFileForDownload').rejects(mockError)

      apiActions
        .getFileForDownload('proj-guid-123', 'fileset-guid-456', 'file-guid-789')
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.getFileForDownload).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('streamResultsZip', () => {
    it('should stream results zip successfully', () => {
      const mockBlob = new Blob(['zip content'], { type: 'application/zip' })
      const mockResponse = { data: mockBlob, status: 200 }
      cy.stub(apiClient, 'streamResultsZip').resolves(mockResponse)

      const projectionGUID = 'test-guid-123'

      cy.wrap(apiActions.streamResultsZip(projectionGUID)).then(
        (result: any) => {
          expect(apiClient.streamResultsZip).to.be.calledOnceWith(
            projectionGUID,
          )
          expect(result).to.deep.equal(mockResponse)
          expect(result.data).to.equal(mockBlob)
        },
      )
    })

    it('should handle error when streaming results zip', () => {
      const mockError = new Error('Download failed')
      cy.stub(apiClient, 'streamResultsZip').rejects(mockError)

      apiActions
        .streamResultsZip('test-guid-123')
        .then(() => {
          // assert-fail block
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.streamResultsZip).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })

  context('deleteFileFromFileSet', () => {
    it('should delete a file from fileset successfully', () => {
      const projectionGUID = 'proj-guid-123'
      const fileSetGUID = 'fileset-guid-456'
      const fileMappingGUID = 'file-guid-789'

      cy.stub(apiClient, 'deleteFileFromFileSet').resolves()

      cy.wrap(
        apiActions.deleteFileFromFileSet(
          projectionGUID,
          fileSetGUID,
          fileMappingGUID,
        ),
      ).then(() => {
        expect(apiClient.deleteFileFromFileSet).to.be.calledOnceWith(
          projectionGUID,
          fileSetGUID,
          fileMappingGUID,
        )
      })
    })

    it('should handle error when deleting a file from fileset', () => {
      const mockError = new Error('Delete file failed')
      cy.stub(apiClient, 'deleteFileFromFileSet').rejects(mockError)

      apiActions
        .deleteFileFromFileSet(
          'proj-guid-123',
          'fileset-guid-456',
          'file-guid-789',
        )
        .then(() => {
          throw new Error('Test should have failed but succeeded unexpectedly')
        })
        .catch((error: Error) => {
          expect(apiClient.deleteFileFromFileSet).to.be.calledOnce
          expect(error).to.equal(mockError)
        })
    })
  })
})
