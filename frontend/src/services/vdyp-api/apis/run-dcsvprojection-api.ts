/* tslint:disable */
/* eslint-disable */
import globalAxios from 'axios'
import type { AxiosResponse, AxiosInstance, AxiosRequestConfig } from 'axios'
import { Configuration } from '../configuration'
// Some imports not used depending on template conditions
// @ts-ignore
import { BASE_PATH, BaseAPI, RequiredError } from '../base'
import type { RequestArgs } from '../base'
import type { FileUpload, Parameters } from '../models'

export const RunDCSVProjectionApiAxiosParamCreator = function (
  configuration?: Configuration,
) {
  return {
    projectionDcsvPostForm: async (
      dcsvInputData?: FileUpload,
      projectionParameters?: Parameters,
      trialRun?: boolean,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/api/v8/projection/dcsv`
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, 'https://example.com')
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'POST',
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any
      const localVarFormParams = new FormData()

      if (trialRun !== undefined) {
        localVarQueryParameter['trialRun'] = trialRun
      }

      if (dcsvInputData !== undefined) {
        localVarFormParams.append('dcsvInputData', dcsvInputData as any)
      }

      if (projectionParameters !== undefined) {
        localVarFormParams.append(
          'projectionParameters',
          projectionParameters as any,
        )
      }

      localVarHeaderParameter['Content-Type'] = 'multipart/form-data'
      const query = new URLSearchParams(localVarUrlObj.search)
      for (const key in localVarQueryParameter) {
        query.set(key, localVarQueryParameter[key])
      }
      for (const key in options.params) {
        query.set(key, options.params[key])
      }
      localVarUrlObj.search = new URLSearchParams(query).toString()
      let headersFromBaseOptions =
        baseOptions && baseOptions.headers ? baseOptions.headers : {}
      localVarRequestOptions.headers = {
        ...localVarHeaderParameter,
        ...headersFromBaseOptions,
        ...options.headers,
      }
      localVarRequestOptions.data = localVarFormParams

      return {
        url:
          localVarUrlObj.pathname + localVarUrlObj.search + localVarUrlObj.hash,
        options: localVarRequestOptions,
      }
    },
  }
}

export const RunDCSVProjectionApiFp = function (configuration?: Configuration) {
  return {
    async projectionDcsvPostForm(
      dcsvInputData?: FileUpload,
      projectionParameters?: Parameters,
      trialRun?: boolean,
      options?: AxiosRequestConfig,
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => Promise<AxiosResponse<void>>
    > {
      const localVarAxiosArgs = await RunDCSVProjectionApiAxiosParamCreator(
        configuration,
      ).projectionDcsvPostForm(
        dcsvInputData,
        projectionParameters,
        trialRun,
        options,
      )
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

export const RunDCSVProjectionApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance,
) {
  return {
    async projectionDcsvPostForm(
      dcsvInputData?: FileUpload,
      projectionParameters?: Parameters,
      trialRun?: boolean,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<void>> {
      return RunDCSVProjectionApiFp(configuration)
        .projectionDcsvPostForm(
          dcsvInputData,
          projectionParameters,
          trialRun,
          options,
        )
        .then((request) => request(axios, basePath))
    },
  }
}

export class RunDCSVProjectionApi extends BaseAPI {
  public async projectionDcsvPostForm(
    dcsvInputData?: FileUpload,
    projectionParameters?: Parameters,
    trialRun?: boolean,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<void>> {
    return RunDCSVProjectionApiFp(this.configuration)
      .projectionDcsvPostForm(
        dcsvInputData,
        projectionParameters,
        trialRun,
        options,
      )
      .then((request) => request(this.axios, this.basePath))
  }
}
