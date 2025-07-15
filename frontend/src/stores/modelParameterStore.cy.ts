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

    expect(store.selectedAgeYearRange).to.be.null
    expect(store.startYear).to.be.null
    expect(store.endYear).to.be.null
    expect(store.yearIncrement).to.be.null
    expect(store.forwardBackwardGrow).to.deep.equal([])
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
  })

  it('should set default values correctly', () => {
    store.setDefaultValues()

    expect(store.derivedBy).to.equal(DEFAULTS.DEFAULT_VALUES.DERIVED_BY)
    expect(store.speciesList[0].species).to.equal('PL')
    expect(store.speciesList[0].percent).to.equal('30.0')
    expect(store.becZone).to.equal(DEFAULTS.DEFAULT_VALUES.BEC_ZONE)
    expect(store.startingAge).to.equal(DEFAULTS.DEFAULT_VALUES.STARTING_AGE)
    expect(store.finishingAge).to.equal(DEFAULTS.DEFAULT_VALUES.FINISHING_AGE)
    expect(store.ageIncrement).to.equal(DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT)

    expect(store.selectedAgeYearRange).to.equal(
      DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE,
    )
    expect(store.startYear).to.equal(DEFAULTS.DEFAULT_VALUES.START_YEAR)
    expect(store.endYear).to.equal(DEFAULTS.DEFAULT_VALUES.END_YEAR)
    expect(store.yearIncrement).to.equal(DEFAULTS.DEFAULT_VALUES.YEAR_INCREMENT)
    expect(store.forwardBackwardGrow).to.deep.equal(
      DEFAULTS.DEFAULT_VALUES.FORWARD_BACKWARD_GROW,
    )
  })

  it('should handle empty species list without errors', () => {
    store.speciesList = []
    store.updateSpeciesGroup()

    expect(store.speciesGroups.length).to.equal(0)
    expect(store.highestPercentSpecies).to.be.null
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

  it('should update forwardBackwardGrow correctly', () => {
    store.forwardBackwardGrow = [
      CONSTANTS.FORWARD_BACKWARD_GROW.FORWARD,
      CONSTANTS.FORWARD_BACKWARD_GROW.BACKWARD,
    ]

    expect(store.forwardBackwardGrow).to.deep.equal([
      CONSTANTS.FORWARD_BACKWARD_GROW.FORWARD,
      CONSTANTS.FORWARD_BACKWARD_GROW.BACKWARD,
    ])
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
})
