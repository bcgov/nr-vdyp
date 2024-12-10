/* tslint:disable */
/* eslint-disable */
import globalAxios from 'axios'
import type { AxiosResponse, AxiosInstance, AxiosRequestConfig } from 'axios'
import { Configuration } from '../configuration'
// Some imports not used depending on template conditions
// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS, BaseAPI, RequiredError } from '../base'
import type { RequestArgs } from '../base'
import type {
  ProjectionDcsvPostRequest,
  ProjectionHcsvPostRequest,
  ProjectionScsvPostRequest,
} from '../models'

/**
 * ProjectionEndpointApi - axios parameter creator
 * @export
 */
export const ProjectionEndpointApiAxiosParamCreator = function (
  configuration?: Configuration,
) {
  return {
    /**
     *
     * @param {ProjectionDcsvPostRequest} [body]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    v8ProjectionDcsvPost: async (
      body?: ProjectionDcsvPostRequest,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/v8/projection/dcsv`
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

      localVarHeaderParameter['Content-Type'] = 'application/json'

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
      const needsSerialization =
        typeof body !== 'string' ||
        (localVarRequestOptions.headers &&
          localVarRequestOptions.headers['Content-Type'] === 'application/json')
      localVarRequestOptions.data = needsSerialization
        ? JSON.stringify(body !== undefined ? body : {})
        : body || ''

      return {
        url:
          localVarUrlObj.pathname + localVarUrlObj.search + localVarUrlObj.hash,
        options: localVarRequestOptions,
      }
    },
    /**
     *
     * @param {ProjectionHcsvPostRequest} [body]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    v8ProjectionHcsvPost: async (
      body?: ProjectionHcsvPostRequest,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/api/v8/projection/hcsv` /* edited */
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
        responseType: 'blob' /* edited */,
      }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any

      localVarHeaderParameter['Content-Type'] = 'application/json'

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
      const needsSerialization =
        typeof body !== 'string' ||
        (localVarRequestOptions.headers &&
          localVarRequestOptions.headers['Content-Type'] === 'application/json')
      localVarRequestOptions.data = needsSerialization
        ? JSON.stringify(body !== undefined ? body : {})
        : body || ''

      return {
        url:
          localVarUrlObj.pathname + localVarUrlObj.search + localVarUrlObj.hash,
        options: localVarRequestOptions,
      }
    },
    /**
     *
     * @param {ProjectionScsvPostRequest} [body]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    v8ProjectionScsvPost: async (
      body?: ProjectionScsvPostRequest,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/v8/projection/scsv`
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

      localVarHeaderParameter['Content-Type'] = 'application/json'

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
      const needsSerialization =
        typeof body !== 'string' ||
        (localVarRequestOptions.headers &&
          localVarRequestOptions.headers['Content-Type'] === 'application/json')
      localVarRequestOptions.data = needsSerialization
        ? JSON.stringify(body !== undefined ? body : {})
        : body || ''

      return {
        url:
          localVarUrlObj.pathname + localVarUrlObj.search + localVarUrlObj.hash,
        options: localVarRequestOptions,
      }
    },
  }
}

/**
 * ProjectionEndpointApi - functional programming interface
 * @export
 */
export const ProjectionEndpointApiFp = function (
  configuration?: Configuration,
) {
  return {
    /**
     *
     * @param {ProjectionDcsvPostRequest} [body]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async v8ProjectionDcsvPost(
      body?: ProjectionDcsvPostRequest,
      options?: AxiosRequestConfig,
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => Promise<AxiosResponse<void>>
    > {
      const localVarAxiosArgs = await ProjectionEndpointApiAxiosParamCreator(
        configuration,
      ).v8ProjectionDcsvPost(body, options)
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
    /**
     *
     * @param {ProjectionHcsvPostRequest} [body]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async v8ProjectionHcsvPost(
      body?: ProjectionHcsvPostRequest,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<Blob>> /* edited */
    > {
      const localVarAxiosArgs = await ProjectionEndpointApiAxiosParamCreator(
        configuration,
      ).v8ProjectionHcsvPost(body, options)
      return (
        axios: AxiosInstance = globalAxios,
        basePath: string = BASE_PATH,
      ) => {
        const axiosRequestArgs: AxiosRequestConfig = {
          ...localVarAxiosArgs.options,
          url: /* edited */ localVarAxiosArgs.url,
        }
        return axios.request<Blob>(axiosRequestArgs)
      }
    },
    /**
     *
     * @param {ProjectionScsvPostRequest} [body]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async v8ProjectionScsvPost(
      body?: ProjectionScsvPostRequest,
      options?: AxiosRequestConfig,
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => Promise<AxiosResponse<void>>
    > {
      const localVarAxiosArgs = await ProjectionEndpointApiAxiosParamCreator(
        configuration,
      ).v8ProjectionScsvPost(body, options)
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
 * ProjectionEndpointApi - factory interface
 * @export
 */
export const ProjectionEndpointApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance,
) {
  return {
    /**
     *
     * @param {ProjectionDcsvPostRequest} [body]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async v8ProjectionDcsvPost(
      body?: ProjectionDcsvPostRequest,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<void>> {
      return ProjectionEndpointApiFp(configuration)
        .v8ProjectionDcsvPost(body, options)
        .then((request) => request(axios, basePath))
    },
    /**
     *
     * @param {ProjectionHcsvPostRequest} [body]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async v8ProjectionHcsvPost(
      body?: ProjectionHcsvPostRequest,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<Blob>> {
      return ProjectionEndpointApiFp(configuration)
        .v8ProjectionHcsvPost(body, options)
        .then((request) => request(axios /* edited */))
    },
    /**
     *
     * @param {ProjectionScsvPostRequest} [body]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async v8ProjectionScsvPost(
      body?: ProjectionScsvPostRequest,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<void>> {
      return ProjectionEndpointApiFp(configuration)
        .v8ProjectionScsvPost(body, options)
        .then((request) => request(axios, basePath))
    },
  }
}

/**
 * ProjectionEndpointApi - object-oriented interface
 * @export
 * @class ProjectionEndpointApi
 * @extends {BaseAPI}
 */
export class ProjectionEndpointApi extends BaseAPI {
  /**
   *
   * @param {ProjectionDcsvPostRequest} [body]
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof ProjectionEndpointApi
   */
  public async v8ProjectionDcsvPost(
    body?: ProjectionDcsvPostRequest,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<void>> {
    return ProjectionEndpointApiFp(this.configuration)
      .v8ProjectionDcsvPost(body, options)
      .then((request) => request(this.axios, this.basePath))
  }
  /**
   *
   * @param {ProjectionHcsvPostRequest} [body]
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof ProjectionEndpointApi
   */
  public async v8ProjectionHcsvPost(
    body?: ProjectionHcsvPostRequest,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<Blob>> /* edited */ {
    return ProjectionEndpointApiFp(this.configuration)
      .v8ProjectionHcsvPost(body, options)
      .then((request) => request(this.axios /* edited */))
  }
  /**
   *
   * @param {ProjectionScsvPostRequest} [body]
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof ProjectionEndpointApi
   */
  public async v8ProjectionScsvPost(
    body?: ProjectionScsvPostRequest,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<void>> {
    return ProjectionEndpointApiFp(this.configuration)
      .v8ProjectionScsvPost(body, options)
      .then((request) => request(this.axios, this.basePath))
  }
}
