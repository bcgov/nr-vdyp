import {
  getAllRunningProjections as apiGetAllRunningProjections,
  getThreadCapacity as apiGetThreadCapacity,
  getStorageStatus as apiGetStorageStatus,
} from '@/services/apiActions'
import type { ProjectionModel, StorageStatusModel, VDYPUserModel } from '@/services/vdyp-api'
import type { AdminProjection, UserTypeCode } from '@/interfaces/interfaces'
import { mapProjectionStatus } from '@/services/projectionService'
import { PROJECTION_STATUS } from '@/constants/constants'

/**
 * Resolves the display name for a projection owner as "F. Lastname" (e.g. "R. MacLeod"),
 * matching the Admin Dashboard's Owner column format. Falls back to whichever of
 * firstName/lastName is present, then displayName, tolerating either being null/empty.
 */
const getOwnerDisplayName = (ownerUser: VDYPUserModel | null | undefined): string => {
  if (!ownerUser) return ''

  const firstName = ownerUser.firstName?.trim()
  const lastName = ownerUser.lastName?.trim()

  if (firstName && lastName) return `${firstName.charAt(0)}. ${lastName}`
  if (lastName) return lastName
  if (firstName) return firstName

  return ownerUser.displayName ?? ''
}

/**
 * Transforms a backend ProjectionModel into the frontend AdminProjection shape
 * used by the Admin Dashboard's running projections table.
 */
export const transformAdminProjection = (model: ProjectionModel): AdminProjection => {
  const batchMapping = model.batchMapping

  return {
    projectionGUID: model.projectionGUID,
    title: model.reportTitle || '',
    ownerDisplayName: getOwnerDisplayName(model.ownerUser),
    userType: (model.ownerUser?.identityProviderCode?.code as UserTypeCode) ?? null,
    startDate: model.startDate,
    status: mapProjectionStatus(model.projectionStatusCode?.code || PROJECTION_STATUS.DRAFT),
    workerCount: batchMapping?.workerCount ?? 0,
    completedPolygonCount: batchMapping?.completedPolygonCount ?? 0,
    polygonCount: batchMapping?.polygonCount ?? 0,
  }
}

/**
 * (Admin Only) Fetches all RUNNING projections across all users and transforms
 * them to the Admin Dashboard's frontend format.
 * @returns A promise that resolves to an array of AdminProjection objects
 */
export const fetchAllRunningProjections = async (): Promise<AdminProjection[]> => {
  const projectionModels = await apiGetAllRunningProjections()
  return projectionModels.map(transformAdminProjection)
}

/**
 * (Admin Only) Fetches the batch service's configured thread pool capacity, used to show
 * current thread load relative to system capacity on the Admin Dashboard.
 * @returns A promise that resolves to the thread capacity.
 */
export const fetchThreadCapacity = async (): Promise<number> => {
  return apiGetThreadCapacity()
}

/**
 * (Admin Only) Fetches the batch service's PVC storage status (percent full, out-of-spec flag),
 * used to show system storage health on the Admin Dashboard.
 * @returns A promise that resolves to the storage status.
 */
export const fetchStorageStatus = async (): Promise<StorageStatusModel> => {
  return apiGetStorageStatus()
}
