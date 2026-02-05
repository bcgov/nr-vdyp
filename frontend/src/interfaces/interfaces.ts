import type { SortOrder, DialogVariant } from '@/types/types'
import { UtilizationClassSetEnum } from '@/services/vdyp-api/models/utilization-class-set-enum'

export interface MessageDialog {
  dialog: boolean
  title: string
  message: string
  dialogWidth?: number
  btnLabel?: string
  variant?: 'info' | 'confirmation' | 'warning' | 'error'
}

export interface JobSearchParams {
  pageNumber: number
  pageSize: number
  sortBy?: string
  sortOrder?: SortOrder
  searchJobName?: string
  startDate?: string | null
  endDate?: string | null
  status?: string
}

export interface CodeSearchParams {
  pageNumber: number
  pageSize: number
}

export interface TableOptions {
  page: number
  itemsPerPage: number
  sortBy: string
  sortDesc: string
}

export interface SpeciesList {
  species: string | null
  percent: string | null
}

export interface SpeciesGroup {
  group: string
  percent: string
  siteSpecies: string
  minimumDBHLimit?: UtilizationClassSetEnum
}

export interface FileUploadSpeciesGroup {
  group: string
  minimumDBHLimit: UtilizationClassSetEnum
}

export interface Tab {
  label: string
  component: string | object // Component name or an actual component
  tabname: string | null // Optional tabname
  disabled: boolean
}

export type ProjectionStatus = 'Draft' | 'Ready' | 'Running' | 'Failed'

export interface Projection {
  projectionGUID: string
  title: string
  description: string
  method: string
  projectionType: string
  lastUpdated: string
  expiration: string
  status: ProjectionStatus
}

export interface TableHeader {
  key: string
  title: string
  sortable: boolean
}

export interface SortOption {
  title: string
  value: string
}

export interface User {
  accessToken: string
  refToken: string
  idToken: string
}

export interface DialogOptions {
  width: number
  noconfirm: boolean
  variant?: DialogVariant
}

export interface ParsedProjectionParameters {
  outputFormat: string | null
  selectedExecutionOptions: string[]
  selectedDebugOptions: string[]
  ageStart: string | null
  ageEnd: string | null
  yearStart: string | null
  yearEnd: string | null
  forceYear: string | null
  ageIncrement: string | null
  metadataToOutput: string | null
  filters: unknown
  utils: unknown[]
  excludedExecutionOptions: string[]
  excludedDebugOptions: string[]
  combineAgeYearRange: string | null
  progressFrequency: string | null
  reportTitle: string | null
}

export interface UploadedFileInfo {
  filename: string
  fileMappingGUID: string
  fileSetGUID: string
}
