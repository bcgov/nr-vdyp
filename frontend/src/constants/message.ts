export const SVC_ERR = Object.freeze({
  DEFAULT: 'Service Communication Error. Please try again later.',
  REQUEST_TIMEOUT: 'Request timed out. Please try again. (Error: Timeout)',
  SERVICE_UNAVAILABLE:
    'The service is currently unavailable. Please try later. (Error: Unavailable)',
  BAD_GATEWAY:
    'The server was unable to complete your request. Please try again later. (Error: Bad Gateway)',
  GATEWAY_TIMEOUT:
    'The server did not respond in time. Please try again later. (Error: Gateway Timeout)',
  INTERNAL_SERVER_ERROR:
    'Service Internal Server Error. Please try again later.',
})

export const AUTH_ERR = Object.freeze({
  AUTH_001:
    'Error during User authentication verification (Error: AUTH_001). Please log in again.',
  AUTH_002:
    'Error during User authentication verification (Error: AUTH_002). Please log in again.',
  AUTH_003:
    'Error during User authentication verification (Error: AUTH_003). Please log in again.',
  AUTH_004:
    'Error during User authentication verification (Error: AUTH_004). Please log in again.',
  AUTH_010:
    'Error during User authentication initialization (Error: AUTH_010). Please log in again.',
  AUTH_011:
    'Error during User authentication initialization (Error: AUTH_011). Please log in again.',
  AUTH_012:
    'Error during User authentication initialization (Error: AUTH_012). Please log in again.',
  AUTH_020:
    'Error during user authentication refresh (Error: AUTH_020). Please log in again.',
  AUTH_021:
    'Error during user authentication refresh (Error: AUTH_021). Please log in again.',
  AUTH_030:
    'Error during user authentication re-validation (Error: AUTH_030). Please log in again.',
  AUTH_031:
    'Session has exceeded the maximum duration, logging out (Error: AUTH_031). Please log in again.',
  AUTH_032:
    'Error during user authentication re-validation (Error: AUTH_032). Please log in again.',
})

export const AXIOS_INST_ERR = Object.freeze({
  SESSION_INACTIVE:
    'Your session is not active. Please log out and try logging in again.',
})

export const MSG_DIALOG_TITLE = Object.freeze({
  DATA_DUPLICATED: 'Data Duplicated!',
  DATA_INCOMPLETE: 'Data Incomplete!',
  MISSING_INFO: 'Missing Information',
  INVALID_INPUT: 'Invalid Input!',
  CONFIRM: 'Confirm',
  NO_MODIFY: 'No Modifications!',
  MISSING_FILE: 'Missing File',
  INVALID_FILE: 'Invalid File!',
  POLYGON_FILE_HEADER_MISMATCH: 'Polygon File Header Mismatch!',
  LAYER_FILE_HEADER_MISMATCH: 'Layer File Header Mismatch!',
  POLYGON_FILE_DUPLICATE_COLUMNS: 'Polygon File Duplicate Columns!',
  LAYER_FILE_DUPLICATE_COLUMNS: 'Layer File Duplicate Columns!',
})

export const MDL_PRM_INPUT_HINT = Object.freeze({
  SITE_ZERO_NOT_KNOW: 'A value of zero indicates not known.',
  SITE_DFT_COMPUTED: 'A default will be computed when the model is run.',
  DENSITY_PCC_APPLY_DFT: 'Applying Default of 50%',
  DENSITY_WO_AGE: 'Density Measurements cannot be supplied without an Age.',
})

export const MDL_PRM_INPUT_ERR = Object.freeze({
  SPCZ_VLD_DUP_W_LABEL: (speciesCode: string, speciesLabel: string) =>
    `Species '${speciesCode} - ${speciesLabel}' already specified`,
  SPCZ_VLD_DUP_WO_LABEL: (speciesCode: string) =>
    `Species '${speciesCode}' already specified`,
  SPCZ_VLD_INPUT_RANGE: (
    speciesPercentMin: number,
    speciesPercentMax: number,
  ) =>
    `Please enter a value between ${speciesPercentMin} and ${speciesPercentMax}`,
  SPCZ_VLD_TOTAL_PCT:
    'Species percentage must add up to a total of 100.0% in order to run a valid model',
  SPCZ_VLD_MISSING_DERIVED_BY:
    "Input field - 'Species % derived by' - is missing essential information which must be filled in order to confirm and continue",
  SPCZ_VLD_TOTAL_PCT_NOT_100: 'Species Percent do not total 100.0%',
  SITE_VLD_AGE_RNG: "'Years' must range from 0 and 500",
  SITE_VLD_HIGHT_RNG: "'Height in Meters' must range from 0.00 and 99.90",
  SITE_VLD_SI_RNG: "'BHA 50 Site Index' must range from 0.00 and 60.00",
  SITE_VLD_SITE_INDEX_REQ:
    "Please select a Site Index option ('Supplied' or 'Computed')",
  SITE_VLD_BEC_ZONE_REQ: 'Please select a BEC Zone',
  SITE_VLD_SPCZ_REQ_VALS_SUP: (selectedSiteSpeciesValue: string | null) =>
    `The species '${selectedSiteSpeciesValue}' must have Years/Height in Meters values supplied`,
  SITE_VLD_SPCZ_REQ_SI_VAL: (selectedSiteSpeciesValue: string | null) =>
    `The species '${selectedSiteSpeciesValue}' must have an BHA 50 Site Index value supplied`,
  DENSITY_VLD_PCT_STCB_AREA_RNG:
    "'Percent Stockable Area' must range from 0 and 100",
  DENSITY_VLD_BSL_AREA_RNG: "'Basal Area' must range from 0.1000 and 250.0000",
  DENSITY_VLD_TPH_RNG: "'Trees per Hectare' must range from 0.10 and 9999.90",
  DENSITY_VLD_CROWN_CLOSURE_RNG: "'Crown Closure' must range from 0 and 100",
  DENSITY_VLD_BSL_AREA_OVER_HEIGHT:
    'Basal Area is above a likely maximum for the entered height. Do you wish to proceed?',
  RPT_VLD_COMP_FNSH_AGE:
    "'Finish Age' must be at least as great as the 'Start Age'",
  RPT_VLD_COMP_END_YEAR:
    "'End Year' must be at least as great as the 'Start Year'",
  RPT_VLD_START_AGE_RNG: (startAgeMin: number, startAgeMax: number) =>
    `'Starting Age' must range from ${startAgeMin} and ${startAgeMax}`,
  RPT_VLD_START_FNSH_RNG: (fnshAgeMin: number, fnshAgeMax: number) =>
    `'Finishing Age' must range from ${fnshAgeMin} and ${fnshAgeMax}`,
  RPT_VLD_AGE_INC_RNG: (ageIncMin: number, ageIncMax: number) =>
    `'Increment' must range from ${ageIncMin} and ${ageIncMax}`,
  RPT_VLD_START_YEAR_RNG: (startYearMin: number, startYearMax: number) =>
    `'Start Year' must range from ${startYearMin} and ${startYearMax}`,
  RPT_VLD_END_YEAR_RNG: (endYearMin: number, endYearMax: number) =>
    `'End Year' must range from ${endYearMin} and ${endYearMax}`,
  RPT_VLD_YEAR_INC_RNG: (yearIncMin: number, yearIncMax: number) =>
    `'Increment' must range from ${yearIncMin} and ${yearIncMax}`,
  RPT_VLD_REPORT_TITLE_REQ: 'Please enter a Report Title.',
  RPT_VLD_PROJECTION_TYPE_REQ: 'Please select a Projection Type.',
})

export const FILE_UPLOAD_ERR = Object.freeze({
  LAYER_FILE_MISSING: 'Layer file is missing. Please upload the required file.',
  POLYGON_FILE_MISSING:
    'Polygon file is missing. Please upload the required file.',
  LAYER_FILE_NOT_CSV_FORMAT:
    'The uploaded Layer file is not in CSV format. Please upload a valid CSV file.',
  POLYGON_FILE_NOT_CSV_FORMAT:
    'The uploaded Polygon file is not in CSV format. Please upload a valid CSV file.',
  POLYGON_FILE_DUPLICATE_COLUMNS:
    'The Polygon file contains duplicate column names. Each column must have a unique name. Please fix the file and try again.',
  LAYER_FILE_DUPLICATE_COLUMNS:
    'The Layer file contains duplicate column names. Each column must have a unique name. Please fix the file and try again.',
  RPT_VLD_REQUIRED_FIELDS_AGE:
    'All required fields (Starting Age, Finishing Age, Increment) must be filled.',
  RPT_VLD_REQUIRED_FIELDS_YEAR:
    'All required fields (Start Year, End Year, Increment) must be filled.',
  MISSING_RESPONSED_FILE:
    'The response is missing one or more required files. Please contact support or try again later.',
  INVALID_RESPONSED_FILE:
    'The response contains invalid or corrupted files. Please contact support or try again later.',
  FAIL_RUN_MODEL: 'Failed to run the projection model.',
})

export const FILE_DOWNLOAD_ERR = Object.freeze({
  NO_DATA: 'No data available to download.',
})

export const MODEL_PARAM_INPUT_ERR = Object.freeze({
  FAIL_RUN_MODEL: 'Failed to run the projection model.',
})

export const PRINT_ERR = Object.freeze({
  NO_DATA: 'No data available to print.',
})

export const PROGRESS_MSG = Object.freeze({
  RUNNING_MODEL: 'Running Model...',
  DELETING_PROJECTION: 'Deleting Projection...',
  LOADING_PROJECTION: 'Loading Projection...',
  RUNNING_PROJECTION: 'Running Projection...',
})

export const SUCCESS_MSG = Object.freeze({
  FILE_UPLOAD_RUN_RESULT:
    'Model run completed successfully. Please check the results in the report tab.',
  FILE_UPLOAD_RUN_RESULT_W_ERR:
    'File successfully downloaded with errors - check error log',
  INPUT_MODEL_PARAM_RUN_RESULT:
    'Model run completed successfully. Please check the results in the report tab.',
  INPUT_MODEL_PARAM_RUN_RESULT_W_ERR:
    'Model run completed with errors - check error log',
  PROJECTION_DELETED: 'Projection has been successfully deleted.',
})

export const PROJECTION_ERR = Object.freeze({
  DELETE_FAILED: 'Failed to delete the projection. Please try again later.',
  LOAD_FAILED: 'Failed to load the projection. Please try again later.',
  MISSING_GUID: 'No projection is currently selected. Please create or select a projection first.',
})
