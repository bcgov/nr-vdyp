/* tslint:disable */
/* eslint-disable */
import type { Parameters } from './parameters'
export interface ProjectionHcsvPostRequest {
  /**
   * @type {Parameters}
   * @memberof ProjectionHcsvPostRequest
   */
  projectionParameters?: Parameters

  /**
   * @type {Blob}
   * @memberof ProjectionHcsvPostRequest
   */
  polygonInputData?: Blob

  /**
   * @type {Blob}
   * @memberof ProjectionHcsvPostRequest
   */
  layerInputData?: Blob
}
