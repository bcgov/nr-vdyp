import {
  GetHelpApi,
  GetRootApi,
  RunHCSVProjectionApi,
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
      formData.get('polygonInputData') as File,
      formData.get('layersInputData') as File,
      formData.get('projectionParameters') as any,
      trialRun,
      options,
    )
  },

  rootGet: (options?: AxiosRequestConfig) => {
    return rootApiInstance.rootGet(options)
  },
}

export default apiClient
