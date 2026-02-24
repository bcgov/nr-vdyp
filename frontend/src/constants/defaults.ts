import * as CONSTANTS from '@/constants/constants'
import { UtilizationClassSetEnum } from '@/services/vdyp-api/models/utilization-class-set-enum'

export const DEFAULT_VALUES = Object.freeze({
  AGE_TYPE: CONSTANTS.AGE_TYPE.TOTAL,
  SPZ_AGE: '60',
  SPZ_HEIGHT: '17.00',
  BHA50_SITE_INDEX: '16.30',
  PERCENT_STOCKABLE_AREA: '55',
  SELECTED_AGE_YEAR_RANGE: CONSTANTS.AGE_YEAR_RANGE.AGE,
  CROWN_CLOSURE: '50',
  BASAL_AREA: '10.0000',
  TPH: '1000.00',
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

// Mapping Species Group to Default Utilization Level for Volume Projection
export const SPECIES_GROUP_VOLUME_UTILIZATION_MAP: {
  [key: string]: UtilizationClassSetEnum
} = {
  AC: UtilizationClassSetEnum._75,
  AT: UtilizationClassSetEnum._75,
  B: UtilizationClassSetEnum._75,
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

// Mapping Species Group to Default Utilization Level for CFO Biomass Projection
export const SPECIES_GROUP_CFO_BIOMASS_UTILIZATION_MAP: {
  [key: string]: UtilizationClassSetEnum
} = {
  AC: UtilizationClassSetEnum._125,
  AT: UtilizationClassSetEnum._125,
  B: UtilizationClassSetEnum._175,
  C: UtilizationClassSetEnum._175,
  D: UtilizationClassSetEnum._125,
  E: UtilizationClassSetEnum._125,
  F: UtilizationClassSetEnum._175,
  H: UtilizationClassSetEnum._175,
  L: UtilizationClassSetEnum._125,
  MB: UtilizationClassSetEnum._125,
  PA: UtilizationClassSetEnum._125,
  PL: UtilizationClassSetEnum._125,
  PW: UtilizationClassSetEnum._125,
  PY: UtilizationClassSetEnum._125,
  S: UtilizationClassSetEnum._175,
  Y: UtilizationClassSetEnum._175,
}
