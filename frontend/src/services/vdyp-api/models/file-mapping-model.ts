import type { ProjectionFileSetModel } from './projection-model'

export interface FileMappingModel {
  fileMappingGUID: string
  projectionFileSet: ProjectionFileSetModel
  comsObjectGUID: string
  filename: string | null
  downloadURL: string | null
  debugModePermitted: boolean
  // Data rows counted at upload for a polygon file; null for layer and result files, and for polygon files uploaded before counts were saved
  estimatedPolygonCount?: number | null
}
