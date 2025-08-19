/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { CONSTANTS, DEFAULTS } from '@/constants'

describe('ModelParameterStore Unit Tests', () => {
  let store: ReturnType<typeof useModelParameterStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useModelParameterStore()
  })

  it('should initialize with default values', () => {
    expect(store.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelOpenStates.siteInfo).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelOpenStates.standInfo).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.CLOSE)

    expect(store.panelState.speciesInfo.confirmed).to.be.false
    expect(store.panelState.speciesInfo.editable).to.be.true

    expect(store.panelState.siteInfo.confirmed).to.be.false
    expect(store.panelState.siteInfo.editable).to.be.false

    expect(store.panelState.standInfo.confirmed).to.be.false
    expect(store.panelState.standInfo.editable).to.be.false

    expect(store.panelState.reportInfo.confirmed).to.be.false
    expect(store.panelState.reportInfo.editable).to.be.false

    expect(store.runModelEnabled).to.be.false

    expect(store.derivedBy).to.be.null
    expect(store.speciesList).to.deep.equal([
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
    ])
    expect(store.speciesGroups).to.deep.equal([])
    expect(store.highestPercentSpecies).to.be.null
    expect(store.selectedSiteSpecies).to.be.null
    expect(store.totalSpeciesPercent).to.equal('0.0')
    expect(store.totalSpeciesGroupPercent).to.equal(0)
    expect(store.becZone).to.be.null
    expect(store.ecoZone).to.be.null
    expect(store.siteSpeciesValues).to.be.null
    expect(store.ageType).to.be.null
    expect(store.spzAge).to.be.null
    expect(store.spzHeight).to.be.null
    expect(store.bha50SiteIndex).to.be.null
    expect(store.percentStockableArea).to.be.null
    expect(store.basalArea).to.be.null
    expect(store.treesPerHectare).to.be.null
    expect(store.minDBHLimit).to.be.null
    expect(store.currentDiameter).to.be.null
    expect(store.crownClosure).to.be.null
    expect(store.selectedAgeYearRange).to.be.null
    expect(store.startingAge).to.be.null
    expect(store.finishingAge).to.be.null
    expect(store.ageIncrement).to.be.null
    expect(store.startYear).to.be.null
    expect(store.endYear).to.be.null
    expect(store.yearIncrement).to.be.null
    expect(store.isForwardGrowEnabled).to.be.true
    expect(store.isBackwardGrowEnabled).to.be.true
    expect(store.isWholeStemEnabled).to.be.true
    expect(store.isCloseUtilEnabled).to.be.false
    expect(store.isNetDecayEnabled).to.be.false
    expect(store.isNetDecayWasteEnabled).to.be.false
    expect(store.isNetDecayWasteBreakageEnabled).to.be.false
    expect(store.isComputedMAIEnabled).to.be.false
    expect(store.isCulminationValuesEnabled).to.be.false
    expect(store.isBySpeciesEnabled).to.be.false
    expect(store.isByLayerEnabled).to.be.false
    expect(store.isProjectionModeEnabled).to.be.false
    expect(store.isPolygonIDEnabled).to.be.false
    expect(store.isCurrentYearEnabled).to.be.false
    expect(store.isReferenceYearEnabled).to.be.false
    expect(store.incSecondaryHeight).to.be.false
    expect(store.specificYear).to.be.null
    expect(store.projectionType).to.be.null
    expect(store.reportTitle).to.be.null
    expect(store.referenceYear).to.be.null
  })

  it('should confirm panel and enable the next panel', () => {
    store.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO)

    expect(store.panelState.speciesInfo.confirmed).to.be.true
    expect(store.panelState.speciesInfo.editable).to.be.false
    expect(store.panelOpenStates.siteInfo).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.siteInfo.editable).to.be.true
  })

  it('should edit panel and disable subsequent panels', () => {
    store.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO)
    store.editPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO)

    expect(store.panelOpenStates.speciesInfo).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.speciesInfo.confirmed).to.be.false
    expect(store.panelState.speciesInfo.editable).to.be.true

    expect(store.panelOpenStates.siteInfo).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelState.siteInfo.confirmed).to.be.false
    expect(store.panelState.siteInfo.editable).to.be.false

    expect(store.panelOpenStates.standInfo).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelState.standInfo.confirmed).to.be.false
    expect(store.panelState.standInfo.editable).to.be.false

    expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelState.reportInfo.confirmed).to.be.false
    expect(store.panelState.reportInfo.editable).to.be.false
  })

  it('should calculate total species percent correctly', () => {
    store.speciesList = [
      { species: 'PL', percent: '40.0' },
      { species: 'AC', percent: '35.5' },
      { species: 'H', percent: '24.5' },
      { species: 'S', percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
    ]
    expect(store.totalSpeciesPercent).to.equal('100.0')
  })

  it('should update species groups correctly', () => {
    store.speciesList = [
      { species: 'PL', percent: '50.0' },
      { species: 'PL', percent: '30.0' },
      { species: 'AC', percent: '20.0' },
      { species: null, percent: null },
    ]
    store.updateSpeciesGroup()

    expect(store.speciesGroups.length).to.equal(2)
    expect(store.speciesGroups[0].group).to.equal('PL')
    expect(store.speciesGroups[0].percent).to.equal('80.0')
    expect(store.speciesGroups[1].group).to.equal('AC')
    expect(store.speciesGroups[1].percent).to.equal('20.0')
    expect(store.highestPercentSpecies).to.equal('PL')
    expect(store.selectedSiteSpecies).to.equal('PL')
  })

  it('should set default values correctly', () => {
    store.setDefaultValues()

    expect(store.derivedBy).to.equal(DEFAULTS.DEFAULT_VALUES.DERIVED_BY)
    expect(store.speciesList).to.deep.equal([
      { species: 'PL', percent: '30.0' },
      { species: 'AC', percent: '30.0' },
      { species: 'H', percent: '30.0' },
      { species: 'S', percent: '10.0' },
      { species: null, percent: '0.0' },
      { species: null, percent: '0.0' },
    ])
    expect(store.speciesGroups.length).to.be.greaterThan(0)
    expect(store.becZone).to.equal(DEFAULTS.DEFAULT_VALUES.BEC_ZONE)
    expect(store.siteSpeciesValues).to.equal(
      DEFAULTS.DEFAULT_VALUES.SITE_SPECIES_VALUES,
    )
    expect(store.ageType).to.equal(DEFAULTS.DEFAULT_VALUES.AGE_TYPE)
    expect(store.spzAge).to.equal(DEFAULTS.DEFAULT_VALUES.SPZ_AGE)
    expect(store.spzHeight).to.equal(DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT)
    expect(store.bha50SiteIndex).to.equal(
      DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX,
    )
    expect(store.percentStockableArea).to.equal(
      DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA,
    )
    expect(store.basalArea).to.equal(null)
    expect(store.treesPerHectare).to.equal(null)
    expect(store.minDBHLimit).to.equal(DEFAULTS.DEFAULT_VALUES.MIN_DBH_LIMIT)
    expect(store.currentDiameter).to.equal(
      DEFAULTS.DEFAULT_VALUES.CURRENT_DIAMETER,
    )
    expect(store.crownClosure).to.equal(DEFAULTS.DEFAULT_VALUES.CROWN_CLOSURE)
    expect(store.selectedAgeYearRange).to.equal(
      DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE,
    )
    expect(store.startingAge).to.equal(DEFAULTS.DEFAULT_VALUES.STARTING_AGE)
    expect(store.finishingAge).to.equal(DEFAULTS.DEFAULT_VALUES.FINISHING_AGE)
    expect(store.ageIncrement).to.equal(DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT)
    expect(store.startYear).to.equal(DEFAULTS.DEFAULT_VALUES.START_YEAR)
    expect(store.endYear).to.equal(DEFAULTS.DEFAULT_VALUES.END_YEAR)
    expect(store.yearIncrement).to.equal(DEFAULTS.DEFAULT_VALUES.YEAR_INCREMENT)
    expect(store.isForwardGrowEnabled).to.equal(
      DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED,
    )
    expect(store.isBackwardGrowEnabled).to.equal(
      DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED,
    )
    expect(store.isWholeStemEnabled).to.equal(
      DEFAULTS.DEFAULT_VALUES.IS_WHOLE_STEM_ENABLED,
    )
    expect(store.projectionType).to.equal(
      DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
    )
    expect(store.reportTitle).to.equal(DEFAULTS.DEFAULT_VALUES.REPORT_TITLE)
    expect(store.referenceYear).to.equal(new Date().getFullYear())
  })

  it('should handle empty species list without errors', () => {
    store.speciesList = []
    store.updateSpeciesGroup()

    expect(store.speciesGroups.length).to.equal(0)
    expect(store.highestPercentSpecies).to.be.null
    expect(store.selectedSiteSpecies).to.be.null
    expect(store.totalSpeciesPercent).to.equal('0.0')
  })

  it('should enable run model button when all panels are confirmed', () => {
    store.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO)
    store.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO)
    store.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO)
    store.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO)

    expect(store.runModelEnabled).to.be.true
  })

  it('should update year range properties correctly', () => {
    store.startYear = 2020
    store.endYear = 2030
    store.yearIncrement = 2

    expect(store.startYear).to.equal(2020)
    expect(store.endYear).to.equal(2030)
    expect(store.yearIncrement).to.equal(2)
  })

  it('should update age range properties correctly', () => {
    store.selectedAgeYearRange = 'Custom Range'
    store.startingAge = 15
    store.finishingAge = 85
    store.ageIncrement = 10

    expect(store.selectedAgeYearRange).to.equal('Custom Range')
    expect(store.startingAge).to.equal(15)
    expect(store.finishingAge).to.equal(85)
    expect(store.ageIncrement).to.equal(10)
  })

  it('should update report info properties correctly', () => {
    store.isForwardGrowEnabled = true
    store.isBackwardGrowEnabled = true
    store.projectionType = 'Volume'
    store.reportTitle = 'Test Report'

    expect(store.isForwardGrowEnabled).to.be.true
    expect(store.isBackwardGrowEnabled).to.be.true
    expect(store.projectionType).to.equal('Volume')
    expect(store.reportTitle).to.equal('Test Report')
  })

  it('should handle full panel confirmation flow', () => {
    const panelOrder = [
      CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
      CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO,
      CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO,
      CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO,
    ]

    panelOrder.forEach((panel, index) => {
      store.confirmPanel(panel)
      expect(store.panelState[panel].confirmed).to.be.true
      expect(store.panelState[panel].editable).to.be.false

      if (index < panelOrder.length - 1) {
        const nextPanel = panelOrder[index + 1]
        expect(store.panelOpenStates[nextPanel]).to.equal(CONSTANTS.PANEL.OPEN)
        expect(store.panelState[nextPanel].editable).to.be.true
        expect(store.runModelEnabled).to.be.false
      } else {
        expect(store.runModelEnabled).to.be.true
      }
    })
  })

  it('should reset subsequent panels when editing a confirmed panel', () => {
    store.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO)
    store.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO)
    store.editPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO)

    expect(store.panelState.speciesInfo.confirmed).to.be.false
    expect(store.panelState.speciesInfo.editable).to.be.true

    expect(store.panelState.siteInfo.confirmed).to.be.false
    expect(store.panelState.siteInfo.editable).to.be.false
    expect(store.panelOpenStates.siteInfo).to.equal(CONSTANTS.PANEL.CLOSE)

    expect(store.panelState.standInfo.confirmed).to.be.false
    expect(store.panelState.standInfo.editable).to.be.false
    expect(store.panelOpenStates.standInfo).to.equal(CONSTANTS.PANEL.CLOSE)

    expect(store.panelState.reportInfo.confirmed).to.be.false
    expect(store.panelState.reportInfo.editable).to.be.false
    expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.CLOSE)

    expect(store.runModelEnabled).to.be.false
  })

  it('should handle species list with duplicates', () => {
    store.speciesList = [
      { species: 'PL', percent: '20.0' },
      { species: 'AC', percent: '30.0' },
      { species: 'PL', percent: '50.0' },
    ]
    store.updateSpeciesGroup()

    expect(store.speciesGroups.length).to.equal(2)
    expect(store.speciesGroups[0].group).to.equal('PL')
    expect(store.speciesGroups[0].percent).to.equal('70.0')
    expect(store.speciesGroups[1].group).to.equal('AC')
    expect(store.speciesGroups[1].percent).to.equal('30.0')
  })

  it('should handle species list with zero percentages', () => {
    store.speciesList = [
      { species: 'PL', percent: '0.0' },
      { species: 'AC', percent: '0.0' },
    ]
    store.updateSpeciesGroup()

    expect(store.speciesGroups.length).to.equal(0)
    expect(store.totalSpeciesPercent).to.equal('0.0')
  })

  it('should update computed properties correctly', () => {
    store.speciesList = [
      { species: 'PL', percent: '50.0' },
      { species: 'AC', percent: '50.0' },
    ]
    expect(store.totalSpeciesPercent).to.equal('100.0')

    store.updateSpeciesGroup()
    expect(store.totalSpeciesGroupPercent).to.equal(100)
  })

  it('should set reference year to current year', () => {
    store.setDefaultValues()
    const currentYear = new Date().getFullYear()
    expect(store.referenceYear).to.equal(currentYear)
  })

  it('should set highest percent species correctly', () => {
    store.speciesList = [
      { species: 'PL', percent: '60.0' },
      { species: 'AC', percent: '40.0' },
    ]
    store.updateSpeciesGroup()

    expect(store.highestPercentSpecies).to.equal('PL')
    expect(store.selectedSiteSpecies).to.equal('PL')
  })

  it('should set minimumDBHLimit from DEFAULTS.SPECIES_GROUP_DEFAULT_UTILIZATION_MAP in updateSpeciesGroup', () => {
    store.speciesList = [
      { species: 'PL', percent: '50.0' },
      { species: 'AC', percent: '50.0' },
    ]
    store.updateSpeciesGroup()

    expect(store.speciesGroups.length).to.equal(2)
    expect(store.speciesGroups[0].group).to.equal('PL')
    expect(store.speciesGroups[0].percent).to.equal('50.0')
    expect(store.speciesGroups[0].siteSpecies).to.equal('PL')
    expect(store.speciesGroups[0].minimumDBHLimit).to.equal(
      DEFAULTS.SPECIES_GROUP_DEFAULT_UTILIZATION_MAP['PL'],
    )

    expect(store.speciesGroups[1].group).to.equal('AC')
    expect(store.speciesGroups[1].percent).to.equal('50.0')
    expect(store.speciesGroups[1].siteSpecies).to.equal('AC')
    expect(store.speciesGroups[1].minimumDBHLimit).to.equal(
      DEFAULTS.SPECIES_GROUP_DEFAULT_UTILIZATION_MAP['AC'],
    )
  })

  it('should handle unmapped species group with default minimumDBHLimit', () => {
    store.speciesList = [{ species: 'XX', percent: '100.0' }]
    store.updateSpeciesGroup()

    expect(store.speciesGroups.length).to.equal(1)
    expect(store.speciesGroups[0].group).to.equal('XX')
    expect(store.speciesGroups[0].percent).to.equal('100.0')
    expect(store.speciesGroups[0].siteSpecies).to.equal('XX')
    expect(store.speciesGroups[0].minimumDBHLimit).to.equal(
      DEFAULTS.SPECIES_GROUP_DEFAULT_UTILIZATION_MAP['XX'] || undefined,
    )
  })

  it('should update basalArea correctly', () => {
    store.basalArea = '10.5'
    expect(store.basalArea).to.equal('10.5')
  })

  it('should update treesPerHectare correctly', () => {
    store.treesPerHectare = '200.0'
    expect(store.treesPerHectare).to.equal('200.0')
  })

  it('should update minDBHLimit correctly', () => {
    store.minDBHLimit = '12.7'
    expect(store.minDBHLimit).to.equal('12.7')
  })

  it('should update currentDiameter correctly', () => {
    store.currentDiameter = '15.3 cm'
    expect(store.currentDiameter).to.equal('15.3 cm')
  })

  it('should update crownClosure correctly', () => {
    store.crownClosure = 75
    expect(store.crownClosure).to.equal(75)
  })

  it('should update volume and utilization options correctly', () => {
    store.isWholeStemEnabled = false
    store.isCloseUtilEnabled = true
    store.isNetDecayEnabled = true
    store.isNetDecayWasteEnabled = true
    store.isNetDecayWasteBreakageEnabled = true

    expect(store.isWholeStemEnabled).to.be.false
    expect(store.isCloseUtilEnabled).to.be.true
    expect(store.isNetDecayEnabled).to.be.true
    expect(store.isNetDecayWasteEnabled).to.be.true
    expect(store.isNetDecayWasteBreakageEnabled).to.be.true
  })

  it('should update yield table options correctly', () => {
    store.isComputedMAIEnabled = true
    store.isCulminationValuesEnabled = true
    store.isBySpeciesEnabled = true
    store.isByLayerEnabled = true

    expect(store.isComputedMAIEnabled).to.be.true
    expect(store.isCulminationValuesEnabled).to.be.true
    expect(store.isBySpeciesEnabled).to.be.true
    expect(store.isByLayerEnabled).to.be.true
  })

  it('should update additional report options correctly', () => {
    store.isProjectionModeEnabled = true
    store.isPolygonIDEnabled = true
    store.isCurrentYearEnabled = true
    store.isReferenceYearEnabled = true
    store.incSecondaryHeight = true

    expect(store.isProjectionModeEnabled).to.be.true
    expect(store.isPolygonIDEnabled).to.be.true
    expect(store.isCurrentYearEnabled).to.be.true
    expect(store.isReferenceYearEnabled).to.be.true
    expect(store.incSecondaryHeight).to.be.true
  })

  it('should update specificYear correctly', () => {
    store.specificYear = 2025
    expect(store.specificYear).to.equal(2025)
  })

  it('should handle all boolean options in combination', () => {
    store.isWholeStemEnabled = false
    store.isCloseUtilEnabled = true
    store.isNetDecayEnabled = true
    store.isNetDecayWasteEnabled = true
    store.isNetDecayWasteBreakageEnabled = true
    store.isComputedMAIEnabled = true
    store.isCulminationValuesEnabled = true
    store.isBySpeciesEnabled = true
    store.isByLayerEnabled = true
    store.isProjectionModeEnabled = true
    store.isPolygonIDEnabled = true
    store.isCurrentYearEnabled = true
    store.isReferenceYearEnabled = true
    store.incSecondaryHeight = true
    store.specificYear = 2050

    expect(store.isWholeStemEnabled).to.be.false
    expect(store.isCloseUtilEnabled).to.be.true
    expect(store.isNetDecayEnabled).to.be.true
    expect(store.isNetDecayWasteEnabled).to.be.true
    expect(store.isNetDecayWasteBreakageEnabled).to.be.true
    expect(store.isComputedMAIEnabled).to.be.true
    expect(store.isCulminationValuesEnabled).to.be.true
    expect(store.isBySpeciesEnabled).to.be.true
    expect(store.isByLayerEnabled).to.be.true
    expect(store.isProjectionModeEnabled).to.be.true
    expect(store.isPolygonIDEnabled).to.be.true
    expect(store.isCurrentYearEnabled).to.be.true
    expect(store.isReferenceYearEnabled).to.be.true
    expect(store.incSecondaryHeight).to.be.true
    expect(store.specificYear).to.equal(2050)
  })

  it('should maintain boolean option states independently', () => {
    // Test that changing one boolean doesn't affect others
    store.isWholeStemEnabled = false
    expect(store.isCloseUtilEnabled).to.be.false // Should remain at default
    expect(store.isNetDecayEnabled).to.be.false // Should remain at default

    store.isCloseUtilEnabled = true
    expect(store.isWholeStemEnabled).to.be.false // Should not change
    expect(store.isNetDecayEnabled).to.be.false // Should remain at default
  })

  it('should handle null and undefined values for specificYear', () => {
    store.specificYear = null
    expect(store.specificYear).to.be.null

    store.specificYear = 2030
    expect(store.specificYear).to.equal(2030)

    store.specificYear = null
    expect(store.specificYear).to.be.null
  })
})
