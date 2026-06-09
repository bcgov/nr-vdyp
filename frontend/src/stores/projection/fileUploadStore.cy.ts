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
    it('should open only reportConfig panel and mark it editable', () => {
      expect(store.panelOpenStates.reportConfig).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelState.reportConfig).to.deep.equal({ confirmed: false, editable: true })
      expect(store.panelState.minimumDBH).to.deep.equal({ confirmed: false, editable: false })
      expect(store.panelState.attachments).to.deep.equal({ confirmed: false, editable: false })
    })

    it('should initialize all data fields to defaults', () => {
      expect(store.runModelEnabled).to.be.false
      expect(store.selectedAgeYearRange).to.equal(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
      expect(store.projectionType).to.equal(DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE)
      expect(store.reportTitle).to.be.null
      expect(store.polygonFileInfo).to.be.null
      expect(store.layerFileInfo).to.be.null
      expect(store.fileUploadSpeciesGroup).to.deep.equal([])
      expect(store.isForwardGrowEnabled).to.be.false
      expect(store.isBackwardGrowEnabled).to.be.false
    })
  })

  describe('confirmPanel', () => {
    it('should confirm a panel, close it, and open the next sequential panel', () => {
      store.confirmPanel('reportConfig')

      expect(store.panelState.reportConfig).to.deep.equal({ confirmed: true, editable: false })
      expect(store.panelOpenStates.reportConfig).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.minimumDBH.editable).to.be.true
    })

    it('should open attachments and enable runModel when all panels confirmed and both files uploaded', () => {
      store.setPolygonFileInfo({ filename: 'poly.csv', fileMappingGUID: 'poly-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'layer.csv', fileMappingGUID: 'layer-guid', fileSetGUID: 'set-1' })
      store.confirmPanel('reportConfig')
      store.confirmPanel('minimumDBH')

      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.runModelEnabled).to.be.true
    })

    it('should not enable runModelEnabled when panels confirmed but files are missing', () => {
      store.confirmPanel('reportConfig')
      store.confirmPanel('minimumDBH')

      expect(store.runModelEnabled).to.be.false
    })
  })

  describe('editPanel', () => {
    beforeEach(() => {
      store.confirmPanel('reportConfig')
      store.confirmPanel('minimumDBH')
    })

    it('should unconfirm the panel, reopen it, and reset all subsequent panels', () => {
      store.editPanel('reportConfig')

      expect(store.panelState.reportConfig).to.deep.equal({ confirmed: false, editable: true })
      expect(store.panelOpenStates.reportConfig).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.minimumDBH).to.deep.equal({ confirmed: false, editable: false })
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)
    })

    it('should set runModelEnabled to false when editing a panel', () => {
      store.setPolygonFileInfo({ filename: 'poly.csv', fileMappingGUID: 'poly-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'layer.csv', fileMappingGUID: 'layer-guid', fileSetGUID: 'set-1' })
      store.updateRunModelEnabled()
      expect(store.runModelEnabled).to.be.true

      store.editPanel('reportConfig')

      expect(store.runModelEnabled).to.be.false
    })
  })

  describe('updateRunModelEnabled', () => {
    it('should require all sequential panels confirmed AND both files uploaded', () => {
      store.setPolygonFileInfo({ filename: 'p.csv', fileMappingGUID: 'p-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'l.csv', fileMappingGUID: 'l-guid', fileSetGUID: 'set-1' })
      expect(store.runModelEnabled).to.be.false // panels not confirmed yet

      store.confirmPanel('reportConfig')
      store.confirmPanel('minimumDBH')
      expect(store.runModelEnabled).to.be.true
    })

    it('should become false when a file is removed after being enabled', () => {
      store.setPolygonFileInfo({ filename: 'p.csv', fileMappingGUID: 'p-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'l.csv', fileMappingGUID: 'l-guid', fileSetGUID: 'set-1' })
      store.confirmPanel('reportConfig')
      store.confirmPanel('minimumDBH')
      expect(store.runModelEnabled).to.be.true

      store.setPolygonFileInfo(null)
      store.updateRunModelEnabled()

      expect(store.runModelEnabled).to.be.false
    })
  })

  describe('setPolygonFileInfo / setLayerFileInfo', () => {
    it('should update and clear file info', () => {
      const polyInfo = { filename: 'poly.csv', fileMappingGUID: 'abc-guid', fileSetGUID: 'set-1' }
      const layerInfo = { filename: 'layer.csv', fileMappingGUID: 'xyz-guid', fileSetGUID: 'set-1' }

      store.setPolygonFileInfo(polyInfo)
      store.setLayerFileInfo(layerInfo)
      expect(store.polygonFileInfo).to.deep.equal(polyInfo)
      expect(store.layerFileInfo).to.deep.equal(layerInfo)

      store.setPolygonFileInfo(null)
      store.setLayerFileInfo(null)
      expect(store.polygonFileInfo).to.be.null
      expect(store.layerFileInfo).to.be.null
    })
  })

  describe('initializeSpeciesGroups', () => {
    it('should create groups matching BIZCONSTANTS with volume utilization defaults', () => {
      store.initializeSpeciesGroups()

      expect(store.fileUploadSpeciesGroup).to.have.length(BIZCONSTANTS.SPECIES_GROUPS.length)
      store.fileUploadSpeciesGroup.forEach(({ group, minimumDBHLimit }) => {
        expect(group).to.be.oneOf(BIZCONSTANTS.SPECIES_GROUPS)
        expect(minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group])
      })
    })
  })

  describe('updateSpeciesGroupsForProjectionType', () => {
    beforeEach(() => {
      store.initializeSpeciesGroups()
    })

    it('should apply CFS Biomass utilization map when type is CFS_BIOMASS', () => {
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)

      store.fileUploadSpeciesGroup.forEach(({ group, minimumDBHLimit }) => {
        expect(minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_CFO_BIOMASS_UTILIZATION_MAP[group])
      })
    })

    it('should apply Volume utilization map when type is Volume or null', () => {
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
      store.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.VOLUME)

      store.fileUploadSpeciesGroup.forEach(({ group, minimumDBHLimit }) => {
        expect(minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group])
      })

      store.updateSpeciesGroupsForProjectionType(null)
      store.fileUploadSpeciesGroup.forEach(({ group, minimumDBHLimit }) => {
        expect(minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group])
      })
    })
  })

  describe('resetStore', () => {
    it('should restore all state to initial defaults', () => {
      store.setPolygonFileInfo({ filename: 'p.csv', fileMappingGUID: 'p-guid', fileSetGUID: 'set-1' })
      store.setLayerFileInfo({ filename: 'l.csv', fileMappingGUID: 'l-guid', fileSetGUID: 'set-1' })
      store.confirmPanel('reportConfig')
      store.confirmPanel('minimumDBH')
      store.isForwardGrowEnabled = true
      store.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
      store.initializeSpeciesGroups()

      store.resetStore()

      expect(store.panelOpenStates.reportConfig).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.CLOSE)
      expect(store.panelState.reportConfig).to.deep.equal({ confirmed: false, editable: true })
      expect(store.panelState.minimumDBH).to.deep.equal({ confirmed: false, editable: false })
      expect(store.runModelEnabled).to.be.false
      expect(store.selectedAgeYearRange).to.equal(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
      expect(store.polygonFileInfo).to.be.null
      expect(store.layerFileInfo).to.be.null
      expect(store.isForwardGrowEnabled).to.be.false
      expect(store.fileUploadSpeciesGroup).to.deep.equal([])
    })
  })

  describe('restoreFromProjectionParams (view mode)', () => {
    it('should open all panels as confirmed and non-editable', () => {
      store.restoreFromProjectionParams(makeParsedParams({ reportTitle: 'My Report' }), true)

      expect(store.panelOpenStates.reportConfig).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.reportConfig).to.deep.equal({ confirmed: true, editable: false })
      expect(store.panelState.minimumDBH).to.deep.equal({ confirmed: true, editable: false })
      expect(store.runModelEnabled).to.be.false
    })

    it('should restore data fields and age range from params', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          reportTitle: 'My Report',
          ageStart: '10',
          ageEnd: '100',
          ageIncrement: '5',
          selectedExecutionOptions: [ExecutionOptionsEnum.ForwardGrowEnabled],
        }),
        true,
      )

      expect(store.reportTitle).to.equal('My Report')
      expect(store.startingAge).to.equal('10')
      expect(store.finishingAge).to.equal('100')
      expect(store.ageIncrement).to.equal('5')
      expect(store.selectedAgeYearRange).to.equal(CONSTANTS.AGE_YEAR_RANGE.AGE)
      expect(store.isForwardGrowEnabled).to.be.true
      expect(store.isBackwardGrowEnabled).to.be.false
    })
  })

  describe('restoreFromProjectionParams (edit mode)', () => {
    it('should progressively open panels based on data completeness', () => {
      store.restoreFromProjectionParams(makeParsedParams({ reportTitle: null }), false)
      expect(store.panelOpenStates.reportConfig).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelState.reportConfig.confirmed).to.be.false
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.CLOSE)

      store.restoreFromProjectionParams(makeParsedParams({ reportTitle: 'My Title', utils: [] }), false)
      expect(store.panelState.reportConfig.confirmed).to.be.true
      expect(store.panelOpenStates.minimumDBH).to.equal(CONSTANTS.PANEL.OPEN)
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.CLOSE)

      store.restoreFromProjectionParams(
        makeParsedParams({ reportTitle: 'My Title', utils: [{ s: 'AC', u: UtilizationClassSetEnum._125 }] }),
        false,
      )
      expect(store.panelState.minimumDBH.confirmed).to.be.true
      expect(store.panelOpenStates.attachments).to.equal(CONSTANTS.PANEL.OPEN)
    })

    it('should set selectedAgeYearRange to YEAR when year params are present', () => {
      store.restoreFromProjectionParams(makeParsedParams({ yearStart: '2020', yearEnd: '2050' }), false)

      expect(store.selectedAgeYearRange).to.equal(CONSTANTS.AGE_YEAR_RANGE.YEAR)
    })
  })

  describe('restoreExecutionOptions (via restoreFromProjectionParams)', () => {
    it('should set all execution option flags from params', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({
          selectedExecutionOptions: [
            ExecutionOptionsEnum.ForwardGrowEnabled,
            ExecutionOptionsEnum.BackGrowEnabled,
            ExecutionOptionsEnum.DoIncludeSpeciesProjection,
            ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
            ExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable,
            ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
            ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
            ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
            ExecutionOptionsEnum.ReportIncludeVolumeMAI,
            ExecutionOptionsEnum.ReportIncludeCulminationValues,
          ],
        }),
        true,
      )

      expect(store.isForwardGrowEnabled).to.be.true
      expect(store.isBackwardGrowEnabled).to.be.true
      expect(store.isBySpeciesEnabled).to.be.true
      expect(store.isPolygonIDEnabled).to.be.true
      expect(store.isProjectionModeEnabled).to.be.true
      expect(store.isCurrentYearEnabled).to.be.true
      expect(store.isReferenceYearEnabled).to.be.true
      expect(store.incSecondaryHeight).to.be.true
      expect(store.isComputedMAIEnabled).to.be.true
      expect(store.isCulminationValuesEnabled).to.be.true
    })
  })

  describe('restoreProjectionTypeAndSpeciesGroups (via restoreFromProjectionParams)', () => {
    it('should set projectionType to CFS_BIOMASS with corresponding utilization', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({ selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass] }),
        true,
      )

      expect(store.projectionType).to.equal(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
      const acGroup = store.fileUploadSpeciesGroup.find((g) => g.group === 'AC')
      expect(acGroup?.minimumDBHLimit).to.equal(DEFAULTS.SPECIES_GROUP_CFO_BIOMASS_UTILIZATION_MAP['AC'])
    })

    it('should set projectionType to VOLUME', () => {
      store.restoreFromProjectionParams(
        makeParsedParams({ selectedExecutionOptions: [ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes] }),
        true,
      )

      expect(store.projectionType).to.equal(CONSTANTS.PROJECTION_TYPE.VOLUME)
    })
  })

  describe('restoreUtilizationLevels (via restoreFromProjectionParams)', () => {
    it('should override only the species listed in params.utils, leaving others unchanged', () => {
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
