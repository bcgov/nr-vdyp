/* tslint:disable */
/* eslint-disable */
import type { FileUpload } from './file-upload'
import type { Parameters } from './parameters'
export interface ProjectionDcsvBody {
  /**
   * @type {FileUpload}
   * @memberof ProjectionDcsvBody
   */
  dcsvInputData?: FileUpload

  /**
   * @type {Parameters}
   * @memberof ProjectionDcsvBody
   */
  projectionParameters?: Parameters
}
