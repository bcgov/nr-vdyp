/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import {
  generateFeatureId,
  generateRandomNumber,
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
    siteIndexRows: [],
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

// ─ ID generators

describe('generateFeatureId', () => {
  it('should return a positive number and differ across calls', () => {
    expect(generateFeatureId()).to.be.a('number').and.be.greaterThan(0)
    const ids = new Set(Array.from({ length: 10 }, () => generateFeatureId()))
    expect(ids.size).to.be.greaterThan(1)
  })
})

describe('generateRandomNumber', () => {
  it('should return a string whose length falls within the specified range', () => {
    expect(generateRandomNumber(8, 8).length).to.equal(8)
    for (let i = 0; i < 20; i++) {
      expect(generateRandomNumber(4, 6).length).to.be.at.least(4).and.at.most(6)
    }
  })

  it('should throw when minDigits > maxDigits', () => {
    expect(() => generateRandomNumber(5, 3)).to.throw('minDigits must be less than or equal to maxDigits')
  })
})


// ─ BCLCS level computations

describe('computeBclcsLevel1', () => {
  it('should return "N" below threshold and "V" at or above (including undefined)', () => {
    expect(computeBclcsLevel1(3)).to.equal('N')
    expect(computeBclcsLevel1(5)).to.equal('V')
    expect(computeBclcsLevel1(undefined)).to.equal('V')
  })
})

describe('computeBclcsLevel2', () => {
  it('should return "N" below threshold and "T" at or above (including undefined)', () => {
    expect(computeBclcsLevel2(5)).to.equal('N')
    expect(computeBclcsLevel2(10)).to.equal('T')
    expect(computeBclcsLevel2(undefined)).to.equal('T')
  })
})

describe('computeBclcsLevel3', () => {
  it('should return "A" for "AT" and "U" for any other zone (including undefined)', () => {
    expect(computeBclcsLevel3('AT')).to.equal('A')
    expect(computeBclcsLevel3('CWH')).to.equal('U')
    expect(computeBclcsLevel3(undefined)).to.equal('U')
  })
})

describe('determineBclcsLevel4', () => {
  it('should return TC when coniferous total >= 75%', () => {
    expect(determineBclcsLevel4([{ siteSpecies: 'FD', percent: '80' }, { siteSpecies: 'AC', percent: '20' }] as any)).to.equal('TC')
    expect(determineBclcsLevel4([{ siteSpecies: 'FD', percent: '75' }, { siteSpecies: 'AC', percent: '25' }] as any)).to.equal('TC')
  })

  it('should return TB when broadleaf total >= 75%', () => {
    expect(determineBclcsLevel4([{ siteSpecies: 'AC', percent: '80' }, { siteSpecies: 'FD', percent: '20' }] as any)).to.equal('TB')
  })

  it('should return TM when neither reaches 75% or groups are empty', () => {
    expect(determineBclcsLevel4([{ siteSpecies: 'FD', percent: '50' }, { siteSpecies: 'AC', percent: '50' }] as any)).to.equal('TM')
    expect(determineBclcsLevel4([])).to.equal('TM')
  })
})

describe('determineBclcsLevel5', () => {
  it('should return DE for >= 61, OP for 26-60, and SP for < 26', () => {
    expect(determineBclcsLevel5(61)).to.equal('DE')
    expect(determineBclcsLevel5(26)).to.equal('OP')
    expect(determineBclcsLevel5(60)).to.equal('OP')
    expect(determineBclcsLevel5(25)).to.equal('SP')
  })
})

// ─ Species helpers

describe('getSpeciesData', () => {
  it('should convert percent to string and return empty string for missing or zero values', () => {
    expect(getSpeciesData([{ species: 'FD', percent: 60 }, { species: 'PL', percent: 40 }])).to.deep.equal([
      { species: 'FD', percent: '60' },
      { species: 'PL', percent: '40' },
    ])
    expect(getSpeciesData([{ species: 'FD', percent: 0 }])[0].percent).to.equal('')
    expect(getSpeciesData([{ species: null, percent: 50 }])[0].percent).to.equal('')
  })
})

describe('flattenSpeciesData', () => {
  it('should flatten species/percent pairs up to count and return empty array when count is 0', () => {
    const data = [{ species: 'FD', percent: '60' }, { species: 'PL', percent: '30' }, { species: 'AC', percent: '10' }]
    expect(flattenSpeciesData(data, 2)).to.deep.equal(['FD', '60', 'PL', '30'])
    expect(flattenSpeciesData(data, 0)).to.deep.equal([])
  })
})

// ─ Parameter builders

describe('buildProjectionParameters', () => {
  it('should parse ages as integers and default to null with fixed output format when omitted', () => {
    const withAges = buildProjectionParameters(createMockModelParameterStore({ startingAge: '10', finishingAge: '200', ageIncrement: '5' }))
    expect(withAges.ageStart).to.equal(10)
    expect(withAges.ageEnd).to.equal(200)
    expect(withAges.ageIncrement).to.equal(5)

    const defaults = buildProjectionParameters(createMockModelParameterStore())
    expect(defaults.ageStart).to.be.null
    expect(defaults.ageEnd).to.be.null
    expect(defaults.outputFormat).to.equal(OutputFormatEnum.CSVYieldTable)
    expect(defaults.metadataToOutput).to.equal(MetadataToOutputEnum.NONE)
  })

  it('should always include the fixed execution options and ForwardGrowEnabled', () => {
    const { selectedExecutionOptions } = buildProjectionParameters(createMockModelParameterStore())
    ;[
      ExecutionOptionsEnum.DoIncludeAgeRowsInYieldTable,
      ExecutionOptionsEnum.DoIncludeColumnHeadersInYieldTable,
      ExecutionOptionsEnum.DoEnableProgressLogging,
      ExecutionOptionsEnum.DoEnableErrorLogging,
      ExecutionOptionsEnum.DoEnableDebugLogging,
      ExecutionOptionsEnum.DoEnableProjectionReport,
      ExecutionOptionsEnum.AllowAggressiveValueEstimation,
      ExecutionOptionsEnum.DoIncludeFileHeader,
      ExecutionOptionsEnum.DoSummarizeProjectionByLayer,
      ExecutionOptionsEnum.ForwardGrowEnabled,
    ].forEach((opt) => expect(selectedExecutionOptions).to.include(opt))
  })

  it('should add the projection-type-specific execution option', () => {
    const volume = buildProjectionParameters(createMockModelParameterStore({ projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME }))
    expect(volume.selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)

    const biomass = buildProjectionParameters(createMockModelParameterStore({ projectionType: CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS }))
    expect(biomass.selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)

    const both = buildProjectionParameters(createMockModelParameterStore({ projectionType: CONSTANTS.PROJECTION_TYPE.BOTH }))
    expect(both.selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)
    expect(both.selectedExecutionOptions).to.include(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)
  })

  it('should map speciesGroups to utils and include all four debug options', () => {
    const store = createMockModelParameterStore({ speciesGroups: [{ group: 'F', minimumDBHLimit: '4.0 cm+' }] })
    const params = buildProjectionParameters(store)

    expect(params.utils).to.have.length(1)
    expect(params.utils![0]).to.deep.include({ speciesName: 'F', utilizationClass: '4.0 cm+' })
    ;[
      DebugOptionsEnum.DoIncludeDebugTimestamps,
      DebugOptionsEnum.DoIncludeDebugEntryExit,
      DebugOptionsEnum.DoIncludeDebugIndentBlocks,
      DebugOptionsEnum.DoIncludeDebugRoutineNames,
    ].forEach((opt) => expect(params.selectedDebugOptions).to.include(opt))
    expect(params.excludedDebugOptions).to.have.length(0)
  })
})

describe('buildModelParameters', () => {
  it('should filter out null species/percent entries and use highestPercentSpecies as fallback siteSpecies', () => {
    const store = createMockModelParameterStore({
      speciesList: [{ species: 'FD', percent: 60 }, { species: null, percent: 40 }, { species: 'PL', percent: null }],
      selectedSiteSpecies: null,
      highestPercentSpecies: 'PL',
    })
    const params = buildModelParameters(store)
    expect(params.species).to.have.length(1)
    expect(params.species[0]).to.deep.equal({ code: 'FD', percent: 60 })
    expect(params.siteSpecies).to.equal('PL')
  })

  it('should map all store fields to ModelParameters correctly', () => {
    const store = createMockModelParameterStore({
      speciesList: [{ species: 'FD', percent: 100 }],
      derivedBy: CONSTANTS.DERIVED_BY.BASAL_AREA,
      becZone: 'CWH',
      ecoZone: '7',
      siteSpeciesValues: CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED,
      selectedSiteSpecies: 'FD',
      siteIndexRows: [{
        speciesCode: 'FD',
        computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX,
        ageType: CONSTANTS.AGE_TYPE.BREAST,
        age: '50',
        height: '20.00',
        bhaSiteIndex: '18.5',
      }],
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
    expect(params.compute).to.equal(CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX)
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

  it('should return null site index fields when siteIndexRows is empty and serialize secondary rows into compute2+ fields', () => {
    const emptyParams = buildModelParameters(createMockModelParameterStore({ siteIndexRows: [] }))
    expect(emptyParams.speciesAge).to.be.null
    expect(emptyParams.speciesHeight).to.be.null
    expect(emptyParams.bha50SiteIndex).to.be.null
    expect(emptyParams.compute2).to.be.null

    const store = createMockModelParameterStore({
      siteIndexRows: [
        { speciesCode: 'FD', computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX, ageType: CONSTANTS.AGE_TYPE.TOTAL, age: '40', height: '18.0', bhaSiteIndex: null },
        { speciesCode: 'PL', computedValue: CONSTANTS.COMPUTED_VALUE.HEIGHT, ageType: CONSTANTS.AGE_TYPE.TOTAL, age: '35', height: null, bhaSiteIndex: '15.0' },
      ],
    })
    const params = buildModelParameters(store)
    expect(params.compute2).to.equal(CONSTANTS.COMPUTED_VALUE.HEIGHT)
    expect(params.age2).to.equal('35')
    expect(params.si2).to.equal('15.0')
    expect(params.compute3).to.be.null
  })
})

// ─ Projection lifecycle

describe('createProjection', () => {
  it('should call createProjectionFunc once and pass reportDescription', () => {
    const mockResult = { ...mockProjectionModel, projectionGUID: 'new-proj-guid' }
    const mockFunc = cy.stub().resolves(mockResult)
    const store = createMockModelParameterStore({ reportDescription: 'Test description' })

    cy.wrap(createProjection(store, mockFunc)).then((result: any) => {
      expect(mockFunc).to.be.calledOnce
      expect(result.projectionGUID).to.equal('new-proj-guid')
      expect(mockFunc.getCall(0).args[2]).to.equal('Test description')
    })
  })
})

describe('saveProjectionOnPanelConfirm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should create a projection when in CREATE mode on the REPORT_DETAILS panel', () => {
    cy.stub(apiClient, 'createProjection').resolves({ data: { ...mockProjectionModel, projectionGUID: 'created-guid' } })

    cy.wrap(saveProjectionOnPanelConfirm(createMockModelParameterStore(), CONSTANTS.MANUAL_INPUT_PANEL.REPORT_DETAILS)).then(() => {
      expect(apiClient.createProjection).to.be.calledOnce
      const appStore = useAppStore()
      expect(appStore.getCurrentProjectionGUID).to.equal('created-guid')
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.EDIT)
    })
  })

  it('should NOT create a projection when in CREATE mode but not on the REPORT_DETAILS panel', () => {
    cy.stub(apiClient, 'createProjection').resolves({ data: mockProjectionModel })

    cy.wrap(saveProjectionOnPanelConfirm(createMockModelParameterStore(), CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO)).then(() => {
      expect(apiClient.createProjection).to.not.be.called
    })
  })

  it('should update an existing projection when in EDIT mode', () => {
    cy.stub(apiClient, 'updateProjectionParams').resolves({ data: mockProjectionModel })
    cy.stub(apiClient, 'getProjection').resolves({ data: mockProjectionModel })

    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('existing-guid')
    appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)

    cy.wrap(saveProjectionOnPanelConfirm(createMockModelParameterStore(), CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO)).then(() => {
      expect(apiClient.updateProjectionParams).to.be.calledOnce
    })
  })

  it('should throw MISSING_GUID when in EDIT mode with no GUID set', () => {
    const appStore = useAppStore()
    appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)

    return saveProjectionOnPanelConfirm(createMockModelParameterStore(), CONSTANTS.MANUAL_INPUT_PANEL.SITE_INFO)
      .then(() => { throw new Error('Test should have failed but succeeded unexpectedly') })
      .catch((error: Error) => { expect(error.message).to.equal(PROJECTION_ERR.MISSING_GUID) })
  })
})

const createSavedModelParams = (overrides: Record<string, any> = {}) => ({
  species: [],
  derivedBy: CONSTANTS.DERIVED_BY.VOLUME,
  becZone: 'CWH',
  ecoZone: null,
  siteIndex: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
  siteSpecies: 'FD',
  compute: null,
  ageYears: CONSTANTS.AGE_TYPE.TOTAL,
  speciesAge: null,
  speciesHeight: null,
  bha50SiteIndex: null,
  compute2: null, ageYears2: null, age2: null, height2: null, si2: null,
  compute3: null, ageYears3: null, age3: null, height3: null, si3: null,
  compute4: null, ageYears4: null, age4: null, height4: null, si4: null,
  compute5: null, ageYears5: null, age5: null, height5: null, si5: null,
  compute6: null, ageYears6: null, age6: null, height6: null, si6: null,
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
    cy.wrap(hasPanelUnsavedChanges(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO, createMockModelParameterStore())).should('equal', false)
  })

  it('should return false when saved and current state match', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, modelParameters: JSON.stringify(createSavedModelParams()) },
    })
    cy.wrap(hasPanelUnsavedChanges(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO, createMockModelParameterStore())).should('equal', false)
  })

  it('should return true when a panel field has changed', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')
    cy.stub(apiClient, 'getProjection').resolves({
      data: {
        ...mockProjectionModel,
        modelParameters: JSON.stringify(createSavedModelParams({ derivedBy: CONSTANTS.DERIVED_BY.BASAL_AREA })),
      },
    })
    cy.wrap(hasPanelUnsavedChanges(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO, createMockModelParameterStore())).should('equal', true)
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

  it('should revert the panel to open and editable state after restoring saved data', () => {
    const appStore = useAppStore()
    appStore.setCurrentProjectionGUID('some-guid')
    cy.stub(apiClient, 'getProjection').resolves({
      data: { ...mockProjectionModel, modelParameters: JSON.stringify(createSavedModelParams()), reportDescription: null },
    })
    cy.wrap(revertPanelToSaved(CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO as any)).then(() => {
      const modelStore = useModelParameterStore()
      expect(modelStore.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(modelStore.panelState.speciesInfo.confirmed).to.be.false
      expect(modelStore.panelState.speciesInfo.editable).to.be.true
    })
  })
})
