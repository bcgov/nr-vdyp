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

export const RunHCSVProjectionApiAxiosParamCreator = function (
  configuration?: Configuration,
) {
  return {
    projectionHcsvPostForm: async (
      polygonInputData?: FileUpload,
      layersInputData?: FileUpload,
      projectionParameters?: Parameters,
      trialRun?: boolean,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/api/v8/projection/hcsv`
      const localVarUrlObj = new URL(localVarPath, 'https://example.com')
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'POST',
        headers: {
          Accept: 'application/octet-stream',
          'Content-Type': 'multipart/form-data',
        } /* edited */,
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any
      const localVarFormParams = new FormData()

      if (trialRun !== undefined) {
        localVarQueryParameter['trialRun'] = trialRun
      }

      if (polygonInputData !== undefined) {
        localVarFormParams.append('polygonInputData', polygonInputData as any)
      }

      if (layersInputData !== undefined) {
        localVarFormParams.append('layersInputData', layersInputData as any)
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

export const RunHCSVProjectionApiFp = function (configuration?: Configuration) {
  return {
    async projectionHcsvPostForm(
      polygonInputData?: FileUpload,
      layersInputData?: FileUpload,
      projectionParameters?: Parameters,
      trialRun?: boolean,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<Blob>> /* edited */
    > {
      const localVarAxiosArgs = await RunHCSVProjectionApiAxiosParamCreator(
        configuration,
      ).projectionHcsvPostForm(
        polygonInputData,
        layersInputData,
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
          url: localVarAxiosArgs.url,
          responseType: 'blob' /* edited */,
        }
        return axios.request(axiosRequestArgs)
      }
    },
  }
}

export const RunHCSVProjectionApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance,
) {
  return {
    async projectionHcsvPostForm(
      polygonInputData?: FileUpload,
      layersInputData?: FileUpload,
      projectionParameters?: Parameters,
      trialRun?: boolean,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<Blob>> /* edited */ {
      return RunHCSVProjectionApiFp(configuration)
        .projectionHcsvPostForm(
          polygonInputData,
          layersInputData,
          projectionParameters,
          trialRun,
          options,
        )
        .then((request) => request(axios))
    },
  }
}

export class RunHCSVProjectionApi extends BaseAPI {
  public async projectionHcsvPostForm(
    polygonInputData?: FileUpload,
    layersInputData?: FileUpload,
    projectionParameters?: Parameters,
    trialRun?: boolean,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<Blob>> /* edited */ {
    return RunHCSVProjectionApiFp(this.configuration)
      .projectionHcsvPostForm(
        polygonInputData,
        layersInputData,
        projectionParameters,
        trialRun,
        options,
      )
      .then((request) => request(this.axios))
  }
}
