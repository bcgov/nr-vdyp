import type { ProjectionFileSetModel } from './projection-model'

export interface FileMappingModel {
  fileMappingGUID: string
  projectionFileSet: ProjectionFileSetModel
  comsObjectGUID: string
  filename: string | null
  downloadURL: string | null
}
