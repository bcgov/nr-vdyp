import { CONSTANTS, CSVHEADERS } from '@/constants'
import {
  OutputFormatEnum,
  SelectedExecutionOptionsEnum,
  SelectedDebugOptionsEnum,
  CombineAgeYearRangeEnum,
  MetadataToOutputEnum,
  type Parameters,
} from '@/services/vdyp-api'
import { projectionHcsvPost } from '@/services/apiActions'
import type { CSVRowType } from '@/types/types'
import { Util } from '@/utils/util'

export const createCSVFiles = (modelParameterStore: any) => {
  const featureId = Util.generateFeatureId()

  const derivedByCode =
    modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.VOLUME
      ? CONSTANTS.INVENTORY_CODES.FIP
      : modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA
        ? CONSTANTS.INVENTORY_CODES.VRI
        : ''

  const polygonData: CSVRowType = [
    CSVHEADERS.POLYGON_HEADERS,
    [
      featureId, // FEATURE_ID
      '', // MAP_ID
      '', // POLYGON_NUMBER
      '', // ORG_UNIT
      '', // TSA_NAME
      '', // TFL_NAME
      derivedByCode || '', // INVENTORY_STANDARD_CODE
      '', // TSA_NUMBER
      '', // SHRUB_HEIGHT
      '', // SHRUB_CROWN_CLOSURE
      '', // SHRUB_COVER_PATTERN
      '', // HERB_COVER_TYPE_CODE
      '', // HERB_COVER_PCT
      '', // HERB_COVER_PATTERN_CODE
      '', // BRYOID_COVER_PCT
      modelParameterStore.becZone || '', // BEC_ZONE_CODE
      modelParameterStore.ecoZone || '', // CFS_ECOZONE
      modelParameterStore.percentStockableArea || '', // PRE_DISTURBANCE_STOCKABILITY
      '1', // YIELD_FACTOR - see VDYP7Console Interface Guide.pdf
      '', // NON_PRODUCTIVE_DESCRIPTOR_CD
      '', // BCLCS_LEVEL1_CODE
      '', // BCLCS_LEVEL2_CODE
      '', // BCLCS_LEVEL3_CODE
      '', // BCLCS_LEVEL4_CODE
      '', // BCLCS_LEVEL5_CODE
      '', // PHOTO_ESTIMATION_BASE_YEAR
      '', // REFERENCE_YEAR
      '', // PCT_DEAD
      '', // NON_VEG_COVER_TYPE_1
      '', // NON_VEG_COVER_PCT_1
      '', // NON_VEG_COVER_PATTERN_1
      '', // NON_VEG_COVER_TYPE_2
      '', // NON_VEG_COVER_PCT_2
      '', // NON_VEG_COVER_PATTERN_2
      '', // NON_VEG_COVER_TYPE_3
      '', // NON_VEG_COVER_PCT_3
      '', // NON_VEG_COVER_PATTERN_3
      '', // LAND_COVER_CLASS_CD_1
      '', // LAND_COVER_PCT_1
      '', // LAND_COVER_CLASS_CD_2
      '', // LAND_COVER_PCT_2
      '', // LAND_COVER_CLASS_CD_3
      '', // LAND_COVER_PCT_3
    ],
  ]

  const layerData: CSVRowType = [
    CSVHEADERS.LAYER_HEADERS,
    [
      featureId, // FEATURE_ID
      '', // TREE_COVER_LAYER_ESTIMATED_ID
      '', // MAP_ID
      '', // POLYGON_NUMBER
      '', // LAYER_LEVEL_CODE
      '', // VDYP7_LAYER_CD
      '', // LAYER_STOCKABILITY
      '', // FOREST_COVER_RANK_CODE
      '', // NON_FOREST_DESCRIPTOR_CODE
      '', // EST_SITE_INDEX_SPECIES_CD
      '', // ESTIMATED_SITE_INDEX
      '', // CROWN_CLOSURE
      '', // BASAL_AREA_75
      '', // STEMS_PER_HA_75
      modelParameterStore.speciesList[0].species || '', // SPECIES_CD_1
      modelParameterStore.speciesList[0].percent || '', // SPECIES_PCT_1
      modelParameterStore.speciesList[1].species || '', // SPECIES_CD_2
      modelParameterStore.speciesList[1].percent || '', // SPECIES_PCT_2
      modelParameterStore.speciesList[2].species || '', // SPECIES_CD_3
      modelParameterStore.speciesList[2].percent || '', // SPECIES_PCT_3'
      modelParameterStore.speciesList[3].species || '', // SPECIES_CD_4
      modelParameterStore.speciesList[3].percent || '', // SPECIES_PCT_4
      modelParameterStore.speciesList[4].species || '', // SPECIES_CD_5
      modelParameterStore.speciesList[4].percent || '', // SPECIES_PCT_5
      modelParameterStore.speciesList[5].species || '', // SPECIES_CD_6
      modelParameterStore.speciesList[5].percent || '', // SPECIES_PCT_6
      '', // EST_AGE_SPP1
      '', // EST_HEIGHT_SPP1
      '', // EST_AGE_SPP2
      '', // EST_HEIGHT_SPP2
      '', // ADJ_IND
      '', // LOREY_HEIGHT_75
      '', // BASAL_AREA_125
      '', // WS_VOL_PER_HA_75
      '', // WS_VOL_PER_HA_125
      '', // CU_VOL_PER_HA_125
      '', // D_VOL_PER_HA_125
      '', // DW_VOL_PER_HA_125
    ],
  ]

  const convertToCSV = (data: CSVRowType): string => {
    return data
      .map((row) =>
        row
          .map((value) =>
            value !== null && value !== undefined ? String(value) : '',
          )
          .join(','),
      )
      .join('\n')
  }

  const polygonCSV = convertToCSV(polygonData)
  const layerCSV = convertToCSV(layerData)

  const blobPolygon = new Blob([polygonCSV], {
    type: 'text/csv;charset=utf-8;',
  })
  const blobLayer = new Blob([layerCSV], { type: 'text/csv;charset=utf-8;' })

  return { blobPolygon, blobLayer }
}

export const runModel = async (modelParameterStore: any) => {
  const { blobPolygon, blobLayer } = createCSVFiles(modelParameterStore)

  const formData = new FormData()

  const selectedExecutionOptions: Array<SelectedExecutionOptionsEnum> = [
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

  if (modelParameterStore.projectionType === CONSTANTS.PROJECTION_TYPE.VOLUME) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
    )
  } else if (
    modelParameterStore.projectionType === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
  ) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
    )
  }

  if (modelParameterStore.incSecondaryHeight) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
    )
  }

  if (
    modelParameterStore.includeInReport &&
    modelParameterStore.includeInReport.includes(
      CONSTANTS.INCLUDE_IN_REPORT.SPECIES_COMPOSITION,
    )
  ) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeSpeciesProjection,
    )
  }

  const selectedDebugOptions: Array<SelectedDebugOptionsEnum> = [
    SelectedDebugOptionsEnum.DoIncludeDebugTimestamps,
    SelectedDebugOptionsEnum.DoIncludeDebugEntryExit,
    SelectedDebugOptionsEnum.DoIncludeDebugIndentBlocks,
    SelectedDebugOptionsEnum.DoIncludeDebugRoutineNames,
  ]

  const projectionParameters: Parameters = {
    ageStart: modelParameterStore.startingAge,
    ageEnd: modelParameterStore.finishingAge,
    ageIncrement: modelParameterStore.ageIncrement,
    outputFormat: OutputFormatEnum.CSVYieldTable,
    selectedExecutionOptions,
    selectedDebugOptions,
    combineAgeYearRange: CombineAgeYearRangeEnum.Intersect,
    metadataToOutput: MetadataToOutputEnum.VERSION,
  }

  formData.append(
    'projectionParameters',
    new Blob([JSON.stringify(projectionParameters)], {
      type: 'application/json',
    }),
  )
  formData.append(
    'polygonInputData',
    blobPolygon,
    CONSTANTS.FILE_NAME.INPUT_POLY_CSV,
  )
  formData.append(
    'layersInputData',
    blobLayer,
    CONSTANTS.FILE_NAME.INPUT_LAYER_CSV,
  )

  const result = await projectionHcsvPost(formData, false)
  return result
}
