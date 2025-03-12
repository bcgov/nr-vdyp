import globalAxios from 'axios'
import type { AxiosResponse, AxiosInstance, AxiosRequestConfig } from 'axios'
import { Configuration } from '../configuration'
import { BASE_PATH, BaseAPI } from '../base'
import type { RequestArgs } from '../base'
import type { FileUpload, Parameters } from '../models'
import { ParameterNamesEnum } from '../models'
import { env } from '@/env'

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
      const localVarUrlObj = new URL(localVarPath, env.VITE_API_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'POST',
        headers: {
          Accept: 'application/octet-stream',
          'Content-Type': 'multipart/form-data',
        },
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any
      const localVarFormParams = new FormData()

      if (trialRun !== undefined) {
        localVarQueryParameter[ParameterNamesEnum.TRIAL_RUN] = trialRun
      }

      if (polygonInputData !== undefined) {
        const polygonFile = polygonInputData as unknown as File
        console.debug(
          `Polygon Input Data - name: ${polygonFile.name}, size: ${polygonFile.size}, type: ${polygonFile.type}`,
        )

        localVarFormParams.append(
          ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA,
          polygonFile as any,
        )
      }

      if (layersInputData !== undefined) {
        const layersFile = layersInputData as unknown as File
        console.debug(
          `Layers Input Data - name: ${layersFile.name}, size: ${layersFile.size}, type: ${layersFile.type}`,
        )

        localVarFormParams.append(
          ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA,
          layersFile as any,
        )
      }

      if (projectionParameters !== undefined) {
        if (projectionParameters instanceof Blob) {
          projectionParameters.text().then((text) => {
            console.debug(`Projection Parameters: ${text}`)
          })
        } else {
          console.debug(
            `Projection Parameters: ${JSON.stringify(projectionParameters)}`,
          )
        }

        localVarFormParams.append(
          ParameterNamesEnum.PROJECTION_PARAMETERS,
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
      const headersFromBaseOptions =
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
      (axios?: AxiosInstance, basePath?: string) => Promise<AxiosResponse<Blob>>
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
          url: basePath + localVarAxiosArgs.url,
          responseType: 'blob',
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
    ): Promise<AxiosResponse<Blob>> {
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
  ): Promise<AxiosResponse<Blob>> {
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
