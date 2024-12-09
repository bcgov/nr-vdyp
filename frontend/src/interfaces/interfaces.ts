import type { MessageType, SortOrder } from '@/types/types'

export interface ProgressCircularState {
  isShow: boolean
  message: string | undefined
}

export interface NotificationState {
  isShow: boolean
  message: string
  type: MessageType
  timeoutId: number | null
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
