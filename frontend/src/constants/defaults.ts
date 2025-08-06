import * as CONSTANTS from '@/constants/constants'
import { UtilizationClassSetEnum } from '@/services/vdyp-api/models/utilization-class-set-enum'

export const DEFAULT_VALUES = Object.freeze({
  DERIVED_BY: CONSTANTS.DERIVED_BY.VOLUME,
  BEC_ZONE: 'IDF',
  SITE_SPECIES_VALUES: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
  AGE_TYPE: CONSTANTS.AGE_TYPE.TOTAL,
  SPZ_AGE: 60,
  SPZ_HEIGHT: '17.00',
  BHA50_SITE_INDEX: '16.30',
  PERCENT_STOCKABLE_AREA: 55,
  SELECTED_AGE_YEAR_RANGE: CONSTANTS.AGE_YEAR_RANGE.AGE,
  CROWN_CLOSURE: 0,
  BASAL_AREA: '10.0000',
  TPH: '1000.00',
  MIN_DBH_LIMIT: CONSTANTS.MINIMUM_DBH_LIMITS.CM7_5,
  CURRENT_DIAMETER: '11.3',
  STARTING_AGE: 0,
  FINISHING_AGE: 250,
  AGE_INCREMENT: 25,
  START_YEAR: 1940,
  END_YEAR: 2040,
  YEAR_INCREMENT: 10,
  IS_FORWARD_GROW_ENABLED: true,
  IS_BACKWARD_GROW_ENABLED: true,
  IS_WHOLE_STEM_ENABLED: true,
  IS_CLOSE_UTIL_ENABLED: false,
  IS_NET_DECAY_ENABLED: false,
  IS_NET_DECAY_WASTE_ENABLED: false,
  IS_NET_DECAY_WASTE_BREAKAGE_ENABLED: false,
  IS_COMPUTED_MAI_ENABLED: false,
  IS_CULMINATION_VALUES_ENABLED: false,
  IS_BY_SPECIES_ENABLED: false,
  IS_LAYER_ENABLED: false,
  IS_PROJECTION_MODE_ENABLED: false,
  IS_POLYGON_ID_ENABLED: false,
  IS_CURRENT_YEAR_ENABLED: false,
  IS_REFERENCE_YEAR_ENABLED: false,
  INC_SECONDARY_HEIGHT: false,
  VOLUME_REPORTED: [CONSTANTS.VOLUME_REPORTED.WHOLE_STEM],
  PROJECTION_TYPE: CONSTANTS.PROJECTION_TYPE.VOLUME,
  REPORT_TITLE: 'A Sample Report Title',
  MODEL_SELECTION: CONSTANTS.MODEL_SELECTION.FILE_UPLOAD,
})

// Mapping Species Group to Default Utilization Level
export const SPECIES_GROUP_DEFAULT_UTILIZATION_MAP: {
  [key: string]: UtilizationClassSetEnum
} = {
  AC: UtilizationClassSetEnum._75,
  AT: UtilizationClassSetEnum._75,
  B: UtilizationClassSetEnum._125,
  C: UtilizationClassSetEnum._75,
  D: UtilizationClassSetEnum._75,
  E: UtilizationClassSetEnum._75,
  F: UtilizationClassSetEnum._75,
  H: UtilizationClassSetEnum._75,
  L: UtilizationClassSetEnum._75,
  MB: UtilizationClassSetEnum._75,
  PA: UtilizationClassSetEnum._75,
  PL: UtilizationClassSetEnum._75,
  PW: UtilizationClassSetEnum._75,
  PY: UtilizationClassSetEnum._75,
  S: UtilizationClassSetEnum._75,
  Y: UtilizationClassSetEnum._75,
}

// default value for projection
export const CROWN_CLOSURE_DEFAULT_4PROJ = 50
export const BASAL_AREA_DEFAULT_4PROJ = DEFAULT_VALUES.BASAL_AREA
export const TPH_DEFAULT_4PROJ = DEFAULT_VALUES.TPH
