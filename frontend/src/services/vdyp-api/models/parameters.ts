import { CombineAgeYearRangeEnum } from './combine-age-year-range-enum'
import type { Filters } from './filters'
import { MetadataToOutputEnum } from './metadata-to-output-enum'
import { OutputFormatEnum } from './output-format-enum'
import type { ProgressFrequency } from './progress-frequency'
import { DebugOptionsEnum } from './debug-options-enum'
import { ExecutionOptionsEnum } from './execution-options-enum'
import type { UtilizationParameter } from './utilization-parameter'
export interface Parameters {
  outputFormat?: OutputFormatEnum
  selectedExecutionOptions?: Array<ExecutionOptionsEnum>
  excludedExecutionOptions?: Array<ExecutionOptionsEnum>
  selectedDebugOptions?: Array<DebugOptionsEnum>
  excludedDebugOptions?: Array<DebugOptionsEnum>
  ageStart?: number | null
  minAgeStart?: number | null
  maxAgeStart?: number | null
  ageEnd?: number | null
  minAgeEnd?: number | null
  maxAgeEnd?: number | null
  yearStart?: number | null
  yearEnd?: number | null
  forceYear?: number | null
  ageIncrement?: number | null
  minAgeIncrement?: number | null
  maxAgeIncrement?: number | null
  combineAgeYearRange?: CombineAgeYearRangeEnum
  progressFrequency?: ProgressFrequency
  metadataToOutput?: MetadataToOutputEnum
  reportTitle?: string | null
  reportDescription?: string | null
  filters?: Filters
  utils?: Array<UtilizationParameter> // Species Utilization Level
}
