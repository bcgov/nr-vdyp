import { getUserProjections } from '@/services/apiActions'
import type { ProjectionModel } from '@/services/vdyp-api'
import type { Projection, ProjectionStatus } from '@/interfaces/interfaces'
import { MODEL_SELECTION, PROJECTION_STATUS, PROJECTION_TYPE, PROJECTION_LIST_HEADER_KEY, SORT_ORDER } from '@/constants/constants'
import {
  ExecutionOptionsEnum,
} from '@/services/vdyp-api'

/**
 * Maps backend ProjectionStatusCode to frontend ProjectionStatus
 */
const mapProjectionStatus = (statusCode: string): ProjectionStatus => {
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
    return projectionModels.map((model) => transformProjection(model))
  } catch (error) {
    console.error('Error fetching user projections:', error)
    throw error
  }
}

/**
 * Sorts projections by a given key and order
 * @param projections The array of projections to sort
 * @param sortBy The key to sort by
 * @param sortOrder The order to sort ('asc' or 'desc')
 * @returns A new sorted array of projections
 */
export const sortProjections = (
  projections: Projection[],
  sortBy: string,
  sortOrder: typeof SORT_ORDER.ASC | typeof SORT_ORDER.DESC,
): Projection[] => {
  return [...projections].sort((a, b) => {
    const aValue = a[sortBy as keyof Projection]
    const bValue = b[sortBy as keyof Projection]

    if (sortBy === PROJECTION_LIST_HEADER_KEY.LAST_UPDATED || sortBy === PROJECTION_LIST_HEADER_KEY.EXPIRATION) {
      const aDate = new Date(aValue).getTime()
      const bDate = new Date(bValue).getTime()
      return sortOrder === SORT_ORDER.ASC ? aDate - bDate : bDate - aDate
    }

    if (typeof aValue === 'string' && typeof bValue === 'string') {
      return sortOrder === SORT_ORDER.ASC
        ? aValue.localeCompare(bValue, undefined, { numeric: true })
        : bValue.localeCompare(aValue, undefined, { numeric: true })
    }

    if (aValue < bValue) return sortOrder === SORT_ORDER.ASC ? -1 : 1
    if (aValue > bValue) return sortOrder === SORT_ORDER.ASC ? 1 : -1
    return 0
  })
}

/**
 * Paginates projections
 * @param projections The array of projections to paginate
 * @param currentPage The current page number (1-based)
 * @param itemsPerPage The number of items per page
 * @returns A new array of projections for the current page
 */
export const paginateProjections = (
  projections: Projection[],
  currentPage: number,
  itemsPerPage: number,
): Projection[] => {
  const start = (currentPage - 1) * itemsPerPage
  const end = start + itemsPerPage
  return projections.slice(start, end)
}

/**
 * Calculates total pages
 * @param totalItems The total number of items
 * @param itemsPerPage The number of items per page
 * @returns The total number of pages
 */
export const calculateTotalPages = (
  totalItems: number,
  itemsPerPage: number,
): number => {
  return Math.ceil(totalItems / itemsPerPage)
}
