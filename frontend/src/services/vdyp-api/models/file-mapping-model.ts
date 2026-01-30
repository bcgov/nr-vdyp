import type { ProjectionFileSetModel } from './projection-model'

export interface FileMappingModel {
  fileMappingGUID: string
  projectionFileSet: ProjectionFileSetModel
  comsObjectGUID: string
  downloadURL: string | null
}
