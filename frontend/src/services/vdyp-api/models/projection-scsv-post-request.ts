/* tslint:disable */
/* eslint-disable */
import type { Parameters } from './parameters'
export interface ProjectionScsvPostRequest {
  /**
   * @type {Parameters}
   * @memberof ProjectionScsvPostRequest
   */
  projectionParameters?: Parameters

  /**
   * @type {Blob}
   * @memberof ProjectionScsvPostRequest
   */
  polygonInputData?: Blob

  /**
   * @type {Blob}
   * @memberof ProjectionScsvPostRequest
   */
  layerInputData?: Blob

  /**
   * @type {Blob}
   * @memberof ProjectionScsvPostRequest
   */
  historyInputData?: Blob

  /**
   * @type {Blob}
   * @memberof ProjectionScsvPostRequest
   */
  nonVegetationInputData?: Blob

  /**
   * @type {Blob}
   * @memberof ProjectionScsvPostRequest
   */
  otherVegetationInputData?: Blob

  /**
   * @type {Blob}
   * @memberof ProjectionScsvPostRequest
   */
  polygonIdInputData?: Blob

  /**
   * @type {Blob}
   * @memberof ProjectionScsvPostRequest
   */
  speciesInputData?: Blob

  /**
   * @type {Blob}
   * @memberof ProjectionScsvPostRequest
   */
  vriAdjustInputData?: Blob
}
