/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import {
  buildExecutionOptions,
  buildDebugOptions,
  buildProjectionParameters,
  runProjectionFileUpload,
  ensureProjectionExists,
  saveProjectionOnPanelConfirm,
  hasMinimumDBHUnsavedChanges,
  revertPanelToSaved,
} from '@/services/projection/fileUploadService'
import { CONSTANTS } from '@/constants'
import { PROJECTION_VIEW_MODE } from '@/constants/constants'
import { PROJECTION_ERR } from '@/constants/message'
import {
  ExecutionOptionsEnum,
  DebugOptionsEnum,
  OutputFormatEnum,
  CombineAgeYearRangeEnum,
  MetadataToOutputEnum,
} from '@/services/vdyp-api'
import { useAppStore } from '@/stores/projection/appStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import apiClient from '@/services/apiClient'

const createMockFileUploadStore = (overrides: Record<string, unknown> = {}) =>
  ({
    projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME,
    isForwardGrowEnabled: false,
    isBackwardGrowEnabled: false,
    isByLayerEnabled: false,
    isBySpeciesEnabled: false,
    isProjectionModeEnabled: false,
    isPolygonIDEnabled: false,
    isCurrentYearEnabled: false,
    isReferenceYearEnabled: false,
    incSecondaryHeight: false,
    selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.AGE,
    startingAge: null,
    finishingAge: null,
    ageIncrement: null,
    yearIncrement: null,
    startYear: null,
    endYear: null,
    specificYear: null,
    reportTitle: null,
    fileUploadSpeciesGroup: [],
    reportDescription: null,
    ...overrides,
  }) as any

const mockProjectionModel = {
  projectionGUID: 'test-guid',
  reportTitle: 'Test Report',
  reportDescription: null,
  projectionStatusCode: { code: 'DRAFT', description: '', displayOrder: 0 },
  lastUpdatedDate: '2024-01-01',
  expiryDate: '2024-07-01',
  projectionParameters: null,
}

describe('buildDebugOptions', () => {
  it('should always return the four standard debug options as selected', () => {
    const { selectedDebugOptions, excludedDebugOptions } = buildDebugOptions()

    expect(selectedDebugOptions).to.include(DebugOptionsEnum.DoIncludeDebugTimestamps)
    expect(selectedDebugOptions).to.include(DebugOptionsEnum.DoIncludeDebugEntryExit)
    expect(selectedDebugOptions).to.include(DebugOptionsEnum.DoIncludeDebugIndentBlocks)
    expect(selectedDebugOptions).to.include(DebugOptionsEnum.DoIncludeDebugRoutineNames)
    expect(excludedDebugOptions).to.have.length(0)
  })
})

describe('buildExecutionOptions', () => {
  it('should always include the fixed selected options', () => {
    const { selectedExecutionOptions } = buildExecutionOptions(createMockFileUploadStore())

    const alwaysSelected = [
      ExecutionOptionsEnum.DoIncludeFileHeader,
      ExecutionOptionsEnum.DoIncludeAgeRowsInYieldTable,
      ExecutionOptionsEnum.DoIncludeYearRowsInYieldTable,
      ExecutionOptionsEnum.DoIncludeColumnHeadersInYieldTable,
      ExecutionOptionsEnum.DoAllowBasalAreaAndTreesPerHectareValueSubstitution,
      ExecutionOptionsEnum.DoEnableProgressLogging,
      ExecutionOptionsEnum.DoEnableErrorLogging,
      ExecutionOptionsEnum.DoEnableDebugLogging,
      ExecutionOptionsEnum.ForwardGrowEnabled,
    ]
    alwaysSelected.forEach((opt) => expect(selectedExecutionOptions).to.include(opt))
  })

  it('should always include the fixed excluded options', () => {
    const { excludedExecutionOptions } = buildExecutionOptions(createMockFileUploadStore())

    const alwaysExcluded = [
      ExecutionOptionsEnum.DoSaveIntermediateFiles,
      ExecutionOptionsEnum.AllowAggressiveValueEstimation,
      ExecutionOptionsEnum.DoIncludeProjectionFiles,
    ]
    alwaysExcluded.forEach((opt) => expect(excludedExecutionOptions).to.include(opt))
  })

  it('should add DoIncludeProjectedMOFVolumes to selected when projectionType is VOLUME', () => {
    const store = createMockFileUploadStore({ projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME })
    const { selectedExecutionOptions, excludedExecutionOptions } = buildExecutionOptions(store)

    expect(selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)
    expect(excludedExecutionOptions).to.not.include(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)
  })

  it('should add DoIncludeProjectedCFSBiomass to selected when projectionType is CFS_BIOMASS', () => {
    const store = createMockFileUploadStore({ projectionType: CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS })
    const { selectedExecutionOptions, excludedExecutionOptions } = buildExecutionOptions(store)

    expect(selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)
    expect(excludedExecutionOptions).to.not.include(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)
  })

  it('should always include ForwardGrowEnabled in selected regardless of isForwardGrowEnabled', () => {
    const storeEnabled = createMockFileUploadStore({ isForwardGrowEnabled: true })
    const { selectedExecutionOptions: selected1, excludedExecutionOptions: excluded1 } = buildExecutionOptions(storeEnabled)
    expect(selected1).to.include(ExecutionOptionsEnum.ForwardGrowEnabled)
    expect(excluded1).to.not.include(ExecutionOptionsEnum.ForwardGrowEnabled)

    const storeDisabled = createMockFileUploadStore({ isForwardGrowEnabled: false })
    const { selectedExecutionOptions: selected2, excludedExecutionOptions: excluded2 } = buildExecutionOptions(storeDisabled)
    expect(selected2).to.include(ExecutionOptionsEnum.ForwardGrowEnabled)
    expect(excluded2).to.not.include(ExecutionOptionsEnum.ForwardGrowEnabled)
  })

  it('should add DoSummarizeProjectionByLayer to selected and ByPolygon to excluded when isByLayerEnabled', () => {
    const store = createMockFileUploadStore({ isByLayerEnabled: true })
    const { selectedExecutionOptions, excludedExecutionOptions } = buildExecutionOptions(store)

    expect(selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoSummarizeProjectionByLayer)
    expect(excludedExecutionOptions).to.include(ExecutionOptionsEnum.DoSummarizeProjectionByPolygon)
  })

  it('should add DoSummarizeProjectionByPolygon to selected and ByLayer to excluded when not isByLayerEnabled', () => {
    const store = createMockFileUploadStore({ isByLayerEnabled: false })
    const { selectedExecutionOptions, excludedExecutionOptions } = buildExecutionOptions(store)

    expect(selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoSummarizeProjectionByPolygon)
    expect(excludedExecutionOptions).to.include(ExecutionOptionsEnum.DoSummarizeProjectionByLayer)
  })
})

describe('buildProjectionParameters', () => {
  it('should set ageStart/ageEnd and null yearStart/yearEnd in AGE range mode', () => {
    const store = createMockFileUploadStore({
      selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.AGE,
      startingAge: '10',
      finishingAge: '100',
      ageIncrement: '5',
    })
    const params = buildProjectionParameters(store)

    expect(params.ageStart).to.equal(10)
    expect(params.ageEnd).to.equal(100)
    expect(params.ageIncrement).to.equal(5)
    expect(params.yearStart).to.be.null
    expect(params.yearEnd).to.be.null
  })

  it('should set yearStart/yearEnd and ageIncrement from yearIncrement in YEAR range mode', () => {
    const store = createMockFileUploadStore({
      selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.YEAR,
      startYear: '2020',
      endYear: '2050',
      yearIncrement: '2',
    })
    const params = buildProjectionParameters(store)

    expect(params.yearStart).to.equal(2020)
    expect(params.yearEnd).to.equal(2050)
    expect(params.ageIncrement).to.equal(2)
    expect(params.ageStart).to.be.null
    expect(params.ageEnd).to.be.null
  })

  it('should set forceYear from specificYear', () => {
    const store = createMockFileUploadStore({ specificYear: '2030' })
    const params = buildProjectionParameters(store)

    expect(params.forceYear).to.equal(2030)
  })

  it('should set forceYear to null when specificYear is falsy', () => {
    const store = createMockFileUploadStore({ specificYear: null })
    const params = buildProjectionParameters(store)

    expect(params.forceYear).to.be.null
  })

  it('should always use CSVYieldTable output format and Intersect combineAgeYearRange', () => {
    const params = buildProjectionParameters(createMockFileUploadStore())

    expect(params.outputFormat).to.equal(OutputFormatEnum.CSVYieldTable)
    expect(params.combineAgeYearRange).to.equal(CombineAgeYearRangeEnum.Intersect)
    expect(params.metadataToOutput).to.equal(MetadataToOutputEnum.VERSION)
  })

  it('should map fileUploadSpeciesGroup to utils', () => {
    const store = createMockFileUploadStore({
      fileUploadSpeciesGroup: [
        { group: 'Fir', minimumDBHLimit: '4.0 cm+' },
        { group: 'Pine', minimumDBHLimit: '7.5 cm+' },
      ],
    })
    const params = buildProjectionParameters(store)

    expect(params.utils).to.have.length(2)
    expect(params.utils![0]).to.deep.include({ speciesName: 'Fir', utilizationClass: '4.0 cm+' })
    expect(params.utils![1]).to.deep.include({ speciesName: 'Pine', utilizationClass: '7.5 cm+' })
  })

  it('should include reportTitle from the store', () => {
    const store = createMockFileUploadStore({ reportTitle: 'My Report' })
    const params = buildProjectionParameters(store)

    expect(params.reportTitle).to.equal('My Report')
  })
})

describe('runProjectionFileUpload', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should throw when no projection GUID is set', () => {
    return runProjectionFileUpload()
      .then(() => {
        throw new Error('Test should have failed but succeeded unexpectedly')
      })
      .catch((error: Error) => {
        expect(error.message).to.equal(PROJECTION_ERR.MISSING_GUID)
      })
  })

  it('should call apiClient.runProjection with the current GUID', () => {
    cy.stub(apiClient, 'runProjection').resolves({ data: mockProjectionModel })

    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('test-guid')

    cy.wrap(runProjectionFileUpload()).then((result: any) => {
      expect(apiClient.runProjection).to.be.calledOnceWith('test-guid')
      expect(result.projectionGUID).to.equal('test-guid')
    })
  })
})

describe('ensureProjectionExists', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should return the existing GUID without creating a new projection', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('existing-guid')

    cy.wrap(ensureProjectionExists(createMockFileUploadStore())).then((guid: any) => {
      expect(guid).to.equal('existing-guid')
    })
  })

  it('should create a new projection and return its GUID when none exists', () => {
    cy.stub(apiClient, 'createProjection').resolves({ data: { ...mockProjectionModel, projectionGUID: 'new-guid' } })

    const mockStore = createMockFileUploadStore()
    cy.wrap(ensureProjectionExists(mockStore)).then((guid: any) => {
      expect(guid).to.equal('new-guid')
      expect(apiClient.createProjection).to.be.calledOnce

      const appStore = useAppStore()
      expect(appStore.getCurrentProjectionGUID).to.equal('new-guid')
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.EDIT)
    })
  })
})

describe('saveProjectionOnPanelConfirm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should create a new projection when confirming reportInfo with no existing GUID', () => {
    cy.stub(apiClient, 'createProjection').resolves({ data: { ...mockProjectionModel, projectionGUID: 'created-guid' } })

    const mockStore = createMockFileUploadStore()
    cy.wrap(saveProjectionOnPanelConfirm(mockStore, CONSTANTS.FILE_UPLOAD_PANEL.REPORT_CONFIG)).then(() => {
      expect(apiClient.createProjection).to.be.calledOnce

      const appStore = useAppStore()
      expect(appStore.getCurrentProjectionGUID).to.equal('created-guid')
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.EDIT)
    })
  })

  it('should update an existing projection when confirming reportInfo with an existing GUID', () => {
    cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
    cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })
    cy.stub(apiClient, 'createProjection').resolves({ data: mockProjectionModel })

    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('existing-guid')

    const mockStore = createMockFileUploadStore()
    cy.wrap(saveProjectionOnPanelConfirm(mockStore, CONSTANTS.FILE_UPLOAD_PANEL.REPORT_CONFIG)).then(() => {
      expect(apiClient.updateProjectionParams).to.be.calledOnce
      expect(apiClient.createProjection).to.not.be.called
    })
  })

  it('should update params when confirming a non-reportConfig panel in EDIT mode', () => {
    cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
    cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })

    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('existing-guid')
    appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)

    const mockStore = createMockFileUploadStore()
    cy.wrap(saveProjectionOnPanelConfirm(mockStore, CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH)).then(() => {
      expect(apiClient.updateProjectionParams).to.be.calledOnce
    })
  })

  it('should throw when confirming a non-reportConfig panel with no GUID in EDIT mode', () => {
    const appStore = useAppStore()
    appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)

    return saveProjectionOnPanelConfirm(createMockFileUploadStore(), CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH)
      .then(() => {
        throw new Error('Test should have failed but succeeded unexpectedly')
      })
      .catch((error: Error) => {
        expect(error.message).to.equal(PROJECTION_ERR.MISSING_GUID)
      })
  })
})

describe('hasMinimumDBHUnsavedChanges', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should return false when no projectionGUID is set', () => {
    const store = createMockFileUploadStore({
      fileUploadSpeciesGroup: [{ group: 'F', minimumDBHLimit: '7.5+' }],
    })
    cy.wrap(hasMinimumDBHUnsavedChanges(store)).should('equal', false)
  })

  it('should return false when minimumDBHLimit matches the volume default (no utils saved)', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')

    const projParams = JSON.stringify({
      selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes],
      utils: [],
    })
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, projectionParameters: projParams },
    })

    const store = createMockFileUploadStore({
      fileUploadSpeciesGroup: [{ group: 'F', minimumDBHLimit: '7.5+' }],
    })
    cy.wrap(hasMinimumDBHUnsavedChanges(store)).should('equal', false)
  })

  it('should return true when minimumDBHLimit differs from the volume default (no utils saved)', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')

    const projParams = JSON.stringify({
      selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes],
      utils: [],
    })
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, projectionParameters: projParams },
    })

    const store = createMockFileUploadStore({
      fileUploadSpeciesGroup: [{ group: 'F', minimumDBHLimit: '12.5+' }],
    })
    cy.wrap(hasMinimumDBHUnsavedChanges(store)).should('equal', true)
  })

  it('should return false when minimumDBHLimit matches the saved utils value', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')

    const projParams = JSON.stringify({
      selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes],
      utils: [{ s: 'F', u: '12.5+' }],
    })
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, projectionParameters: projParams },
    })

    const store = createMockFileUploadStore({
      fileUploadSpeciesGroup: [{ group: 'F', minimumDBHLimit: '12.5+' }],
    })
    cy.wrap(hasMinimumDBHUnsavedChanges(store)).should('equal', false)
  })

  it('should return true when minimumDBHLimit differs from the saved utils value', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')

    const projParams = JSON.stringify({
      selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes],
      utils: [{ s: 'F', u: '12.5+' }],
    })
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, projectionParameters: projParams },
    })

    const store = createMockFileUploadStore({
      fileUploadSpeciesGroup: [{ group: 'F', minimumDBHLimit: '7.5+' }],
    })
    cy.wrap(hasMinimumDBHUnsavedChanges(store)).should('equal', true)
  })
})

describe('revertPanelToSaved', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should return early without error when no projectionGUID is set', () => {
    cy.wrap(revertPanelToSaved(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_CONFIG as any)).then(() => {
      const fileUploadStore = useFileUploadStore()
      expect(fileUploadStore.panelState.reportConfig.confirmed).to.be.false
    })
  })

  it('should keep the cancelled panel open and editable after revert', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')

    const projParams = JSON.stringify({
      selectedExecutionOptions: [],
      utils: [],
    })
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, projectionParameters: projParams, reportDescription: 'saved desc' },
    })

    cy.wrap(revertPanelToSaved(CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH as any)).then(() => {
      const fileUploadStore = useFileUploadStore()
      expect(fileUploadStore.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.OPEN)
      expect(fileUploadStore.panelState.minimumDBH.confirmed).to.be.false
      expect(fileUploadStore.panelState.minimumDBH.editable).to.be.true
    })
  })
})
