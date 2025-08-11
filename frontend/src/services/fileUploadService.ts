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
    ExecutionOptionsEnum.DoIncludeSpeciesProjection,
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
    //
    {
      flag: fileUploadStore.isByLayerEnabled,
      option: ExecutionOptionsEnum.DoSummarizeProjectionByLayer,
    },
    {
      flag: !fileUploadStore.isByLayerEnabled,
      option: ExecutionOptionsEnum.DoSummarizeProjectionByPolygon,
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
      option:
        ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
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
    outputFormat: OutputFormatEnum.CSVYieldTable,
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
