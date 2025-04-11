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
})

export const MDL_PRM_INPUT_HINT = Object.freeze({
  SITE_ZERO_NOT_KNOW: 'A value of zero indicates not known.',
  SITE_DFT_COMPUTED: 'A default will be computed when the model is run.',
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
  SITE_VLD_SI_RNG: "'Site Index' must range from 0.00 and 60.00",
  SITE_VLD_SPCZ_REQ_SI_VAL: (selectedSiteSpeciesValue: string | null) =>
    `The species '${selectedSiteSpeciesValue}' must have an BHA 50 Site Index value supplied`,
  DENSITY_VLD_PCT_STCB_AREA_RNG:
    "'Percent Stockable Area' must range from 0 and 100",
  RPT_VLD_COMP_FNSH_AGE:
    "'Finish Age' must be at least as great as the 'Start Age'",
  RPT_VLD_START_AGE_RNG: (startAgeMin: number, startAgeMax: number) =>
    `'Starting Age' must range from ${startAgeMin} and ${startAgeMax}`,
  RPT_VLD_START_FNSH_RNG: (fnshAgeMin: number, fnshAgeMax: number) =>
    `'Finishing Age' must range from ${fnshAgeMin} and ${fnshAgeMax}`,
  RPT_VLD_AGE_INC_RNG: (ageIncMin: number, ageIncMax: number) =>
    `'Age Increment' must range from ${ageIncMin} and ${ageIncMax}`,
})

export const FILE_UPLOAD_ERR = Object.freeze({
  LAYER_FILE_MISSING: 'Layer file is missing. Please upload the required file.',
  POLYGON_FILE_MISSING:
    'Polygon file is missing. Please upload the required file.',
  LAYER_FILE_NOT_CSV_FORMAT:
    'The uploaded Layer file is not in CSV format. Please upload a valid CSV file.',
  POLYGON_FILE_NOT_CSV_FORMAT:
    'The uploaded Polygon file is not in CSV format. Please upload a valid CSV file.',
  RPT_VLD_REQUIRED_FIELDS:
    'All required fields (Starting Age, Finishing Age, Age Increment) must be filled.',
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
})

export const SUCCESS_MSG = Object.freeze({
  FILE_UPLOAD_RUN_RESULT:
    'File successfully downloaded. Please check the results in the report tabs.',
  FILE_UPLOAD_RUN_RESULT_W_ERR:
    'File successfully downloaded with errors - check error log',
  INPUT_MODEL_PARAM_RUN_RESULT:
    'Projection completed successfully. Please check the results in the report tabs.',
  INPUT_MODEL_PARAM_RUN_RESULT_W_ERR:
    'File successfully downloaded with errors - check error log',
})
