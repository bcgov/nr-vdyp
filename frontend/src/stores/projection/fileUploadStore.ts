import { defineStore } from 'pinia'
import { ref } from 'vue'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS } from '@/constants'
import type { FileUploadPanelName, PanelState } from '@/types/types'
import type { FileUploadSpeciesGroup, ParsedProjectionParameters } from '@/interfaces/interfaces'
import { ExecutionOptionsEnum } from '@/services/vdyp-api'

export const useFileUploadStore = defineStore('fileUploadStore', () => {
  // panel open
  const panelOpenStates = ref<Record<FileUploadPanelName, PanelState>>({
    reportInfo: CONSTANTS.PANEL.OPEN,
    attachments: CONSTANTS.PANEL.CLOSE,
  })

  // Panel states for confirming and editing
  const panelState = ref<
    Record<FileUploadPanelName, { confirmed: boolean; editable: boolean }>
  >({
    reportInfo: { confirmed: false, editable: true },
    attachments: { confirmed: false, editable: false },
  })

  const runModelEnabled = ref(false)

  // Method to handle confirm action for each panel
  const confirmPanel = (panelName: FileUploadPanelName) => {
    panelState.value[panelName].confirmed = true
    panelState.value[panelName].editable = false

    // Enable the next panel's confirm and clear buttons
    const panelOrder: FileUploadPanelName[] = [
      CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO,
      CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS,
    ]
    const currentIndex = panelOrder.indexOf(panelName)
    if (currentIndex !== -1 && currentIndex < panelOrder.length - 1) {
      // The next panel opens automatically, switching to the editable.
      const nextPanel = panelOrder[currentIndex + 1]
      panelOpenStates.value[nextPanel] = CONSTANTS.PANEL.OPEN
      panelState.value[nextPanel].editable = true
    }

    // Check if all panels are confirmed to enable the 'Run Model' button
    runModelEnabled.value = panelOrder.every(
      (panel) => panelState.value[panel].confirmed,
    )
  }

  // Method to handle edit action for each panel
  const editPanel = (panelName: FileUploadPanelName) => {
    panelState.value[panelName].confirmed = false
    panelState.value[panelName].editable = true

    // Disable all subsequent panels
    const panelOrder: FileUploadPanelName[] = [
      CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO,
      CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS,
    ]
    const currentIndex = panelOrder.indexOf(panelName)
    if (currentIndex !== -1) {
      for (let i = currentIndex + 1; i < panelOrder.length; i++) {
        // All of the next panels are automatically closed, uneditable, and unconfirmed
        const nextPanel = panelOrder[i]
        panelState.value[nextPanel].confirmed = false
        panelState.value[nextPanel].editable = false
        panelOpenStates.value[nextPanel] = CONSTANTS.PANEL.CLOSE
      }
    }

    // Disable 'Run Model' button
    runModelEnabled.value = false
  }

  // report info
  const selectedAgeYearRange = ref<string | null>(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
  const startingAge = ref<string | null>(null)
  const finishingAge = ref<string | null>(null)
  const ageIncrement = ref<string | null>(null)
  const startYear = ref<string | null>(null)
  const endYear = ref<string | null>(null)
  const yearIncrement = ref<string | null>(null)
  const isForwardGrowEnabled = ref<boolean>(false)
  const isBackwardGrowEnabled = ref<boolean>(false)

  const isComputedMAIEnabled = ref<boolean>(false)
  const isCulminationValuesEnabled = ref<boolean>(false)
  const isBySpeciesEnabled = ref<boolean>(false)
  const isByLayerEnabled = ref<boolean>(false)
  const isProjectionModeEnabled = ref<boolean>(false)
  const isPolygonIDEnabled = ref<boolean>(false)
  const isCurrentYearEnabled = ref<boolean>(false)
  const isReferenceYearEnabled = ref<boolean>(false)
  const incSecondaryHeight = ref<boolean>(false)
  const specificYear = ref<string | null>(null)

  const projectionType = ref<string | null>(null)
  const reportTitle = ref<string | null>(null)

  // species groups for utilization levels
  const fileUploadSpeciesGroup = ref<FileUploadSpeciesGroup[]>([])

  // initialize species groups with all 16 species
  const initializeSpeciesGroups = () => {
    fileUploadSpeciesGroup.value = BIZCONSTANTS.SPECIES_GROUPS.map((group) => ({
      group,
      minimumDBHLimit: DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group],
    }))
  }

  // update species groups based on projection type
  const updateSpeciesGroupsForProjectionType = (type: string | null) => {
    if (type === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS) {
      // CFO Biomass: use specific values
      fileUploadSpeciesGroup.value.forEach((group) => {
        group.minimumDBHLimit =
          DEFAULTS.SPECIES_GROUP_CFO_BIOMASS_UTILIZATION_MAP[group.group]
      })
    } else {
      // Volume: use default values
      fileUploadSpeciesGroup.value.forEach((group) => {
        group.minimumDBHLimit =
          DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group.group]
      })
    }
  }

  // attachments
  const polygonFile = ref<File | null>(null)
  const layerFile = ref<File | null>(null)

  // File names for display (used when viewing/editing existing projections)
  const polygonFileName = ref<string | null>(null)
  const layerFileName = ref<string | null>(null)

  // Reset store to initial state
  const resetStore = () => {
    // Reset panel states
    panelOpenStates.value = {
      reportInfo: CONSTANTS.PANEL.OPEN,
      attachments: CONSTANTS.PANEL.CLOSE,
    }
    panelState.value = {
      reportInfo: { confirmed: false, editable: true },
      attachments: { confirmed: false, editable: false },
    }
    runModelEnabled.value = false

    // Reset report info
    selectedAgeYearRange.value = DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
    startingAge.value = null
    finishingAge.value = null
    ageIncrement.value = null
    startYear.value = null
    endYear.value = null
    yearIncrement.value = null
    isForwardGrowEnabled.value = false
    isBackwardGrowEnabled.value = false
    isComputedMAIEnabled.value = false
    isCulminationValuesEnabled.value = false
    isBySpeciesEnabled.value = false
    isByLayerEnabled.value = false
    isProjectionModeEnabled.value = false
    isPolygonIDEnabled.value = false
    isCurrentYearEnabled.value = false
    isReferenceYearEnabled.value = false
    incSecondaryHeight.value = false
    specificYear.value = null
    projectionType.value = null
    reportTitle.value = null

    // Reset species groups
    fileUploadSpeciesGroup.value = []

    // Reset attachments
    polygonFile.value = null
    layerFile.value = null
    polygonFileName.value = null
    layerFileName.value = null
  }

  /**
   * Restore store state from parsed projection parameters
   * @param params Parsed projection parameters from the backend
   * @param isViewMode If true, sets all panels to confirmed and non-editable
   */
  const restoreFromProjectionParams = (
    params: ParsedProjectionParameters,
    isViewMode: boolean = false,
  ) => {
    // Restore report info from parameters
    reportTitle.value = params.reportTitle

    // Restore age/year range settings
    startingAge.value = params.ageStart
    finishingAge.value = params.ageEnd
    ageIncrement.value = params.ageIncrement
    startYear.value = params.yearStart
    endYear.value = params.yearEnd

    // Determine selectedAgeYearRange based on what values exist
    const hasAge = params.ageStart !== null || params.ageEnd !== null
    const hasYear = params.yearStart !== null || params.yearEnd !== null
    if (hasAge && hasYear) {
      selectedAgeYearRange.value = params.combineAgeYearRange || 'intersect'
    } else if (hasYear) {
      selectedAgeYearRange.value = CONSTANTS.AGE_YEAR_RANGE.YEAR
    } else {
      selectedAgeYearRange.value = CONSTANTS.AGE_YEAR_RANGE.AGE
    }

    // Restore execution options
    const options = params.selectedExecutionOptions

    isForwardGrowEnabled.value = options.includes(ExecutionOptionsEnum.ForwardGrowEnabled)
    isBackwardGrowEnabled.value = options.includes(ExecutionOptionsEnum.BackGrowEnabled)
    isByLayerEnabled.value = options.includes(ExecutionOptionsEnum.DoSummarizeProjectionByLayer)
    isBySpeciesEnabled.value = options.includes(ExecutionOptionsEnum.DoIncludeSpeciesProjection)
    isPolygonIDEnabled.value = options.includes(ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable)
    isProjectionModeEnabled.value = options.includes(ExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable)
    isCurrentYearEnabled.value = options.includes(ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables)
    isReferenceYearEnabled.value = options.includes(ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables)
    incSecondaryHeight.value = options.includes(ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable)
    isComputedMAIEnabled.value = options.includes(ExecutionOptionsEnum.ReportIncludeVolumeMAI)
    isCulminationValuesEnabled.value = options.includes(ExecutionOptionsEnum.ReportIncludeCulminationValues)

    // Determine projection type
    if (options.includes(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)) {
      projectionType.value = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
    } else if (options.includes(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)) {
      projectionType.value = CONSTANTS.PROJECTION_TYPE.VOLUME
    }

    // Initialize species groups based on projection type
    initializeSpeciesGroups()
    if (projectionType.value) {
      updateSpeciesGroupsForProjectionType(projectionType.value)
    }

    // Set panel states based on view/edit mode
    if (isViewMode) {
      // In view mode, all panels are confirmed and not editable
      panelOpenStates.value = {
        reportInfo: CONSTANTS.PANEL.OPEN,
        attachments: CONSTANTS.PANEL.OPEN,
      }
      panelState.value = {
        reportInfo: { confirmed: true, editable: false },
        attachments: { confirmed: true, editable: false },
      }
      runModelEnabled.value = false
    } else {
      // In edit mode, only first panel is open and editable (same as new projection)
      panelOpenStates.value = {
        reportInfo: CONSTANTS.PANEL.OPEN,
        attachments: CONSTANTS.PANEL.CLOSE,
      }
      panelState.value = {
        reportInfo: { confirmed: false, editable: true },
        attachments: { confirmed: false, editable: false },
      }
      runModelEnabled.value = false
    }
  }

  /**
   * Set file references for display purposes
   * Note: In view mode, we can only display file names, not actual File objects
   * since files need to be downloaded from the server
   * @param polygon The polygon file name from server (or null)
   * @param layer The layer file name from server (or null)
   */
  const setFileReferences = (
    polygon: string | null,
    layer: string | null,
  ) => {
    // Set file names with defaults if not provided
    polygonFileName.value = polygon || 'polygon.csv'
    layerFileName.value = layer || 'layer.csv'
  }

  return {
    // panel open
    panelOpenStates,
    // Panel state
    panelState,
    runModelEnabled,
    confirmPanel,
    editPanel,
    // report info
    selectedAgeYearRange,
    startingAge,
    finishingAge,
    ageIncrement,
    startYear,
    endYear,
    yearIncrement,
    isForwardGrowEnabled,
    isBackwardGrowEnabled,
    isComputedMAIEnabled,
    isCulminationValuesEnabled,
    isBySpeciesEnabled,
    isByLayerEnabled,
    isProjectionModeEnabled,
    isPolygonIDEnabled,
    isCurrentYearEnabled,
    isReferenceYearEnabled,
    incSecondaryHeight,
    specificYear,
    projectionType,
    reportTitle,
    // species groups
    fileUploadSpeciesGroup,
    initializeSpeciesGroups,
    updateSpeciesGroupsForProjectionType,
    // attachments
    polygonFile,
    layerFile,
    polygonFileName,
    layerFileName,
    // restore functions
    resetStore,
    restoreFromProjectionParams,
    setFileReferences,
  }
})
