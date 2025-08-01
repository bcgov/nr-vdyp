import * as CONSTANTS from '@/constants/constants'
import { UtilizationClassSetEnum } from '@/services/vdyp-api/models/utilization-class-set-enum'

export const derivedByOptions = [
  { label: 'Volume', value: 'Volume' },
  { label: 'Basal Area', value: 'Basal Area' },
]

export const becZoneOptions = [
  { label: 'AT - Alpine Tundra', value: 'AT' },
  { label: 'BG - Bunch Grass', value: 'BG' },
  { label: 'BWBS - Boreal White and Black Spruce', value: 'BWBS' },
  { label: 'CDF - Coastal Douglas Fir', value: 'CDF' },
  { label: 'CWH - Coastal Western Hemlock', value: 'CWH' },
  { label: 'ESSF - Engelmann Spruce', value: 'ESSF' },
  { label: 'ICH - Interior Cedar Hemlock', value: 'ICH' },
  { label: 'IDF - Interior Douglas Fir', value: 'IDF' },
  { label: 'MH - Mountain Hemlock', value: 'MH' },
  { label: 'MS - Montane Spruce', value: 'MS' },
  { label: 'PP - Ponderosa Pine', value: 'PP' },
  { label: 'SBPS - Sub-Boreal Pine-Spruce', value: 'SBPS' },
  { label: 'SBS - Sub-Boreal Spruce', value: 'SBS' },
  { label: 'SWB - Spruce-Willow-Birch', value: 'SWB' },
]

export const siteSpeciesValuesOptions = [
  { label: 'Supplied', value: CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED },
  { label: 'Computed', value: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED },
]

export const ageTypeOptions = [
  { label: 'Total', value: CONSTANTS.AGE_TYPE.TOTAL },
  { label: 'Breast', value: CONSTANTS.AGE_TYPE.BREAST },
]

export const ecoZoneOptions = [
  { label: 'Boreal Cordillera', value: '12' },
  { label: 'Boreal Plains', value: '9' },
  { label: 'Montane Cordillera', value: '14' },
  { label: 'Pacific Maritime', value: '13' },
  { label: 'Taiga Plains', value: '4' },
]

export const ageYearRangeOptions = [
  { label: 'Age Range', value: CONSTANTS.AGE_YEAR_RANGE.AGE },
  { label: 'Year Range', value: CONSTANTS.AGE_YEAR_RANGE.YEAR },
]

export const volumeReportedOptions = [
  { label: 'Whole Stem', value: CONSTANTS.VOLUME_REPORTED.WHOLE_STEM },
  { label: 'Close Utilization', value: CONSTANTS.VOLUME_REPORTED.CLOSE_UTIL },
  { label: 'Net Decay', value: CONSTANTS.VOLUME_REPORTED.NET_DECAY },
  {
    label: 'Net Decay and Waste',
    value: CONSTANTS.VOLUME_REPORTED.NET_DECAY_WASTE,
  },
  {
    label: 'Net Decay, Waste and Breakage',
    value: CONSTANTS.VOLUME_REPORTED.NET_DECAY_WASTE_BREAKAGE,
  },
]

export const includeInReportOptions = [
  { label: 'Computed MAI', value: CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI },
  {
    label: 'Culmination Values',
    value: CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES,
  },
  {
    label: 'By Species',
    value: CONSTANTS.INCLUDE_IN_REPORT.BY_SPECIES,
  },
]

export const projectionTypeOptions = [
  { label: 'Volume', value: CONSTANTS.PROJECTION_TYPE.VOLUME },
  { label: 'CFS Biomass', value: CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS },
]

export const modelSelectionOptions = [
  {
    label: 'File Upload',
    value: CONSTANTS.MODEL_SELECTION.FILE_UPLOAD,
  },
  {
    label: 'Input Model Parameters',
    value: CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
  },
]

// Utilization class options with (slider) index, label, and enum value mapping
export const utilizationClassOptions = [
  {
    index: 0,
    label: CONSTANTS.MINIMUM_DBH_LIMITS.CM4_0,
    value: UtilizationClassSetEnum._40,
  },
  {
    index: 1,
    label: CONSTANTS.MINIMUM_DBH_LIMITS.CM7_5,
    value: UtilizationClassSetEnum._75,
  },
  {
    index: 2,
    label: CONSTANTS.MINIMUM_DBH_LIMITS.CM12_5,
    value: UtilizationClassSetEnum._125,
  },
  {
    index: 3,
    label: CONSTANTS.MINIMUM_DBH_LIMITS.CM17_5,
    value: UtilizationClassSetEnum._175,
  },
  {
    index: 4,
    label: CONSTANTS.MINIMUM_DBH_LIMITS.CM22_5,
    value: UtilizationClassSetEnum._225,
  },
]
