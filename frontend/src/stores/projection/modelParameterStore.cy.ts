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

describe('Model Parameter Store Unit Tests', () => {
  let store: ReturnType<typeof useModelParameterStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useModelParameterStore()
  })

  describe('Initial State', () => {
    it('should open detailsInfo panel and close all others', () => {
      expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelOpenStates.siteInfo).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelOpenStates.standInfo).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelOpenStates.reportSettings).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should set detailsInfo as editable and all others as non-editable', () => {
      expect(store.panelState.reportDetails).to.deep.equal({ confirmed: false, editable: true })
      expect(store.panelState.speciesInfo).to.deep.equal({ confirmed: false, editable: false })
      expect(store.panelState.siteInfo).to.deep.equal({ confirmed: false, editable: false })
      expect(store.panelState.standInfo).to.deep.equal({ confirmed: false, editable: false })
      expect(store.panelState.reportSettings).to.deep.equal({ confirmed: false, editable: false })
    })

    it('should initialize runModelEnabled as false', () => {
      expect(store.runModelEnabled).to.be.false
    })

    it('should initialize species list with 6 empty rows', () => {
      expect(store.speciesList).to.have.length(6)
      store.speciesList.forEach((item) => {
        expect(item.species).to.be.null
        expect(item.percent).to.be.null
      })
    })

    it('should initialize speciesGroups as empty array', () => {
      expect(store.speciesGroups).to.deep.equal([])
    })

    it('should initialize selectedAgeYearRange to the default value', () => {
      expect(store.selectedAgeYearRange).to.equal(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
    })

    it('should initialize projectionType to the default value', () => {
      expect(store.projectionType).to.equal(DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE)
    })

    it('should initialize boolean report flags to their default values', () => {
      expect(store.isForwardGrowEnabled).to.be.true
      expect(store.isBackwardGrowEnabled).to.be.true
      expect(store.isByLayerEnabled).to.be.true
      expect(store.isComputedMAIEnabled).to.be.false
      expect(store.isCulminationValuesEnabled).to.be.false
      expect(store.isBySpeciesEnabled).to.be.false
      expect(store.isProjectionModeEnabled).to.be.false
      expect(store.isPolygonIDEnabled).to.be.false
      expect(store.isCurrentYearEnabled).to.be.false
      expect(store.isReferenceYearEnabled).to.be.false
      expect(store.incSecondaryHeight).to.be.false
    })

    it('should initialize nullable fields as null', () => {
      expect(store.derivedBy).to.be.null
      expect(store.becZone).to.be.null
      expect(store.ecoZone).to.be.null
      expect(store.startingAge).to.be.null
      expect(store.finishingAge).to.be.null
      expect(store.reportTitle).to.be.null
    })
  })

  describe('confirmPanel', () => {
    it('should mark detailsInfo as confirmed and close it', () => {
      store.confirmPanel('reportDetails')

      expect(store.panelState.reportDetails.confirmed).to.be.true
      expect(store.panelState.reportDetails.editable).to.be.false
      expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should open speciesInfo and make it editable when detailsInfo is confirmed', () => {
      store.confirmPanel('reportDetails')

      expect(store.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.speciesInfo.editable).to.be.true
    })

    it('should open siteInfo when speciesInfo is confirmed', () => {
      store.confirmPanel('reportDetails')
      store.confirmPanel('speciesInfo')

      expect(store.panelOpenStates.siteInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.siteInfo.editable).to.be.true
    })

    it('should open standInfo when siteInfo is confirmed', () => {
      store.confirmPanel('reportDetails')
      store.confirmPanel('speciesInfo')
      store.confirmPanel('siteInfo')

      expect(store.panelOpenStates.standInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.standInfo.editable).to.be.true
    })

    it('should not enable runModelEnabled when only some panels are confirmed', () => {
      store.confirmPanel('reportDetails')
      store.confirmPanel('speciesInfo')

      expect(store.runModelEnabled).to.be.false
    })

    it('should enable runModelEnabled when all panels are confirmed', () => {
      store.confirmPanel('reportDetails')
      store.confirmPanel('speciesInfo')
      store.confirmPanel('siteInfo')
      store.confirmPanel('standInfo')
      store.confirmPanel('reportSettings')

      expect(store.runModelEnabled).to.be.true
    })
  })

  describe('editPanel', () => {
    beforeEach(() => {
      store.confirmPanel('reportDetails')
      store.confirmPanel('speciesInfo')
      store.confirmPanel('siteInfo')
      store.confirmPanel('standInfo')
      store.confirmPanel('reportSettings')
    })

    it('should unconfirm detailsInfo and open it for editing', () => {
      store.editPanel('reportDetails')

      expect(store.panelState.reportDetails.confirmed).to.be.false
      expect(store.panelState.reportDetails.editable).to.be.true
      expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.OPEN)
    })

    it('should disable all subsequent panels when editing detailsInfo', () => {
      store.editPanel('reportDetails')

      const subsequentPanels = ['speciesInfo', 'siteInfo', 'standInfo', 'reportSettings'] as const
      subsequentPanels.forEach((panel) => {
        expect(store.panelState[panel].confirmed).to.be.false
        expect(store.panelState[panel].editable).to.be.false
        expect(store.panelOpenStates[panel]).to.equal(CONSTANTS.PANEL.CLOSE)
      })
    })

    it('should set runModelEnabled to false when editing a panel', () => {
      expect(store.runModelEnabled).to.be.true

      store.editPanel('speciesInfo')

      expect(store.runModelEnabled).to.be.false
    })

    it('should only close panels after standInfo when editing standInfo', () => {
      store.editPanel('standInfo')

      expect(store.panelState.standInfo.confirmed).to.be.false
      expect(store.panelState.standInfo.editable).to.be.true
      expect(store.panelOpenStates.standInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.reportSettings.confirmed).to.be.false
      expect(store.panelOpenStates.reportSettings).to.equal(CONSTANTS.PANEL.CLOSE)
    })
  })

  describe('totalSpeciesPercent', () => {
    it('should return 0.0 when all species are empty', () => {
      expect(store.totalSpeciesPercent).to.equal('0.0')
    })

    it('should sum species percentages correctly', () => {
      store.speciesList[0].percent = '60'
      store.speciesList[1].percent = '40'

      expect(store.totalSpeciesPercent).to.equal('100.0')
    })

    it('should ignore null percent entries', () => {
      store.speciesList[0].percent = '70'
      store.speciesList[1].percent = null

      expect(store.totalSpeciesPercent).to.equal('70.0')
    })
  })

  describe('updateSpeciesGroup', () => {
    it('should group species with same code and sum their percentages', () => {
      store.speciesList[0] = { species: 'PL', percent: '60' }
      store.speciesList[1] = { species: 'PL', percent: '20' }
      store.speciesList[2] = { species: 'AC', percent: '20' }
      store.updateSpeciesGroup()

      const plGroup = store.speciesGroups.find((g) => g.siteSpecies === 'PL')
      const acGroup = store.speciesGroups.find((g) => g.siteSpecies === 'AC')
      expect(Number.parseFloat(plGroup!.percent)).to.equal(80)
      expect(Number.parseFloat(acGroup!.percent)).to.equal(20)
    })

    it('should sort species groups by descending percent', () => {
      store.speciesList[0] = { species: 'AC', percent: '30' }
      store.speciesList[1] = { species: 'PL', percent: '70' }
      store.updateSpeciesGroup()

      expect(store.speciesGroups[0].siteSpecies).to.equal('PL')
      expect(store.speciesGroups[1].siteSpecies).to.equal('AC')
    })

    it('should set highestPercentSpecies to the top species', () => {
      store.speciesList[0] = { species: 'AC', percent: '30' }
      store.speciesList[1] = { species: 'PL', percent: '70' }
      store.updateSpeciesGroup()

      expect(store.highestPercentSpecies).to.equal('PL')
      expect(store.selectedSiteSpecies).to.equal('PL')
    })

    it('should produce empty groups when all species are null', () => {
      store.updateSpeciesGroup()

      expect(store.speciesGroups).to.deep.equal([])
      expect(store.highestPercentSpecies).to.be.null
    })
  })

  describe('resetStore', () => {
    it('should reset panel open states to initial values', () => {
      store.confirmPanel('reportDetails')
      store.resetStore()

      expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should reset panel confirmed/editable states', () => {
      store.confirmPanel('reportDetails')
      store.confirmPanel('speciesInfo')
      store.resetStore()

      expect(store.panelState.reportDetails).to.deep.equal({ confirmed: false, editable: true })
      expect(store.panelState.speciesInfo).to.deep.equal({ confirmed: false, editable: false })
    })

    it('should reset runModelEnabled to false', () => {
      store.confirmPanel('reportDetails')
      store.confirmPanel('speciesInfo')
      store.confirmPanel('siteInfo')
      store.confirmPanel('standInfo')
      store.confirmPanel('reportSettings')
      expect(store.runModelEnabled).to.be.true

      store.resetStore()

      expect(store.runModelEnabled).to.be.false
    })

    it('should reset speciesList to 6 empty rows', () => {
      store.speciesList[0] = { species: 'PL', percent: '100' }
      store.resetStore()

      expect(store.speciesList).to.have.length(6)
      store.speciesList.forEach((item) => {
        expect(item.species).to.be.null
        expect(item.percent).to.be.null
      })
    })

    it('should reset speciesGroups to empty array', () => {
      store.speciesList[0] = { species: 'PL', percent: '100' }
      store.updateSpeciesGroup()
      expect(store.speciesGroups).to.have.length.greaterThan(0)

      store.resetStore()

      expect(store.speciesGroups).to.deep.equal([])
    })

    it('should reset selectedAgeYearRange to default', () => {
      store.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
      store.resetStore()

      expect(store.selectedAgeYearRange).to.equal(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
    })

    it('should reset reportTitle to default value', () => {
      store.reportTitle = 'Custom Title'
      store.resetStore()

      expect(store.reportTitle).to.equal(DEFAULTS.DEFAULT_VALUES.REPORT_TITLE)
    })

    it('should reset ageType to default value', () => {
      store.ageType = 'SomeType'
      store.resetStore()

      expect(store.ageType).to.equal(DEFAULTS.DEFAULT_VALUES.AGE_TYPE)
    })
  })

  describe('restoreFromProjectionParams (view mode)', () => {
    const params = makeParsedParams({
      reportTitle: 'My Report',
      copyTitle: 'Copy Title',
      ageStart: '20',
      ageEnd: '200',
      ageIncrement: '10',
      selectedExecutionOptions: [
        ExecutionOptionsEnum.ForwardGrowEnabled,
        ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
      ],
    })

    beforeEach(() => {
      store.restoreFromProjectionParams(params, true)
    })

    it('should restore reportTitle and copyTitle', () => {
      expect(store.reportTitle).to.equal('My Report')
      expect(store.copyTitle).to.equal('Copy Title')
    })

    it('should restore age range values', () => {
      expect(store.startingAge).to.equal('20')
      expect(store.finishingAge).to.equal('200')
      expect(store.ageIncrement).to.equal('10')
    })

    it('should restore execution options flags', () => {
      expect(store.isForwardGrowEnabled).to.be.true
      expect(store.isBackwardGrowEnabled).to.be.false
      expect(store.projectionType).to.equal(CONSTANTS.PROJECTION_TYPE.VOLUME)
    })

    it('should open all panels in view mode', () => {
      const panels = ['reportDetails', 'speciesInfo', 'siteInfo', 'standInfo', 'reportSettings'] as const
      panels.forEach((panel) => {
        expect(store.panelOpenStates[panel]).to.equal(CONSTANTS.PANEL.OPEN)
      })
    })

    it('should confirm all panels but keep them non-editable in view mode', () => {
      const panels = ['reportDetails', 'speciesInfo', 'siteInfo', 'standInfo', 'reportSettings'] as const
      panels.forEach((panel) => {
        expect(store.panelState[panel]).to.deep.equal({ confirmed: true, editable: false })
      })
    })

    it('should keep runModelEnabled true in view mode', () => {
      expect(store.runModelEnabled).to.be.true
    })
  })

  describe('restoreFromProjectionParams (edit mode)', () => {
    it('should open detailsInfo when reportTitle is null', () => {
      store.restoreFromProjectionParams(makeParsedParams({ reportTitle: null }), false)

      expect(store.panelOpenStates.reportDetails).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.reportDetails.confirmed).to.be.false
    })

    it('should confirm detailsInfo and open speciesInfo when only reportTitle is set', () => {
      store.restoreFromProjectionParams(makeParsedParams({ reportTitle: 'Title' }), false)

      expect(store.panelState.reportDetails.confirmed).to.be.true
      expect(store.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.speciesInfo.editable).to.be.true
    })

    it('should enable runModelEnabled when all panels fully populated', () => {
      store.speciesList[0] = { species: 'PL', percent: '100' }
      store.updateSpeciesGroup()
      store.becZone = 'CWH'
      store.percentStockableArea = '55'

      store.restoreFromProjectionParams(
        makeParsedParams({ reportTitle: 'Title', ageStart: '20' }),
        false,
      )

      expect(store.runModelEnabled).to.be.true
    })

    it('should set projectionType to CFS_BIOMASS when option is present', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass],
        }),
        false,
      )

      expect(store.projectionType).to.equal(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
    })
  })

  describe('restoreUtilizationLevels (via restoreFromProjectionParams)', () => {
    beforeEach(() => {
      store.speciesList[0] = { species: 'FD', percent: '60' }
      store.speciesList[1] = { species: 'AC', percent: '40' }
      store.updateSpeciesGroup()
    })

    it('should override minimumDBHLimit for matching species group', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({ utils: [{ s: 'F', u: UtilizationClassSetEnum._175 }] }),
        false,
      )

      const fGroup = store.speciesGroups.find((g) => g.group === 'F')
      expect(fGroup?.minimumDBHLimit).to.equal(UtilizationClassSetEnum._175)
    })

    it('should not change groups when utils array is empty', () => {
      const original = store.speciesGroups.find((g) => g.group === 'AC')?.minimumDBHLimit

      store.restoreFromProjectionParams(makeParsedParams({ utils: [] }), false)

      const acGroup = store.speciesGroups.find((g) => g.group === 'AC')
      expect(acGroup?.minimumDBHLimit).to.equal(original)
    })
  })

  describe('restoreFromModelParameters', () => {
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
      ageYears: 'Total',
      speciesAge: 50,
      speciesHeight: 18.5,
      bha50SiteIndex: 16.3,
      stockable: 55,
      cc: 60,
      BA: 25,
      TPH: 800,
      minDBHLimit: '7.5 cm+',
      currentDiameter: null,
      ...overrides,
    })

    it('should restore derivedBy', () => {
      store.restoreFromModelParameters(makeModelParams())

      expect(store.derivedBy).to.equal('Volume')
    })

    it('should restore species list from model parameters', () => {
      store.restoreFromModelParameters(makeModelParams())

      expect(store.speciesList[0].species).to.equal('PL')
      expect(store.speciesList[1].species).to.equal('AC')
      expect(store.speciesList[2].species).to.be.null
    })

    it('should restore site info fields', () => {
      store.restoreFromModelParameters(makeModelParams())

      expect(store.becZone).to.equal('CWH')
      expect(store.ecoZone).to.equal('MH')
      expect(store.spzAge).to.equal('50')
      expect(store.spzHeight).to.equal('18.5')
      expect(store.bha50SiteIndex).to.equal('16.3')
    })

    it('should restore stand info fields', () => {
      store.restoreFromModelParameters(makeModelParams())

      expect(store.percentStockableArea).to.equal('55')
      expect(store.crownClosure).to.equal('60')
      expect(store.basalArea).to.equal('25')
      expect(store.treesPerHectare).to.equal('800')
      expect(store.minDBHLimit).to.equal('7.5 cm+')
    })

    it('should set highestPercentSpecies from siteSpecies field', () => {
      store.restoreFromModelParameters(makeModelParams({ siteSpecies: 'PL' }))

      expect(store.highestPercentSpecies).to.equal('PL')
      expect(store.selectedSiteSpecies).to.equal('PL')
    })

    it('should set referenceYear to current year when not already set', () => {
      store.restoreFromModelParameters(makeModelParams())

      expect(store.referenceYear).to.equal(new Date().getFullYear())
    })
  })
})
