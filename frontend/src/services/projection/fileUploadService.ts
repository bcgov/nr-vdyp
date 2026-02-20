import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'
import { PROJECTION_ERR } from '@/constants/message'
import type { FileUploadSpeciesGroup } from '@/interfaces/interfaces'
import type { FileUploadPanelName } from '@/types/types'
import {
  OutputFormatEnum,
  ExecutionOptionsEnum,
  DebugOptionsEnum,
  MetadataToOutputEnum,
  CombineAgeYearRangeEnum,
  type Parameters,
  type ProjectionModel
} from '@/services/vdyp-api'
import {
  createProjection as projServiceCreateProjection,
  runProjection as projServiceRunProjection,
  updateProjectionParams,
  getProjectionById,
  parseProjectionParams,
} from '@/services/projectionService'
import { PROJECTION_VIEW_MODE } from '@/constants/constants'
import type { UtilizationParameter } from '@/services/vdyp-api/models/utilization-parameter'
import { addExecutionOptionsFromMappings } from '@/utils/util'

/**
 * Builds an array of selected and excluded execution options based on the file upload store.
 * @param fileUploadStore The store containing model parameters.
 * @returns An object containing selected and excluded execution option enums.
 */
export const buildExecutionOptions = (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
): {
  selectedExecutionOptions: ExecutionOptionsEnum[]
  excludedExecutionOptions: ExecutionOptionsEnum[]
} => {
  const selectedExecutionOptions = [
    ExecutionOptionsEnum.DoIncludeFileHeader,
    ExecutionOptionsEnum.DoIncludeAgeRowsInYieldTable,
    ExecutionOptionsEnum.DoIncludeYearRowsInYieldTable,
    ExecutionOptionsEnum.DoIncludeColumnHeadersInYieldTable,
    ExecutionOptionsEnum.DoAllowBasalAreaAndTreesPerHectareValueSubstitution,
    ExecutionOptionsEnum.DoEnableProgressLogging,
    ExecutionOptionsEnum.DoEnableErrorLogging,
    ExecutionOptionsEnum.DoEnableDebugLogging,
  ]

  const excludedExecutionOptions: ExecutionOptionsEnum[] = [
    ExecutionOptionsEnum.DoSaveIntermediateFiles,
    ExecutionOptionsEnum.AllowAggressiveValueEstimation,
    ExecutionOptionsEnum.DoIncludeProjectionFiles,
    ExecutionOptionsEnum.DoDelayExecutionFolderDeletion,
    ExecutionOptionsEnum.DoIncludeProjectedMOFBiomass,
    ExecutionOptionsEnum.ReportIncludeWholeStemVolume,
    ExecutionOptionsEnum.ReportIncludeCloseUtilizationVolume,
    ExecutionOptionsEnum.ReportIncludeNetDecayVolume,
    ExecutionOptionsEnum.ReportIncludeNDWasteVolume,
    ExecutionOptionsEnum.ReportIncludeNDWasteBrkgVolume,
    ExecutionOptionsEnum.ReportIncludeVolumeMAI,
    ExecutionOptionsEnum.ReportIncludeSpeciesComp,
    ExecutionOptionsEnum.ReportIncludeCulminationValues,
  ]

  const optionMappings = [
    {
      flag: fileUploadStore.projectionType === CONSTANTS.PROJECTION_TYPE.VOLUME,
      option: ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
    },
    {
      flag:
        fileUploadStore.projectionType ===
        CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS,
      option: ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
    },
    {
      flag: fileUploadStore.isForwardGrowEnabled,
      option: ExecutionOptionsEnum.ForwardGrowEnabled,
    },
    {
      flag: fileUploadStore.isBackwardGrowEnabled,
      option: ExecutionOptionsEnum.BackGrowEnabled,
    },
    {
      flag: fileUploadStore.isByLayerEnabled,
      option: ExecutionOptionsEnum.DoSummarizeProjectionByLayer,
    },
    {
      flag: !fileUploadStore.isByLayerEnabled,
      option: ExecutionOptionsEnum.DoSummarizeProjectionByPolygon,
    },
    {
      flag: fileUploadStore.isBySpeciesEnabled,
      option: ExecutionOptionsEnum.DoIncludeSpeciesProjection,
    },
    {
      flag: fileUploadStore.isProjectionModeEnabled,
      option: ExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable,
    },
    {
      flag: fileUploadStore.isPolygonIDEnabled,
      option: ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
    },
    {
      flag: fileUploadStore.isCurrentYearEnabled,
      option: ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
    },
    {
      flag: fileUploadStore.isReferenceYearEnabled,
      option: ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
    },
    {
      flag: fileUploadStore.incSecondaryHeight,
      option: ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
    },
  ]

  addExecutionOptionsFromMappings(
    selectedExecutionOptions,
    excludedExecutionOptions,
    optionMappings,
  )

  return { selectedExecutionOptions, excludedExecutionOptions }
}

/**
 * Builds an array of selected and excluded debug options.
 * @returns An object containing selected and excluded debug option enums.
 */
export const buildDebugOptions = (): {
  selectedDebugOptions: DebugOptionsEnum[]
  excludedDebugOptions: DebugOptionsEnum[]
} => {
  const selectedDebugOptions: DebugOptionsEnum[] = [
    DebugOptionsEnum.DoIncludeDebugTimestamps,
    DebugOptionsEnum.DoIncludeDebugEntryExit,
    DebugOptionsEnum.DoIncludeDebugIndentBlocks,
    DebugOptionsEnum.DoIncludeDebugRoutineNames,
  ]
  const excludedDebugOptions: DebugOptionsEnum[] = []

  return { selectedDebugOptions, excludedDebugOptions }
}

/**
 * Builds projection parameters from the file upload store.
 * @param fileUploadStore The store containing model parameters.
 * @returns The projection parameters object.
 */
export const buildProjectionParameters = (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
): Parameters => {
  const { selectedExecutionOptions, excludedExecutionOptions } =
    buildExecutionOptions(fileUploadStore)
  const { selectedDebugOptions, excludedDebugOptions } = buildDebugOptions()

  const isAgeRange =
    fileUploadStore.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE
  const isYearRange =
    fileUploadStore.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.YEAR

  let ageIncrement: number | null = null
  if (isYearRange && fileUploadStore.yearIncrement) {
    ageIncrement = Number.parseInt(fileUploadStore.yearIncrement)
  } else if (fileUploadStore.ageIncrement) {
    ageIncrement = Number.parseInt(fileUploadStore.ageIncrement)
  }

  const projectionParameters: Parameters = {
    ageStart:
      isAgeRange && fileUploadStore.startingAge
        ? Number.parseInt(fileUploadStore.startingAge)
        : null,
    ageEnd:
      isAgeRange && fileUploadStore.finishingAge
        ? Number.parseInt(fileUploadStore.finishingAge)
        : null,
    ageIncrement,
    yearStart:
      isYearRange && fileUploadStore.startYear
        ? Number.parseInt(fileUploadStore.startYear)
        : null,
    yearEnd:
      isYearRange && fileUploadStore.endYear
        ? Number.parseInt(fileUploadStore.endYear)
        : null,
    forceYear: fileUploadStore.specificYear
      ? Number.parseInt(fileUploadStore.specificYear)
      : null,
    outputFormat: OutputFormatEnum.CSVYieldTable,
    selectedExecutionOptions: selectedExecutionOptions,
    excludedExecutionOptions: excludedExecutionOptions,
    selectedDebugOptions: selectedDebugOptions,
    excludedDebugOptions: excludedDebugOptions,
    reportTitle: fileUploadStore.reportTitle,
    combineAgeYearRange: CombineAgeYearRangeEnum.Intersect,
    metadataToOutput: MetadataToOutputEnum.VERSION,
    utils: fileUploadStore.fileUploadSpeciesGroup.map(
      (sg: FileUploadSpeciesGroup) =>
        ({
          speciesName: sg.group,
          utilizationClass: sg.minimumDBHLimit,
        }) as UtilizationParameter,
    ),
  }

  return projectionParameters
}

/**
 * Runs the current projection by sending it to batch processing.
 * The projection must already be created and its GUID stored in the app store.
 *
 * @returns The updated ProjectionModel with RUNNING status.
 * @throws Error if the projection GUID is not available in the app store.
 */
export const runProjectionFileUpload = async (
): Promise<ProjectionModel> => {
  const appStore = useAppStore()
  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID) {
    throw new Error(PROJECTION_ERR.MISSING_GUID)
  }
  return await projServiceRunProjection(projectionGUID)
}

/**
 * Reverts the file upload store to the last saved projection state from the backend.
 * Called when the user clicks Cancel while editing a panel in File Upload mode.
 * The cancelled panel is forced back to open and editable so the user can continue editing.
 *
 * @param panelName The panel whose Cancel button was clicked.
 * @throws Error if the revert operation fails.
 */
export const revertPanelToSaved = async (panelName: FileUploadPanelName): Promise<void> => {
  const appStore = useAppStore()
  const fileUploadStore = useFileUploadStore()

  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID) return

  const projectionModel = await getProjectionById(projectionGUID)
  const params = parseProjectionParams(projectionModel.projectionParameters)
  fileUploadStore.restoreFromProjectionParams(params, false)
  fileUploadStore.reportDescription = projectionModel.reportDescription ?? null
  fileUploadStore.updateRunModelEnabled()

  // Keep the cancelled panel open and editable so the user can continue editing
  fileUploadStore.panelOpenStates[panelName] = CONSTANTS.PANEL.OPEN
  fileUploadStore.panelState[panelName] = { confirmed: false, editable: true }
}

/**
 * Saves the projection when a panel's Next button is clicked (File Upload mode).
 *
 * - CREATE mode + first panel (reportInfo): Creates a new projection with parameters only (no files),
 *   stores the GUID, and switches to EDIT mode.
 * - EDIT mode + reportInfo: Updates the existing projection parameters.
 * - EDIT mode + attachments: Updates projection parameters and handles file uploads
 *   (deletes old files, uploads new files if provided).
 *
 * @param fileUploadStore The store containing file upload parameters and files.
 * @param panelName The name of the panel being confirmed.
 * @throws Error if the save operation fails.
 */
export const saveProjectionOnPanelConfirm = async (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
  panelName: string,
): Promise<void> => {
  const appStore = useAppStore()

  // When confirming reportInfo, omit utils so that minimumDBH completion can be inferred
  // on restore (utils present = minimumDBH confirmed; utils empty = only reportInfo confirmed).
  const baseParams = buildProjectionParameters(fileUploadStore)
  const projectionParameters = panelName === CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO
    ? { ...baseParams, utils: [] }
    : baseParams

  if (
    appStore.viewMode === PROJECTION_VIEW_MODE.CREATE &&
    panelName === CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO
  ) {
    // Create mode + first panel: create projection with params only (no files yet)
    const result = await projServiceCreateProjection(projectionParameters, undefined, fileUploadStore.reportDescription)
    appStore.setCurrentProjectionGUID(result.projectionGUID)
    appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
  } else if (appStore.viewMode === PROJECTION_VIEW_MODE.EDIT) {
    const projectionGUID = appStore.getCurrentProjectionGUID
    if (!projectionGUID) {
      throw new Error(PROJECTION_ERR.MISSING_GUID)
    }

    // Update projection parameters
    await updateProjectionParams(projectionGUID, projectionParameters, fileUploadStore.reportDescription)

    // Note: Files are now uploaded immediately when selected in the AttachmentsPanel,
    // so no file upload handling is needed here. This function only updates parameters.
  }
}
