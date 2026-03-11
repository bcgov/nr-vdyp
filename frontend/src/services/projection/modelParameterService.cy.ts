/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import {
  generateFeatureId,
  generateRandomNumber,
  generatePolygonNumber,
  generateTreeCoverLayerEstimatedId,
  computeBclcsLevel1,
  computeBclcsLevel2,
  computeBclcsLevel3,
  determineBclcsLevel4,
  determineBclcsLevel5,
  getSpeciesData,
  flattenSpeciesData,
  buildProjectionParameters,
  buildModelParameters,
  createProjection,
  saveProjectionOnPanelConfirm,
  hasPanelUnsavedChanges,
  revertPanelToSaved,
} from '@/services/projection/modelParameterService'
import { CONSTANTS } from '@/constants'
import { PROJECTION_VIEW_MODE } from '@/constants/constants'
import { PROJECTION_ERR } from '@/constants/message'
import {
  ExecutionOptionsEnum,
  DebugOptionsEnum,
  OutputFormatEnum,
  MetadataToOutputEnum,
} from '@/services/vdyp-api'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import apiClient from '@/services/apiClient'

const createMockModelParameterStore = (overrides: Record<string, unknown> = {}) =>
  ({
    projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME,
    derivedBy: CONSTANTS.DERIVED_BY.VOLUME,
    siteSpeciesValues: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
    isForwardGrowEnabled: false,
    isBackwardGrowEnabled: false,
    isComputedMAIEnabled: false,
    isCulminationValuesEnabled: false,
    isBySpeciesEnabled: false,
    incSecondaryHeight: false,
    startingAge: null,
    finishingAge: null,
    ageIncrement: null,
    reportTitle: null,
    copyTitle: null,
    reportDescription: null,
    becZone: 'CWH',
    ecoZone: null,
    percentStockableArea: 55,
    referenceYear: 2023,
    speciesGroups: [],
    speciesList: [],
    highestPercentSpecies: 'FD',
    selectedSiteSpecies: null,
    spzAge: null,
    spzHeight: null,
    bha50SiteIndex: null,
    crownClosure: null,
    basalArea: null,
    treesPerHectare: null,
    ageType: CONSTANTS.AGE_TYPE.TOTAL,
    minDBHLimit: '7.5 cm+',
    currentDiameter: null,
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
  modelParameters: null,
}

describe('generateFeatureId', () => {
  it('should return a positive number', () => {
    const id = generateFeatureId()
    expect(id).to.be.a('number')
    expect(id).to.be.greaterThan(0)
  })

  it('should return a 9 or 10 digit number', () => {
    const id = generateFeatureId()
    const digits = id.toString().length
    expect(digits).to.be.at.least(9).and.at.most(10)
  })

  it('should generate different IDs on successive calls', () => {
    const ids = new Set(Array.from({ length: 10 }, () => generateFeatureId()))
    expect(ids.size).to.be.greaterThan(1)
  })
})

describe('generateRandomNumber', () => {
  it('should return a string', () => {
    expect(generateRandomNumber(3, 5)).to.be.a('string')
  })

  it('should return exactly minDigits when min equals max', () => {
    const result = generateRandomNumber(8, 8)
    expect(result.length).to.equal(8)
  })

  it('should return a number within the digit range', () => {
    for (let i = 0; i < 20; i++) {
      const result = generateRandomNumber(4, 6)
      expect(result.length).to.be.at.least(4).and.at.most(6)
    }
  })

  it('should throw when minDigits is greater than maxDigits', () => {
    expect(() => generateRandomNumber(5, 3)).to.throw('minDigits must be less than or equal to maxDigits')
  })
})

describe('generatePolygonNumber', () => {
  it('should return an 8-digit string', () => {
    const result = generatePolygonNumber()
    expect(result).to.be.a('string')
    expect(result.length).to.equal(8)
  })

  it('should return a numeric string', () => {
    const result = generatePolygonNumber()
    expect(Number(result)).to.not.be.NaN
  })
})

describe('generateTreeCoverLayerEstimatedId', () => {
  it('should return a string with 4 to 10 digits', () => {
    for (let i = 0; i < 20; i++) {
      const result = generateTreeCoverLayerEstimatedId()
      expect(result.length).to.be.at.least(4).and.at.most(10)
    }
  })
})

describe('computeBclcsLevel1', () => {
  it('should return NON_VEG ("N") when percentStockableArea is below threshold (5)', () => {
    expect(computeBclcsLevel1(3)).to.equal('N')
    expect(computeBclcsLevel1(0)).to.equal('N')
  })

  it('should return VEG ("V") when percentStockableArea is at or above threshold (5)', () => {
    expect(computeBclcsLevel1(5)).to.equal('V')
    expect(computeBclcsLevel1(50)).to.equal('V')
  })

  it('should return VEG ("V") when percentStockableArea is undefined (defaults to threshold)', () => {
    expect(computeBclcsLevel1(undefined)).to.equal('V')
  })
})

describe('computeBclcsLevel2', () => {
  it('should return NON_TREED ("N") when percentStockableArea is below threshold (10)', () => {
    expect(computeBclcsLevel2(5)).to.equal('N')
    expect(computeBclcsLevel2(0)).to.equal('N')
  })

  it('should return TREED ("T") when percentStockableArea is at or above threshold (10)', () => {
    expect(computeBclcsLevel2(10)).to.equal('T')
    expect(computeBclcsLevel2(55)).to.equal('T')
  })

  it('should return TREED ("T") when percentStockableArea is undefined (defaults to threshold)', () => {
    expect(computeBclcsLevel2(undefined)).to.equal('T')
  })
})

describe('computeBclcsLevel3', () => {
  it('should return ALPINE ("A") when becZone is "AT"', () => {
    expect(computeBclcsLevel3('AT')).to.equal('A')
  })

  it('should return DEFAULT ("U") for any other becZone', () => {
    expect(computeBclcsLevel3('CWH')).to.equal('U')
    expect(computeBclcsLevel3('BG')).to.equal('U')
    expect(computeBclcsLevel3('ESSF')).to.equal('U')
  })

  it('should return DEFAULT ("U") when becZone is undefined', () => {
    expect(computeBclcsLevel3(undefined)).to.equal('U')
  })
})

describe('determineBclcsLevel4', () => {
  it('should return TC when coniferous species total >= 75%', () => {
    const groups = [
      { siteSpecies: 'FD', percent: '80' },
      { siteSpecies: 'AC', percent: '20' },
    ]
    expect(determineBclcsLevel4(groups as any)).to.equal('TC')
  })

  it('should return TB when broadleaf species total >= 75%', () => {
    const groups = [
      { siteSpecies: 'AC', percent: '80' },
      { siteSpecies: 'FD', percent: '20' },
    ]
    expect(determineBclcsLevel4(groups as any)).to.equal('TB')
  })

  it('should return TM when neither coniferous nor broadleaf reaches 75%', () => {
    const groups = [
      { siteSpecies: 'FD', percent: '50' },
      { siteSpecies: 'AC', percent: '50' },
    ]
    expect(determineBclcsLevel4(groups as any)).to.equal('TM')
  })

  it('should return TM for an empty species group array', () => {
    expect(determineBclcsLevel4([])).to.equal('TM')
  })

  it('should return TC when coniferous total is exactly 75%', () => {
    const groups = [
      { siteSpecies: 'FD', percent: '75' },
      { siteSpecies: 'AC', percent: '25' },
    ]
    expect(determineBclcsLevel4(groups as any)).to.equal('TC')
  })
})

describe('determineBclcsLevel5', () => {
  it('should return DE for percentStockableArea >= 61', () => {
    expect(determineBclcsLevel5(61)).to.equal('DE')
    expect(determineBclcsLevel5(100)).to.equal('DE')
  })

  it('should return OP for percentStockableArea between 26 and 60 (inclusive)', () => {
    expect(determineBclcsLevel5(26)).to.equal('OP')
    expect(determineBclcsLevel5(60)).to.equal('OP')
  })

  it('should return SP for percentStockableArea below 26', () => {
    expect(determineBclcsLevel5(25)).to.equal('SP')
    expect(determineBclcsLevel5(0)).to.equal('SP')
  })
})

describe('getSpeciesData', () => {
  it('should map species and percent to string pairs', () => {
    const input = [
      { species: 'FD', percent: 60 },
      { species: 'PL', percent: 40 },
    ]
    const result = getSpeciesData(input)
    expect(result).to.deep.equal([
      { species: 'FD', percent: '60' },
      { species: 'PL', percent: '40' },
    ])
  })

  it('should return empty string for percent when percent is 0', () => {
    const result = getSpeciesData([{ species: 'FD', percent: 0 }])
    expect(result[0].percent).to.equal('')
  })

  it('should return empty string for percent when percent is null', () => {
    const result = getSpeciesData([{ species: 'FD', percent: null }])
    expect(result[0].percent).to.equal('')
  })

  it('should return empty string for percent when species is null', () => {
    const result = getSpeciesData([{ species: null, percent: 50 }])
    expect(result[0].percent).to.equal('')
  })
})

describe('flattenSpeciesData', () => {
  it('should flatten species and percent into a single array', () => {
    const data = [
      { species: 'FD', percent: '60' },
      { species: 'PL', percent: '40' },
    ]
    expect(flattenSpeciesData(data, 2)).to.deep.equal(['FD', '60', 'PL', '40'])
  })

  it('should respect the count limit', () => {
    const data = [
      { species: 'FD', percent: '60' },
      { species: 'PL', percent: '30' },
      { species: 'AC', percent: '10' },
    ]
    expect(flattenSpeciesData(data, 2)).to.deep.equal(['FD', '60', 'PL', '30'])
  })

  it('should return an empty array when count is 0', () => {
    const data = [{ species: 'FD', percent: '100' }]
    expect(flattenSpeciesData(data, 0)).to.deep.equal([])
  })
})

describe('buildProjectionParameters', () => {
  it('should parse startingAge, finishingAge, ageIncrement as integers', () => {
    const store = createMockModelParameterStore({
      startingAge: '10',
      finishingAge: '200',
      ageIncrement: '5',
    })
    const params = buildProjectionParameters(store)

    expect(params.ageStart).to.equal(10)
    expect(params.ageEnd).to.equal(200)
    expect(params.ageIncrement).to.equal(5)
  })

  it('should set ageStart/ageEnd/ageIncrement to null when not provided', () => {
    const params = buildProjectionParameters(createMockModelParameterStore())

    expect(params.ageStart).to.be.null
    expect(params.ageEnd).to.be.null
    expect(params.ageIncrement).to.be.null
  })

  it('should always set yearStart and yearEnd to null', () => {
    const params = buildProjectionParameters(createMockModelParameterStore())

    expect(params.yearStart).to.be.null
    expect(params.yearEnd).to.be.null
  })

  it('should always use CSVYieldTable output format', () => {
    const params = buildProjectionParameters(createMockModelParameterStore())

    expect(params.outputFormat).to.equal(OutputFormatEnum.CSVYieldTable)
  })

  it('should always use MetadataToOutput NONE', () => {
    const params = buildProjectionParameters(createMockModelParameterStore())

    expect(params.metadataToOutput).to.equal(MetadataToOutputEnum.NONE)
  })

  it('should include the fixed selected execution options', () => {
    const { selectedExecutionOptions } = buildProjectionParameters(createMockModelParameterStore())

    const alwaysSelected = [
      ExecutionOptionsEnum.DoIncludeAgeRowsInYieldTable,
      ExecutionOptionsEnum.DoIncludeColumnHeadersInYieldTable,
      ExecutionOptionsEnum.DoEnableProgressLogging,
      ExecutionOptionsEnum.DoEnableErrorLogging,
      ExecutionOptionsEnum.DoEnableDebugLogging,
      ExecutionOptionsEnum.DoEnableProjectionReport,
      ExecutionOptionsEnum.AllowAggressiveValueEstimation,
      ExecutionOptionsEnum.DoIncludeFileHeader,
      ExecutionOptionsEnum.DoSummarizeProjectionByLayer,
    ]
    alwaysSelected.forEach((opt) => expect(selectedExecutionOptions).to.include(opt))
  })

  it('should add DoIncludeProjectedMOFVolumes to selected when projectionType is VOLUME', () => {
    const store = createMockModelParameterStore({ projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME })
    const { selectedExecutionOptions, excludedExecutionOptions } = buildProjectionParameters(store)

    expect(selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)
    expect(excludedExecutionOptions).to.not.include(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)
  })

  it('should add DoIncludeProjectedCFSBiomass to selected when projectionType is CFS_BIOMASS', () => {
    const store = createMockModelParameterStore({ projectionType: CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS })
    const { selectedExecutionOptions, excludedExecutionOptions } = buildProjectionParameters(store)

    expect(selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)
    expect(excludedExecutionOptions).to.not.include(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)
  })

  it('should add ForwardGrowEnabled to selected when isForwardGrowEnabled is true', () => {
    const store = createMockModelParameterStore({ isForwardGrowEnabled: true })
    const { selectedExecutionOptions } = buildProjectionParameters(store)

    expect(selectedExecutionOptions).to.include(ExecutionOptionsEnum.ForwardGrowEnabled)
  })

  it('should add ForwardGrowEnabled to excluded when isForwardGrowEnabled is false', () => {
    const store = createMockModelParameterStore({ isForwardGrowEnabled: false })
    const { excludedExecutionOptions } = buildProjectionParameters(store)

    expect(excludedExecutionOptions).to.include(ExecutionOptionsEnum.ForwardGrowEnabled)
  })

  it('should map speciesGroups to utils', () => {
    const store = createMockModelParameterStore({
      speciesGroups: [
        { group: 'F', minimumDBHLimit: '4.0 cm+' },
        { group: 'S', minimumDBHLimit: '7.5 cm+' },
      ],
    })
    const params = buildProjectionParameters(store)

    expect(params.utils).to.have.length(2)
    expect(params.utils![0]).to.deep.include({ speciesName: 'F', utilizationClass: '4.0 cm+' })
    expect(params.utils![1]).to.deep.include({ speciesName: 'S', utilizationClass: '7.5 cm+' })
  })

  it('should include the four standard debug options', () => {
    const params = buildProjectionParameters(createMockModelParameterStore())

    expect(params.selectedDebugOptions).to.include(DebugOptionsEnum.DoIncludeDebugTimestamps)
    expect(params.selectedDebugOptions).to.include(DebugOptionsEnum.DoIncludeDebugEntryExit)
    expect(params.selectedDebugOptions).to.include(DebugOptionsEnum.DoIncludeDebugIndentBlocks)
    expect(params.selectedDebugOptions).to.include(DebugOptionsEnum.DoIncludeDebugRoutineNames)
    expect(params.excludedDebugOptions).to.have.length(0)
  })
})

describe('buildModelParameters', () => {
  it('should filter out species with null species or null percent', () => {
    const store = createMockModelParameterStore({
      speciesList: [
        { species: 'FD', percent: 60 },
        { species: null, percent: 40 },
        { species: 'PL', percent: null },
      ],
    })
    const params = buildModelParameters(store)

    expect(params.species).to.have.length(1)
    expect(params.species[0]).to.deep.equal({ code: 'FD', percent: 60 })
  })

  it('should map store fields to ModelParameters correctly', () => {
    const store = createMockModelParameterStore({
      speciesList: [{ species: 'FD', percent: 100 }],
      derivedBy: CONSTANTS.DERIVED_BY.BASAL_AREA,
      becZone: 'CWH',
      ecoZone: '7',
      siteSpeciesValues: CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED,
      selectedSiteSpecies: 'FD',
      ageType: CONSTANTS.AGE_TYPE.BREAST,
      spzAge: '50',
      spzHeight: '20.00',
      bha50SiteIndex: '18.5',
      percentStockableArea: 70,
      crownClosure: 60,
      basalArea: '25.0',
      treesPerHectare: '800',
      minDBHLimit: '12.5 cm+',
      currentDiameter: '10',
    })
    const params = buildModelParameters(store)

    expect(params.derivedBy).to.equal(CONSTANTS.DERIVED_BY.BASAL_AREA)
    expect(params.becZone).to.equal('CWH')
    expect(params.ecoZone).to.equal('7')
    expect(params.siteIndex).to.equal(CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED)
    expect(params.siteSpecies).to.equal('FD')
    expect(params.ageYears).to.equal(CONSTANTS.AGE_TYPE.BREAST)
    expect(params.speciesAge).to.equal('50')
    expect(params.speciesHeight).to.equal('20.00')
    expect(params.bha50SiteIndex).to.equal('18.5')
    expect(params.stockable).to.equal(70)
    expect(params.cc).to.equal(60)
    expect(params.BA).to.equal('25.0')
    expect(params.TPH).to.equal('800')
    expect(params.minDBHLimit).to.equal('12.5 cm+')
    expect(params.currentDiameter).to.equal('10')
  })

  it('should use highestPercentSpecies as siteSpecies when selectedSiteSpecies is null', () => {
    const store = createMockModelParameterStore({
      selectedSiteSpecies: null,
      highestPercentSpecies: 'PL',
    })
    const params = buildModelParameters(store)

    expect(params.siteSpecies).to.equal('PL')
  })

  it('should set speciesAge, speciesHeight, bha50SiteIndex to null when falsy', () => {
    const store = createMockModelParameterStore({
      spzAge: null,
      spzHeight: null,
      bha50SiteIndex: null,
    })
    const params = buildModelParameters(store)

    expect(params.speciesAge).to.be.null
    expect(params.speciesHeight).to.be.null
    expect(params.bha50SiteIndex).to.be.null
  })
})

describe('createProjection', () => {
  it('should call the provided createProjectionFunc with built parameters', () => {
    const mockResult = { ...mockProjectionModel, projectionGUID: 'new-proj-guid' }
    const mockFunc = cy.stub().resolves(mockResult)

    const store = createMockModelParameterStore({
      reportDescription: 'My description',
    })

    cy.wrap(createProjection(store, mockFunc)).then((result: any) => {
      expect(mockFunc).to.be.calledOnce
      expect(result.projectionGUID).to.equal('new-proj-guid')
    })
  })

  it('should pass reportDescription to the createProjectionFunc', () => {
    const mockFunc = cy.stub().resolves(mockProjectionModel)
    const store = createMockModelParameterStore({ reportDescription: 'Test description' })

    cy.wrap(createProjection(store, mockFunc)).then(() => {
      const [, , reportDescription] = mockFunc.getCall(0).args
      expect(reportDescription).to.equal('Test description')
    })
  })
})

describe('saveProjectionOnPanelConfirm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should create a new projection when in CREATE mode and confirming detailsInfo panel', () => {
    cy.stub(apiClient, 'createProjection').resolves({ data: { ...mockProjectionModel, projectionGUID: 'created-guid' } })

    const store = createMockModelParameterStore()
    cy.wrap(saveProjectionOnPanelConfirm(store, CONSTANTS.MANUAL_INPUT_PANEL.REPORT_DETAILS)).then(() => {
      expect(apiClient.createProjection).to.be.calledOnce

      const appStore = useAppStore()
      expect(appStore.getCurrentProjectionGUID).to.equal('created-guid')
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.EDIT)
    })
  })

  it('should update an existing projection when in EDIT mode', () => {
    cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
    cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })

    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('existing-guid')
    appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)

    const store = createMockModelParameterStore()
    cy.wrap(saveProjectionOnPanelConfirm(store, CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO)).then(() => {
      expect(apiClient.updateProjectionParams).to.be.calledOnce
    })
  })

  it('should throw MISSING_GUID when in EDIT mode with no GUID set', () => {
    const appStore = useAppStore()
    appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)

    return saveProjectionOnPanelConfirm(
      createMockModelParameterStore(),
      CONSTANTS.MANUAL_INPUT_PANEL.SITE_INFO,
    )
      .then(() => {
        throw new Error('Test should have failed but succeeded unexpectedly')
      })
      .catch((error: Error) => {
        expect(error.message).to.equal(PROJECTION_ERR.MISSING_GUID)
      })
  })

  it('should NOT create a projection when in CREATE mode but not on detailsInfo panel', () => {
    cy.stub(apiClient, 'createProjection').resolves({ data: mockProjectionModel })

    const store = createMockModelParameterStore()
    cy.wrap(saveProjectionOnPanelConfirm(store, CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO)).then(() => {
      expect(apiClient.createProjection).to.not.be.called
    })
  })
})

const createSavedModelParams = (overrides: Record<string, any> = {}) => ({
  species: [],
  derivedBy: CONSTANTS.DERIVED_BY.VOLUME,
  becZone: 'CWH',
  ecoZone: null,
  siteIndex: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
  siteSpecies: 'FD',
  ageYears: CONSTANTS.AGE_TYPE.TOTAL,
  speciesAge: null,
  speciesHeight: null,
  bha50SiteIndex: null,
  stockable: null,
  cc: null,
  BA: null,
  TPH: null,
  minDBHLimit: '7.5 cm+',
  currentDiameter: null,
  ...overrides,
})

describe('hasPanelUnsavedChanges', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should return false when no projectionGUID is set', () => {
    cy.wrap(
      hasPanelUnsavedChanges(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO, createMockModelParameterStore()),
    ).should('equal', false)
  })

  it('should return false for unknown panel name', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, modelParameters: JSON.stringify(createSavedModelParams()) },
    })
    cy.wrap(hasPanelUnsavedChanges('unknownPanel', createMockModelParameterStore())).should('equal', false)
  })

  it('should return false for SPECIES_INFO when no modelParameters are saved', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, modelParameters: null },
    })
    cy.wrap(
      hasPanelUnsavedChanges(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO, createMockModelParameterStore()),
    ).should('equal', false)
  })

  it('should return false for SPECIES_INFO when nothing has changed', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, modelParameters: JSON.stringify(createSavedModelParams()) },
    })
    cy.wrap(
      hasPanelUnsavedChanges(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO, createMockModelParameterStore()),
    ).should('equal', false)
  })

  it('should return true for SPECIES_INFO when derivedBy has changed', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')
    cy.stub(apiClient, 'getProjection').resolves({
      data: {
        ...mockProjectionModel,
        modelParameters: JSON.stringify(createSavedModelParams({ derivedBy: CONSTANTS.DERIVED_BY.BASAL_AREA })),
      },
    })
    cy.wrap(
      hasPanelUnsavedChanges(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO, createMockModelParameterStore()),
    ).should('equal', true)
  })

  it('should return true for STAND_INFO when percentStockableArea has changed', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')
    cy.stub(apiClient, 'getProjection').resolves({
      data: {
        ...mockProjectionModel,
        modelParameters: JSON.stringify(createSavedModelParams({ stockable: 90 })),
      },
    })
    cy.wrap(
      hasPanelUnsavedChanges(CONSTANTS.MANUAL_INPUT_PANEL.STAND_INFO, createMockModelParameterStore()),
    ).should('equal', true)
  })
})

describe('revertPanelToSaved', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should return early without error when no projectionGUID is set', () => {
    cy.wrap(revertPanelToSaved(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO as any)).then(() => {
      const modelStore = useModelParameterStore()
      expect(modelStore.panelState.speciesInfo.confirmed).to.be.false
    })
  })

  it('should open and set the cancelled panel back to editable after revert', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')
    cy.stub(apiClient, 'getProjection').resolves({
      data: {
        ...mockProjectionModel,
        modelParameters: JSON.stringify(createSavedModelParams()),
        reportDescription: null,
      },
    })
    cy.wrap(revertPanelToSaved(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO as any)).then(() => {
      const modelStore = useModelParameterStore()
      expect(modelStore.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(modelStore.panelState.speciesInfo.confirmed).to.be.false
      expect(modelStore.panelState.speciesInfo.editable).to.be.true
    })
  })
})
