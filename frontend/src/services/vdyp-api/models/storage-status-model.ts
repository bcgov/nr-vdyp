export interface StorageStatusModel {
  percentFull: number
  usedBytes: number
  totalBytes: number
  expectedBytes: number
  outOfSpec: boolean
  thresholdPercent: number
}
