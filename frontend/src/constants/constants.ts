export const KEYCLOAK = Object.freeze({
  PKCE_METHOD: 'S256',
  ONLOAD: 'check-sso',
  CHECK_LOGIN_IFRAME: false,
  IDP_AZUR_IDIR: 'azureidir', // Identity Provider: IDIR with MFA
  MAX_SESSION_DURATION: 8 * 60 * 60 * 1000, // 8 hours (in milliseconds)
  UPDATE_TOKEN_MIN_VALIDITY: 120, // 2 min, in seconds
  IS_TOKEN_EXP_MIN_VALIDITY: 120, // 2 min, in seconds
  ENABLE_LOGGING: false,
})

export const SORT_ORDER = Object.freeze({
  ASC: 'ASC',
  DESC: 'DESC',
})

export const PANEL = Object.freeze({
  OPEN: 0,
  CLOSE: -1,
})

export const DERIVED_BY = Object.freeze({
  VOLUME: 'Volume',
  BASAL_AREA: 'Basal Area',
})

export const SITE_SPECIES_VALUES = Object.freeze({
  SUPPLIED: 'Supplied',
})

export const AGE_YEAR_RANGE = Object.freeze({
  AGE: 'age',
  YEAR: 'year',
})

export const VOLUME_REPORTED = Object.freeze({
  WHOLE_STEM: 'Whole Stem',
  CLOSE_UTIL: 'Close Utilization',
  NET_DECAY: 'Net Decay',
  NET_DECAY_WASTE: 'Net Decay and Waste',
  NET_DECAY_WASTE_BREAKAGE: 'Net Decay, Waste and Breakage',
})

export const INCLUDE_IN_REPORT = Object.freeze({
  COMPUTED_MAI: 'Computed MAI',
  SPECIES_COMPOSITION: 'Species Composition',
  CULMINATION_VALUES: 'Culmination Values',
})

export const PROJECTION_TYPE = Object.freeze({
  VOLUME: 'Volume',
  CFS_BIOMASS: 'CFS Biomass',
})

export const SPECIAL_INDICATORS = Object.freeze({
  NA: 'N/A',
  NOT_USED: '<Not Used>',
})

export const MODEL_PARAMETER_PANEL = Object.freeze({
  SPECIES_INFO: 'speciesInfo',
  SITE_INFO: 'siteInfo',
  STAND_INFO: 'standInfo',
  REPORT_INFO: 'reportInfo',
})

export const FILE_UPLOAD_PANEL = Object.freeze({
  REPORT_INFO: 'reportInfo',
  ATTACHMENTS: 'attachments',
})

export const NUM_INPUT_LIMITS = Object.freeze({
  SPECIES_PERCENT_MAX: 100,
  SPECIES_PERCENT_MIN: 0,
  SPECIES_PERCENT_STEP: 5,
  SPECIES_PERCENT_DECIMAL_NUM: 1,
  TOTAL_SPECIES_PERCENT: 100,
  BHA50_SITE_INDEX_MAX: 60,
  BHA50_SITE_INDEX_MIN: 0,
  BHA50_SITE_INDEX_STEP: 1,
  BHA50_SITE_INDEX_DECIMAL_NUM: 2,
  PERCENT_STOCKABLE_AREA_MAX: 100,
  PERCENT_STOCKABLE_AREA_MIN: 0,
  PERCENT_STOCKABLE_AREA_STEP: 5,
  STARTING_AGE_MAX: 500,
  STARTING_AGE_MIN: 0,
  STARTING_AGE_STEP: 10,
  FINISHING_AGE_MAX: 450,
  FINISHING_AGE_MIN: 1,
  FINISHING_AGE_STEP: 10,
  AGE_INC_MAX: 350,
  AGE_INC_MIN: 1,
  AGE_INC_STEP: 5,
  START_YEAR_MIN: 1400,
  START_YEAR_MAX: 2500,
  START_YEAR_STEP: 1,
  END_YEAR_MIN: 1400,
  END_YEAR_MAX: 2500,
  END_YEAR_STEP: 1,
  YEAR_INC_MIN: 1,
  YEAR_INC_MAX: 350,
  YEAR_INC_STEP: 1,
})

export const CONTINUOUS_INC_DEC = Object.freeze({
  INTERVAL: 100, // (e.g., 100ms)
})

export const SPIN_BUTTON = Object.freeze({
  UP: '▲',
  DOWN: '▼',
})

export const NOTIFICATION = Object.freeze({
  SHOW_TIME: 5000, // in milliseconds (5 sec)
})

export const AXIOS = Object.freeze({
  ACCEPT: 'application/json',
  CONTENT_TYPE: 'application/json',
})

export const MESSAGE_TYPE = Object.freeze({
  INFO: 'info',
  SUCCESS: 'success',
  ERROR: 'error',
  WARNING: 'warning',
})

export const HEADER_SELECTION = Object.freeze({
  MODEL_PARAMETER_SELECTION: 'Model Parameter Selection',
  FILE_UPLOAD: 'File Upload',
})

export const MODEL_SELECTION = Object.freeze({
  FILE_UPLOAD: 'File Upload',
  INPUT_MODEL_PARAMETERS: 'Input Model Parameters',
})

export const MODEL_PARAM_TAB_NAME = Object.freeze({
  MODEL_PARAM_SELECTION: 'Parameter Selection',
  MODEL_REPORT: 'Model Report',
  VIEW_LOG_FILE: 'View Log File',
  VIEW_ERROR_MESSAGES: 'View Error Messages',
})

export const FILE_UPLOAD_TAB_NAME = Object.freeze({
  FILE_UPLOAD: 'Parameter Selection',
  MODEL_REPORT: 'Model Report',
  VIEW_LOG_FILE: 'View Log File',
  VIEW_ERROR_MESSAGES: 'View Error Messages',
})

export const MODEL_PARAM_TAB_INDEX = {
  PARAM_SELECTION: 0,
  MODEL_REPORT: 1,
  VIEW_LOG_FILE: 2,
  VIEW_ERROR_MESSAGES: 3,
}

export const FILE_UPLOAD_TAB_INDEX = {
  PARAM_SELECTION: 0,
  MODEL_REPORT: 1,
  VIEW_LOG_FILE: 2,
  VIEW_ERROR_MESSAGES: 3,
}

export const FILE_NAME = Object.freeze({
  PROJECTION_RESULT_ZIP: 'vdyp-output.zip',
  ERROR_TXT: 'ErrorLog.txt',
  LOG_TXT: 'ProgressLog.txt',
  DEBUG_TXT: 'DebugLog.txt',
  YIELD_TABLE_CSV: 'YieldTable.csv',
  INPUT_POLY_CSV: 'VDYP7_INPUT_POLY.csv',
  INPUT_LAYER_CSV: 'VDYP7_INPUT_LAYER.csv',
})

export const BUTTON_LABEL = Object.freeze({
  CONT_EDIT: 'Continue Editing',
})

export const REPORTING_TAB = Object.freeze({
  MODEL_REPORT: 'ModelReport',
  VIEW_ERR_MSG: 'ViewErrorMessages',
  VIEW_LOG_FILE: 'ViewLogFile',
})
