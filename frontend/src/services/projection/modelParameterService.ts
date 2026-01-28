import { BIZCONSTANTS, CONSTANTS, CSVHEADERS, DEFAULTS } from '@/constants'
import { PROJECTION_ERR } from '@/constants/message'
import {
  OutputFormatEnum,
  ExecutionOptionsEnum,
  DebugOptionsEnum,
  MetadataToOutputEnum,
  type Parameters,
  type ProjectionModel
} from '@/services/vdyp-api'
import {
  createProjection as projServiceCreateProjection,
  runProjection as projServiceRunProjection,
  updateProjectionWithFiles,
} from '@/services/projectionService'
import { useAppStore } from '@/stores/projection/appStore'
import { PROJECTION_VIEW_MODE } from '@/constants/constants'
import type { CSVRowType } from '@/types/types'
import type { SpeciesGroup, SpeciesList, ParsedCsvFileContent } from '@/interfaces/interfaces'
import type { UtilizationParameter } from '@/services/vdyp-api/models/utilization-parameter'
import { isBlank, addExecutionOptionsFromMappings } from '@/utils/util'

/**
 * Generates a unique 9-digit or 10-digit feature ID using the current timestamp and random values.
 * @returns {number} A unique feature ID.
 */
export const generateFeatureId = (): number => {
  const timestamp = Date.now()
  const timestampPart = timestamp % 100000000
  const randomPart = Math.floor(Math.random() * 99) + 1 // 1 to 99
  return Number(`${randomPart}${timestampPart}`)
}

/**
 * Generates a random number as a string within the specified digit range.
 * @param minDigits Minimum number of digits (inclusive).
 * @param maxDigits Maximum number of digits (inclusive).
 * @returns A random number as a string.
 */
export const generateRandomNumber = (
  minDigits: number,
  maxDigits: number,
): string => {
  if (minDigits > maxDigits) {
    throw new Error('minDigits must be less than or equal to maxDigits')
  }
  const min = Math.pow(10, minDigits - 1)
  const max = Math.pow(10, maxDigits) - 1
  return (Math.floor(Math.random() * (max - min + 1)) + min).toString()
}

/**
 * Generates a fixed 8-digit random polygon number.
 * @returns An 8-digit random polygon number as a string.
 */
export const generatePolygonNumber = (): string => {
  const randomPart = generateRandomNumber(8, 8)
  return `${randomPart}`
}

/**
 * Generates a random TREE_COVER_LAYER_ESTIMATED_ID with 4 to 10 digits.
 * @returns A random TREE_COVER_LAYER_ESTIMATED_ID.
 */
export const generateTreeCoverLayerEstimatedId = (): string => {
  return generateRandomNumber(4, 10)
}

/**
 * Converts an array of CSV rows into a CSV string.
 * @param data The CSV row type data.
 * @returns A CSV-formatted string.
 */
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

/**
 * Computes BCLCS Level 1 based on the percent stockable area.
 * @param percentStockableArea The percent stockable area.
 * @returns The corresponding BCLCS Level 1 code.
 */
export const computeBclcsLevel1 = (
  percentStockableArea: number | undefined,
): string => {
  const threshold = BIZCONSTANTS.BCLCS_LEVEL1_THRESHOLD
  return (percentStockableArea ?? threshold) < threshold
    ? BIZCONSTANTS.BCLCS_LEVEL1_NON_VEG
    : BIZCONSTANTS.BCLCS_LEVEL1_VEG
}

/**
 * Computes BCLCS Level 2 based on the percent stockable area.
 * @param percentStockableArea The percent stockable area.
 * @returns The corresponding BCLCS Level 2 code.
 */
export const computeBclcsLevel2 = (
  percentStockableArea: number | undefined,
): string => {
  const threshold = BIZCONSTANTS.BCLCS_LEVEL2_THRESHOLD
  return (percentStockableArea ?? threshold) < threshold
    ? BIZCONSTANTS.BCLCS_LEVEL2_NON_TREED
    : BIZCONSTANTS.BCLCS_LEVEL2_TREED
}

/**
 * Computes BCLCS Level 3 based on the BEC zone.
 * @param becZone The BEC zone.
 * @returns The corresponding BCLCS Level 3 code.
 */
export const computeBclcsLevel3 = (becZone: string | undefined): string => {
  return (becZone ?? BIZCONSTANTS.BCLCS_LEVEL3_DEFAULT) ===
    BIZCONSTANTS.BCLCS_LEVEL3_BECZONE_AT
    ? BIZCONSTANTS.BCLCS_LEVEL3_ALPINE
    : BIZCONSTANTS.BCLCS_LEVEL3_DEFAULT
}

/**
 * Determines the BCLCS Level 4 code based on species groups.
 * @param speciesGroups Array of species groups.
 * @returns The corresponding BCLCS Level 4 code.
 */
export const determineBclcsLevel4 = (speciesGroups: SpeciesGroup[]): string => {
  let coniferousTotal = 0
  let broadleafTotal = 0

  for (const speciesGroup of speciesGroups) {
    const speciesCode = speciesGroup.siteSpecies
    const percent = Number.parseFloat(speciesGroup.percent)

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

/**
 * Determines the BCLCS Level 5 code based on the percent stockable area.
 * @param percentStockableArea The percent stockable area.
 * @returns The corresponding BCLCS Level 5 code.
 */
export const determineBclcsLevel5 = (percentStockableArea: number): string => {
  if (percentStockableArea >= 61) return BIZCONSTANTS.BCLCS_LEVEL5_DE
  if (percentStockableArea >= 26) return BIZCONSTANTS.BCLCS_LEVEL5_OP
  return BIZCONSTANTS.BCLCS_LEVEL5_SP
}

/**
 * Extracts species data (species code and percentage) from the species list.
 * @param speciesList The list of species objects.
 * @returns An array of objects with species code and percentage.
 */
export const getSpeciesData = (
  speciesList: any[],
): { species: string; percent: string }[] => {
  return speciesList.map((item) => ({
    species: item.species,
    percent:
      item.percent === 0 || item.percent === null || item.species === null
        ? ''
        : String(item.percent),
  }))
}

/**
 * Flattens species data into an array suitable for CSV row insertion.
 * @param speciesData Array of species data objects.
 * @param count The number of species to include.
 * @returns A flattened array of species codes and percentages.
 */
export const flattenSpeciesData = (
  speciesData: { species: string; percent: string }[],
  count: number,
): any[] => {
  return speciesData.slice(0, count).reduce((acc: any[], cur) => {
    acc.push(cur.species, cur.percent)
    return acc
  }, [])
}

/**
 * Creates CSV data for the polygon file.
 * @param modelParameterStore The store containing model parameters.
 * @param featureId The generated feature ID.
 * @param mapId The map ID.
 * @param polygonNumber The generated polygon number.
 * @returns The CSV row type for the polygon data.
 */
const createPolygonData = (
  modelParameterStore: any,
  featureId: number,
  mapId: string,
  polygonNumber: string,
): CSVRowType => {
  let derivedByCode = ''
  if (modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.VOLUME) {
    derivedByCode = BIZCONSTANTS.INVENTORY_CODES.FIP
  } else if (
    modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA
  ) {
    derivedByCode = BIZCONSTANTS.INVENTORY_CODES.VRI
  }

  const bclcsLevel1 = computeBclcsLevel1(
    modelParameterStore.percentStockableArea,
  )
  const bclcsLevel2 = computeBclcsLevel2(
    modelParameterStore.percentStockableArea,
  )
  const bclcsLevel3 = computeBclcsLevel3(modelParameterStore.becZone)
  const bclcsLevel4 = determineBclcsLevel4(modelParameterStore.speciesGroups)
  const bclcsLevel5 = determineBclcsLevel5(
    modelParameterStore.percentStockableArea,
  )
  const referenceYear = modelParameterStore.referenceYear

  const row = [
    featureId, // FEATURE_ID
    mapId, // MAP_ID
    polygonNumber, // POLYGON_NUMBER
    BIZCONSTANTS.UNKNOWN_CD, // ORG_UNIT
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
    modelParameterStore.ecoZone ?? '', // CFS_ECOZONE
    modelParameterStore.percentStockableArea ?? '', // PRE_DISTURBANCE_STOCKABILITY
    BIZCONSTANTS.YIELD_FACTOR_CD, // YIELD_FACTOR
    '', // NON_PRODUCTIVE_DESCRIPTOR_CD
    bclcsLevel1 || '', // BCLCS_LEVEL1_CODE
    bclcsLevel2 || '', // BCLCS_LEVEL2_CODE
    bclcsLevel3 || '', // BCLCS_LEVEL3_CODE
    bclcsLevel4 || '', // BCLCS_LEVEL4_CODE
    bclcsLevel5 || '', // BCLCS_LEVEL5_CODE
    '', // PHOTO_ESTIMATION_BASE_YEAR
    referenceYear, // REFERENCE_YEAR
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
  ]
  return [CSVHEADERS.POLYGON_HEADERS, row]
}

/**
 * Creates CSV data for the layer file.
 * @param modelParameterStore The store containing model parameters.
 * @param featureId The generated feature ID.
 * @param mapId The map ID.
 * @param polygonNumber The generated polygon number.
 * @param treeCoverLayerEstimatedId The generated tree cover layer estimated ID.
 * @returns The CSV row type for the layer data.
 */
const createLayerData = (
  modelParameterStore: any,
  featureId: number,
  mapId: string,
  polygonNumber: string,
  treeCoverLayerEstimatedId: string,
): CSVRowType => {
  const speciesData = getSpeciesData(modelParameterStore.speciesList)
  const speciesRow = flattenSpeciesData(speciesData, 6)
  const crownClosure =
    modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.VOLUME &&
    modelParameterStore.siteSpeciesValues ===
      CONSTANTS.SITE_SPECIES_VALUES.COMPUTED &&
    (isBlank(modelParameterStore.crownClosure) ||
      modelParameterStore.crownClosure === 0)
      ? DEFAULTS.CROWN_CLOSURE_DEFAULT_4PROJ
      : modelParameterStore.crownClosure

  const basalArea =
    modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA &&
    modelParameterStore.siteSpeciesValues ===
      CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      ? modelParameterStore.basalArea
      : DEFAULTS.BASAL_AREA_DEFAULT_4PROJ

  const treesPerHectare =
    modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA &&
    modelParameterStore.siteSpeciesValues ===
      CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      ? modelParameterStore.treesPerHectare
      : DEFAULTS.TPH_DEFAULT_4PROJ

  const row = [
    featureId, // FEATURE_ID
    treeCoverLayerEstimatedId, // TREE_COVER_LAYER_ESTIMATED_ID
    mapId, // MAP_ID
    polygonNumber, // POLYGON_NUMBER
    BIZCONSTANTS.LAYER_LEVEL_CD, // LAYER_LEVEL_CODE
    BIZCONSTANTS.VDYP7_LAYER_CD, // VDYP7_LAYER_CD
    modelParameterStore.percentStockableArea ?? '', // LAYER_STOCKABILITY
    BIZCONSTANTS.FOREST_COVER_RANK_CD, // FOREST_COVER_RANK_CODE
    '', // NON_FOREST_DESCRIPTOR_CODE
    modelParameterStore.highestPercentSpecies, // EST_SITE_INDEX_SPECIES_CD
    modelParameterStore.bha50SiteIndex ?? '', // ESTIMATED_SITE_INDEX
    crownClosure, // CROWN_CLOSURE
    basalArea, // BASAL_AREA_75
    treesPerHectare, // STEMS_PER_HA_75
    ...speciesRow, // Species codes and percentages (6 pairs)
    modelParameterStore.spzAge, // EST_AGE_SPP1
    modelParameterStore.spzHeight, // EST_HEIGHT_SPP1
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
  ]
  return [CSVHEADERS.LAYER_HEADERS, row]
}

/**
 * Creates CSV files (as Blobs) for the model parameters.
 * @param modelParameterStore The store containing model parameters.
 * @returns An object with two properties: blobPolygon and blobLayer, representing the CSV Blobs.
 */
export const createCSVFiles = (modelParameterStore: any) => {
  const featureId = generateFeatureId()
  const mapId = BIZCONSTANTS.MAP_ID
  const polygonNumber = generatePolygonNumber()
  const treeCoverLayerEstimatedId = generateTreeCoverLayerEstimatedId()

  const polygonData = createPolygonData(
    modelParameterStore,
    featureId,
    mapId,
    polygonNumber,
  )
  const layerData = createLayerData(
    modelParameterStore,
    featureId,
    mapId,
    polygonNumber,
    treeCoverLayerEstimatedId,
  )

  const polygonCSV = convertToCSV(polygonData)
  const layerCSV = convertToCSV(layerData)

  const blobPolygon = new Blob([polygonCSV], {
    type: 'text/csv;charset=utf-8;',
  })
  const blobLayer = new Blob([layerCSV], { type: 'text/csv;charset=utf-8;' })

  return { blobPolygon, blobLayer }
}

/**
 * Parses a CSV string into headers and values, returning a lookup function.
 * Uses strict empty-string check (not falsy check) to preserve "0" values.
 */
const createCsvLookup = (
  csvContent: string,
): ((header: string) => string | null) => {
  const lines = csvContent.split('\n').filter((l) => l.trim())
  if (lines.length < 2) return () => null

  const headers = lines[0].split(',').map((h) => h.trim())
  const values = lines[1].split(',').map((v) => v.trim())

  return (header: string): string | null => {
    const index = headers.indexOf(header)
    if (index < 0 || index >= values.length) return null
    return values[index] !== '' ? values[index] : null
  }
}

/**
 * Parses polygon and layer CSV file content into a structured object.
 * This is the reverse of createCSVFiles - it reads back the CSV files
 * generated by createPolygonData and createLayerData.
 * @param polygonCsvContent The content of the polygon CSV file.
 * @param layerCsvContent The content of the layer CSV file.
 * @returns A ParsedCsvFileContent object with extracted values.
 */
export const parseCsvFileContent = (
  polygonCsvContent: string,
  layerCsvContent: string,
): ParsedCsvFileContent => {
  const getPolygonValue = createCsvLookup(polygonCsvContent)
  const getLayerValue = createCsvLookup(layerCsvContent)

  // Restore derivedBy from INVENTORY_STANDARD_CODE
  const inventoryCode = getPolygonValue('INVENTORY_STANDARD_CODE')
  let derivedBy: string | null = null
  if (inventoryCode === BIZCONSTANTS.INVENTORY_CODES.FIP) {
    derivedBy = CONSTANTS.DERIVED_BY.VOLUME
  } else if (inventoryCode === BIZCONSTANTS.INVENTORY_CODES.VRI) {
    derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
  }

  // Restore reference year
  const refYear = getPolygonValue('REFERENCE_YEAR')

  // Restore species list from layer file
  const speciesList: SpeciesList[] = []
  for (let i = 1; i <= 6; i++) {
    const species = getLayerValue(`SPECIES_CD_${i}`)
    const percent = getLayerValue(`SPECIES_PCT_${i}`)
    speciesList.push({
      species: species,
      percent: percent,
    })
  }

  // Restore site species from layer
  const highestPercentSpecies = getLayerValue('EST_SITE_INDEX_SPECIES_CD')

  return {
    derivedBy,
    becZone: getPolygonValue('BEC_ZONE_CODE'),
    ecoZone: getPolygonValue('CFS_ECOZONE'),
    percentStockableArea: getPolygonValue('PRE_DISTURBANCE_STOCKABILITY'),
    referenceYear: refYear ? Number.parseInt(refYear) : null,
    speciesList,
    highestPercentSpecies,
    siteSpeciesValues: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED, // FIXME VDYP-922
    spzAge: getLayerValue('EST_AGE_SPP1'),
    spzHeight: getLayerValue('EST_HEIGHT_SPP1'),
    bha50SiteIndex: getLayerValue('ESTIMATED_SITE_INDEX'),
    crownClosure: getLayerValue('CROWN_CLOSURE'),
    basalArea: getLayerValue('BASAL_AREA_75'),
    treesPerHectare: getLayerValue('STEMS_PER_HA_75'),
  }
}

/**
 * Builds an array of selected and excluded execution options based on the model parameter store.
 * @param modelParameterStore The store containing model parameters.
 * @returns An object containing selected and excluded execution option enums.
 */
const buildExecutionOptions = (
  modelParameterStore: any,
): {
  selectedExecutionOptions: ExecutionOptionsEnum[]
  excludedExecutionOptions: ExecutionOptionsEnum[]
} => {
  const selectedExecutionOptions: ExecutionOptionsEnum[] = [
    ExecutionOptionsEnum.DoIncludeAgeRowsInYieldTable,
    ExecutionOptionsEnum.DoIncludeColumnHeadersInYieldTable,
    ExecutionOptionsEnum.DoEnableProgressLogging,
    ExecutionOptionsEnum.DoEnableErrorLogging,
    ExecutionOptionsEnum.DoEnableDebugLogging,
    ExecutionOptionsEnum.DoEnableProjectionReport,
    ExecutionOptionsEnum.AllowAggressiveValueEstimation,
    ExecutionOptionsEnum.DoIncludeFileHeader,
    ExecutionOptionsEnum.DoSummarizeProjectionByLayer,
    ExecutionOptionsEnum.ReportIncludeWholeStemVolume,
    ExecutionOptionsEnum.ReportIncludeCloseUtilizationVolume,
    ExecutionOptionsEnum.ReportIncludeNetDecayVolume,
    ExecutionOptionsEnum.ReportIncludeNDWasteVolume,
    ExecutionOptionsEnum.ReportIncludeNDWasteBrkgVolume,
  ]

  const excludedExecutionOptions: ExecutionOptionsEnum[] = [
    ExecutionOptionsEnum.DoSaveIntermediateFiles,
    ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
    ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
    ExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable,
    ExecutionOptionsEnum.DoIncludeYearRowsInYieldTable,
    ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
    ExecutionOptionsEnum.DoSummarizeProjectionByPolygon,
    ExecutionOptionsEnum.DoIncludeProjectedMOFBiomass,
    ExecutionOptionsEnum.DoAllowBasalAreaAndTreesPerHectareValueSubstitution,
    ExecutionOptionsEnum.DoIncludeProjectionFiles,
    ExecutionOptionsEnum.DoDelayExecutionFolderDeletion,
  ]

  const optionMappings = [
    {
      flag:
        modelParameterStore.projectionType === CONSTANTS.PROJECTION_TYPE.VOLUME,
      option: ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
    },
    {
      flag:
        modelParameterStore.projectionType ===
        CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS,
      option: ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
    },
    {
      flag: modelParameterStore.isForwardGrowEnabled,
      option: ExecutionOptionsEnum.ForwardGrowEnabled,
    },
    {
      flag: modelParameterStore.isBackwardGrowEnabled,
      option: ExecutionOptionsEnum.BackGrowEnabled,
    },
    {
      flag: modelParameterStore.isComputedMAIEnabled,
      option: ExecutionOptionsEnum.ReportIncludeVolumeMAI,
    },
    {
      flag: modelParameterStore.isCulminationValuesEnabled,
      option: ExecutionOptionsEnum.ReportIncludeCulminationValues,
    },
    {
      flag: modelParameterStore.isBySpeciesEnabled,
      option: ExecutionOptionsEnum.DoIncludeSpeciesProjection,
    },
    {
      flag: modelParameterStore.incSecondaryHeight,
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
const buildDebugOptions = (): {
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
 * Builds projection parameters from the model parameter store.
 * @param modelParameterStore The store containing model parameters.
 * @returns The projection parameters object.
 */
export const buildProjectionParameters = (
  modelParameterStore: any,
): Parameters => {
  const { selectedExecutionOptions, excludedExecutionOptions } =
    buildExecutionOptions(modelParameterStore)
  const { selectedDebugOptions, excludedDebugOptions } = buildDebugOptions()

  return {
    ageStart: modelParameterStore.startingAge
      ? Number.parseInt(modelParameterStore.startingAge)
      : null,
    ageEnd: modelParameterStore.finishingAge
      ? Number.parseInt(modelParameterStore.finishingAge)
      : null,
    ageIncrement: modelParameterStore.ageIncrement
      ? Number.parseInt(modelParameterStore.ageIncrement)
      : null,
    yearStart: null,
    yearEnd: null,
    reportTitle: modelParameterStore.reportTitle || DEFAULTS.DEFAULT_VALUES.REPORT_TITLE,
    outputFormat: OutputFormatEnum.CSVYieldTable,
    selectedExecutionOptions,
    excludedExecutionOptions,
    selectedDebugOptions,
    excludedDebugOptions,
    metadataToOutput: MetadataToOutputEnum.NONE,
    utils: modelParameterStore.speciesGroups.map(
      (sg: SpeciesGroup) =>
        ({
          speciesName: sg.group,
          utilizationClass: sg.minimumDBHLimit,
        }) as UtilizationParameter,
    ),
  }
}

/**
 * Creates a projection with the generated CSV files and parameters (without running it).
 * @param modelParameterStore The store containing model parameters.
 * @param createProjectionFunc Optional projection function (defaults to projServiceCreateProjection).
 * @returns The created ProjectionModel.
 */
export const createProjection = async (
  modelParameterStore: any,
  createProjectionFunc: (
    parameters: Parameters,
    polygonFile?: File | Blob,
    layerFile?: File | Blob,
  ) => Promise<ProjectionModel> = projServiceCreateProjection,
): Promise<ProjectionModel> => {
  const { blobPolygon, blobLayer } = createCSVFiles(modelParameterStore)
  const projectionParameters = buildProjectionParameters(modelParameterStore)

  const result = await createProjectionFunc(
    projectionParameters,
    blobPolygon,
    blobLayer,
  )
  return result
}

/**
 * Runs the current projection by sending it to batch processing.
 * The projection must already be created and its GUID stored in the app store.
 *
 * @returns The updated ProjectionModel with RUNNING status.
 * @throws Error if the projection GUID is not available in the app store.
 */
export const runProjection = async (
): Promise<ProjectionModel> => {
  const appStore = useAppStore()
  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID) {
    throw new Error(PROJECTION_ERR.MISSING_GUID)
  }
  return await projServiceRunProjection(projectionGUID)
}

/**
 * Saves the projection when a panel's Next button is clicked (Input Model Parameters mode).
 *
 * - CREATE mode + first panel (speciesInfo): Creates a new projection with parameters and CSV files,
 *   stores the GUID, and switches to EDIT mode.
 * - EDIT mode (any panel): Updates the existing projection parameters and re-uploads CSV files.
 *
 * @param modelParameterStore The store containing model parameters.
 * @param panelName The name of the panel being confirmed.
 * @throws Error if the save operation fails.
 */
export const saveProjectionOnPanelConfirm = async (
  modelParameterStore: any,
  panelName: string,
): Promise<void> => {
  const appStore = useAppStore()

  if (
    appStore.viewMode === PROJECTION_VIEW_MODE.CREATE &&
    panelName === CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO
  ) {
    // Create mode + first panel: create projection with CSV files
    const result = await createProjection(modelParameterStore)
    appStore.setCurrentProjectionGUID(result.projectionGUID)
    appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
  } else if (appStore.viewMode === PROJECTION_VIEW_MODE.EDIT) {
    // Edit mode: update projection with new CSV files
    const projectionGUID = appStore.getCurrentProjectionGUID
    if (!projectionGUID) {
      throw new Error(PROJECTION_ERR.MISSING_GUID)
    }
    const { blobPolygon, blobLayer } = createCSVFiles(modelParameterStore)
    const projectionParameters = buildProjectionParameters(modelParameterStore)
    await updateProjectionWithFiles(projectionGUID, projectionParameters, blobPolygon, blobLayer)
  }
}
