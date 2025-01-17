import type { MessageType, SortOrder } from '@/types/types'

export interface NotificationState {
  isShow: boolean
  message: string
  type: MessageType
  color: MessageType
  timeoutId: number | null
}

export interface MessageDialog {
  dialog: boolean
  title: string
  message: string
  dialogWidth?: number
  btnLabel?: string
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
}

export interface Tab {
  label: string
  component: string | object // Component name or an actual component
  tabname: string | null // Optional tabname
}
