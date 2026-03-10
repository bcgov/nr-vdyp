import {
  PANEL,
  MANUAL_INPUT_PANEL,
  FILE_UPLOAD_PANEL,
  SORT_ORDER,
  MESSAGE_TYPE,
  REPORTING_TAB,
  PROJECTION_VIEW_MODE,
} from '@/constants/constants'

export type MessageType =
  | ''
  | typeof MESSAGE_TYPE.INFO
  | typeof MESSAGE_TYPE.SUCCESS
  | typeof MESSAGE_TYPE.ERROR
  | typeof MESSAGE_TYPE.WARNING

export type PanelName =
  | typeof MANUAL_INPUT_PANEL.REPORT_DETAILS
  | typeof MANUAL_INPUT_PANEL.SPECIES_INFO
  | typeof MANUAL_INPUT_PANEL.SITE_INFO
  | typeof MANUAL_INPUT_PANEL.STAND_INFO
  | typeof MANUAL_INPUT_PANEL.REPORT_SETTINGS

export type FileUploadPanelName =
  | typeof FILE_UPLOAD_PANEL.REPORT_CONFIG
  | typeof FILE_UPLOAD_PANEL.MINIMUM_DBH
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

export type DialogVariant = 'info' | 'confirmation' | 'warning' | 'error' | 'destructive'

export type ProjectionViewMode = (typeof PROJECTION_VIEW_MODE)[keyof typeof PROJECTION_VIEW_MODE]
