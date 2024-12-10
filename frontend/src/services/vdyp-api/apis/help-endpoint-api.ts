/* tslint:disable */
/* eslint-disable */
import globalAxios from 'axios'
import type { AxiosResponse, AxiosInstance, AxiosRequestConfig } from 'axios'
import { Configuration } from '../configuration'
// Some imports not used depending on template conditions
// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS, BaseAPI, RequiredError } from '../base'
import type { RequestArgs } from '../base'
import type { ParameterDetailsMessage } from '../models'

/**
 * HelpEndpointApi - axios parameter creator
 * @export
 */
export const HelpEndpointApiAxiosParamCreator = function (
  configuration?: Configuration,
) {
  return {
    /**
     *
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    v8HelpGet: async (
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/api/v8/help` /* edited */
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
        responseType: 'json' /* edited */,
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
 * HelpEndpointApi - functional programming interface
 * @export
 */
export const HelpEndpointApiFp = function (configuration?: Configuration) {
  return {
    /**
     *
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async v8HelpGet(
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<ParameterDetailsMessage[] /* edited */>>
    > {
      const localVarAxiosArgs =
        await HelpEndpointApiAxiosParamCreator(configuration).v8HelpGet(options)
      return (
        axios: AxiosInstance = globalAxios,
        basePath: string = BASE_PATH,
      ) => {
        const axiosRequestArgs: AxiosRequestConfig = {
          ...localVarAxiosArgs.options,
          url: /* edited */ localVarAxiosArgs.url,
        }
        return axios.request<ParameterDetailsMessage[] /* edited */>(
          axiosRequestArgs,
        )
      }
    },
  }
}

/**
 * HelpEndpointApi - factory interface
 * @export
 */
export const HelpEndpointApiFactory = function (
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
    async v8HelpGet(
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<ParameterDetailsMessage[] /* edited */>> {
      return HelpEndpointApiFp(configuration)
        .v8HelpGet(options)
        .then((request) => request(axios /* edited */))
    },
  }
}

/**
 * HelpEndpointApi - object-oriented interface
 * @export
 * @class HelpEndpointApi
 * @extends {BaseAPI}
 */
export class HelpEndpointApi extends BaseAPI {
  /**
   *
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof HelpEndpointApi
   */
  public async v8HelpGet(
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<ParameterDetailsMessage[] /* edited */>> {
    return HelpEndpointApiFp(this.configuration)
      .v8HelpGet(options)
      .then((request) => request(this.axios /* edited */))
  }
}
