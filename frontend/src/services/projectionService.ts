import {
  getUserProjections,
  createProjection as apiCreateProjection,
  getProjection as apiGetProjection,
  updateProjectionParams as apiUpdateProjectionParams,
  deleteProjection as apiDeleteProjection,
  runProjection as apiRunProjection,
  deleteFileFromFileSet as apiDeleteFileFromFileSet,
  getFileSetFiles as apiGetFileSetFiles,
  getFileForDownload as apiGetFileForDownload,
} from '@/services/apiActions'
import type { ProjectionModel, Parameters, FileMappingModel, ModelParameters } from '@/services/vdyp-api'
import type { Projection, ProjectionStatus, ParsedProjectionParameters } from '@/interfaces/interfaces'
import {
  MODEL_SELECTION,
  PROJECTION_STATUS,
  PROJECTION_TYPE,
} from '@/constants/constants'
import { ExecutionOptionsEnum } from '@/services/vdyp-api'

// ============================================================================
// Projection List
// ============================================================================

/**
 * Maps backend ProjectionStatusCode to frontend ProjectionStatus
 */
export const mapProjectionStatus = (statusCode: string): ProjectionStatus => {
  const statusMap: Record<string, ProjectionStatus> = PROJECTION_STATUS
  return statusMap[statusCode] || PROJECTION_STATUS.DRAFT
}

/**
 * Parses projection parameters JSON string
 */
const parseProjectionParameters = (
  parametersJson: string | null,
): Record<string, unknown> => {
  if (!parametersJson) {
    return {}
  }
  try {
    return JSON.parse(parametersJson)
  } catch {
    return {}
  }
}

/**
 * Determines projection type based on projection parameters
 */
const getProjectionType = (parameters: Record<string, unknown>): string => {
  const selectedOptions = parameters.selectedExecutionOptions as
    | string[]
    | undefined
  if (selectedOptions?.includes(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)) {
    return PROJECTION_TYPE.VOLUME
  }
  if (selectedOptions?.includes(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)) {
    return PROJECTION_TYPE.CFS_BIOMASS
  }
  return ''
}

/**
 * Determines method based on projection parameters
 */
const getMethod = (parameters: Record<string, unknown>): string => {
  const selectedOptions = parameters.selectedExecutionOptions as
    | string[]
    | undefined
  if (selectedOptions?.includes(ExecutionOptionsEnum.DoEnableProjectionReport)) {
    return MODEL_SELECTION.INPUT_MODEL_PARAMETERS
  }
  return MODEL_SELECTION.FILE_UPLOAD
}

/**
 * Transforms a backend ProjectionModel to frontend Projection interface
 */
const transformProjection = (model: ProjectionModel): Projection => {
  const parameters = parseProjectionParameters(model.projectionParameters)

  return {
    projectionGUID: model.projectionGUID,
    title: model.reportTitle || '',
    description: model.reportDescription || '',
    method: getMethod(parameters),
    projectionType: getProjectionType(parameters),
    lastUpdated: model.lastUpdatedDate || '',
    expiration: model.expiryDate || '',
    status: mapProjectionStatus(model.projectionStatusCode?.code || PROJECTION_STATUS.DRAFT),
  }
}

/**
 * Fetches all projections for the authenticated user and transforms them to frontend format
 * @returns A promise that resolves to an array of Projection objects
 */
export const fetchUserProjections = async (): Promise<Projection[]> => {
  try {
    const projectionModels = await getUserProjections()

    // Debug: Log backend response
    // console.log('=== fetchUserProjections Backend Response ===')
    // console.log('Total projections received:', projectionModels.length)
    // projectionModels.forEach((model, index) => {
    //   console.log(`--- Projection ${index + 1} ---`)
    //   console.log('  projectionGUID:', model.projectionGUID)
    //   console.log('  reportTitle:', model.reportTitle)
    //   console.log('  reportDescription:', model.reportDescription)
    //   console.log('  projectionStatusCode:', model.projectionStatusCode)
    //   console.log('  lastUpdatedDate:', model.lastUpdatedDate)
    //   console.log('  expiryDate:', model.expiryDate)
    //   console.log('  projectionParameters:', model.projectionParameters)
    //   console.log('  Full model:', JSON.stringify(model, null, 2))
    // })
    // console.log('=== End fetchUserProjections ===')

    return projectionModels.map((model) => transformProjection(model))
  } catch (error) {
    console.error('Error fetching user projections:', error)
    throw error
  }
}

// ============================================================================
// Projection Create
// ============================================================================

/**
 * Creates a new projection with the given parameters.
 * @param parameters The projection parameters
 * @param modelParameters Optional The model parameters for Manual Input mode
 * @returns A promise that resolves to the created ProjectionModel
 */
export const createProjection = async (
  parameters: Parameters,
  modelParameters?: ModelParameters,
): Promise<ProjectionModel> => {
  try {
    const parametersJson = JSON.stringify(parameters)
    console.log('=== createProjection ===')
    console.log('Parameters JSON length:', parametersJson.length, 'bytes')
    console.log('Parameters:', parametersJson)
    if (modelParameters) {
      console.log('ModelParameters:', JSON.stringify(modelParameters))
    }

    const projectionModel = await apiCreateProjection(parameters, modelParameters)

    console.log('=== createProjection Backend Response ===')
    console.log('projectionParameters:', projectionModel.projectionParameters)
    console.log('modelParameters:', projectionModel.modelParameters)
    console.log('Full projectionModel:', JSON.stringify(projectionModel, null, 2))
    console.log('=== End createProjection ===')

    return projectionModel
  } catch (error) {
    console.error('Error creating projection:', error)
    throw error
  }
}

/**
 * Runs a projection by sending it to batch processing.
 * The projection must be in DRAFT status with valid parameters and uploaded files.
 * @param projectionGUID The projection GUID
 * @returns A promise that resolves to the updated ProjectionModel with RUNNING status
 */
export const runProjection = async (
  projectionGUID: string,
): Promise<ProjectionModel> => {
  try {
    return await apiRunProjection(projectionGUID)
  } catch (error) {
    console.error('Error running projection:', error)
    throw error
  }
}

/**
 * Fetches a projection by its GUID
 * @param projectionGUID The projection GUID
 * @returns A promise that resolves to the ProjectionModel
 */
export const getProjectionById = async (
  projectionGUID: string,
): Promise<ProjectionModel> => {
  try {
    return await apiGetProjection(projectionGUID)
  } catch (error) {
    console.error('Error fetching projection:', error)
    throw error
  }
}

/**
 * Updates only the projection parameters (without model parameters).
 * Use this for basic parameter updates that don't require model parameter changes.
 * @param projectionGUID The projection GUID
 * @param parameters The updated projection parameters
 * @returns A promise that resolves to the updated ProjectionModel
 */
export const updateProjectionParams = async (
  projectionGUID: string,
  parameters: Parameters,
): Promise<ProjectionModel> => {
  try {
    console.log('=== updateProjectionParams ===')
    console.log('org projectionParameters:', JSON.stringify(parameters))

    const updatedProjection = await apiUpdateProjectionParams(projectionGUID, parameters)
    console.log('updated projectionParameters:', updatedProjection.projectionParameters)

    const projectionModel = await apiGetProjection(projectionGUID)

    console.log('get projectionParameters:', projectionModel.projectionParameters)
    // console.log('Full projectionModel:', JSON.stringify(projectionModel, null, 2))

    // Return the updated projection
    return projectionModel
  } catch (error) {
    console.error('Error updating projection parameters:', error)
    throw error
  }
}

/**
 * Updates projection parameters along with model parameters.
 * Use this for Manual Input mode where both projection settings
 * and model-specific parameters need to be updated together.
 * @param projectionGUID The projection GUID
 * @param parameters The updated projection parameters
 * @param modelParameters Optional model parameters for Manual Input mode
 * @returns A promise that resolves to the updated ProjectionModel
 */
export const updateProjectionParamsWithModel = async (
  projectionGUID: string,
  parameters: Parameters,
  modelParameters?: ModelParameters,
): Promise<ProjectionModel> => {
  try {
    console.log('=== updateProjectionParamsWithModel ===')

    console.log('org projectionParameters:', JSON.stringify(parameters))
    console.log('org modelParameters:', JSON.stringify(modelParameters))

    // Step 1: Update projection parameters and model parameters
    const updatedProjection = await apiUpdateProjectionParams(projectionGUID, parameters, modelParameters)
    console.log('updated projectionParameters:', updatedProjection.projectionParameters)
    console.log('updated modelParameters:', updatedProjection.modelParameters)

    // Step 2: Get the updated projection
    const projectionModel = await apiGetProjection(projectionGUID)

    console.log('get projectionParameters:', projectionModel.projectionParameters)
    console.log('get modelParameters:', projectionModel.modelParameters)
    // console.log('Full projectionModel:', JSON.stringify(projectionModel, null, 2))

    // Return the updated projection
    return projectionModel
  } catch (error) {
    console.error('Error updating projection with model parameters:', error)
    throw error
  }
}

/**
 * Deletes a file from a fileset
 * @param projectionGUID The projection GUID
 * @param fileSetGUID The fileset GUID
 * @param fileMappingGUID The file mapping GUID
 * @returns A promise that resolves when the file is deleted
 */
export const deleteFileFromFileSet = async (
  projectionGUID: string,
  fileSetGUID: string,
  fileMappingGUID: string,
): Promise<void> => {
  try {
    await apiDeleteFileFromFileSet(projectionGUID, fileSetGUID, fileMappingGUID)
  } catch (error) {
    console.error('Error deleting file from fileset:', error)
    throw error
  }
}

/**
 * Deletes all files from a fileset
 * @param projectionGUID The projection GUID
 * @param fileSetGUID The fileset GUID
 * @returns A promise that resolves when all files are deleted
 */
export const deleteAllFilesFromFileSet = async (
  projectionGUID: string,
  fileSetGUID: string,
): Promise<void> => {
  try {
    const files = await apiGetFileSetFiles(projectionGUID, fileSetGUID)
    for (const file of files) {
      if (file.fileMappingGUID) {
        await apiDeleteFileFromFileSet(projectionGUID, fileSetGUID, file.fileMappingGUID)
      }
    }
  } catch (error) {
    console.error('Error deleting all files from fileset:', error)
    throw error
  }
}

/**
 * Deletes a projection and all its associated files
 * @param projectionGUID The projection GUID
 * @returns A promise that resolves when the projection and files are deleted
 */
export const deleteProjectionWithFiles = async (
  projectionGUID: string,
): Promise<void> => {
  try {
    // First, get the projection to find the filesets
    const projection = await apiGetProjection(projectionGUID)

    // Delete files from polygon fileset if exists
    if (projection.polygonFileSet?.projectionFileSetGUID) {
      await deleteAllFilesFromFileSet(
        projectionGUID,
        projection.polygonFileSet.projectionFileSetGUID,
      )
    }

    // Delete files from layer fileset if exists
    if (projection.layerFileSet?.projectionFileSetGUID) {
      await deleteAllFilesFromFileSet(
        projectionGUID,
        projection.layerFileSet.projectionFileSetGUID,
      )
    }

    // Finally, delete the projection itself
    await apiDeleteProjection(projectionGUID)
  } catch (error) {
    console.error('Error deleting projection with files:', error)
    throw error
  }
}

// ============================================================================
// File Set Operations
// ============================================================================

/**
 * Fetches all files in a fileset for a projection
 * @param projectionGUID The projection GUID
 * @param fileSetGUID The fileset GUID
 * @returns A promise that resolves to an array of FileMappingModel
 */
export const getFileSetFiles = async (
  projectionGUID: string,
  fileSetGUID: string,
): Promise<FileMappingModel[]> => {
  try {
    return await apiGetFileSetFiles(projectionGUID, fileSetGUID)
  } catch (error) {
    console.error('Error fetching fileset files:', error)
    throw error
  }
}

/**
 * Gets a file for download with presigned URL
 * @param projectionGUID The projection GUID
 * @param fileSetGUID The fileset GUID
 * @param fileMappingGUID The file mapping GUID
 * @returns A promise that resolves to the FileMappingModel with download URL
 */
export const getFileForDownload = async (
  projectionGUID: string,
  fileSetGUID: string,
  fileMappingGUID: string,
): Promise<FileMappingModel> => {
  try {
    return await apiGetFileForDownload(projectionGUID, fileSetGUID, fileMappingGUID)
  } catch (error) {
    console.error('Error getting file for download:', error)
    throw error
  }
}

// ============================================================================
// Projection Parameter Parsing
// ============================================================================

/**
 * Parses the projectionParameters JSON string into a typed object
 * @param parametersJson The JSON string from ProjectionModel.projectionParameters
 * @returns Parsed projection parameters object
 */
export const parseProjectionParams = (
  parametersJson: string | null | undefined,
): ParsedProjectionParameters => {
  const defaults: ParsedProjectionParameters = {
    outputFormat: null,
    selectedExecutionOptions: [],
    selectedDebugOptions: [],
    ageStart: null,
    ageEnd: null,
    yearStart: null,
    yearEnd: null,
    forceYear: null,
    ageIncrement: null,
    metadataToOutput: null,
    filters: null,
    utils: [],
    excludedExecutionOptions: [],
    excludedDebugOptions: [],
    combineAgeYearRange: null,
    progressFrequency: null,
    reportTitle: null,
  }

  if (!parametersJson) {
    return defaults
  }

  try {
    const parsed = JSON.parse(parametersJson)
    return {
      outputFormat: parsed.outputFormat ?? null,
      selectedExecutionOptions: Array.isArray(parsed.selectedExecutionOptions)
        ? parsed.selectedExecutionOptions
        : [],
      selectedDebugOptions: Array.isArray(parsed.selectedDebugOptions)
        ? parsed.selectedDebugOptions
        : [],
      ageStart: parsed.ageStart ?? null,
      ageEnd: parsed.ageEnd ?? null,
      yearStart: parsed.yearStart ?? null,
      yearEnd: parsed.yearEnd ?? null,
      forceYear: parsed.forceYear ?? null,
      ageIncrement: parsed.ageIncrement ?? null,
      metadataToOutput: parsed.metadataToOutput ?? null,
      filters: parsed.filters ?? null,
      utils: Array.isArray(parsed.utils) ? parsed.utils : [],
      excludedExecutionOptions: Array.isArray(parsed.excludedExecutionOptions)
        ? parsed.excludedExecutionOptions
        : [],
      excludedDebugOptions: Array.isArray(parsed.excludedDebugOptions)
        ? parsed.excludedDebugOptions
        : [],
      combineAgeYearRange: parsed.combineAgeYearRange ?? null,
      progressFrequency: parsed.progressFrequency ?? null,
      reportTitle: parsed.reportTitle ?? null,
    }
  } catch {
    console.error('Error parsing projection parameters JSON')
    return defaults
  }
}

/**
 * Determines if the projection is read-only based on its status
 * @param status The projection status
 * @returns true if the projection should be read-only
 */
export const isProjectionReadOnly = (status: ProjectionStatus): boolean => {
  return status === PROJECTION_STATUS.READY || status === PROJECTION_STATUS.RUNNING
}

