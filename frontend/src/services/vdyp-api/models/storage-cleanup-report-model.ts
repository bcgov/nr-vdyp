import type { CleanupSetResultModel } from './cleanup-set-result-model'

export interface StorageCleanupReportModel {
  dryRun: boolean
  scanned: number
  totalBytes: number
  sets: Array<CleanupSetResultModel>
  skippedNames: Array<string>
}
