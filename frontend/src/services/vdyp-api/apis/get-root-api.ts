/* tslint:disable */
/* eslint-disable */
import globalAxios from 'axios'
import type { AxiosResponse, AxiosInstance, AxiosRequestConfig } from 'axios'
import { Configuration } from '../configuration'
// Some imports not used depending on template conditions
// @ts-ignore
import { BASE_PATH, BaseAPI, RequiredError } from '../base'
import type { RequestArgs } from '../base'
import type { RootResource } from '../models'

/**
 * GetRootApi - axios parameter creator
 * @export
 */
export const GetRootApiAxiosParamCreator = function (
  configuration?: Configuration,
) {
  return {
    /**
     *
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    rootGet: async (options: AxiosRequestConfig = {}): Promise<RequestArgs> => {
      const localVarPath = `/api/v8`
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
 * GetRootApi - functional programming interface
 * @export
 */
export const GetRootApiFp = function (configuration?: Configuration) {
  return {
    /**
     *
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async rootGet(
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<RootResource>> /* edited */
    > {
      const localVarAxiosArgs =
        await GetRootApiAxiosParamCreator(configuration).rootGet(options)
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
 * GetRootApi - factory interface
 * @export
 */
export const GetRootApiFactory = function (
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
    async rootGet(
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<RootResource>> /* edited */ {
      return GetRootApiFp(configuration)
        .rootGet(options)
        .then((request) => request(axios, basePath))
    },
  }
}

/**
 * GetRootApi - object-oriented interface
 * @export
 * @class GetRootApi
 * @extends {BaseAPI}
 */
export class GetRootApi extends BaseAPI {
  /**
   *
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof GetRootApi
   */
  public async rootGet(
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<RootResource>> /* edited */ {
    return GetRootApiFp(this.configuration)
      .rootGet(options)
      .then((request) => request(this.axios, this.basePath))
  }
}
