import type { FileUpload } from './file-upload'
import type { Parameters } from './parameters'
export interface ProjectionHcsvBody {
  polygonInputData?: FileUpload
  layersInputData?: FileUpload
  projectionParameters?: Parameters
}
