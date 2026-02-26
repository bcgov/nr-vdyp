import {
  GetHelpApi,
  GetRootApi,
  ProjectionApi,
  RunHCSVProjectionApi,
  ParameterNamesEnum,
} from '@/services/vdyp-api/'
import type { Parameters, ModelParameters } from '@/services/vdyp-api/'
import axiosInstance from '@/services/axiosInstance'
import type { AxiosRequestConfig } from 'axios'

// Create API instances with the provided axiosInstance.
const helpApiInstance = new GetHelpApi(undefined, undefined, axiosInstance)
const rootApiInstance = new GetRootApi(undefined, undefined, axiosInstance)
const projectionApiInstance = new ProjectionApi(
  undefined,
  undefined,
  axiosInstance,
)
const runHCSVProjectionApiInstance = new RunHCSVProjectionApi(
  undefined,
  undefined,
  axiosInstance,
)

export const apiClient = {
  /**
   * Retrieves help details from the help API.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for help details.
   */
  helpGet: (options?: AxiosRequestConfig) => {
    return helpApiInstance.helpGet(options)
  },

  /**
   * THIS IS for Non-Persistent Projection Run Version
   * Sends a projection HCSV POST request to the API.
   * It extracts the required files and parameters from the provided FormData.
   * @param formData The FormData containing the polygon file, layer file, and projection parameters.
   * @param trialRun Boolean flag indicating if this is a trial run.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the projection response.
   */
  projectionHcsvPost: (
    formData: FormData,
    trialRun: boolean,
    options?: AxiosRequestConfig,
  ) => {
    const customOptions: AxiosRequestConfig = {
      method: 'POST',
      headers: {
        Accept: 'application/octet-stream, application/json',
        'Content-Type': 'multipart/form-data',
        ...options?.headers,
      },
      responseType: 'blob',
      ...options,
    }

    return runHCSVProjectionApiInstance.projectionHcsvPostForm(
      formData.get(ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA) as File,
      formData.get(ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA) as File,
      formData.get(ParameterNamesEnum.PROJECTION_PARAMETERS) as any,
      trialRun,
      customOptions,
    )
  },

  /**
   * Retrieves the root details from the API.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the root details.
   */
  rootGet: (options?: AxiosRequestConfig) => {
    return rootApiInstance.rootGet(options)
  },

  /**
   * Retrieves all projections for the authenticated user.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the list of user projections.
   */
  getUserProjections: async (options?: AxiosRequestConfig) => {
    return projectionApiInstance.getUserProjections(options)
  },

  /**
   * Creates a new empty projection with default parameters.
   * @param parameters The projection parameters.
   * @param modelParameters Optional model parameters for Manual Input mode.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the created projection.
   */
  createProjection: (parameters: Parameters, modelParameters?: ModelParameters, reportDescription?: string | null, options?: AxiosRequestConfig) => {
    return projectionApiInstance.createProjection(parameters, modelParameters, reportDescription, options)
  },

  /**
   * Retrieves a projection by its GUID.
   * @param projectionGUID The projection GUID.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the projection details.
   */
  getProjection: (projectionGUID: string, options?: AxiosRequestConfig) => {
    return projectionApiInstance.getProjection(projectionGUID, options)
  },

  /**
   * Updates projection parameters.
   * @param projectionGUID The projection GUID.
   * @param parameters The updated projection parameters.
   * @param modelParameters Optional model parameters for Manual Input mode.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the updated projection.
   */
  updateProjectionParams: (
    projectionGUID: string,
    parameters: Parameters,
    modelParameters?: ModelParameters,
    reportDescription?: string | null,
    options?: AxiosRequestConfig,
  ) => {
    return projectionApiInstance.updateProjectionParams(
      projectionGUID,
      parameters,
      modelParameters,
      reportDescription,
      options,
    )
  },

  /**
   * Runs a projection (sends to batch processing).
   * @param projectionGUID The projection GUID.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the updated projection with RUNNING status.
   */
  runProjection: (projectionGUID: string, options?: AxiosRequestConfig) => {
    return projectionApiInstance.runProjection(projectionGUID, options)
  },

  /**
   * Cancels a running projection.
   * @param projectionGUID The projection GUID.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the updated projection with DRAFT status.
   */
  cancelProjection: (projectionGUID: string, options?: AxiosRequestConfig) => {
    return projectionApiInstance.cancelProjection(projectionGUID, options)
  },

  /**
   * Duplicates a projection (copies inputs, resets status to Draft).
   * @param projectionGUID The projection GUID to duplicate.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the newly created duplicate ProjectionModel.
   */
  duplicateProjection: (projectionGUID: string, options?: AxiosRequestConfig) => {
    return projectionApiInstance.duplicateProjection(projectionGUID, options)
  },

  /**
   * Deletes a projection.
   * @param projectionGUID The projection GUID.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the delete operation.
   */
  deleteProjection: (projectionGUID: string, options?: AxiosRequestConfig) => {
    return projectionApiInstance.deleteProjection(projectionGUID, options)
  },

  /**
   * Retrieves all files in a fileset for a projection.
   * @param projectionGUID The projection GUID.
   * @param fileSetGUID The fileset GUID.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the list of file mappings.
   */
  getFileSetFiles: (
    projectionGUID: string,
    fileSetGUID: string,
    options?: AxiosRequestConfig,
  ) => {
    return projectionApiInstance.getFileSetFiles(
      projectionGUID,
      fileSetGUID,
      options,
    )
  },

  /**
   * Uploads a file to a fileset.
   * @param projectionGUID The projection GUID.
   * @param fileSetGUID The fileset GUID.
   * @param file The file to upload.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the updated projection.
   */
  uploadFileToFileSet: (
    projectionGUID: string,
    fileSetGUID: string,
    file: File,
    options?: AxiosRequestConfig,
  ) => {
    return projectionApiInstance.uploadFileToFileSet(
      projectionGUID,
      fileSetGUID,
      file,
      options,
    )
  },

  /**
   * Gets a file for download with presigned URL.
   * @param projectionGUID The projection GUID.
   * @param fileSetGUID The fileset GUID.
   * @param fileMappingGUID The file mapping GUID.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the file mapping with download URL.
   */
  getFileForDownload: (
    projectionGUID: string,
    fileSetGUID: string,
    fileMappingGUID: string,
    options?: AxiosRequestConfig,
  ) => {
    return projectionApiInstance.getFileForDownload(
      projectionGUID,
      fileSetGUID,
      fileMappingGUID,
      options,
    )
  },

  /**
   * Streams the results ZIP file for a projection via backend proxy.
   * @param projectionGUID The projection GUID.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the zip blob.
   */
  streamResultsZip: (projectionGUID: string, options?: AxiosRequestConfig) => {
    const customOptions: AxiosRequestConfig = {
      headers: {
        Accept: 'application/zip',
        ...options?.headers,
      },
      responseType: 'blob',
      ...options,
    }
    return projectionApiInstance.streamResultsZip(projectionGUID, customOptions)
  },

  /**
   * Deletes a file from a fileset.
   * @param projectionGUID The projection GUID.
   * @param fileSetGUID The fileset GUID.
   * @param fileMappingGUID The file mapping GUID.
   * @param options Optional Axios request configuration.
   * @returns The Axios promise for the delete operation.
   */
  deleteFileFromFileSet: (
    projectionGUID: string,
    fileSetGUID: string,
    fileMappingGUID: string,
    options?: AxiosRequestConfig,
  ) => {
    return projectionApiInstance.deleteFileFromFileSet(
      projectionGUID,
      fileSetGUID,
      fileMappingGUID,
      options,
    )
  },
}

export default apiClient
