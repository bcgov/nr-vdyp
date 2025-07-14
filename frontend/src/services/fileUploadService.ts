import { useFileUploadStore } from '@/stores/fileUploadStore'
import { CONSTANTS } from '@/constants'
import {
  OutputFormatEnum,
  SelectedExecutionOptionsEnum,
  SelectedDebugOptionsEnum,
  MetadataToOutputEnum,
  CombineAgeYearRangeEnum,
  ParameterNamesEnum,
} from '@/services/vdyp-api'
import { projectionHcsvPost } from '@/services/apiActions'

export const getSelectedExecutionOptions = (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
) => {
  const selectedExecutionOptions = [
    SelectedExecutionOptionsEnum.ForwardGrowEnabled,
    SelectedExecutionOptionsEnum.DoIncludeFileHeader,
    SelectedExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable,
    SelectedExecutionOptionsEnum.DoIncludeAgeRowsInYieldTable,
    SelectedExecutionOptionsEnum.DoIncludeYearRowsInYieldTable,
    SelectedExecutionOptionsEnum.DoSummarizeProjectionByLayer,
    SelectedExecutionOptionsEnum.DoIncludeColumnHeadersInYieldTable,
    SelectedExecutionOptionsEnum.DoAllowBasalAreaAndTreesPerHectareValueSubstitution,
    SelectedExecutionOptionsEnum.DoEnableProgressLogging,
    SelectedExecutionOptionsEnum.DoEnableErrorLogging,
    SelectedExecutionOptionsEnum.DoEnableDebugLogging,
  ]

  if (fileUploadStore.projectionType === CONSTANTS.PROJECTION_TYPE.VOLUME) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
    )
  } else if (
    fileUploadStore.projectionType === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
  ) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
    )
  }

  if (
    fileUploadStore.includeInReport &&
    fileUploadStore.includeInReport.includes(
      CONSTANTS.INCLUDE_IN_REPORT.BY_SPECIES,
    )
  ) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeSpeciesProjection,
    )
  }

  return selectedExecutionOptions
}

export const getSelectedDebugOptions = () => {
  const selectedDebugOptions: Array<SelectedDebugOptionsEnum> = [
    SelectedDebugOptionsEnum.DoIncludeDebugTimestamps,
    SelectedDebugOptionsEnum.DoIncludeDebugEntryExit,
    SelectedDebugOptionsEnum.DoIncludeDebugIndentBlocks,
    SelectedDebugOptionsEnum.DoIncludeDebugRoutineNames,
  ]

  return selectedDebugOptions
}

export const getFormData = (
  fileUploadStore: ReturnType<typeof useFileUploadStore>,
) => {
  const projectionParameters = {
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
    selectedExecutionOptions: getSelectedExecutionOptions(fileUploadStore),
    selectedDebugOptions: getSelectedDebugOptions(),
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
