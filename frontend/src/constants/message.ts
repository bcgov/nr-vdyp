export const SVC_ERR = Object.freeze({
  DEFAULT: 'service communication error. Please try again later.',
  DEFAULT_TITLE: 'Communication Error',
  REQUEST_TIMEOUT: 'request timed out. Please try again later.',
  REQUEST_TIMEOUT_TITLE: 'Timeout Error',
  SERVICE_UNAVAILABLE:
    'the service being unavailable. Please try again later.',
  SERVICE_UNAVAILABLE_TITLE: 'Service Unavailable',
  BAD_GATEWAY:
    'the server being unable to complete the request. Please try again later.',
  BAD_GATEWAY_TITLE: 'Bad Gateway',
  GATEWAY_TIMEOUT:
    'the server not responding in time. Please try again later.',
  GATEWAY_TIMEOUT_TITLE: 'Gateway Timeout',
  INTERNAL_SERVER_ERROR:
    'internal server error. Please try again later.',
  INTERNAL_SERVER_ERROR_TITLE: 'Internal Server Error',
  BAD_REQUEST: 'the server not understanding the request. Please try again later.',
  BAD_REQUEST_TITLE: 'Bad Request',
  FORBIDDEN: 'insufficient permission to access the resource. Please try again later.',
  FORBIDDEN_TITLE: 'Forbidden',
  UNAUTHORIZED: 'the requirement to log in to access the resource. Please try again later.',
  UNAUTHORIZED_TITLE: 'Unauthorized',
  NOT_FOUND: 'the requested resource not being found. Please try again later.',
  NOT_FOUND_TITLE: 'Not Found',
  NOT_ACCEPTABLE: 'the requested format not being supported. Please try again later.',
  NOT_ACCEPTABLE_TITLE: 'Unsupported Format',
  UNSUPPORTED_MEDIA_TYPE: 'the media type not being supported. Please try again later.',
  UNSUPPORTED_MEDIA_TYPE_TITLE: 'Unsupported Media Type',
  PROCESSING_ERROR: 'the request not being processed properly. Please try again later.',
  PROCESSING_ERROR_TITLE: 'Processing Error',
  REQUEST_CANCELED: 'the request being canceled. Please try again later.',
  REQUEST_CANCELED_TITLE: 'Request Cancelled'
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
  AUTH_032:
    'Error during user authentication re-validation (Error: AUTH_032). Please log in again.',
  TITLE: 'Authentication Error'
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
  MISSING_FILE: 'Missing File',
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
  FAIL_RUN_MODEL: 'Failed to run the projection',
})

export const FILE_DOWNLOAD_ERR = Object.freeze({
  NO_DATA: 'No data available to download.',
  NO_DATA_TITLE: 'No data to download',
})

export const MODEL_PARAM_INPUT_ERR = Object.freeze({
  FAIL_RUN_MODEL: 'Failed to run the projection',
})

export const PRINT_ERR = Object.freeze({
  NO_DATA: 'No data available to print.',
  NO_DATA_TITLE: 'No data to print',
})

export const PROGRESS_MSG = Object.freeze({
  DELETING_PROJECTION: 'Deleting Projection...',
  LOADING_PROJECTION: 'Loading Projection...',
  RUNNING_PROJECTION: 'Running Projection...',
  SAVING_PROJECTION: 'Saving Projection...',
  CANCELLING_PROJECTION: 'Cancelling Projection...',
  DOWNLOADING_PROJECTION: 'Downloading Projection...',
  LOADING_RESULTS: 'Loading Projection Results...',
  UPLOADING_FILE: 'Uploading File...',
  DELETING_FILE: 'Deleting File...',
})

export const SUCCESS_MSG = Object.freeze({
  INPUT_MODEL_PARAM_RUN_RESULT:
    'Model run completed successfully. Please check the results in the report tab.',
  INPUT_MODEL_PARAM_RUN_RESULT_W_ERR:
    'Model run completed with errors - check error log',
  PROJECTION_RUN_RESULT_TITLE: 'Projection Completed Successfully',
  PROJECTION_RUN_RESULT_W_ERR_TITLE: 'Projection Completed Successfully with Errors',
  PROJECTION_DELETED: 'Projection has been successfully deleted.',
  PROJECTION_DELETED_TITLE: 'Projection deleted',
  PROJECTION_CANCELLED: 'Projection has been successfully cancelled.',
  PROJECTION_CANCELLED_TITLE: 'Projection Cancelled',
  BATCH_PROJECTION_STARTED:
    'Projection has been submitted for batch processing. You can check the status in the Projection List.',
  BATCH_PROJECTION_STARTED_TITLE: 'Projection Started',
  DOWNLOAD_SUCCESS: (fileName: string) =>
    `${fileName} was successfully downloaded to your device`,
  DOWNLOAD_SUCCESS_TITLE: 'Download Complete',
})

export const PROJECTION_ERR = Object.freeze({
  DELETE_FAILED: 'Failed to delete the projection. Please try again later.',
  DELETE_FAILED_TITLE: 'Projection Delete Failed',
  LOAD_FAILED: 'Failed to load the projection. Please try again later.',
  LOAD_FAILED_TITLE: 'Projection Load Failed',
  MISSING_GUID: 'No projection is currently selected. Please create or select a projection first.',
  SAVE_FAILED: 'Failed to save the projection. Please try again.',
  SAVE_FAILED_TITLE: 'Projection Save Failed',
  FILE_UPLOAD_FAILED: 'Failed to upload the file. Please try again.',
  FILE_UPLOAD_FAILED_TITLE: 'File Upload Failed',
  FILE_DELETE_FAILED: 'Failed to remove the file. Please try again.',
  FILE_DELETE_FAILED_TITLE: 'File Removal Failed',
  CANCEL_FAILED: 'Failed to cancel the projection. Please try again later.',
  CANCEL_FAILED_TITLE: 'Projection Cancel Failed',
  CANCEL_ALREADY_COMPLETED: 'Cannot cancel the projection run. Projection has already completed. Results are ready.',
  CANCEL_ALREADY_COMPLETED_TITLE: 'Projection Completed',
  CANCEL_ALREADY_FAILED: 'Projection has already finished with errors.',
  CANCEL_ALREADY_FAILED_TITLE: 'Projection Failed',
  CANCEL_NOT_RUNNING: 'Projection is no longer running.',
  CANCEL_NOT_RUNNING_TITLE: 'Cannot Cancel',
  DOWNLOAD_FAILED: (fileName: string) =>
    `${fileName} was unable to download to your device. Please try your download again.`,
  DOWNLOAD_FAILED_TITLE: 'Download Failed',
  RESULTS_LOAD_FAILED: 'Failed to load projection results. Please try downloading the report instead.',
  RESULTS_LOAD_FAILED_TITLE: 'Results Load Failed',
})

export const FILE_REMOVAL_DIALOG = Object.freeze({
  TITLE: 'Remove File?',
  POLYGON_MESSAGE: (filename: string) => `Do you wish to remove polygon file: ${filename} from this projection?`,
  LAYER_MESSAGE: (filename: string) => `Do you wish to remove layer file: ${filename} from this projection?`,
})
