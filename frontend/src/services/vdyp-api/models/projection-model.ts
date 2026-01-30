export interface UserTypeCodeModel {
  description: string
  displayOrder: number
  code: string
}

export interface VDYPUserModel {
  vdypUserGUID: string
  oidcGUID: string
  userTypeCode: UserTypeCodeModel
  firstName: string
  lastName: string
}

export interface FileSetTypeCodeModel {
  description: string
  displayOrder: number
  code: string
}

export interface ProjectionFileSetModel {
  projectionFileSetGUID: string
  ownerModel: VDYPUserModel
  fileSetTypeCode: FileSetTypeCodeModel
  fileSetName: string | null
}

export interface CalculationEngineCodeModel {
  description: string
  displayOrder: number
  code: string
}

export interface ProjectionStatusCodeModel {
  description: string
  displayOrder: number
  code: string
}

export interface ProjectionModel {
  projectionGUID: string
  ownerUser: VDYPUserModel
  polygonFileSet: ProjectionFileSetModel
  layerFileSet: ProjectionFileSetModel
  resultFileSet: ProjectionFileSetModel
  projectionParameters: string
  modelParameters: string | null
  startDate: string | null
  endDate: string | null
  calculationEngineCode: CalculationEngineCodeModel
  projectionStatusCode: ProjectionStatusCodeModel
  lastUpdatedDate: string | null
  reportTitle: string | null
  reportDescription: string | null
  expiryDate: string | null
}
