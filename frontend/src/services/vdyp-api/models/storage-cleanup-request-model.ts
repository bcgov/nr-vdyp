export interface StorageCleanupRequestModel {
  dryRun: boolean
  protectedJobGuids: Array<string>
}
