/* tslint:disable */
/* eslint-disable */
import type { FileUpload } from './file-upload'
import type { Parameters } from './parameters'
export interface ProjectionHcsvBody {
  /**
   * @type {FileUpload}
   * @memberof ProjectionHcsvBody
   */
  polygonInputData?: FileUpload

  /**
   * @type {FileUpload}
   * @memberof ProjectionHcsvBody
   */
  layersInputData?: FileUpload

  /**
   * @type {Parameters}
   * @memberof ProjectionHcsvBody
   */
  projectionParameters?: Parameters
}
