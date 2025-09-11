/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS } from '@/constants'
import { UtilizationClassSetEnum } from '@/services/vdyp-api/models/utilization-class-set-enum'

describe('FileUploadStore Unit Tests', () => {
  let store: ReturnType<typeof useFileUploadStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useFileUploadStore()
  })

  it('should initialize with default values', () => {
    store.setDefaultValues()

    expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)

    expect(store.panelState.reportInfo.confirmed).to.be.false
    expect(store.panelState.reportInfo.editable).to.be.true

    expect(store.panelState.attachments.confirmed).to.be.false
    expect(store.panelState.attachments.editable).to.be.false

    expect(store.runModelEnabled).to.be.false

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
    expect(store.isByLayerEnabled).to.equal(
      DEFAULTS.DEFAULT_VALUES.IS_BY_LAYER_ENABLED,
    )
    expect(store.projectionType).to.equal(
      DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
    )
    expect(store.reportTitle).to.equal(DEFAULTS.DEFAULT_VALUES.REPORT_TITLE)
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
    expect(store.isForwardGrowEnabled).to.equal(
      DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED,
    )
    expect(store.isBackwardGrowEnabled).to.equal(
      DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED,
    )
    expect(store.isByLayerEnabled).to.equal(
      DEFAULTS.DEFAULT_VALUES.IS_BY_LAYER_ENABLED,
    )
    expect(store.projectionType).to.equal(
      DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
    )
    expect(store.reportTitle).to.equal(DEFAULTS.DEFAULT_VALUES.REPORT_TITLE)
    
    // Check that species groups are initialized
    expect(store.fileUploadSpeciesGroup).to.have.length(16)
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
    store.projectionType = 'Volume'
    store.reportTitle = 'Test Report'
    store.isForwardGrowEnabled = true
    store.isBackwardGrowEnabled = true
    store.isByLayerEnabled = false

    expect(store.projectionType).to.equal('Volume')
    expect(store.reportTitle).to.equal('Test Report')
    expect(store.isForwardGrowEnabled).to.be.true
    expect(store.isBackwardGrowEnabled).to.be.true
    expect(store.isByLayerEnabled).to.be.false
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

  it('should handle full confirmation flow', () => {
    // Initial state
    expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.reportInfo.editable).to.be.true
    expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelState.attachments.editable).to.be.false
    expect(store.runModelEnabled).to.be.false

    // Confirm reportInfo
    store.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO)
    expect(store.panelState.reportInfo.confirmed).to.be.true
    expect(store.panelState.reportInfo.editable).to.be.false
    expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.OPEN)
    expect(store.panelState.attachments.editable).to.be.true
    expect(store.runModelEnabled).to.be.false

    // Confirm attachments
    store.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS)
    expect(store.panelState.attachments.confirmed).to.be.true
    expect(store.panelState.attachments.editable).to.be.false
    expect(store.runModelEnabled).to.be.true
  })

  it('should reset subsequent panels when editing a confirmed panel', () => {
    // Confirm both panels
    store.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO)
    store.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS)
    expect(store.runModelEnabled).to.be.true

    // Edit reportInfo
    store.editPanel(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO)
    expect(store.panelState.reportInfo.confirmed).to.be.false
    expect(store.panelState.reportInfo.editable).to.be.true
    expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
    expect(store.panelState.attachments.confirmed).to.be.false
    expect(store.panelState.attachments.editable).to.be.false
    expect(store.runModelEnabled).to.be.false
  })

  it('should not enable run model button when not all panels are confirmed', () => {
    store.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO)
    expect(store.runModelEnabled).to.be.false
  })

  it('should handle setting and unsetting attachment files', () => {
    const polygonFile = new File(['polygon'], 'polygon.csv', {
      type: 'text/csv',
    })
    const layerFile = new File(['layer'], 'layer.csv', { type: 'text/csv' })

    store.polygonFile = polygonFile
    store.layerFile = layerFile
    expect(store.polygonFile).to.equal(polygonFile)
    expect(store.layerFile).to.equal(layerFile)

    store.polygonFile = null
    store.layerFile = null
    expect(store.polygonFile).to.be.null
    expect(store.layerFile).to.be.null
  })

  describe('Species Groups Functionality', () => {
    beforeEach(() => {
      store.setDefaultValues()
    })

    it('should initialize species groups with correct default values', () => {
      expect(store.fileUploadSpeciesGroup).to.have.length(16)
      expect(store.fileUploadSpeciesGroup).to.deep.equal(
        BIZCONSTANTS.SPECIES_GROUPS.map((group) => ({
          group,
          minimumDBHLimit: DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group],
        }))
      )
    })

    it('should initialize species groups when initializeSpeciesGroups is called', () => {
      // Clear species groups
      store.fileUploadSpeciesGroup.length = 0
      expect(store.fileUploadSpeciesGroup).to.have.length(0)

      // Initialize species groups
      store.initializeSpeciesGroups()
      expect(store.fileUploadSpeciesGroup).to.have.length(16)

      // Check each species group has correct default values
      store.fileUploadSpeciesGroup.forEach((group) => {
        expect(BIZCONSTANTS.SPECIES_GROUPS).to.include(group.group)
        expect(group.minimumDBHLimit).to.equal(
          DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group.group]
        )
      })
    })

    it('should update species groups for Volume projection type', () => {
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.VOLUME)

      store.fileUploadSpeciesGroup.forEach((group) => {
        expect(group.minimumDBHLimit).to.equal(
          DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group.group]
        )
      })
    })

    it('should update species groups for CFO Biomass projection type', () => {
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

      store.fileUploadSpeciesGroup.forEach((group) => {
        expect(group.minimumDBHLimit).to.equal(
          DEFAULTS.SPECIES_GROUP_CFO_BIOMASS_UTILIZATION_MAP[group.group]
        )
      })
    })

    it('should handle null projection type and default to Volume', () => {
      store.updateSpeciesGroupsForProjectionType(null)

      store.fileUploadSpeciesGroup.forEach((group) => {
        expect(group.minimumDBHLimit).to.equal(
          DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group.group]
        )
      })
    })

    it('should contain all expected species groups', () => {
      const expectedSpeciesGroups = ['AC', 'AT', 'B', 'C', 'D', 'E', 'F', 'H', 'L', 'MB', 'PA', 'PL', 'PW', 'PY', 'S', 'Y']
      
      expect(store.fileUploadSpeciesGroup).to.have.length(expectedSpeciesGroups.length)
      
      const actualGroups = store.fileUploadSpeciesGroup.map(sg => sg.group)
      expectedSpeciesGroups.forEach(expectedGroup => {
        expect(actualGroups).to.include(expectedGroup)
      })
    })

    it('should have correct default utilization values for Volume projection', () => {
      // All should default to 7.5+ for Volume projection
      store.fileUploadSpeciesGroup.forEach((group) => {
        expect(group.minimumDBHLimit).to.equal(UtilizationClassSetEnum._75)
      })
    })

    it('should have correct utilization values for CFO Biomass projection', () => {
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

      // Check specific values for CFO Biomass
      const expectedValues = {
        'AC': UtilizationClassSetEnum._125,
        'AT': UtilizationClassSetEnum._125,
        'B': UtilizationClassSetEnum._175,
        'C': UtilizationClassSetEnum._175,
        'D': UtilizationClassSetEnum._125,
        'E': UtilizationClassSetEnum._125,
        'F': UtilizationClassSetEnum._175,
        'H': UtilizationClassSetEnum._175,
        'L': UtilizationClassSetEnum._125,
        'MB': UtilizationClassSetEnum._125,
        'PA': UtilizationClassSetEnum._125,
        'PL': UtilizationClassSetEnum._125,
        'PW': UtilizationClassSetEnum._125,
        'PY': UtilizationClassSetEnum._125,
        'S': UtilizationClassSetEnum._175,
        'Y': UtilizationClassSetEnum._175,
      }

      store.fileUploadSpeciesGroup.forEach((group) => {
        expect(group.minimumDBHLimit).to.equal(expectedValues[group.group as keyof typeof expectedValues])
      })
    })

    it('should maintain species group structure after projection type changes', () => {
      const originalGroups = store.fileUploadSpeciesGroup.map(sg => sg.group)
      
      // Change to CFO Biomass
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
      let currentGroups = store.fileUploadSpeciesGroup.map(sg => sg.group)
      expect(currentGroups).to.deep.equal(originalGroups)
      
      // Change back to Volume
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.VOLUME)
      currentGroups = store.fileUploadSpeciesGroup.map(sg => sg.group)
      expect(currentGroups).to.deep.equal(originalGroups)
    })
  })
})
