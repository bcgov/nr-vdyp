import apiClient from '@/services/apiClient'
import type {
  ParameterDetailsMessage,
  ProjectionModel,
  RootResource,
  Parameters,
  FileMappingModel,
} from '@/services/vdyp-api'

/**
 * Fetches help details from the API.
 * @returns A promise that resolves to an array of ParameterDetailsMessage.
 */
export const helpGet = async (): Promise<ParameterDetailsMessage[]> => {
  try {
    const response = await apiClient.helpGet()
    return response.data
  } catch (error) {
    console.error('Error fetching help details:', error)
    throw error
  }
}

/**
 * Sends a projection HCSV POST request to the API using the provided FormData.
 * Returns the full Axios response including headers and data.
 * @param formData The FormData containing necessary files and parameters.
 * @param trialRun Boolean flag indicating if this is a trial run (default is false).
 * @returns A promise that resolves to the full Axios response.
 */
export const projectionHcsvPost = async (
  formData: FormData,
  trialRun: boolean = false,
): Promise<any> => {
  try {
    const response = await apiClient.projectionHcsvPost(formData, trialRun)

    // Check status and content type
    if (
      (response.status === 200 || response.status === 201) &&
      response.headers['content-type']?.includes('application/octet-stream')
    ) {
      // Success response, returns a blob
      return response
    } else if (
      response.status === 400 &&
      response.headers['content-type']?.includes('application/json')
    ) {
      // Errors including validation messages
      const errorData = await response.data.text()
      throw new Error(errorData)
    } else {
      throw new Error(`Unexpected response: ${response.status}`)
    }
  } catch (error) {
    console.error('Error running projection:', {
      message: (error as any).message,
      code: (error as any).code,
      response: (error as any).response,
      request: (error as any).request,
      config: (error as any).config,
    })
    throw error
  }
}

/**
 * Fetches root details from the API.
 * @returns A promise that resolves to a RootResource object.
 */
export const rootGet = async (): Promise<RootResource> => {
  try {
    const response = await apiClient.rootGet()
    return response.data
  } catch (error) {
    console.error('Error fetching root details:', error)
    throw error
  }
}

/**
 * Fetches all projections for the authenticated user.
 * @returns A promise that resolves to an array of ProjectionModel.
 */
export const getUserProjections = async (): Promise<ProjectionModel[]> => {
  try {
    const response = await apiClient.getUserProjections()
    return response.data
  } catch (error) {
    console.error('Error fetching user projections:', error)
    throw error
  }
}

/**
 * Creates a new empty projection with default parameters.
 * @param parameters The projection parameters.
 * @returns A promise that resolves to the created ProjectionModel.
 */
export const createProjection = async (
  parameters: Parameters,
): Promise<ProjectionModel> => {
  try {
    const response = await apiClient.createProjection(parameters)
    return response.data
  } catch (error) {
    console.error('Error creating projection:', error)
    throw error
  }
}

/**
 * Runs a projection by sending it to batch processing.
 * @param projectionGUID The projection GUID.
 * @returns A promise that resolves to the updated ProjectionModel with RUNNING status.
 */
export const runProjection = async (
  projectionGUID: string,
): Promise<ProjectionModel> => {
  try {
    const response = await apiClient.runProjection(projectionGUID)
    return response.data
  } catch (error) {
    console.error('Error running projection:', error)
    throw error
  }
}

/**
 * Fetches a projection by its GUID.
 * @param projectionGUID The projection GUID.
 * @returns A promise that resolves to the ProjectionModel.
 */
export const getProjection = async (
  projectionGUID: string,
): Promise<ProjectionModel> => {
  try {
    const response = await apiClient.getProjection(projectionGUID)
    return response.data
  } catch (error) {
    console.error('Error fetching projection:', error)
    throw error
  }
}

/**
 * Updates projection parameters.
 * @param projectionGUID The projection GUID.
 * @param parameters The updated projection parameters.
 * @returns A promise that resolves to the updated ProjectionModel.
 */
export const updateProjectionParams = async (
  projectionGUID: string,
  parameters: Parameters,
): Promise<ProjectionModel> => {
  try {
    const response = await apiClient.updateProjectionParams(
      projectionGUID,
      parameters,
    )
    return response.data
  } catch (error) {
    console.error('Error updating projection parameters:', error)
    throw error
  }
}

/**
 * Deletes a projection.
 * @param projectionGUID The projection GUID.
 * @returns A promise that resolves when the projection is deleted.
 */
export const deleteProjection = async (
  projectionGUID: string,
): Promise<void> => {
  try {
    await apiClient.deleteProjection(projectionGUID)
  } catch (error) {
    console.error('Error deleting projection:', error)
    throw error
  }
}

/**
 * Fetches all files in a fileset for a projection.
 * @param projectionGUID The projection GUID.
 * @param fileSetGUID The fileset GUID.
 * @returns A promise that resolves to an array of FileMappingModel.
 */
export const getFileSetFiles = async (
  projectionGUID: string,
  fileSetGUID: string,
): Promise<FileMappingModel[]> => {
  try {
    const response = await apiClient.getFileSetFiles(
      projectionGUID,
      fileSetGUID,
    )
    return response.data
  } catch (error) {
    console.error('Error fetching fileset files:', error)
    throw error
  }
}

/**
 * Uploads a file to a fileset.
 * @param projectionGUID The projection GUID.
 * @param fileSetGUID The fileset GUID.
 * @param file The file to upload.
 * @returns A promise that resolves to the updated ProjectionModel.
 */
export const uploadFileToFileSet = async (
  projectionGUID: string,
  fileSetGUID: string,
  file: File,
): Promise<ProjectionModel> => {
  try {
    const response = await apiClient.uploadFileToFileSet(
      projectionGUID,
      fileSetGUID,
      file,
    )
    return response.data
  } catch (error) {
    console.error('Error uploading file to fileset:', error)
    throw error
  }
}

/**
 * Gets a file for download with presigned URL.
 * @param projectionGUID The projection GUID.
 * @param fileSetGUID The fileset GUID.
 * @param fileMappingGUID The file mapping GUID.
 * @returns A promise that resolves to the FileMappingModel with download URL.
 */
export const getFileForDownload = async (
  projectionGUID: string,
  fileSetGUID: string,
  fileMappingGUID: string,
): Promise<FileMappingModel> => {
  try {
    const response = await apiClient.getFileForDownload(
      projectionGUID,
      fileSetGUID,
      fileMappingGUID,
    )
    return response.data
  } catch (error) {
    console.error('Error getting file for download:', error)
    throw error
  }
}

/**
 * Deletes a file from a fileset.
 * @param projectionGUID The projection GUID.
 * @param fileSetGUID The fileset GUID.
 * @param fileMappingGUID The file mapping GUID.
 * @returns A promise that resolves when the file is deleted.
 */
export const deleteFileFromFileSet = async (
  projectionGUID: string,
  fileSetGUID: string,
  fileMappingGUID: string,
): Promise<void> => {
  try {
    await apiClient.deleteFileFromFileSet(
      projectionGUID,
      fileSetGUID,
      fileMappingGUID,
    )
  } catch (error) {
    console.error('Error deleting file from fileset:', error)
    throw error
  }
}
