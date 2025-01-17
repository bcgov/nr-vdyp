import * as CONSTANTS from '@/constants/constants'

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
]

export const ecoZoneOptions = [
  { label: 'Boreal Cordillera', value: '1' },
  { label: 'Boreal Plains', value: '2' },
  { label: 'Montane Cordillera', value: '3' },
  { label: 'Pacific Maritime', value: '4' },
  { label: 'Taiga Plains', value: '5' },
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
    label: 'Species Composition',
    value: CONSTANTS.INCLUDE_IN_REPORT.SPECIES_COMPOSITION,
  },
  {
    label: 'Culmination Values',
    value: CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES,
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
