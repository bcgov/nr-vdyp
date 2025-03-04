import {
  GetHelpApi,
  GetRootApi,
  RunHCSVProjectionApi,
  ParameterNamesEnum,
} from '@/services/vdyp-api/'
import axiosInstance from '@/services/axiosInstance'
import type { AxiosRequestConfig } from 'axios'

const helpApiInstance = new GetHelpApi(undefined, undefined, axiosInstance)
const rootApiInstance = new GetRootApi(undefined, undefined, axiosInstance)
const projectionApiInstance = new RunHCSVProjectionApi(
  undefined,
  undefined,
  axiosInstance,
)

export const apiClient = {
  helpGet: (options?: AxiosRequestConfig) => {
    return helpApiInstance.helpGet(options)
  },

  projectionHcsvPost: (
    formData: FormData,
    trialRun: boolean,
    options?: AxiosRequestConfig,
  ) => {
    return projectionApiInstance.projectionHcsvPostForm(
      formData.get(ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA) as File,
      formData.get(ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA) as File,
      formData.get(ParameterNamesEnum.PROJECTION_PARAMETERS) as any,
      trialRun,
      options,
    )
  },

  rootGet: (options?: AxiosRequestConfig) => {
    return rootApiInstance.rootGet(options)
  },
}

export default apiClient
