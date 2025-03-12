import type { FileUpload } from './file-upload'
import type { Parameters } from './parameters'
export interface ProjectionScsvBody {
  polygonInputData?: FileUpload
  layersInputData?: FileUpload
  historyInputData?: FileUpload
  nonVegetationInputData?: FileUpload
  otherVegetationInputData?: FileUpload
  polygonIdInputData?: FileUpload
  speciesInputData?: FileUpload
  vriAdjustInputData?: FileUpload
  projectionParameters?: Parameters
}
