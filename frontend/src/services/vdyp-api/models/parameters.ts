/* tslint:disable */
/* eslint-disable */
import { CombineAgeYearRangeEnum } from './combine-age-year-range-enum'
import type { Filters } from './filters'
import { MetadataToOutputEnum } from './metadata-to-output-enum'
import { OutputFormatEnum } from './output-format-enum'
import type { ProgressFrequency } from './progress-frequency'
import { SelectedDebugOptionsEnum } from './selected-debug-options-enum'
import { SelectedExecutionOptionsEnum } from './selected-execution-options-enum'
import type { UtilizationParameter } from './utilization-parameter'
export interface Parameters {
  /**
   * @type {OutputFormatEnum}
   * @memberof Parameters
   */
  outputFormat?: OutputFormatEnum

  /**
   * @type {Array<SelectedExecutionOptionsEnum>}
   * @memberof Parameters
   */
  selectedExecutionOptions?: Array<SelectedExecutionOptionsEnum>

  /**
   * @type {Array<SelectedDebugOptionsEnum>}
   * @memberof Parameters
   */
  selectedDebugOptions?: Array<SelectedDebugOptionsEnum>

  /**
   * @type {number}
   * @memberof Parameters
   */
  ageStart?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  minAgeStart?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  maxAgeStart?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  ageEnd?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  minAgeEnd?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  maxAgeEnd?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  yearStart?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  yearEnd?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  forceYear?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  ageIncrement?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  minAgeIncrement?: number

  /**
   * @type {number}
   * @memberof Parameters
   */
  maxAgeIncrement?: number

  /**
   * @type {CombineAgeYearRangeEnum}
   * @memberof Parameters
   */
  combineAgeYearRange?: CombineAgeYearRangeEnum

  /**
   * @type {ProgressFrequency}
   * @memberof Parameters
   */
  progressFrequency?: ProgressFrequency

  /**
   * @type {MetadataToOutputEnum}
   * @memberof Parameters
   */
  metadataToOutput?: MetadataToOutputEnum

  /**
   * @type {Filters}
   * @memberof Parameters
   */
  filters?: Filters

  /**
   * @type {Array<UtilizationParameter>}
   * @memberof Parameters
   */
  utils?: Array<UtilizationParameter>
}
