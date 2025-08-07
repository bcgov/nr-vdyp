import { useFileUploadStore } from '@/stores/fileUploadStore'
import { CONSTANTS } from '@/constants'
import {
  OutputFormatEnum,
  ExecutionOptionsEnum,
  DebugOptionsEnum,
  MetadataToOutputEnum,
  CombineAgeYearRangeEnum,
  ParameterNamesEnum,
  type Parameters,
} from '@/services/vdyp-api'
import { projectionHcsvPost } from '@/services/apiActions'

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
    ExecutionOptionsEnum.DoSummarizeProjectionByPolygon,
    ExecutionOptionsEnum.AllowAggressiveValueEstimation,
    ExecutionOptionsEnum.DoIncludeProjectionFiles,
    ExecutionOptionsEnum.DoDelayExecutionFolderDeletion,
    ExecutionOptionsEnum.DoIncludeProjectedMOFBiomass,
  ]

  if (fileUploadStore.projectionType === CONSTANTS.PROJECTION_TYPE.VOLUME) {
    selectedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
    )
  } else {
    excludedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
    )
  }

  if (
    fileUploadStore.projectionType === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
  ) {
    selectedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
    )
  } else {
    excludedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
    )
  }

  if (fileUploadStore.isForwardGrowEnabled) {
    selectedExecutionOptions.push(ExecutionOptionsEnum.ForwardGrowEnabled)
  } else {
    excludedExecutionOptions.push(ExecutionOptionsEnum.ForwardGrowEnabled)
  }

  if (fileUploadStore.isBackwardGrowEnabled) {
    selectedExecutionOptions.push(ExecutionOptionsEnum.BackGrowEnabled)
  } else {
    excludedExecutionOptions.push(ExecutionOptionsEnum.BackGrowEnabled)
  }

  if (fileUploadStore.isByLayerEnabled) {
    selectedExecutionOptions.push(
      ExecutionOptionsEnum.DoSummarizeProjectionByLayer,
    )
  } else {
    excludedExecutionOptions.push(
      ExecutionOptionsEnum.DoSummarizeProjectionByLayer,
    )
  }

  if (fileUploadStore.isProjectionModeEnabled) {
    selectedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable,
    )
  } else {
    excludedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable,
    )
  }

  if (fileUploadStore.isPolygonIDEnabled) {
    selectedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
    )
  } else {
    excludedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
    )
  }

  if (fileUploadStore.isCurrentYearEnabled) {
    selectedExecutionOptions.push(
      ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
    )
  } else {
    excludedExecutionOptions.push(
      ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
    )
  }

  if (fileUploadStore.isReferenceYearEnabled) {
    selectedExecutionOptions.push(
      ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
    )
  } else {
    excludedExecutionOptions.push(
      ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
    )
  }

  if (fileUploadStore.incSecondaryHeight) {
    selectedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
    )
  } else {
    excludedExecutionOptions.push(
      ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
    )
  }

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

export const getFormData = (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
) => {
  const { selectedExecutionOptions, excludedExecutionOptions } =
    buildExecutionOptions(fileUploadStore)
  const { selectedDebugOptions, excludedDebugOptions } = buildDebugOptions()

  const projectionParameters: Parameters = {
    ageStart:
      fileUploadStore.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE
        ? fileUploadStore.startingAge
        : null,
    ageEnd:
      fileUploadStore.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE
        ? fileUploadStore.finishingAge
        : null,
    ageIncrement:
      fileUploadStore.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.YEAR
        ? fileUploadStore.yearIncrement
        : fileUploadStore.ageIncrement,
    yearStart:
      fileUploadStore.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.YEAR
        ? fileUploadStore.startYear
        : null,
    yearEnd:
      fileUploadStore.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.YEAR
        ? fileUploadStore.endYear
        : null,
    outputFormat: OutputFormatEnum.CSVYieldTable, // TODO - All of new parameter will only work for new outputFormat TextReport (see VDYP-695 comment)
    selectedExecutionOptions: selectedExecutionOptions,
    excludedExecutionOptions: excludedExecutionOptions,
    selectedDebugOptions: selectedDebugOptions,
    excludedDebugOptions: excludedDebugOptions,
    combineAgeYearRange: CombineAgeYearRangeEnum.Intersect,
    metadataToOutput: MetadataToOutputEnum.VERSION,
  }

  const formData = new FormData()

  formData.append(
    ParameterNamesEnum.PROJECTION_PARAMETERS,
    new Blob([JSON.stringify(projectionParameters)], {
      type: 'application/json',
    }),
  )
  formData.append(
    ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA,
    fileUploadStore.polygonFile as Blob,
  )
  formData.append(
    ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA,
    fileUploadStore.layerFile as Blob,
  )

  return formData
}

export const runModelFileUpload = async (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
  projectionHcsvPostFunc = projectionHcsvPost,
) => {
  const formData = getFormData(fileUploadStore)
  const response = await projectionHcsvPostFunc(formData, false)
  return response
}
