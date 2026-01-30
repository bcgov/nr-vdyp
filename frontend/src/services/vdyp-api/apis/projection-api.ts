import globalAxios from 'axios'
import type { AxiosResponse, AxiosInstance, AxiosRequestConfig } from 'axios'
import { Configuration } from '../configuration'
import { BASE_PATH, BaseAPI } from '../base'
import type { RequestArgs } from '../base'
import type {
  ProjectionModel,
  FileMappingModel,
  Parameters,
  FileUpload,
  ModelParameters,
} from '../models'
import { ParameterNamesEnum } from '../models'
import { env } from '@/env'

export const ProjectionApiAxiosParamCreator = function (
  configuration?: Configuration,
) {
  return {
    /**
     * Get all projections for the current user
     * @GET /api/v8/projection/me
     */
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
      const localVarHeaderParameter = {} as Record<string, string>
      const localVarQueryParameter = {} as Record<string, string>

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

    /**
     * Create a new empty projection with default parameters
     * @POST /api/v8/projection/new
     */
    createProjection: async (
      parameters: Parameters,
      modelParameters?: ModelParameters,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/api/v8/projection/new`
      const localVarUrlObj = new URL(localVarPath, env.VITE_API_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'POST',
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as Record<string, string>

      const formData = new FormData()
      formData.append(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
        new Blob([JSON.stringify(parameters)], { type: 'application/json' }),
      )

      if (modelParameters) {
        formData.append(
          ParameterNamesEnum.MODEL_PARAMETERS,
          new Blob([JSON.stringify(modelParameters)], { type: 'application/json' }),
        )
      }

      localVarHeaderParameter['Content-Type'] = 'multipart/form-data'

      const headersFromBaseOptions = baseOptions?.headers ?? {}
      localVarRequestOptions.headers = {
        ...localVarHeaderParameter,
        ...headersFromBaseOptions,
        ...options.headers,
      }
      localVarRequestOptions.data = formData

      // Debug: Log authentication token info
      console.log('=== createProjection API Debug ===')
      console.log('URL:', localVarUrlObj.pathname)
      console.log('baseOptions:', baseOptions)
      console.log('baseOptions.headers:', baseOptions?.headers)
      console.log('options.headers:', options.headers)
      console.log('Final headers:', localVarRequestOptions.headers)
      const authHeader =
        localVarRequestOptions.headers?.Authorization ||
        localVarRequestOptions.headers?.authorization ||
        ''
      console.log('Authorization header:', authHeader || 'NOT FOUND')

      return {
        url:
          localVarUrlObj.pathname + localVarUrlObj.search + localVarUrlObj.hash,
        options: localVarRequestOptions,
      }
    },

    /**
     * Get projection details by ID
     * @GET /api/v8/projection/{projectionGUID}
     */
    getProjection: async (
      projectionGUID: string,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      if (projectionGUID === null || projectionGUID === undefined) {
        throw new Error(
          'Required parameter projectionGUID was null or undefined.',
        )
      }
      const localVarPath = `/api/v8/projection/${encodeURIComponent(projectionGUID)}`
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
      const localVarHeaderParameter = {} as Record<string, string>
      const localVarQueryParameter = {} as Record<string, string>

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

    /**
     * Update projection parameters
     * @PUT /api/v8/projection/{projectionGUID}/params
     */
    updateProjectionParams: async (
      projectionGUID: string,
      parameters: Parameters,
      modelParameters?: ModelParameters,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      if (projectionGUID === null || projectionGUID === undefined) {
        throw new Error(
          'Required parameter projectionGUID was null or undefined.',
        )
      }
      const localVarPath = `/api/v8/projection/${encodeURIComponent(projectionGUID)}/params`
      const localVarUrlObj = new URL(localVarPath, env.VITE_API_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'PUT',
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as Record<string, string>

      const formData = new FormData()
      formData.append(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
        new Blob([JSON.stringify(parameters)], { type: 'application/json' }),
      )

      if (modelParameters) {
        formData.append(
          ParameterNamesEnum.MODEL_PARAMETERS,
          new Blob([JSON.stringify(modelParameters)], { type: 'application/json' }),
        )
      }

      localVarHeaderParameter['Content-Type'] = 'multipart/form-data'

      const headersFromBaseOptions = baseOptions?.headers ?? {}
      localVarRequestOptions.headers = {
        ...localVarHeaderParameter,
        ...headersFromBaseOptions,
        ...options.headers,
      }
      localVarRequestOptions.data = formData

      return {
        url:
          localVarUrlObj.pathname + localVarUrlObj.search + localVarUrlObj.hash,
        options: localVarRequestOptions,
      }
    },

    /**
     * Delete a projection
     * @DELETE /api/v8/projection/{projectionGUID}
     */
    deleteProjection: async (
      projectionGUID: string,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      if (projectionGUID === null || projectionGUID === undefined) {
        throw new Error(
          'Required parameter projectionGUID was null or undefined.',
        )
      }
      const localVarPath = `/api/v8/projection/${encodeURIComponent(projectionGUID)}`
      const localVarUrlObj = new URL(localVarPath, env.VITE_API_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'DELETE',
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as Record<string, string>

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

    /**
     * Get all files in a fileset for a projection
     * @GET /api/v8/projection/{projectionGUID}/fileset/{fileSetGUID}
     */
    getFileSetFiles: async (
      projectionGUID: string,
      fileSetGUID: string,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      if (projectionGUID === null || projectionGUID === undefined) {
        throw new Error(
          'Required parameter projectionGUID was null or undefined.',
        )
      }
      if (fileSetGUID === null || fileSetGUID === undefined) {
        throw new Error(
          'Required parameter fileSetGUID was null or undefined.',
        )
      }
      const localVarPath = `/api/v8/projection/${encodeURIComponent(projectionGUID)}/fileset/${encodeURIComponent(fileSetGUID)}`
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
      const localVarHeaderParameter = {} as Record<string, string>
      const localVarQueryParameter = {} as Record<string, string>

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

    /**
     * Upload a file to a fileset
     * @POST /api/v8/projection/{projectionGUID}/fileset/{fileSetGUID}/file
     */
    uploadFileToFileSet: async (
      projectionGUID: string,
      fileSetGUID: string,
      file: File,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      if (projectionGUID === null || projectionGUID === undefined) {
        throw new Error(
          'Required parameter projectionGUID was null or undefined.',
        )
      }
      if (fileSetGUID === null || fileSetGUID === undefined) {
        throw new Error(
          'Required parameter fileSetGUID was null or undefined.',
        )
      }
      if (file === null || file === undefined) {
        throw new Error('Required parameter file was null or undefined.')
      }
      const localVarPath = `/api/v8/projection/${encodeURIComponent(projectionGUID)}/fileset/${encodeURIComponent(fileSetGUID)}/file`
      const localVarUrlObj = new URL(localVarPath, env.VITE_API_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'POST',
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as Record<string, string>

      const formData = new FormData()
      formData.append('file', file)

      localVarHeaderParameter['Content-Type'] = 'multipart/form-data'

      const headersFromBaseOptions = baseOptions?.headers ?? {}
      localVarRequestOptions.headers = {
        ...localVarHeaderParameter,
        ...headersFromBaseOptions,
        ...options.headers,
      }
      localVarRequestOptions.data = formData

      return {
        url:
          localVarUrlObj.pathname + localVarUrlObj.search + localVarUrlObj.hash,
        options: localVarRequestOptions,
      }
    },

    /**
     * Get a file for download with presigned URL
     * @GET /api/v8/projection/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}
     */
    getFileForDownload: async (
      projectionGUID: string,
      fileSetGUID: string,
      fileMappingGUID: string,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      if (projectionGUID === null || projectionGUID === undefined) {
        throw new Error(
          'Required parameter projectionGUID was null or undefined.',
        )
      }
      if (fileSetGUID === null || fileSetGUID === undefined) {
        throw new Error(
          'Required parameter fileSetGUID was null or undefined.',
        )
      }
      if (fileMappingGUID === null || fileMappingGUID === undefined) {
        throw new Error(
          'Required parameter fileMappingGUID was null or undefined.',
        )
      }
      const localVarPath = `/api/v8/projection/${encodeURIComponent(projectionGUID)}/fileset/${encodeURIComponent(fileSetGUID)}/file/${encodeURIComponent(fileMappingGUID)}`
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
      const localVarHeaderParameter = {} as Record<string, string>
      const localVarQueryParameter = {} as Record<string, string>

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

    /**
     * Delete a file from a fileset
     * @DELETE /api/v8/projection/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}
     */
    deleteFileFromFileSet: async (
      projectionGUID: string,
      fileSetGUID: string,
      fileMappingGUID: string,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      if (projectionGUID === null || projectionGUID === undefined) {
        throw new Error(
          'Required parameter projectionGUID was null or undefined.',
        )
      }
      if (fileSetGUID === null || fileSetGUID === undefined) {
        throw new Error(
          'Required parameter fileSetGUID was null or undefined.',
        )
      }
      if (fileMappingGUID === null || fileMappingGUID === undefined) {
        throw new Error(
          'Required parameter fileMappingGUID was null or undefined.',
        )
      }
      const localVarPath = `/api/v8/projection/${encodeURIComponent(projectionGUID)}/fileset/${encodeURIComponent(fileSetGUID)}/file/${encodeURIComponent(fileMappingGUID)}`
      const localVarUrlObj = new URL(localVarPath, env.VITE_API_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'DELETE',
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as Record<string, string>

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

    /**
     * Run a projection (send to batch processing)
     * @POST /api/v8/projection/{projectionGUID}/run
     */
    runProjection: async (
      projectionGUID: string,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      if (projectionGUID === null || projectionGUID === undefined) {
        throw new Error(
          'Required parameter projectionGUID was null or undefined.',
        )
      }
      const localVarPath = `/api/v8/projection/${encodeURIComponent(projectionGUID)}/run`
      const localVarUrlObj = new URL(localVarPath, env.VITE_API_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }
      const localVarRequestOptions: AxiosRequestConfig = {
        method: 'POST',
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as Record<string, string>

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

    /**
     * Run HCSV projection
     * @POST /api/v8/projection/hcsv
     */
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
        ...baseOptions,
        ...options,
      }
      const localVarHeaderParameter = {} as Record<string, string>
      const localVarQueryParameter = {} as Record<string, string>
      const localVarFormParams = new FormData()

      if (trialRun !== undefined) {
        localVarQueryParameter[ParameterNamesEnum.TRIAL_RUN] = String(trialRun)
      }

      if (polygonInputData !== undefined) {
        const polygonFile = polygonInputData as unknown as File
        console.debug(
          `Polygon Input Data - name: ${polygonFile.name}, size: ${polygonFile.size}, type: ${polygonFile.type}`,
        )
        localVarFormParams.append(
          ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA,
          polygonFile as Blob,
        )
      }

      if (layersInputData !== undefined) {
        const layersFile = layersInputData as unknown as File
        console.debug(
          `Layers Input Data - name: ${layersFile.name}, size: ${layersFile.size}, type: ${layersFile.type}`,
        )
        localVarFormParams.append(
          ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA,
          layersFile as Blob,
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
          projectionParameters as Blob,
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
      const headersFromBaseOptions = baseOptions?.headers ?? {}
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

export const ProjectionApiFp = function (configuration?: Configuration) {
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
        await ProjectionApiAxiosParamCreator(configuration).getUserProjections(
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

    async createProjection(
      parameters: Parameters,
      modelParameters?: ModelParameters,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<ProjectionModel>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(configuration).createProjection(
          parameters,
          modelParameters,
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

    async getProjection(
      projectionGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<ProjectionModel>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(configuration).getProjection(
          projectionGUID,
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

    async updateProjectionParams(
      projectionGUID: string,
      parameters: Parameters,
      modelParameters?: ModelParameters,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<ProjectionModel>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(
          configuration,
        ).updateProjectionParams(projectionGUID, parameters, modelParameters, options)
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

    async deleteProjection(
      projectionGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<void>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(configuration).deleteProjection(
          projectionGUID,
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

    async getFileSetFiles(
      projectionGUID: string,
      fileSetGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<FileMappingModel[]>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(configuration).getFileSetFiles(
          projectionGUID,
          fileSetGUID,
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

    async uploadFileToFileSet(
      projectionGUID: string,
      fileSetGUID: string,
      file: File,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<ProjectionModel>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(configuration).uploadFileToFileSet(
          projectionGUID,
          fileSetGUID,
          file,
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

    async getFileForDownload(
      projectionGUID: string,
      fileSetGUID: string,
      fileMappingGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<FileMappingModel>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(configuration).getFileForDownload(
          projectionGUID,
          fileSetGUID,
          fileMappingGUID,
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

    async deleteFileFromFileSet(
      projectionGUID: string,
      fileSetGUID: string,
      fileMappingGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<void>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(
          configuration,
        ).deleteFileFromFileSet(
          projectionGUID,
          fileSetGUID,
          fileMappingGUID,
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

    async runProjection(
      projectionGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<
      (
        axios?: AxiosInstance,
        basePath?: string,
      ) => Promise<AxiosResponse<ProjectionModel>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(configuration).runProjection(
          projectionGUID,
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

    async projectionHcsvPostForm(
      polygonInputData?: FileUpload,
      layersInputData?: FileUpload,
      projectionParameters?: Parameters,
      trialRun?: boolean,
      options?: AxiosRequestConfig,
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => Promise<AxiosResponse<Blob>>
    > {
      const localVarAxiosArgs =
        await ProjectionApiAxiosParamCreator(
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
        }
        return axios.request(axiosRequestArgs).catch((error) => {
          console.error(
            'Backend API error:',
            error.response ? error.response.data : error.message,
          )
          throw error
        })
      }
    },
  }
}

export const ProjectionApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance,
) {
  return {
    async getUserProjections(
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<ProjectionModel[]>> {
      return ProjectionApiFp(configuration)
        .getUserProjections(options)
        .then((request) => request(axios, basePath))
    },

    async createProjection(
      parameters: Parameters,
      modelParameters?: ModelParameters,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<ProjectionModel>> {
      return ProjectionApiFp(configuration)
        .createProjection(parameters, modelParameters, options)
        .then((request) => request(axios, basePath))
    },

    async getProjection(
      projectionGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<ProjectionModel>> {
      return ProjectionApiFp(configuration)
        .getProjection(projectionGUID, options)
        .then((request) => request(axios, basePath))
    },

    async updateProjectionParams(
      projectionGUID: string,
      parameters: Parameters,
      modelParameters?: ModelParameters,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<ProjectionModel>> {
      return ProjectionApiFp(configuration)
        .updateProjectionParams(projectionGUID, parameters, modelParameters, options)
        .then((request) => request(axios, basePath))
    },

    async deleteProjection(
      projectionGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<void>> {
      return ProjectionApiFp(configuration)
        .deleteProjection(projectionGUID, options)
        .then((request) => request(axios, basePath))
    },

    async getFileSetFiles(
      projectionGUID: string,
      fileSetGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<FileMappingModel[]>> {
      return ProjectionApiFp(configuration)
        .getFileSetFiles(projectionGUID, fileSetGUID, options)
        .then((request) => request(axios, basePath))
    },

    async uploadFileToFileSet(
      projectionGUID: string,
      fileSetGUID: string,
      file: File,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<ProjectionModel>> {
      return ProjectionApiFp(configuration)
        .uploadFileToFileSet(projectionGUID, fileSetGUID, file, options)
        .then((request) => request(axios, basePath))
    },

    async getFileForDownload(
      projectionGUID: string,
      fileSetGUID: string,
      fileMappingGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<FileMappingModel>> {
      return ProjectionApiFp(configuration)
        .getFileForDownload(projectionGUID, fileSetGUID, fileMappingGUID, options)
        .then((request) => request(axios, basePath))
    },

    async deleteFileFromFileSet(
      projectionGUID: string,
      fileSetGUID: string,
      fileMappingGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<void>> {
      return ProjectionApiFp(configuration)
        .deleteFileFromFileSet(
          projectionGUID,
          fileSetGUID,
          fileMappingGUID,
          options,
        )
        .then((request) => request(axios, basePath))
    },

    async runProjection(
      projectionGUID: string,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<ProjectionModel>> {
      return ProjectionApiFp(configuration)
        .runProjection(projectionGUID, options)
        .then((request) => request(axios, basePath))
    },

    async projectionHcsvPostForm(
      polygonInputData?: FileUpload,
      layersInputData?: FileUpload,
      projectionParameters?: Parameters,
      trialRun?: boolean,
      options?: AxiosRequestConfig,
    ): Promise<AxiosResponse<Blob>> {
      return ProjectionApiFp(configuration)
        .projectionHcsvPostForm(
          polygonInputData,
          layersInputData,
          projectionParameters,
          trialRun,
          options,
        )
        .then((request) => request(axios, basePath))
    },
  }
}

export class ProjectionApi extends BaseAPI {
  /**
   * Get all projections for the current user
   */
  public async getUserProjections(
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<ProjectionModel[]>> {
    return ProjectionApiFp(this.configuration)
      .getUserProjections(options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Create a new empty projection with default parameters
   */
  public async createProjection(
    parameters: Parameters,
    modelParameters?: ModelParameters,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<ProjectionModel>> {
    return ProjectionApiFp(this.configuration)
      .createProjection(parameters, modelParameters, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Get projection details by ID
   */
  public async getProjection(
    projectionGUID: string,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<ProjectionModel>> {
    return ProjectionApiFp(this.configuration)
      .getProjection(projectionGUID, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Update projection parameters
   */
  public async updateProjectionParams(
    projectionGUID: string,
    parameters: Parameters,
    modelParameters?: ModelParameters,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<ProjectionModel>> {
    return ProjectionApiFp(this.configuration)
      .updateProjectionParams(projectionGUID, parameters, modelParameters, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Delete a projection
   */
  public async deleteProjection(
    projectionGUID: string,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<void>> {
    return ProjectionApiFp(this.configuration)
      .deleteProjection(projectionGUID, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Get all files in a fileset for a projection
   */
  public async getFileSetFiles(
    projectionGUID: string,
    fileSetGUID: string,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<FileMappingModel[]>> {
    return ProjectionApiFp(this.configuration)
      .getFileSetFiles(projectionGUID, fileSetGUID, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Upload a file to a fileset
   */
  public async uploadFileToFileSet(
    projectionGUID: string,
    fileSetGUID: string,
    file: File,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<ProjectionModel>> {
    return ProjectionApiFp(this.configuration)
      .uploadFileToFileSet(projectionGUID, fileSetGUID, file, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Get a file for download with presigned URL
   */
  public async getFileForDownload(
    projectionGUID: string,
    fileSetGUID: string,
    fileMappingGUID: string,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<FileMappingModel>> {
    return ProjectionApiFp(this.configuration)
      .getFileForDownload(projectionGUID, fileSetGUID, fileMappingGUID, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Delete a file from a fileset
   */
  public async deleteFileFromFileSet(
    projectionGUID: string,
    fileSetGUID: string,
    fileMappingGUID: string,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<void>> {
    return ProjectionApiFp(this.configuration)
      .deleteFileFromFileSet(
        projectionGUID,
        fileSetGUID,
        fileMappingGUID,
        options,
      )
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Run a projection (send to batch processing)
   */
  public async runProjection(
    projectionGUID: string,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<ProjectionModel>> {
    return ProjectionApiFp(this.configuration)
      .runProjection(projectionGUID, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Run HCSV projection
   */
  public async projectionHcsvPostForm(
    polygonInputData?: FileUpload,
    layersInputData?: FileUpload,
    projectionParameters?: Parameters,
    trialRun?: boolean,
    options?: AxiosRequestConfig,
  ): Promise<AxiosResponse<Blob>> {
    return ProjectionApiFp(this.configuration)
      .projectionHcsvPostForm(
        polygonInputData,
        layersInputData,
        projectionParameters,
        trialRun,
        options,
      )
      .then((request) => request(this.axios, this.basePath))
  }
}
