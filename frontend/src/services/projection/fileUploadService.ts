import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS, DEFAULTS } from '@/constants'
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
  mapProjectionStatus,
} from '@/services/projectionService'
import { PROJECTION_VIEW_MODE } from '@/constants/constants'
import type { UtilizationParameter } from '@/services/vdyp-api/models/utilization-parameter'
import { addExecutionOptionsFromMappings, numEq, strEq } from '@/utils/util'

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
    ExecutionOptionsEnum.ForwardGrowEnabled,
    ExecutionOptionsEnum.DoSummarizeProjectionByLayer,
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
    ExecutionOptionsEnum.BackGrowEnabled,
    ExecutionOptionsEnum.DoSummarizeProjectionByPolygon,
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
    copyTitle: fileUploadStore.copyTitle,
    combineAgeYearRange: CombineAgeYearRangeEnum.Intersect,
    metadataToOutput: MetadataToOutputEnum.VERSION,
    utils: fileUploadStore.fileUploadSpeciesGroup.map(
      (sg: FileUploadSpeciesGroup): UtilizationParameter => ({
        speciesName: sg.group,
        utilizationClass: sg.minimumDBHLimit,
      }),
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
 * Checks whether the Minimum DBH panel's current data differs from the last saved state in the backend.
 * Fetches the projection from the backend, parses the utils from projectionParameters,
 * and compares the resulting utilization values against the current fileUploadSpeciesGroup.
 *
 * @param fileUploadStore The store containing file upload parameters.
 * @returns True if any species group's minimumDBHLimit differs from the saved state.
 */
export const hasMinimumDBHUnsavedChanges = async (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
): Promise<boolean> => {
  const appStore = useAppStore()
  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID) return false

  const projectionModel = await getProjectionById(projectionGUID)
  const savedParams = parseProjectionParams(projectionModel.projectionParameters)

  // Determine the saved projection type to derive correct defaults
  const isSavedCFSBiomass = savedParams.selectedExecutionOptions.includes(
    ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
  )
  const defaultMap = isSavedCFSBiomass
    ? DEFAULTS.SPECIES_GROUP_CFO_BIOMASS_UTILIZATION_MAP
    : DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP

  // Build saved utilization map from backend utils (backend returns {s, u} shape)
  const utilsMap: Record<string, string> = {}
  for (const util of savedParams.utils) {
    const utilObj = util as { s: string; u: string }
    if (utilObj.s && utilObj.u) {
      utilsMap[utilObj.s] = utilObj.u
    }
  }

  const hasUtils = savedParams.utils.length > 0

  return fileUploadStore.fileUploadSpeciesGroup.some((group) => {
    const savedValue = hasUtils
      ? (utilsMap[group.group] ?? defaultMap[group.group])
      : defaultMap[group.group]
    return group.minimumDBHLimit !== savedValue
  })
}

type Store = ReturnType<typeof useFileUploadStore>
type SavedParams = ReturnType<typeof parseProjectionParams>

const reportConfigTextChanged = (store: Store, saved: SavedParams, savedDescription: string | null): boolean => {
  const isAgeRange = store.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE
  const isYearRange = store.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.YEAR
  return (
    !strEq(store.reportTitle, saved.reportTitle) ||
    !strEq(store.reportDescription ?? null, savedDescription) ||
    !numEq(isAgeRange ? store.startingAge : null, saved.ageStart) ||
    !numEq(isAgeRange ? store.finishingAge : null, saved.ageEnd) ||
    !numEq(isYearRange ? store.startYear : null, saved.yearStart) ||
    !numEq(isYearRange ? store.endYear : null, saved.yearEnd) ||
    !numEq(isYearRange ? store.yearIncrement : store.ageIncrement, saved.ageIncrement) ||
    !numEq(store.specificYear, saved.forceYear)
  )
}

const reportConfigOptionsChanged = (store: Store, opts: string[]): boolean => {
  const savedProjectionType = opts.includes(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)
    ? CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
    : CONSTANTS.PROJECTION_TYPE.VOLUME
  if (store.projectionType !== savedProjectionType) return true

  const flagOptionPairs: [boolean, ExecutionOptionsEnum][] = [
    [store.isForwardGrowEnabled, ExecutionOptionsEnum.ForwardGrowEnabled],
    [store.isBackwardGrowEnabled, ExecutionOptionsEnum.BackGrowEnabled],
    [store.isComputedMAIEnabled, ExecutionOptionsEnum.ReportIncludeVolumeMAI],
    [store.isCulminationValuesEnabled, ExecutionOptionsEnum.ReportIncludeCulminationValues],
    [store.isBySpeciesEnabled, ExecutionOptionsEnum.DoIncludeSpeciesProjection],
    [store.isProjectionModeEnabled, ExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable],
    [store.isPolygonIDEnabled, ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable],
    [store.isCurrentYearEnabled, ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables],
    [store.isReferenceYearEnabled, ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables],
    [store.incSecondaryHeight, ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable],
  ]
  return flagOptionPairs.some(([flag, option]) => flag !== opts.includes(option))
}

export const hasReportConfigUnsavedChanges = async (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
): Promise<boolean> => {
  const appStore = useAppStore()
  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID) return false

  const projectionModel = await getProjectionById(projectionGUID)
  const savedParams = parseProjectionParams(projectionModel.projectionParameters)

  return (
    reportConfigTextChanged(fileUploadStore, savedParams, projectionModel.reportDescription ?? null) ||
    reportConfigOptionsChanged(fileUploadStore, savedParams.selectedExecutionOptions)
  )
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
  if (!projectionGUID) {
    fileUploadStore.resetStore()
    fileUploadStore.initializeSpeciesGroups()
    return
  }

  const projectionModel = await getProjectionById(projectionGUID)
  const params = parseProjectionParams(projectionModel.projectionParameters)
  fileUploadStore.restoreFromProjectionParams(params, false)
  fileUploadStore.reportDescription = projectionModel.reportDescription ?? null

  // editPanel resets the cancelled panel and all subsequent panels to unconfirmed/closed.
  // This prevents stale confirmed states (restored from the backend) from blocking
  // confirmPanel calls when the user re-navigates via Next, and prevents the attachments
  // panel from being opened incorrectly by applyEditModePanelStates.
  fileUploadStore.editPanel(panelName)
}

/**
 * Ensures a projection exists in the backend. If no projection GUID is stored,
 * creates a new projection with the current store parameters and stores the GUID.
 * Used by the AttachmentsPanel so files can be uploaded before other panels are confirmed.
 *
 * @param fileUploadStore The store containing file upload parameters.
 * @returns The projection GUID (existing or newly created).
 * @throws Error if projection creation fails.
 */
export const ensureProjectionExists = async (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
): Promise<string> => {
  const appStore = useAppStore()
  const existingGUID = appStore.getCurrentProjectionGUID
  if (existingGUID) return existingGUID

  const baseParams = buildProjectionParameters(fileUploadStore)
  const projectionParameters = { ...baseParams, utils: [] }
  const result = await projServiceCreateProjection(projectionParameters, undefined, fileUploadStore.reportDescription)
  appStore.setCurrentProjectionGUID(result.projectionGUID)
  appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
  return result.projectionGUID
}

/**
 * Saves the projection when a panel's Next button is clicked (File Upload mode).
 *
 * - reportConfig panel (no GUID yet): Creates a new projection with parameters only (no files),
 *   stores the GUID, and switches to EDIT mode.
 * - reportConfig panel (GUID exists, e.g. created via file upload): Updates the existing projection.
 * - Other panels in EDIT mode: Updates the existing projection parameters.
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

  // When confirming reportConfig, omit utils so that minimumDBH completion can be inferred
  // on restore (utils present = minimumDBH confirmed; utils empty = only reportConfig confirmed).
  const baseParams = buildProjectionParameters(fileUploadStore)
  const projectionParameters = panelName === CONSTANTS.FILE_UPLOAD_PANEL.REPORT_CONFIG
    ? { ...baseParams, utils: [] }
    : baseParams

  if (panelName === CONSTANTS.FILE_UPLOAD_PANEL.REPORT_CONFIG) {
    const existingGUID = appStore.getCurrentProjectionGUID
    if (existingGUID) {
      // Projection already exists - update it
      const result = await updateProjectionParams(existingGUID, projectionParameters, fileUploadStore.reportDescription)
      appStore.setCurrentProjectionStatus(mapProjectionStatus(result.projectionStatusCode.code))
    } else {
      // No projection yet - create one
      const result = await projServiceCreateProjection(projectionParameters, undefined, fileUploadStore.reportDescription)
      appStore.setCurrentProjectionGUID(result.projectionGUID)
      appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
    }
  } else if (appStore.viewMode === PROJECTION_VIEW_MODE.EDIT) {
    const projectionGUID = appStore.getCurrentProjectionGUID
    if (!projectionGUID) {
      throw new Error(PROJECTION_ERR.MISSING_GUID)
    }

    // Update projection parameters and sync status
    const result = await updateProjectionParams(projectionGUID, projectionParameters, fileUploadStore.reportDescription)
    appStore.setCurrentProjectionStatus(mapProjectionStatus(result.projectionStatusCode.code))
  }
}
