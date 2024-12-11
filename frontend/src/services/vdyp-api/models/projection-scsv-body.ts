/* tslint:disable */
/* eslint-disable */
import type { FileUpload } from './file-upload'
import type { Parameters } from './parameters'
export interface ProjectionScsvBody {
  /**
   * @type {FileUpload}
   * @memberof ProjectionScsvBody
   */
  polygonInputData?: FileUpload

  /**
   * @type {FileUpload}
   * @memberof ProjectionScsvBody
   */
  layersInputData?: FileUpload

  /**
   * @type {FileUpload}
   * @memberof ProjectionScsvBody
   */
  historyInputData?: FileUpload

  /**
   * @type {FileUpload}
   * @memberof ProjectionScsvBody
   */
  nonVegetationInputData?: FileUpload

  /**
   * @type {FileUpload}
   * @memberof ProjectionScsvBody
   */
  otherVegetationInputData?: FileUpload

  /**
   * @type {FileUpload}
   * @memberof ProjectionScsvBody
   */
  polygonIdInputData?: FileUpload

  /**
   * @type {FileUpload}
   * @memberof ProjectionScsvBody
   */
  speciesInputData?: FileUpload

  /**
   * @type {FileUpload}
   * @memberof ProjectionScsvBody
   */
  vriAdjustInputData?: FileUpload

  /**
   * @type {Parameters}
   * @memberof ProjectionScsvBody
   */
  projectionParameters?: Parameters
}
