import {
  GetHelpApi,
  GetRootApi,
  RunHCSVProjectionApi,
  ParameterNamesEnum,
} from '@/services/vdyp-api/'
import axiosInstance from '@/services/axiosInstance'
import type { AxiosRequestConfig } from 'axios'

// Create API instances with the provided axiosInstance.
const helpApiInstance = new GetHelpApi(undefined, undefined, axiosInstance)
const rootApiInstance = new GetRootApi(undefined, undefined, axiosInstance)
const projectionApiInstance = new RunHCSVProjectionApi(
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

    return projectionApiInstance.projectionHcsvPostForm(
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
}

export default apiClient
