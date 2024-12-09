/* tslint:disable */
/* eslint-disable */
import globalAxios from 'axios'
import type { AxiosResponse, AxiosInstance, AxiosRequestConfig } from 'axios'
import { Configuration } from '../configuration'
// Some imports not used depending on template conditions
// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS, BaseAPI, RequiredError } from '../base'
import type { RequestArgs } from '../base'
/**
 * RootEndpointApi - axios parameter creator
 * @export
 */
export const RootEndpointApiAxiosParamCreator = function (
  configuration?: Configuration,
) {
  return {
    /**
     *
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    v8Get: async (options: AxiosRequestConfig = {}): Promise<RequestArgs> => {
      const localVarPath = `/api/v8` /* edited */
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
 * RootEndpointApi - functional programming interface
 * @export
 */
export const RootEndpointApiFp = function (configuration?: Configuration) {
  return {
    /**
     *
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async v8Get(
      options?: AxiosRequestConfig,
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => Promise<AxiosResponse<void>>
    > {
      const localVarAxiosArgs =
        await RootEndpointApiAxiosParamCreator(configuration).v8Get(options)
      return (
        axios: AxiosInstance = globalAxios,
        basePath: string = BASE_PATH,
      ) => {
        const axiosRequestArgs: AxiosRequestConfig = {
          ...localVarAxiosArgs.options,
          url: /* edited */ localVarAxiosArgs.url,
        }
        return axios.request(axiosRequestArgs)
      }
    },
  }
}

/**
 * RootEndpointApi - factory interface
 * @export
 */
export const RootEndpointApiFactory = function (
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
    async v8Get(options?: AxiosRequestConfig): Promise<AxiosResponse<void>> {
      return RootEndpointApiFp(configuration)
        .v8Get(options)
        .then((request) => request(axios /* edited */))
    },
  }
}

/**
 * RootEndpointApi - object-oriented interface
 * @export
 * @class RootEndpointApi
 * @extends {BaseAPI}
 */
export class RootEndpointApi extends BaseAPI {
  /**
   *
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof RootEndpointApi
   */
  public async v8Get(
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<void>> {
    return RootEndpointApiFp(this.configuration)
      .v8Get(options)
      .then((request) => request(this.axios /* edited */))
  }
}
