import globalAxios from 'axios'
import type { AxiosResponse, AxiosInstance, AxiosRequestConfig } from 'axios'
import { Configuration } from '../configuration'
import { BASE_PATH, BaseAPI } from '../base'
import type { RequestArgs } from '../base'
import type { ProjectionModel } from '../models'
import { env } from '@/env'

export const GetUserProjectionsApiAxiosParamCreator = function (
  configuration?: Configuration,
) {
  return {
    getUserProjections: async (
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/api/v8/projection/me`
      const localVarUrlObj = new URL(localVarPath, env.VITE_API_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'GET',
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any

      const query = new URLSearchParams(localVarUrlObj.search)
      for (const key in localVarQueryParameter) {
        query.set(key, localVarQueryParameter[key])
      }
      for (const key in options.params) {
        query.set(key, options.params[key])
      }
      localVarUrlObj.search = new URLSearchParams(query).toString()
      const headersFromBaseOptions = baseOptions?.headers ?? {}
      localVarRequestOptions.headers = {
        ...localVarHeaderParameter,
        ...headersFromBaseOptions,
        ...options.headers,
      }

      return {
        url:
          localVarUrlObj.pathname + localVarUrlObj.search + localVarUrlObj.hash,
        options: localVarRequestOptions,
      }
    },
  }
}

export const GetUserProjectionsApiFp = function (configuration?: Configuration) {
  return {
    async getUserProjections(
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<ProjectionModel[]>>
    > {
      const localVarAxiosArgs =
        await GetUserProjectionsApiAxiosParamCreator(configuration).getUserProjections(options)
      return (
        axios: AxiosInstance = globalAxios,
        basePath: string = BASE_PATH,
      ) => {
        const axiosRequestArgs: AxiosRequestConfig = {
          ...localVarAxiosArgs.options,
          url: basePath + localVarAxiosArgs.url,
        }
        return axios.request(axiosRequestArgs)
      }
    },
  }
}

export const GetUserProjectionsApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance,
) {
  return {
    async getUserProjections(
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<ProjectionModel[]>> {
      return GetUserProjectionsApiFp(configuration)
        .getUserProjections(options)
        .then((request) => request(axios, basePath))
    },
  }
}

export class GetUserProjectionsApi extends BaseAPI {
  public async getUserProjections(
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<ProjectionModel[]>> {
    return GetUserProjectionsApiFp(this.configuration)
      .getUserProjections(options)
      .then((request) => request(this.axios, this.basePath))
  }
}
