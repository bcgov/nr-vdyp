/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS } from '@/constants'
import { ExecutionOptionsEnum, UtilizationClassSetEnum } from '@/services/vdyp-api'
import type { ParsedProjectionParameters } from '@/interfaces/interfaces'

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

describe('File Upload Store Unit Tests', () => {
  let store: ReturnType<typeof useFileUploadStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useFileUploadStore()
  })

  describe('Initial State', () => {
    it('should open reportInfo panel and close others', () => {
      expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should set reportInfo as editable and others as non-editable', () => {
      expect(store.panelState.reportInfo).to.deep.equal({ confirmed: false, editable: true })
      expect(store.panelState.minimumDBH).to.deep.equal({ confirmed: false, editable: false })
      expect(store.panelState.attachments).to.deep.equal({ confirmed: false, editable: false })
    })

    it('should initialize runModelEnabled as false', () => {
      expect(store.runModelEnabled).to.be.false
    })

    it('should initialize selectedAgeYearRange to the default value', () => {
      expect(store.selectedAgeYearRange).to.equal(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
    })

    it('should initialize all age/year fields as null', () => {
      expect(store.startingAge).to.be.null
      expect(store.finishingAge).to.be.null
      expect(store.ageIncrement).to.be.null
      expect(store.startYear).to.be.null
      expect(store.endYear).to.be.null
      expect(store.yearIncrement).to.be.null
    })

    it('should initialize all boolean flags as false', () => {
      expect(store.isForwardGrowEnabled).to.be.false
      expect(store.isBackwardGrowEnabled).to.be.false
      expect(store.isComputedMAIEnabled).to.be.false
      expect(store.isCulminationValuesEnabled).to.be.false
      expect(store.isBySpeciesEnabled).to.be.false
      expect(store.isByLayerEnabled).to.be.false
      expect(store.isProjectionModeEnabled).to.be.false
      expect(store.isPolygonIDEnabled).to.be.false
      expect(store.isCurrentYearEnabled).to.be.false
      expect(store.isReferenceYearEnabled).to.be.false
      expect(store.incSecondaryHeight).to.be.false
    })

    it('should initialize projectionType, reportTitle, reportDescription as null', () => {
      expect(store.projectionType).to.be.null
      expect(store.reportTitle).to.be.null
      expect(store.reportDescription).to.be.null
    })

    it('should initialize file-related state as null/false', () => {
      expect(store.polygonFile).to.be.null
      expect(store.layerFile).to.be.null
      expect(store.polygonFileInfo).to.be.null
      expect(store.layerFileInfo).to.be.null
      expect(store.isUploadingPolygon).to.be.false
      expect(store.isUploadingLayer).to.be.false
      expect(store.isDeletingFile).to.be.false
    })

    it('should initialize fileUploadSpeciesGroup as empty array', () => {
      expect(store.fileUploadSpeciesGroup).to.deep.equal([])
    })
  })

  describe('confirmPanel', () => {
    it('should mark reportInfo as confirmed and close it', () => {
      store.confirmPanel('reportInfo')

      expect(store.panelState.reportInfo.confirmed).to.be.true
      expect(store.panelState.reportInfo.editable).to.be.false
      expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should open minimumDBH and make it editable when reportInfo is confirmed', () => {
      store.confirmPanel('reportInfo')

      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.minimumDBH.editable).to.be.true
    })

    it('should open attachments panel when minimumDBH (last sequential panel) is confirmed', () => {
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')

      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.OPEN)
    })

    it('should not enable runModelEnabled when panels confirmed but no files uploaded', () => {
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')

      expect(store.runModelEnabled).to.be.false
    })

    it('should enable runModelEnabled when all sequential panels confirmed and both files uploaded', () => {
      store.setPolygonFileInfo({ filename: 'poly.csv', fileMappingGUID: 'poly-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'layer.csv', fileMappingGUID: 'layer-guid', fileSetGUID: 'set-1' })
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')

      expect(store.runModelEnabled).to.be.true
    })
  })

  describe('editPanel', () => {
    beforeEach(() => {
      // Fully confirm both sequential panels first
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')
    })

    it('should unconfirm reportInfo and open it for editing', () => {
      store.editPanel('reportInfo')

      expect(store.panelState.reportInfo.confirmed).to.be.false
      expect(store.panelState.reportInfo.editable).to.be.true
      expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.OPEN)
    })

    it('should close and disable subsequent sequential panels when editing reportInfo', () => {
      store.editPanel('reportInfo')

      expect(store.panelState.minimumDBH.confirmed).to.be.false
      expect(store.panelState.minimumDBH.editable).to.be.false
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should close the attachments panel when editing any sequential panel', () => {
      store.editPanel('reportInfo')

      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should set runModelEnabled to false when editing a panel', () => {
      store.setPolygonFileInfo({ filename: 'poly.csv', fileMappingGUID: 'poly-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'layer.csv', fileMappingGUID: 'layer-guid', fileSetGUID: 'set-1' })
      // watch is async - call explicitly to sync state before asserting
      store.updateRunModelEnabled()
      expect(store.runModelEnabled).to.be.true

      store.editPanel('reportInfo')

      expect(store.runModelEnabled).to.be.false
    })

    it('should only close panels after minimumDBH when editing minimumDBH (no subsequent sequential panels)', () => {
      store.editPanel('minimumDBH')

      expect(store.panelState.minimumDBH.confirmed).to.be.false
      expect(store.panelState.minimumDBH.editable).to.be.true
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.OPEN)
      // Attachments must close too
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
    })
  })

  describe('updateRunModelEnabled', () => {
    it('should be false when no panels confirmed and no files', () => {
      store.updateRunModelEnabled()

      expect(store.runModelEnabled).to.be.false
    })

    it('should be false when only panels are confirmed but no files', () => {
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')

      expect(store.runModelEnabled).to.be.false
    })

    it('should be false when files uploaded but no panels confirmed', () => {
      store.setPolygonFileInfo({ filename: 'p.csv', fileMappingGUID: 'p-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'l.csv', fileMappingGUID: 'l-guid', fileSetGUID: 'set-1' })

      expect(store.runModelEnabled).to.be.false
    })

    it('should be false when only polygon file is set (layer missing)', () => {
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')
      store.setPolygonFileInfo({ filename: 'p.csv', fileMappingGUID: 'p-guid', fileSetGUID: 'set-1' })

      expect(store.runModelEnabled).to.be.false
    })

    it('should be false when only layer file is set (polygon missing)', () => {
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')
      store.setLayerFileInfo({ filename: 'l.csv', fileMappingGUID: 'l-guid', fileSetGUID: 'set-1' })

      expect(store.runModelEnabled).to.be.false
    })

    it('should become false again when a file is removed after being enabled', () => {
      store.setPolygonFileInfo({ filename: 'p.csv', fileMappingGUID: 'p-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'l.csv', fileMappingGUID: 'l-guid', fileSetGUID: 'set-1' })
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')
      expect(store.runModelEnabled).to.be.true

      store.setPolygonFileInfo(null)
      // watch is async - call explicitly to sync state before asserting
      store.updateRunModelEnabled()

      expect(store.runModelEnabled).to.be.false
    })
  })

  describe('setPolygonFileInfo / setLayerFileInfo', () => {
    it('should update polygonFileInfo', () => {
      const info = { filename: 'poly.csv', fileMappingGUID: 'abc-guid', fileSetGUID: 'set-1' }
      store.setPolygonFileInfo(info)

      expect(store.polygonFileInfo).to.deep.equal(info)
    })

    it('should update layerFileInfo', () => {
      const info = { filename: 'layer.csv', fileMappingGUID: 'xyz-guid', fileSetGUID: 'set-1' }
      store.setLayerFileInfo(info)

      expect(store.layerFileInfo).to.deep.equal(info)
    })

    it('should set polygonFileInfo back to null', () => {
      store.setPolygonFileInfo({ filename: 'poly.csv', fileMappingGUID: 'abc-guid', fileSetGUID: 'set-1' })
      store.setPolygonFileInfo(null)

      expect(store.polygonFileInfo).to.be.null
    })

    it('should set layerFileInfo back to null', () => {
      store.setLayerFileInfo({ filename: 'layer.csv', fileMappingGUID: 'xyz-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo(null)

      expect(store.layerFileInfo).to.be.null
    })
  })

  describe('initializeSpeciesGroups', () => {
    it('should create 16 species groups matching BIZCONSTANTS.SPECIES_GROUPS', () => {
      store.initializeSpeciesGroups()

      expect(store.fileUploadSpeciesGroup).to.have.length(BIZCONSTANTS.SPECIES_GROUPS.length)
      BIZCONSTANTS.SPECIES_GROUPS.forEach((group, i) => {
        expect(store.fileUploadSpeciesGroup[i].group).to.equal(group)
      })
    })

    it('should assign volume utilization defaults to all groups', () => {
      store.initializeSpeciesGroups()

      store.fileUploadSpeciesGroup.forEach(({ group, minimumDBHLimit }) => {
        expect(minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group])
      })
    })
  })


  describe('updateSpeciesGroupsForProjectionType', () => {
    beforeEach(() => {
      store.initializeSpeciesGroups()
    })

    it('should set CFS Biomass utilization map when type is CFS_BIOMASS', () => {
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

      store.fileUploadSpeciesGroup.forEach(({ group, minimumDBHLimit }) => {
        expect(minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_CFO_BIOMASS_UTILIZATION_MAP[group])
      })
    })

    it('should set Volume utilization map when type is Volume', () => {
      // Switch to CFS Biomass first, then back to Volume
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.VOLUME)

      store.fileUploadSpeciesGroup.forEach(({ group, minimumDBHLimit }) => {
        expect(minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group])
      })
    })

    it('should set Volume utilization map when type is null', () => {
      store.updateSpeciesGroupsForProjectionType(null)

      store.fileUploadSpeciesGroup.forEach(({ group, minimumDBHLimit }) => {
        expect(minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group])
      })
    })
  })

  describe('resetStore', () => {
    it('should reset panel open states', () => {
      store.confirmPanel('reportInfo')
      store.resetStore()

      expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should reset panel confirmed/editable states', () => {
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')
      store.resetStore()

      expect(store.panelState.reportInfo).to.deep.equal({ confirmed: false, editable: true })
      expect(store.panelState.minimumDBH).to.deep.equal({ confirmed: false, editable: false })
      expect(store.panelState.attachments).to.deep.equal({ confirmed: false, editable: false })
    })

    it('should reset runModelEnabled to false', () => {
      store.setPolygonFileInfo({ filename: 'p.csv', fileMappingGUID: 'p-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'l.csv', fileMappingGUID: 'l-guid', fileSetGUID: 'set-1' })
      store.confirmPanel('reportInfo')
      store.confirmPanel('minimumDBH')
      expect(store.runModelEnabled).to.be.true

      store.resetStore()

      expect(store.runModelEnabled).to.be.false
    })

    it('should reset selectedAgeYearRange to default', () => {
      store.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
      store.resetStore()

      expect(store.selectedAgeYearRange).to.equal(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
    })

    it('should reset file info and upload states', () => {
      store.setPolygonFileInfo({ filename: 'p.csv', fileMappingGUID: 'p-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'l.csv', fileMappingGUID: 'l-guid', fileSetGUID: 'set-1' })
      store.isUploadingPolygon = true
      store.isUploadingLayer = true
      store.isDeletingFile = true
      store.resetStore()

      expect(store.polygonFileInfo).to.be.null
      expect(store.layerFileInfo).to.be.null
      expect(store.isUploadingPolygon).to.be.false
      expect(store.isUploadingLayer).to.be.false
      expect(store.isDeletingFile).to.be.false
    })

    it('should clear fileUploadSpeciesGroup', () => {
      store.initializeSpeciesGroups()
      expect(store.fileUploadSpeciesGroup).to.have.length.greaterThan(0)

      store.resetStore()

      expect(store.fileUploadSpeciesGroup).to.deep.equal([])
    })

    it('should reset all boolean options to false', () => {
      store.isForwardGrowEnabled = true
      store.isBackwardGrowEnabled = true
      store.isComputedMAIEnabled = true
      store.isCulminationValuesEnabled = true
      store.resetStore()

      expect(store.isForwardGrowEnabled).to.be.false
      expect(store.isBackwardGrowEnabled).to.be.false
      expect(store.isComputedMAIEnabled).to.be.false
      expect(store.isCulminationValuesEnabled).to.be.false
    })
  })

  describe('restoreFromProjectionParams (view mode)', () => {
    const params = makeParsedParams({
      reportTitle: 'My Report',
      ageStart: '10',
      ageEnd: '100',
      ageIncrement: '5',
      selectedExecutionOptions: [
        ExecutionOptionsEnum.ForwardGrowEnabled,
        ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
      ],
      utils: [{ s: 'AC', u: UtilizationClassSetEnum._125 }],
    })

    beforeEach(() => {
      store.restoreFromProjectionParams(params, true)
    })

    it('should restore reportTitle', () => {
      expect(store.reportTitle).to.equal('My Report')
    })

    it('should restore age range values', () => {
      expect(store.startingAge).to.equal('10')
      expect(store.finishingAge).to.equal('100')
      expect(store.ageIncrement).to.equal('5')
      expect(store.selectedAgeYearRange).to.equal(CONSTANTS.AGE_YEAR_RANGE.AGE)
    })

    it('should restore execution options flags', () => {
      expect(store.isForwardGrowEnabled).to.be.true
      expect(store.isBackwardGrowEnabled).to.be.false
    })

    it('should open all panels in view mode', () => {
      expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.OPEN)
    })

    it('should confirm all panels but keep them non-editable in view mode', () => {
      expect(store.panelState.reportInfo).to.deep.equal({ confirmed: true, editable: false })
      expect(store.panelState.minimumDBH).to.deep.equal({ confirmed: true, editable: false })
      expect(store.panelState.attachments).to.deep.equal({ confirmed: true, editable: false })
    })

    it('should keep runModelEnabled false in view mode', () => {
      expect(store.runModelEnabled).to.be.false
    })
  })

  describe('restoreFromProjectionParams (edit mode)', () => {
    it('should open reportInfo and close others when reportTitle is null', () => {
      store.restoreFromProjectionParams(makeParsedParams({ reportTitle: null }), false)

      expect(store.panelOpenStates.reportInfo).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelState.reportInfo.confirmed).to.be.false
    })

    it('should confirm reportInfo and open minimumDBH when only reportTitle is set', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({ reportTitle: 'My Title', utils: [] }),
        false,
      )

      expect(store.panelState.reportInfo.confirmed).to.be.true
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.minimumDBH.editable).to.be.true
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should confirm both sequential panels and open attachments when both are fully specified', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          reportTitle: 'My Title',
          utils: [{ s: 'AC', u: UtilizationClassSetEnum._125 }],
        }),
        false,
      )

      expect(store.panelState.reportInfo.confirmed).to.be.true
      expect(store.panelState.minimumDBH.confirmed).to.be.true
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.OPEN)
    })

    it('should keep runModelEnabled false in edit mode (no files)', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          reportTitle: 'My Title',
          utils: [{ s: 'AC', u: UtilizationClassSetEnum._125 }],
        }),
        false,
      )

      expect(store.runModelEnabled).to.be.false
    })

    it('should restore year range when only year params are present', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({ yearStart: '2020', yearEnd: '2050' }),
        false,
      )

      expect(store.selectedAgeYearRange).to.equal(CONSTANTS.AGE_YEAR_RANGE.YEAR)
    })
  })

  describe('restoreExecutionOptions (via restoreFromProjectionParams)', () => {
    const allOptions = [
      ExecutionOptionsEnum.ForwardGrowEnabled,
      ExecutionOptionsEnum.BackGrowEnabled,
      ExecutionOptionsEnum.DoSummarizeProjectionByLayer,
      ExecutionOptionsEnum.DoIncludeSpeciesProjection,
      ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
      ExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable,
      ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
      ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
      ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
      ExecutionOptionsEnum.ReportIncludeVolumeMAI,
      ExecutionOptionsEnum.ReportIncludeCulminationValues,
    ]

    beforeEach(() => {
      store.restoreFromProjectionParams(
        makeParsedParams({ selectedExecutionOptions: allOptions }),
        true,
      )
    })

    it('should set isForwardGrowEnabled', () => {
      expect(store.isForwardGrowEnabled).to.be.true
    })

    it('should set isBackwardGrowEnabled', () => {
      expect(store.isBackwardGrowEnabled).to.be.true
    })

    it('should set isByLayerEnabled', () => {
      expect(store.isByLayerEnabled).to.be.true
    })

    it('should set isBySpeciesEnabled', () => {
      expect(store.isBySpeciesEnabled).to.be.true
    })

    it('should set isPolygonIDEnabled', () => {
      expect(store.isPolygonIDEnabled).to.be.true
    })

    it('should set isProjectionModeEnabled', () => {
      expect(store.isProjectionModeEnabled).to.be.true
    })

    it('should set isCurrentYearEnabled', () => {
      expect(store.isCurrentYearEnabled).to.be.true
    })

    it('should set isReferenceYearEnabled', () => {
      expect(store.isReferenceYearEnabled).to.be.true
    })

    it('should set incSecondaryHeight', () => {
      expect(store.incSecondaryHeight).to.be.true
    })

    it('should set isComputedMAIEnabled', () => {
      expect(store.isComputedMAIEnabled).to.be.true
    })

    it('should set isCulminationValuesEnabled', () => {
      expect(store.isCulminationValuesEnabled).to.be.true
    })
  })

  describe('restoreProjectionTypeAndSpeciesGroups (via restoreFromProjectionParams)', () => {
    it('should set projectionType to CFS_BIOMASS', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass],
        }),
        true,
      )

      expect(store.projectionType).to.equal(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
    })

    it('should set projectionType to VOLUME', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes],
        }),
        true,
      )

      expect(store.projectionType).to.equal(CONSTANTS.PROJECTION_TYPE.VOLUME)
    })

    it('should initialize species groups with CFS Biomass utilization when type is CFS_BIOMASS', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass],
        }),
        true,
      )

      expect(store.fileUploadSpeciesGroup).to.have.length(BIZCONSTANTS.SPECIES_GROUPS.length)
      const acGroup = store.fileUploadSpeciesGroup.find((g) => g.group === 'AC')
      expect(acGroup?.minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_CFO_BIOMASS_UTILIZATION_MAP['AC'])
    })
  })

  describe('restoreUtilizationLevels (via restoreFromProjectionParams)', () => {
    it('should override species group utilization with values from params.utils', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes],
          utils: [{ s: 'F', u: UtilizationClassSetEnum._175 }],
        }),
        true,
      )

      const fGroup = store.fileUploadSpeciesGroup.find((g) => g.group === 'F')
      expect(fGroup?.minimumDBHLimit).to.equal(UtilizationClassSetEnum._175)
    })

    it('should not change species groups when utils array is empty', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes],
          utils: [],
        }),
        true,
      )

      const acGroup = store.fileUploadSpeciesGroup.find((g) => g.group === 'AC')
      expect(acGroup?.minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP['AC'])
    })

    it('should only override species that appear in utils (others remain unchanged)', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes],
          utils: [{ s: 'B', u: UtilizationClassSetEnum._225 }],
        }),
        true,
      )

      const bGroup = store.fileUploadSpeciesGroup.find((g) => g.group === 'B')
      const acGroup = store.fileUploadSpeciesGroup.find((g) => g.group === 'AC')
      expect(bGroup?.minimumDBHLimit).to.equal(UtilizationClassSetEnum._225)
      expect(acGroup?.minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP['AC'])
    })
  })
})
