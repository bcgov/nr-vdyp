/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useProjectionLoader } from '@/composables/useProjectionLoader'
import { useAppStore } from '@/stores/projection/appStore'
import { MODEL_SELECTION, PROJECTION_STATUS, PROJECTION_VIEW_MODE } from '@/constants/constants'
import { ExecutionOptionsEnum } from '@/services/vdyp-api'
import apiClient from '@/services/apiClient'

const makeProjectionModel = (overrides: Record<string, unknown> = {}) => ({
  projectionGUID: 'test-guid',
  reportTitle: 'Test Projection',
  reportDescription: null,
  projectionStatusCode: { code: 'DRAFT', description: 'Draft', displayOrder: 0 },
  projectionParameters: null,
  modelParameters: null,
  polygonFileSet: null,
  layerFileSet: null,
  createDate: '2024-01-01T00:00:00Z',
  ...overrides,
})

const makeParamsJson = (executionOptions: string[]) =>
  JSON.stringify({ selectedExecutionOptions: executionOptions })

describe('useProjectionLoader Unit Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('Initial State', () => {
    it('should initialize isLoading as false', () => {
      const { isLoading } = useProjectionLoader()
      expect(isLoading.value).to.be.false
    })

    it('should initialize loadError as null', () => {
      const { loadError } = useProjectionLoader()
      expect(loadError.value).to.be.null
    })
  })

  describe('loadProjection - INPUT_MODEL_PARAMETERS flow', () => {
    it('should return true on successful load', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([ExecutionOptionsEnum.DoEnableProjectionReport]),
        }),
      })

      const { loadProjection } = useProjectionLoader()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then((result: any) => {
        expect(result).to.be.true
      })
    })

    it('should set modelSelection to INPUT_MODEL_PARAMETERS', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([ExecutionOptionsEnum.DoEnableProjectionReport]),
        }),
      })

      const { loadProjection } = useProjectionLoader()
      const appStore = useAppStore()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(appStore.modelSelection).to.equal(MODEL_SELECTION.INPUT_MODEL_PARAMETERS)
      })
    })

    it('should set viewMode on appStore', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([ExecutionOptionsEnum.DoEnableProjectionReport]),
        }),
      })

      const { loadProjection } = useProjectionLoader()
      const appStore = useAppStore()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.VIEW)).then(() => {
        expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.VIEW)
      })
    })

    it('should set currentProjectionGUID on appStore', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([ExecutionOptionsEnum.DoEnableProjectionReport]),
        }),
      })

      const { loadProjection } = useProjectionLoader()
      const appStore = useAppStore()
      cy.wrap(loadProjection('proj-guid-123', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(appStore.currentProjectionGUID).to.equal('proj-guid-123')
      })
    })

    it('should set currentProjectionStatus from projectionStatusCode', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([ExecutionOptionsEnum.DoEnableProjectionReport]),
          projectionStatusCode: { code: 'READY', description: 'Ready', displayOrder: 1 },
        }),
      })

      const { loadProjection } = useProjectionLoader()
      const appStore = useAppStore()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.VIEW)).then(() => {
        expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.READY)
      })
    })

    it('should default to DRAFT status when projectionStatusCode is absent', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([ExecutionOptionsEnum.DoEnableProjectionReport]),
          projectionStatusCode: null,
        }),
      })

      const { loadProjection } = useProjectionLoader()
      const appStore = useAppStore()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.DRAFT)
      })
    })

    it('should set isLoading to false after successful load', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([ExecutionOptionsEnum.DoEnableProjectionReport]),
        }),
      })

      const { loadProjection, isLoading } = useProjectionLoader()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(isLoading.value).to.be.false
      })
    })
  })

  describe('loadProjection - FILE_UPLOAD flow', () => {
    it('should set modelSelection to FILE_UPLOAD when DoEnableProjectionReport is absent', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([ExecutionOptionsEnum.ForwardGrowEnabled]),
        }),
      })

      const { loadProjection } = useProjectionLoader()
      const appStore = useAppStore()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(appStore.modelSelection).to.equal(MODEL_SELECTION.FILE_UPLOAD)
      })
    })

    it('should call getFileSetFiles for polygon fileset', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([]),
          polygonFileSet: { projectionFileSetGUID: 'poly-set-guid' },
        }),
      })
      cy.stub(apiClient, 'getFileSetFiles').resolves({
        data: [{ filename: 'polygon.csv', fileMappingGUID: 'file-map-guid' }],
      })

      const { loadProjection } = useProjectionLoader()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(apiClient.getFileSetFiles).to.have.been.calledWith('test-guid', 'poly-set-guid')
      })
    })

    it('should call getFileSetFiles for layer fileset', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([]),
          layerFileSet: { projectionFileSetGUID: 'layer-set-guid' },
        }),
      })
      cy.stub(apiClient, 'getFileSetFiles').resolves({
        data: [{ filename: 'layer.csv', fileMappingGUID: 'file-map-guid-2' }],
      })

      const { loadProjection } = useProjectionLoader()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(apiClient.getFileSetFiles).to.have.been.calledWith('test-guid', 'layer-set-guid')
      })
    })

    it('should not call getFileSetFiles when no polygon or layer fileset is present', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([]),
          polygonFileSet: null,
          layerFileSet: null,
        }),
      })
      const getFileSetFilesStub = cy.stub(apiClient, 'getFileSetFiles').resolves({ data: [] })

      const { loadProjection } = useProjectionLoader()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(getFileSetFilesStub).to.not.have.been.called
      })
    })

    it('should apply polygon file info to fileUploadStore when fileset has files', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([]),
          polygonFileSet: { projectionFileSetGUID: 'poly-set-guid' },
        }),
      })
      cy.stub(apiClient, 'getFileSetFiles').resolves({
        data: [{ filename: 'VDYP7_INPUT_POLY.csv', fileMappingGUID: 'poly-file-guid' }],
      })

      const { loadProjection } = useProjectionLoader()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then((result: any) => {
        expect(result).to.be.true
      })
    })
  })

  describe('loadProjection - duplicatedFromInfo', () => {
    it('should set duplicatedFromInfo when copyTitle is present in params', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: JSON.stringify({
            selectedExecutionOptions: [ExecutionOptionsEnum.DoEnableProjectionReport],
            copyTitle: 'Original Projection',
          }),
          createDate: '2024-06-01T00:00:00Z',
        }),
      })

      const { loadProjection } = useProjectionLoader()
      const appStore = useAppStore()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.VIEW)).then(() => {
        expect(appStore.duplicatedFromInfo).to.not.be.null
        expect(appStore.duplicatedFromInfo!.originalName).to.equal('Original Projection')
        expect(appStore.duplicatedFromInfo!.duplicatedAt).to.equal('2024-06-01T00:00:00Z')
      })
    })

    it('should clear duplicatedFromInfo when copyTitle is absent', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: makeParamsJson([ExecutionOptionsEnum.DoEnableProjectionReport]),
        }),
      })

      const { loadProjection } = useProjectionLoader()
      const appStore = useAppStore()
      appStore.setDuplicatedFromInfo({ originalName: 'Previous', duplicatedAt: '2024-01-01T00:00:00Z' })

      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(appStore.duplicatedFromInfo).to.be.null
      })
    })

    it('should use new Date ISO string as duplicatedAt when createDate is missing', () => {
      cy.stub(apiClient, 'getProjection').resolves({
        data: makeProjectionModel({
          projectionParameters: JSON.stringify({
            selectedExecutionOptions: [],
            copyTitle: 'Source Projection',
          }),
          createDate: null,
        }),
      })

      const { loadProjection } = useProjectionLoader()
      const appStore = useAppStore()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.VIEW)).then(() => {
        expect(appStore.duplicatedFromInfo).to.not.be.null
        expect(appStore.duplicatedFromInfo!.duplicatedAt).to.be.a('string')
      })
    })
  })

  describe('loadProjection - error handling', () => {
    it('should return false when API throws', () => {
      cy.stub(apiClient, 'getProjection').rejects(new Error('Network error'))

      const { loadProjection } = useProjectionLoader()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then((result: any) => {
        expect(result).to.be.false
      })
    })

    it('should set loadError when API throws', () => {
      cy.stub(apiClient, 'getProjection').rejects(new Error('Network error'))

      const { loadError, loadProjection } = useProjectionLoader()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(loadError.value).to.not.be.null
        expect(loadError.value).to.include('Failed to load')
      })
    })

    it('should set isLoading to false after an error', () => {
      cy.stub(apiClient, 'getProjection').rejects(new Error('Network error'))

      const { isLoading, loadProjection } = useProjectionLoader()
      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(isLoading.value).to.be.false
      })
    })

    it('should clear loadError on a subsequent successful load', () => {
      const { loadError, loadProjection } = useProjectionLoader()

      cy.stub(apiClient, 'getProjection')
        .onFirstCall()
        .rejects(new Error('Network error'))
        .onSecondCall()
        .resolves({
          data: makeProjectionModel({
            projectionParameters: makeParamsJson([ExecutionOptionsEnum.DoEnableProjectionReport]),
          }),
        })

      cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
        expect(loadError.value).to.not.be.null
        cy.wrap(loadProjection('test-guid', PROJECTION_VIEW_MODE.EDIT)).then(() => {
          expect(loadError.value).to.be.null
        })
      })
    })
  })
})
