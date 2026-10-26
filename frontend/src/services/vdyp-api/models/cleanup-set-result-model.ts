import type { CleanupOutcomeModel } from './cleanup-outcome-model'

export interface CleanupSetResultModel {
  jobGuid: string
  folderName: string
  bytes: number
  outcome: CleanupOutcomeModel
  detail: string | null
}
