import { BIZCONSTANTS, CONSTANTS, CSVHEADERS } from '@/constants'
import {
  OutputFormatEnum,
  SelectedExecutionOptionsEnum,
  SelectedDebugOptionsEnum,
  MetadataToOutputEnum,
  type Parameters,
} from '@/services/vdyp-api'
import { projectionHcsvPost } from '@/services/apiActions'
import type { CSVRowType } from '@/types/types'
import type { SpeciesGroup } from '@/interfaces/interfaces'

/**
 * Generates a unique 9-digit or 10-digit number using the current timestamp and random values.
 * @returns {number} A 9-digit or 10-digit number
 */
const generateFeatureId = (): number => {
  const timestamp = Date.now()

  // Use the last 8 digits of the timestamp (0 to 99999999)
  const timestampPart = timestamp % 100000000

  const randomPart = Math.floor(Math.random() * 99) + 1 // 1 to 99
  const numberStr = `${randomPart}${timestampPart}`
  return Number(numberStr)
}

/**
 * Generates a random number within a specified range.
 * @param min Minimum number of digits (inclusive).
 * @param max Maximum number of digits (inclusive).
 * @returns A random number as a string.
 */
const generateRandomNumber = (minDigits: number, maxDigits: number): string => {
  if (minDigits > maxDigits) {
    throw new Error('minDigits must be less than or equal to maxDigits')
  }

  const min = Math.pow(10, minDigits - 1)
  const max = Math.pow(10, maxDigits) - 1

  return (Math.floor(Math.random() * (max - min + 1)) + min).toString()
}

/**
 * Generates a random POLYGON_NUMBER (4 to 9 digits).
 * Polygon Number starts with the Map_id and then up to 8 numbers
 * @returns A random POLYGON_NUMBER.
 */
const generatePolygonNumber = (mapId: string): string => {
  const randomPart = generateRandomNumber(1, 8) // 1 to 8-digit
  return `${mapId}${randomPart}`
}

/**
 * Generates a random TREE_COVER_LAYER_ESTIMATED_ID (4 to 10 digits).
 * @returns A random TREE_COVER_LAYER_ESTIMATED_ID.
 */
const generateTreeCoverLayerEstimatedId = (): string => {
  return generateRandomNumber(4, 10)
}

const determineBclcsLevel5 = (percentStockableArea: number): string => {
  if (percentStockableArea >= 61) return BIZCONSTANTS.BCLCS_LEVEL5_DE
  if (percentStockableArea >= 26) return BIZCONSTANTS.BCLCS_LEVEL5_OP
  return BIZCONSTANTS.BCLCS_LEVEL5_SP
}

const determineBclcsLevel4 = (speciesGroups: SpeciesGroup[]): string => {
  let coniferousTotal = 0
  let broadleafTotal = 0

  for (const speciesGroup of speciesGroups) {
    const speciesCode = speciesGroup.siteSpecies
    const percent = parseFloat(speciesGroup.percent)

    if (BIZCONSTANTS.CONIFEROUS_SPECIES.has(speciesCode)) {
      coniferousTotal += percent
    } else if (BIZCONSTANTS.BROADLEAF_SPECIES.has(speciesCode)) {
      broadleafTotal += percent
    }
  }

  if (coniferousTotal >= 75) {
    return BIZCONSTANTS.BCLCS_LEVEL4_TC
  } else if (broadleafTotal >= 75) {
    return BIZCONSTANTS.BCLCS_LEVEL4_TB
  } else {
    return BIZCONSTANTS.BCLCS_LEVEL4_TM
  }
}

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

export const createCSVFiles = (modelParameterStore: any) => {
  const featureId = generateFeatureId()
  const mapId = BIZCONSTANTS.MAP_ID
  const polygonNumber = generatePolygonNumber(mapId)
  const treeCoverLayerEstimatedId = generateTreeCoverLayerEstimatedId()
  const derivedByCode =
    modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.VOLUME
      ? BIZCONSTANTS.INVENTORY_CODES.FIP
      : modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA
        ? BIZCONSTANTS.INVENTORY_CODES.VRI
        : ''

  const bclcsLevel1 =
    (modelParameterStore.percentStockableArea ??
      BIZCONSTANTS.BCLCS_LEVEL1_THRESHOLD) < BIZCONSTANTS.BCLCS_LEVEL1_THRESHOLD
      ? BIZCONSTANTS.BCLCS_LEVEL1_NON_VEG
      : BIZCONSTANTS.BCLCS_LEVEL1_VEG

  const bclcsLevel2 =
    (modelParameterStore.percentStockableArea ??
      BIZCONSTANTS.BCLCS_LEVEL2_THRESHOLD) < BIZCONSTANTS.BCLCS_LEVEL2_THRESHOLD
      ? BIZCONSTANTS.BCLCS_LEVEL2_NON_TREED
      : BIZCONSTANTS.BCLCS_LEVEL2_TREED

  const bclcsLevel3 =
    (modelParameterStore.becZone ?? BIZCONSTANTS.BCLCS_LEVEL3_DEFAULT) ===
    BIZCONSTANTS.BCLCS_LEVEL3_BECZONE_AT
      ? BIZCONSTANTS.BCLCS_LEVEL3_ALPINE
      : BIZCONSTANTS.BCLCS_LEVEL3_DEFAULT

  const bclcsLevel4 = determineBclcsLevel4(modelParameterStore.speciesGroups)

  const bclcsLevel5 = determineBclcsLevel5(
    modelParameterStore.percentStockableArea,
  )

  const species1 = modelParameterStore.speciesList[0].species
  const spczPct1 = modelParameterStore.speciesList[0].percent

  const species2 = modelParameterStore.speciesList[1].species
  const spczPct2 =
    modelParameterStore.speciesList[1].percent === 0 ||
    modelParameterStore.speciesList[1].percent == null
      ? ''
      : modelParameterStore.speciesList[1].percent

  const species3 = modelParameterStore.speciesList[2].species
  const spczPct3 =
    modelParameterStore.speciesList[2].percent === 0 ||
    modelParameterStore.speciesList[2].percent == null
      ? ''
      : modelParameterStore.speciesList[2].percent

  const species4 = modelParameterStore.speciesList[3].species
  const spczPct4 =
    modelParameterStore.speciesList[3].percent === 0 ||
    modelParameterStore.speciesList[3].percent == null
      ? ''
      : modelParameterStore.speciesList[3].percent

  const species5 = modelParameterStore.speciesList[4].species
  const spczPct5 =
    modelParameterStore.speciesList[4].percent === 0 ||
    modelParameterStore.speciesList[4].percent == null
      ? ''
      : modelParameterStore.speciesList[4].percent

  const species6 = modelParameterStore.speciesList[5].species
  const spczPct6 =
    modelParameterStore.speciesList[5].percent === 0 ||
    modelParameterStore.speciesList[5].percent == null
      ? ''
      : modelParameterStore.speciesList[5].percent

  const polygonData: CSVRowType = [
    CSVHEADERS.POLYGON_HEADERS,
    [
      featureId, // FEATURE_ID
      mapId, // MAP_ID
      polygonNumber, // POLYGON_NUMBER
      '', // ORG_UNIT
      BIZCONSTANTS.UNKNOWN_CD, // TSA_NAME
      BIZCONSTANTS.UNKNOWN_CD, // TFL_NAME
      derivedByCode || '', // INVENTORY_STANDARD_CODE
      BIZCONSTANTS.UNKNOWN_CD, // TSA_NUMBER
      '', // SHRUB_HEIGHT
      '', // SHRUB_CROWN_CLOSURE
      '', // SHRUB_COVER_PATTERN
      '', // HERB_COVER_TYPE_CODE
      '', // HERB_COVER_PCT
      '', // HERB_COVER_PATTERN_CODE
      '', // BRYOID_COVER_PCT
      modelParameterStore.becZone, // BEC_ZONE_CODE
      modelParameterStore.ecoZone || '', // CFS_ECOZONE
      modelParameterStore.percentStockableArea || '', // PRE_DISTURBANCE_STOCKABILITY
      BIZCONSTANTS.YIELD_FACTOR_CD, // YIELD_FACTOR
      '', // NON_PRODUCTIVE_DESCRIPTOR_CD
      bclcsLevel1 || '', // BCLCS_LEVEL1_CODE
      bclcsLevel2 || '', // BCLCS_LEVEL2_CODE
      bclcsLevel3 || '', // BCLCS_LEVEL3_CODE
      bclcsLevel4 || '', // BCLCS_LEVEL4_CODE
      bclcsLevel5 || '', // BCLCS_LEVEL5_CODE
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
      treeCoverLayerEstimatedId, // TREE_COVER_LAYER_ESTIMATED_ID
      mapId, // MAP_ID
      polygonNumber, // POLYGON_NUMBER
      BIZCONSTANTS.LAYER_LEVEL_CD, // LAYER_LEVEL_CODE (note: equal to layerId)
      BIZCONSTANTS.VDYP7_LAYER_CD, // VDYP7_LAYER_CD
      '', // LAYER_STOCKABILITY
      BIZCONSTANTS.FOREST_COVER_RANK_CD, // FOREST_COVER_RANK_CODE
      '', // NON_FOREST_DESCRIPTOR_CODE
      modelParameterStore.highestPercentSpecies, // EST_SITE_INDEX_SPECIES_CD
      modelParameterStore.bha50SiteIndex || '', // ESTIMATED_SITE_INDEX
      '', // CROWN_CLOSURE
      '', // BASAL_AREA_75
      '', // STEMS_PER_HA_75
      species1, // SPECIES_CD_1
      spczPct1, // SPECIES_PCT_1
      species2, // SPECIES_CD_2
      spczPct2, // SPECIES_PCT_2
      species3, // SPECIES_CD_3
      spczPct3, // SPECIES_PCT_3'
      species4, // SPECIES_CD_4
      spczPct4, // SPECIES_PCT_4
      species5, // SPECIES_CD_5
      spczPct5, // SPECIES_PCT_5
      species6, // SPECIES_CD_6
      spczPct6, // SPECIES_PCT_6
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
    SelectedExecutionOptionsEnum.DoIncludeAgeRowsInYieldTable,
    SelectedExecutionOptionsEnum.DoIncludeColumnHeadersInYieldTable,
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
    selectedExecutionOptions: selectedExecutionOptions,
    selectedDebugOptions: selectedDebugOptions,
    metadataToOutput: MetadataToOutputEnum.NONE,
  }

  formData.append(
    'projectionParameters',
    new Blob([JSON.stringify(projectionParameters)], {
      type: 'application/json',
    }),
  )
  formData.append(
    'HCSV-Polygon',
    blobPolygon,
    CONSTANTS.FILE_NAME.INPUT_POLY_CSV,
  )
  formData.append('HCSV-Layers', blobLayer, CONSTANTS.FILE_NAME.INPUT_LAYER_CSV)

  const result = await projectionHcsvPost(formData, false)
  return result
}
