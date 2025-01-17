import type { FileUpload } from './file-upload'
import type { Parameters } from './parameters'
export interface ProjectionDcsvBody {
  dcsvInputData?: FileUpload
  projectionParameters?: Parameters
}
