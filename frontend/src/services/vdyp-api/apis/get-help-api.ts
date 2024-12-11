/* tslint:disable */
/* eslint-disable */
import globalAxios from 'axios'
import type { AxiosResponse, AxiosInstance, AxiosRequestConfig } from 'axios'
import { Configuration } from '../configuration'
// Some imports not used depending on template conditions
// @ts-ignore
import { BASE_PATH, BaseAPI, RequiredError } from '../base'
import type { RequestArgs } from '../base'
import type { ParameterDetailsMessage } from '../models'
/**
 * GetHelpApi - axios parameter creator
 * @export
 */
export const GetHelpApiAxiosParamCreator = function (
  configuration?: Configuration,
) {
  return {
    /**
     *
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    helpGet: async (options: AxiosRequestConfig = {}): Promise<RequestArgs> => {
      const localVarPath = `/api/v8/help`
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, 'https://example.com')
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
      let headersFromBaseOptions =
        baseOptions && baseOptions.headers ? baseOptions.headers : {}
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

/**
 * GetHelpApi - functional programming interface
 * @export
 */
export const GetHelpApiFp = function (configuration?: Configuration) {
  return {
    /**
     *
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async helpGet(
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<ParameterDetailsMessage[] /* edited */>>
    > {
      const localVarAxiosArgs =
        await GetHelpApiAxiosParamCreator(configuration).helpGet(options)
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

/**
 * GetHelpApi - factory interface
 * @export
 */
export const GetHelpApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance,
) {
  return {
    /**
     *
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async helpGet(
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<ParameterDetailsMessage[] /* edited */>> {
      return GetHelpApiFp(configuration)
        .helpGet(options)
        .then((request) => request(axios, basePath))
    },
  }
}

/**
 * GetHelpApi - object-oriented interface
 * @export
 * @class GetHelpApi
 * @extends {BaseAPI}
 */
export class GetHelpApi extends BaseAPI {
  /**
   *
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof GetHelpApi
   */
  public async helpGet(
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<ParameterDetailsMessage[] /* edited */>> {
    return GetHelpApiFp(this.configuration)
      .helpGet(options)
      .then((request) => request(this.axios, this.basePath))
  }
}
