/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import { CONSTANTS, DEFAULTS } from '@/constants'

describe('FileUploadStore Unit Tests', () => {
  let store: ReturnType<typeof useFileUploadStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useFileUploadStore()
  })

  it('should initialize with default values', () => {
    expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)

    expect(store.panelState.reportInfo.confirmed).to.be.false
    expect(store.panelState.reportInfo.editable).to.be.true

    expect(store.panelState.attachments.confirmed).to.be.false
    expect(store.panelState.attachments.editable).to.be.false

    expect(store.runModelEnabled).to.be.false

    expect(store.selectedAgeYearRange).to.be.null
    expect(store.startingAge).to.be.null
    expect(store.finishingAge).to.be.null
    expect(store.ageIncrement).to.be.null
    expect(store.startYear).to.be.null
    expect(store.endYear).to.be.null
    expect(store.yearIncrement).to.be.null
    expect(store.volumeReported).to.deep.equal([])
    expect(store.includeInReport).to.deep.equal([])
    expect(store.projectionType).to.be.null
    expect(store.reportTitle).to.be.null

    expect(store.polygonFile).to.be.null
    expect(store.layerFile).to.be.null
  })

  it('should confirm panel and enable the next panel', () => {
    store.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO)

    expect(store.panelState.reportInfo.confirmed).to.be.true
    expect(store.panelState.reportInfo.editable).to.be.false
    expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.attachments.editable).to.be.true
  })

  it('should edit panel and disable subsequent panels', () => {
    store.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO)
    store.editPanel(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO)

    expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.reportInfo.confirmed).to.be.false
    expect(store.panelState.reportInfo.editable).to.be.true

    expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelState.attachments.confirmed).to.be.false
    expect(store.panelState.attachments.editable).to.be.false
  })

  it('should enable run model button when all panels are confirmed', () => {
    store.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO)
    store.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS)

    expect(store.runModelEnabled).to.be.true
  })

  it('should set default values correctly', () => {
    store.setDefaultValues()

    expect(store.selectedAgeYearRange).to.equal(
      DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE,
    )
    expect(store.startingAge).to.equal(DEFAULTS.DEFAULT_VALUES.STARTING_AGE)
    expect(store.finishingAge).to.equal(DEFAULTS.DEFAULT_VALUES.FINISHING_AGE)
    expect(store.ageIncrement).to.equal(DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT)
    expect(store.startYear).to.equal(DEFAULTS.DEFAULT_VALUES.START_YEAR)
    expect(store.endYear).to.equal(DEFAULTS.DEFAULT_VALUES.END_YEAR)
    expect(store.yearIncrement).to.equal(DEFAULTS.DEFAULT_VALUES.YEAR_INCREMENT)
    expect(store.volumeReported).to.deep.equal(
      DEFAULTS.DEFAULT_VALUES.VOLUME_REPORTED,
    )
    expect(store.projectionType).to.equal(
      DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
    )
    expect(store.reportTitle).to.equal(DEFAULTS.DEFAULT_VALUES.REPORT_TITLE)
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
    store.volumeReported = ['Whole Stem']
    store.includeInReport = ['Computed MAI']
    store.projectionType = 'Volume'
    store.reportTitle = 'Test Report'

    expect(store.volumeReported).to.deep.equal(['Whole Stem'])
    expect(store.includeInReport).to.deep.equal(['Computed MAI'])
    expect(store.projectionType).to.equal('Volume')
    expect(store.reportTitle).to.equal('Test Report')
  })

  it('should update attachment files correctly', () => {
    const polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })
    const layerFile = new File(['layer content'], 'layer.csv', {
      type: 'text/csv',
    })

    store.polygonFile = polygonFile
    store.layerFile = layerFile

    expect(store.polygonFile).to.equal(polygonFile)
    expect(store.layerFile).to.equal(layerFile)
  })
})
