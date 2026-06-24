/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { CONSTANTS, DEFAULTS } from '@/constants'
import { ExecutionOptionsEnum, UtilizationClassSetEnum } from '@/services/vdyp-api'
import type { ParsedProjectionParameters } from '@/interfaces/interfaces'
import type { ModelParameters } from '@/services/vdyp-api'

const makeParsedParams = (overrides: Partial<ParsedProjectionParameters> = {}): ParsedProjectionParameters => ({
  outputFormat: null,
  selectedExecutionOptions: [],
  selectedDebugOptions: [],
  ageStart: null,
  ageEnd: null,
  yearStart: null,
  yearEnd: null,
  forceYear: null,
  ageIncrement: null,
  metadataToOutput: null,
  filters: null,
  utils: [],
  excludedExecutionOptions: [],
  excludedDebugOptions: [],
  combineAgeYearRange: null,
  progressFrequency: null,
  reportTitle: null,
  copyTitle: null,
  ...overrides,
})

const makeModelParams = (overrides: Partial<ModelParameters> = {}): ModelParameters => ({
  derivedBy: 'Volume',
  species: [
    { code: 'PL', percent: 70 },
    { code: 'AC', percent: 30 },
  ],
  siteSpecies: 'PL',
  becZone: 'CWH',
  ecoZone: 'MH',
  siteIndex: 'Supplied',
  compute: null,
  ageYears: 'Total',
  speciesAge: '50',
  speciesHeight: '18.5',
  bha50SiteIndex: '16.3',
  compute2: null,
  ageYears2: null,
  age2: null,
  height2: null,
  si2: null,
  compute3: null,
  ageYears3: null,
  age3: null,
  height3: null,
  si3: null,
  compute4: null,
  ageYears4: null,
  age4: null,
  height4: null,
  si4: null,
  compute5: null,
  ageYears5: null,
  age5: null,
  height5: null,
  si5: null,
  compute6: null,
  ageYears6: null,
  age6: null,
  height6: null,
  si6: null,
  stockable: 55,
  cc: 60,
  BA: 25,
  TPH: 800,
  minDBHLimit: '7.5 cm+',
  currentDiameter: null,
  ...overrides,
})

describe('Model Parameter Store', () => {
  let store: ReturnType<typeof useModelParameterStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useModelParameterStore()
  })

  it('should initialize panel states and key defaults', () => {
    expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.reportDetails).to.deep.equal({ confirmed: false, editable: true })
    ;(['speciesInfo', 'siteInfo', 'standInfo', 'reportSettings'] as const).forEach((panel) => {
      expect(store.panelOpenStates[panel]).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelState[panel]).to.deep.equal({ confirmed: false, editable: false })
    })
    expect(store.runModelEnabled).to.be.false
    expect(store.speciesGroups).to.deep.equal([])
    expect(store.siteIndexRows).to.deep.equal([])
    expect(store.selectedAgeYearRange).to.equal(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
    expect(store.projectionType).to.equal(DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE)
    expect(store.isForwardGrowEnabled).to.be.true
    expect(store.isBackwardGrowEnabled).to.be.true
    expect(store.isByLayerEnabled).to.be.true
    expect(store.isComputedMAIEnabled).to.be.false
  })

  it('should progress panels on confirmPanel and enable runModelEnabled only when all confirmed', () => {
    store.confirmPanel('reportDetails')

    expect(store.panelState.reportDetails).to.deep.equal({ confirmed: true, editable: false })
    expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.speciesInfo.editable).to.be.true
    expect(store.runModelEnabled).to.be.false

    store.confirmPanel('speciesInfo')
    store.confirmPanel('siteInfo')
    store.confirmPanel('standInfo')
    store.confirmPanel('reportSettings')
    expect(store.runModelEnabled).to.be.true
  })

  it('should reopen panel unconfirmed, reset subsequent panels, and disable runModelEnabled on editPanel', () => {
    ;(['reportDetails', 'speciesInfo', 'siteInfo', 'standInfo', 'reportSettings'] as const).forEach((p) =>
      store.confirmPanel(p),
    )

    store.editPanel('reportDetails')

    expect(store.panelState.reportDetails).to.deep.equal({ confirmed: false, editable: true })
    expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.OPEN)
    ;(['speciesInfo', 'siteInfo', 'standInfo', 'reportSettings'] as const).forEach((panel) => {
      expect(store.panelState[panel].confirmed).to.be.false
      expect(store.panelState[panel].editable).to.be.false
      expect(store.panelOpenStates[panel]).to.equal(CONSTANTS.PANEL.CLOSE)
    })
    expect(store.runModelEnabled).to.be.false
  })

  it('should group species by code, sum percents, sort descending, and preserve siteIndexRow data across updates', () => {
    store.speciesList[0] = { species: 'PL', percent: '50' }
    store.speciesList[1] = { species: 'PL', percent: '20' }
    store.speciesList[2] = { species: 'AC', percent: '30' }
    store.updateSpeciesGroup()

    expect(store.speciesGroups[0].siteSpecies).to.equal('PL')
    expect(Number.parseFloat(store.speciesGroups[0].percent)).to.equal(70)
    expect(store.speciesGroups[1].siteSpecies).to.equal('AC')
    expect(store.highestPercentSpecies).to.equal('PL')
    expect(store.selectedSiteSpecies).to.equal('PL')
    expect(store.siteIndexRows).to.have.length(2)

    store.siteIndexRows[0].bhaSiteIndex = '25.0'
    store.updateSpeciesGroup()

    expect(store.siteIndexRows[0].speciesCode).to.equal('PL')
    expect(store.siteIndexRows[0].bhaSiteIndex).to.equal('25.0')
  })

  it('should apply mode-specific siteIndexRow defaults based on derivedBy and siteSpeciesValues', () => {
    store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
    store.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
    store.speciesList[0] = { species: 'PL', percent: '60' }
    store.speciesList[1] = { species: 'AC', percent: '40' }
    store.updateSpeciesGroup()

    expect(store.siteIndexRows[0].computedValue).to.equal(CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX)
    expect(store.siteIndexRows[1].computedValue).to.be.null
    expect(store.siteIndexRows[1].bhaSiteIndex).to.be.null

    store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
    store.updateSpeciesGroup()

    store.siteIndexRows.forEach((row) => {
      expect(row.bhaSiteIndex).to.equal(DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX)
      expect(row.computedValue).to.be.null
      expect(row.age).to.be.null
      expect(row.height).to.be.null
    })
  })

  it('should reflect computed conditions in isVolumeComputed and isSupplied', () => {
    store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
    store.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
    expect(store.isVolumeComputed).to.be.true

    store.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
    expect(store.isVolumeComputed).to.be.false

    store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
    store.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
    expect(store.isVolumeComputed).to.be.false

    store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
    expect(store.isSupplied).to.be.true

    store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
    expect(store.isSupplied).to.be.false
  })

  it('should reset panel states and all data fields to initial values', () => {
    store.speciesList[0] = { species: 'PL', percent: '100' }
    store.updateSpeciesGroup()
    store.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
    store.reportTitle = 'Custom Title'
    store.ageType = 'SomeType'
    ;(['reportDetails', 'speciesInfo', 'siteInfo', 'standInfo', 'reportSettings'] as const).forEach((p) =>
      store.confirmPanel(p),
    )

    store.resetStore()

    expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.reportDetails).to.deep.equal({ confirmed: false, editable: true })
    expect(store.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelState.speciesInfo).to.deep.equal({ confirmed: false, editable: false })
    expect(store.runModelEnabled).to.be.false
    expect(store.speciesGroups).to.deep.equal([])
    expect(store.siteIndexRows).to.deep.equal([])
    expect(store.selectedAgeYearRange).to.equal(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
    expect(store.reportTitle).to.equal(DEFAULTS.DEFAULT_VALUES.REPORT_TITLE)
    expect(store.ageType).to.equal(DEFAULTS.DEFAULT_VALUES.AGE_TYPE)
  })

  it('should restore all fields and set all panels open+confirmed+non-editable in view mode', () => {
    const params = makeParsedParams({
      reportTitle: 'My Report',
      copyTitle: 'Copy Title',
      ageStart: '20',
      ageEnd: '200',
      ageIncrement: '10',
      selectedExecutionOptions: [ExecutionOptionsEnum.ForwardGrowEnabled],
    })
    store.restoreFromProjectionParams(params, true)

    expect(store.reportTitle).to.equal('My Report')
    expect(store.copyTitle).to.equal('Copy Title')
    expect(store.startingAge).to.equal('20')
    expect(store.runModelEnabled).to.be.true
    ;(['reportDetails', 'speciesInfo', 'siteInfo', 'standInfo', 'reportSettings'] as const).forEach((panel) => {
      expect(store.panelOpenStates[panel]).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState[panel]).to.deep.equal({ confirmed: true, editable: false })
    })
  })

  it('should restore panel flow based on reportTitle and set projectionType from options in edit mode', () => {
    store.restoreFromProjectionParams(makeParsedParams({ reportTitle: null }), false)
    expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.reportDetails.confirmed).to.be.false

    store.resetStore()
    store.speciesList[0] = { species: 'PL', percent: '100' }
    store.updateSpeciesGroup()
    store.becZone = 'CWH'
    store.percentStockableArea = '55'
    store.restoreFromProjectionParams(
      makeParsedParams({
        reportTitle: 'Title',
        ageStart: '20',
        selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass],
      }),
      false,
    )

    expect(store.panelState.reportDetails.confirmed).to.be.true
    expect(store.runModelEnabled).to.be.true
    expect(store.projectionType).to.equal(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
  })

  it('should override minimumDBHLimit for matching species group via utils', () => {
    store.speciesList[0] = { species: 'FD', percent: '100' }
    store.updateSpeciesGroup()

    store.restoreFromProjectionParams(
      makeParsedParams({ utils: [{ s: 'F', u: UtilizationClassSetEnum._175 }] }),
      false,
    )

    const fGroup = store.speciesGroups.find((g) => g.group === 'F')
    expect(fGroup?.minimumDBHLimit).to.equal(UtilizationClassSetEnum._175)
  })

  it('should restore all model parameter data fields and primary siteIndexRow for Supplied and Computed modes', () => {
    store.restoreFromModelParameters(makeModelParams({ siteIndex: CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED }))

    expect(store.derivedBy).to.equal('Volume')
    expect(store.becZone).to.equal('CWH')
    expect(store.ecoZone).to.equal('MH')
    expect(store.spzAge).to.equal('50')
    expect(store.spzHeight).to.equal('18.5')
    expect(store.bha50SiteIndex).to.equal('16.3')
    expect(store.percentStockableArea).to.equal('55')
    expect(store.crownClosure).to.equal('60')
    expect(store.basalArea).to.equal('25')
    expect(store.treesPerHectare).to.equal('800')
    expect(store.minDBHLimit).to.equal('7.5 cm+')
    expect(store.highestPercentSpecies).to.equal('PL')
    expect(store.selectedSiteSpecies).to.equal('PL')
    expect(store.referenceYear).to.equal(new Date().getFullYear())
    expect(store.siteIndexRows[0].computedValue).to.be.null
    expect(store.siteIndexRows[0].bhaSiteIndex).to.equal('16.3')
    expect(store.siteIndexRows[0].age).to.be.null
    expect(store.siteIndexRows[0].height).to.be.null

    store.restoreFromModelParameters(
      makeModelParams({ siteIndex: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED, compute: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX }),
    )
    expect(store.siteIndexRows[0].computedValue).to.equal(CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX)
    expect(store.siteIndexRows[0].bhaSiteIndex).to.be.null

    store.restoreFromModelParameters(
      makeModelParams({ siteIndex: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED, compute: CONSTANTS.COMPUTED_VALUE.HEIGHT }),
    )
    expect(store.siteIndexRows[0].computedValue).to.equal(CONSTANTS.COMPUTED_VALUE.HEIGHT)
    expect(store.siteIndexRows[0].height).to.be.null
    expect(store.siteIndexRows[0].bhaSiteIndex).to.equal('16.3')
  })

  it('should restore secondary siteIndexRows for Computed and Supplied modes', () => {
    store.restoreFromModelParameters(
      makeModelParams({
        siteIndex: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
        compute: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX,
        compute2: CONSTANTS.COMPUTED_VALUE.HEIGHT,
        ageYears2: CONSTANTS.AGE_TYPE.TOTAL,
        age2: '50',
        height2: null,
        si2: '20.0',
      }),
    )

    expect(store.siteIndexRows[1].computedValue).to.equal(CONSTANTS.COMPUTED_VALUE.HEIGHT)
    expect(store.siteIndexRows[1].ageType).to.equal(CONSTANTS.AGE_TYPE.TOTAL)
    expect(store.siteIndexRows[1].age).to.equal('50')
    expect(store.siteIndexRows[1].height).to.be.null
    expect(store.siteIndexRows[1].bhaSiteIndex).to.equal('20.0')

    store.restoreFromModelParameters(makeModelParams({ siteIndex: CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED, si2: '22.5' }))

    expect(store.siteIndexRows[1].computedValue).to.be.null
    expect(store.siteIndexRows[1].bhaSiteIndex).to.equal('22.5')
    expect(store.siteIndexRows[1].age).to.be.null
    expect(store.siteIndexRows[1].height).to.be.null
  })
})
