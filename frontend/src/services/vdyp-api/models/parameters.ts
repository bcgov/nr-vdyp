import { CombineAgeYearRangeEnum } from './combine-age-year-range-enum'
import type { Filters } from './filters'
import { MetadataToOutputEnum } from './metadata-to-output-enum'
import { OutputFormatEnum } from './output-format-enum'
import type { ProgressFrequency } from './progress-frequency'
import { SelectedDebugOptionsEnum } from './selected-debug-options-enum'
import { SelectedExecutionOptionsEnum } from './selected-execution-options-enum'
import type { UtilizationParameter } from './utilization-parameter'
export interface Parameters {
  outputFormat?: OutputFormatEnum
  selectedExecutionOptions?: Array<SelectedExecutionOptionsEnum>
  selectedDebugOptions?: Array<SelectedDebugOptionsEnum>
  ageStart?: number
  minAgeStart?: number
  maxAgeStart?: number
  ageEnd?: number
  minAgeEnd?: number
  maxAgeEnd?: number
  yearStart?: number
  yearEnd?: number
  forceYear?: number
  ageIncrement?: number
  minAgeIncrement?: number
  maxAgeIncrement?: number
  combineAgeYearRange?: CombineAgeYearRangeEnum
  progressFrequency?: ProgressFrequency
  metadataToOutput?: MetadataToOutputEnum
  filters?: Filters
  utils?: Array<UtilizationParameter>
}
