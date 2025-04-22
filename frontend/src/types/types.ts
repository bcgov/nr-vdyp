import {
  PANEL,
  MODEL_PARAMETER_PANEL,
  FILE_UPLOAD_PANEL,
  SORT_ORDER,
  MESSAGE_TYPE,
  REPORTING_TAB,
} from '@/constants/constants'

export type MessageType =
  | ''
  | typeof MESSAGE_TYPE.INFO
  | typeof MESSAGE_TYPE.SUCCESS
  | typeof MESSAGE_TYPE.ERROR
  | typeof MESSAGE_TYPE.WARNING

export type PanelName =
  | typeof MODEL_PARAMETER_PANEL.SPECIES_INFO
  | typeof MODEL_PARAMETER_PANEL.SITE_INFO
  | typeof MODEL_PARAMETER_PANEL.STAND_DENSITY
  | typeof MODEL_PARAMETER_PANEL.REPORT_INFO

export type FileUploadPanelName =
  | typeof FILE_UPLOAD_PANEL.REPORT_INFO
  | typeof FILE_UPLOAD_PANEL.ATTACHMENTS

export type PanelState = typeof PANEL.OPEN | typeof PANEL.CLOSE

// Define a type for sort order lowercase letters
export type SortOrder = Lowercase<(typeof SORT_ORDER)[keyof typeof SORT_ORDER]>

export type CSVRowType = (string | number | null)[][]

export type ReportingTab =
  | typeof REPORTING_TAB.MODEL_REPORT
  | typeof REPORTING_TAB.VIEW_ERR_MSG
  | typeof REPORTING_TAB.VIEW_LOG_FILE

export type Density = 'default' | 'comfortable' | 'compact'

export type Variant =
  | 'outlined'
  | 'plain'
  | 'underlined'
  | 'filled'
  | 'solo'
  | 'solo-inverted'
  | 'solo-filled'

export type NumStrNullType = number | string | null
