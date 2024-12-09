import {
  HelpEndpointApi,
  ProjectionEndpointApi,
  RootEndpointApi,
} from '@/services/vdyp-api/'
import axiosInstance from '@/services/axiosInstance'

const helpApiInstance = new HelpEndpointApi(undefined, undefined, axiosInstance)
const projectionApiInstance = new ProjectionEndpointApi(
  undefined,
  undefined,
  axiosInstance,
)
const rootApiInstance = new RootEndpointApi(undefined, undefined, axiosInstance)

export const apiClient = {
  helpGet: (options?: any) => {
    return helpApiInstance.v8HelpGet(options)
  },

  projectionHcsvPost: (body?: any, options?: any) => {
    return projectionApiInstance.v8ProjectionHcsvPost(body, options)
  },

  rootGet: (options?: any) => {
    return rootApiInstance.v8Get(options)
  },
}

export default apiClient
