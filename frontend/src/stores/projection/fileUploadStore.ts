import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS } from '@/constants'
import type { FileUploadPanelName, PanelState } from '@/types/types'
import type { FileUploadSpeciesGroup, ParsedProjectionParameters, UploadedFileInfo } from '@/interfaces/interfaces'
import { ExecutionOptionsEnum, UtilizationClassSetEnum } from '@/services/vdyp-api'

export const useFileUploadStore = defineStore('fileUploadStore', () => {
  // panel open
  const panelOpenStates = ref<Record<FileUploadPanelName, PanelState>>({
    reportInfo: CONSTANTS.PANEL.OPEN,
    minimumDBH: CONSTANTS.PANEL.CLOSE,
    attachments: CONSTANTS.PANEL.CLOSE,
  })

  // Panel states for confirming and editing
  const panelState = ref<
    Record<FileUploadPanelName, { confirmed: boolean; editable: boolean }>
  >({
    reportInfo: { confirmed: false, editable: true },
    minimumDBH: { confirmed: false, editable: false },
    attachments: { confirmed: false, editable: false },
  })

  const runModelEnabled = ref(false)

  // Sequential panel order (attachments is excluded - always accessible)
  const sequentialPanelOrder: FileUploadPanelName[] = [
    CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO,
    CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH,
  ]

  // Update runModelEnabled based on sequential panels + files uploaded
  const updateRunModelEnabled = () => {
    const sequentialPanelsConfirmed = sequentialPanelOrder.every(
      (panel) => panelState.value[panel].confirmed,
    )
    const filesUploaded = polygonFileInfo.value !== null && layerFileInfo.value !== null
    runModelEnabled.value = sequentialPanelsConfirmed && filesUploaded
  }

  // Method to handle confirm action for each panel
  const confirmPanel = (panelName: FileUploadPanelName) => {
    panelState.value[panelName].confirmed = true
    panelState.value[panelName].editable = false
    // Close the confirmed panel
    panelOpenStates.value[panelName] = CONSTANTS.PANEL.CLOSE

    // Enable the next sequential panel
    const currentIndex = sequentialPanelOrder.indexOf(panelName)
    if (currentIndex !== -1 && currentIndex < sequentialPanelOrder.length - 1) {
      // The next panel opens automatically, switching to the editable.
      const nextPanel = sequentialPanelOrder[currentIndex + 1]
      panelOpenStates.value[nextPanel] = CONSTANTS.PANEL.OPEN
      panelState.value[nextPanel].editable = true
    }

    // When the last sequential panel (minimumDBH) is confirmed, open the attachments panel
    if (panelName === CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH) {
      panelOpenStates.value[CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS] = CONSTANTS.PANEL.OPEN
    }

    // Check if run model should be enabled
    updateRunModelEnabled()
  }

  // Method to handle edit action for each panel
  const editPanel = (panelName: FileUploadPanelName) => {
    panelState.value[panelName].confirmed = false
    panelState.value[panelName].editable = true
    // Open the panel being edited
    panelOpenStates.value[panelName] = CONSTANTS.PANEL.OPEN

    // Disable all subsequent sequential panels and close them
    const currentIndex = sequentialPanelOrder.indexOf(panelName)
    if (currentIndex !== -1) {
      for (let i = currentIndex + 1; i < sequentialPanelOrder.length; i++) {
        // All of the next panels are automatically closed, uneditable, and unconfirmed
        const nextPanel = sequentialPanelOrder[i]
        panelState.value[nextPanel].confirmed = false
        panelState.value[nextPanel].editable = false
        panelOpenStates.value[nextPanel] = CONSTANTS.PANEL.CLOSE
      }
    }

    // Also close the attachments panel (not in sequentialPanelOrder but must close when editing prerequisites)
    panelOpenStates.value[CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS] = CONSTANTS.PANEL.CLOSE

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
  const reportDescription = ref<string | null>(null)

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

  // uploaded file info (from server)
  const polygonFileInfo = ref<UploadedFileInfo | null>(null)
  const layerFileInfo = ref<UploadedFileInfo | null>(null)

  // file upload state
  const isUploadingPolygon = ref<boolean>(false)
  const isUploadingLayer = ref<boolean>(false)
  const isDeletingFile = ref<boolean>(false)

  // Watch file info changes to update runModelEnabled
  watch([polygonFileInfo, layerFileInfo], () => {
    updateRunModelEnabled()
  })

  // Set uploaded file info
  const setPolygonFileInfo = (info: UploadedFileInfo | null) => {
    polygonFileInfo.value = info
  }

  const setLayerFileInfo = (info: UploadedFileInfo | null) => {
    layerFileInfo.value = info
  }

  // Reset store to initial state
  const resetStore = () => {
    // Reset panel states
    panelOpenStates.value = {
      reportInfo: CONSTANTS.PANEL.OPEN,
      minimumDBH: CONSTANTS.PANEL.CLOSE,
      attachments: CONSTANTS.PANEL.CLOSE,
    }
    panelState.value = {
      reportInfo: { confirmed: false, editable: true },
      minimumDBH: { confirmed: false, editable: false },
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
    reportTitle.value = DEFAULTS.DEFAULT_VALUES.REPORT_TITLE
    reportDescription.value = null

    // Reset species groups
    fileUploadSpeciesGroup.value = []

    // Reset attachments
    polygonFile.value = null
    layerFile.value = null

    // Reset uploaded file info
    polygonFileInfo.value = null
    layerFileInfo.value = null
    isUploadingPolygon.value = false
    isUploadingLayer.value = false
    isDeletingFile.value = false
  }

  const restoreAgeYearRange = (params: ParsedProjectionParameters) => {
    const hasAge = params.ageStart !== null || params.ageEnd !== null
    const hasYear = params.yearStart !== null || params.yearEnd !== null
    if (hasAge) {
      selectedAgeYearRange.value = CONSTANTS.AGE_YEAR_RANGE.AGE
    } else if (hasYear) {
      selectedAgeYearRange.value = CONSTANTS.AGE_YEAR_RANGE.YEAR
    }
  }

  const restoreExecutionOptions = (options: string[]) => {
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
  }

  const restoreProjectionTypeAndSpeciesGroups = (options: string[]) => {
    if (options.includes(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)) {
      projectionType.value = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
    } else if (options.includes(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)) {
      projectionType.value = CONSTANTS.PROJECTION_TYPE.VOLUME
    }
    initializeSpeciesGroups()
    updateSpeciesGroupsForProjectionType(projectionType.value)
  }

  const restoreUtilizationLevels = (params: ParsedProjectionParameters) => {
    if (!Array.isArray(params.utils) || params.utils.length === 0) return
    const utilsMap: Record<string, UtilizationClassSetEnum> = {}
    for (const util of params.utils) {
      const utilObj = util as { s: string; u: string }
      if (utilObj.s && utilObj.u) {
        utilsMap[utilObj.s] = utilObj.u as UtilizationClassSetEnum
      }
    }
    fileUploadSpeciesGroup.value = fileUploadSpeciesGroup.value.map((group) => ({
      ...group,
      minimumDBHLimit: utilsMap[group.group] || group.minimumDBHLimit,
    }))
  }

  const applyEditModePanelStates = (params: ParsedProjectionParameters) => {
    // Infer panel confirmed states from saved data without extra fields:
    // - reportInfo: confirmed if reportTitle is set (required field for confirmation)
    // - minimumDBH: confirmed if utils are present (reportInfo saves utils=[]; minimumDBH saves full utils)
    const isReportInfoConfirmed = params.reportTitle !== null
    const isMinimumDBHConfirmed = Array.isArray(params.utils) && params.utils.length > 0
    // Only the first uncompleted panel should be open; confirmed panels are closed.
    // Attachments opens when both prerequisites (reportInfo + minimumDBH) are confirmed.
    panelOpenStates.value = {
      reportInfo: isReportInfoConfirmed ? CONSTANTS.PANEL.CLOSE : CONSTANTS.PANEL.OPEN,
      minimumDBH: isReportInfoConfirmed && !isMinimumDBHConfirmed ? CONSTANTS.PANEL.OPEN : CONSTANTS.PANEL.CLOSE,
      attachments: isReportInfoConfirmed && isMinimumDBHConfirmed ? CONSTANTS.PANEL.OPEN : CONSTANTS.PANEL.CLOSE,
    }
    panelState.value = {
      reportInfo: { confirmed: isReportInfoConfirmed, editable: !isReportInfoConfirmed },
      minimumDBH: {
        confirmed: isMinimumDBHConfirmed,
        editable: isReportInfoConfirmed && !isMinimumDBHConfirmed,
      },
      attachments: { confirmed: false, editable: false },
    }
    runModelEnabled.value = false
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
    reportTitle.value = params.reportTitle
    startingAge.value = params.ageStart
    finishingAge.value = params.ageEnd
    ageIncrement.value = params.ageIncrement
    startYear.value = params.yearStart
    endYear.value = params.yearEnd
    yearIncrement.value = params.ageIncrement
    specificYear.value = params.forceYear

    restoreAgeYearRange(params)
    restoreExecutionOptions(params.selectedExecutionOptions)
    restoreProjectionTypeAndSpeciesGroups(params.selectedExecutionOptions)
    restoreUtilizationLevels(params)

    if (isViewMode) {
      panelOpenStates.value = {
        reportInfo: CONSTANTS.PANEL.OPEN,
        minimumDBH: CONSTANTS.PANEL.OPEN,
        attachments: CONSTANTS.PANEL.OPEN,
      }
      panelState.value = {
        reportInfo: { confirmed: true, editable: false },
        minimumDBH: { confirmed: true, editable: false },
        attachments: { confirmed: true, editable: false },
      }
      runModelEnabled.value = false
    } else {
      applyEditModePanelStates(params)
    }
  }

  return {
    // panel open
    panelOpenStates,
    // Panel state
    panelState,
    runModelEnabled,
    confirmPanel,
    editPanel,
    updateRunModelEnabled,
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
    reportDescription,
    // species groups
    fileUploadSpeciesGroup,
    initializeSpeciesGroups,
    updateSpeciesGroupsForProjectionType,
    // attachments
    polygonFile,
    layerFile,
    // uploaded file info
    polygonFileInfo,
    layerFileInfo,
    isUploadingPolygon,
    isUploadingLayer,
    isDeletingFile,
    setPolygonFileInfo,
    setLayerFileInfo,
    // restore functions
    resetStore,
    restoreFromProjectionParams,
  }
})
