<template>
  <v-card>
    <AppMessageDialog
      :dialog="messageDialog.dialog"
      :title="messageDialog.title"
      :message="messageDialog.message"
      :dialogWidth="messageDialog.dialogWidth"
      :btnLabel="messageDialog.btnLabel"
      :variant="messageDialog.variant"
      @update:dialog="(value) => (messageDialog.dialog = value)"
      @close="handleDialogClose"
    />
    <v-expansion-panels v-model="panelOpenStates[panelName]">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates[panelName] === CONSTANTS.PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">File Upload</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text">
          <v-form ref="form">
            <v-row class="file-upload-row">
              <v-col cols="6" class="file-upload-col file-upload-col-left">
                <span
                  class="bcds-file-input-label"
                  :class="{ 'bcds-file-input-label--disabled': isReadOnly }"
                >
                  {{ isReadOnly ? 'Polygon File' : 'Select Polygon File' }}
                </span>
                <!-- View mode -->
                <div v-if="isReadOnly" class="file-display-container file-display-container--disabled">
                  <template v-if="fileUploadStore.polygonFileInfo">
                    <v-icon class="file-display-icon">mdi-paperclip</v-icon>
                    <span class="file-display-name">{{ fileUploadStore.polygonFileInfo.filename }}</span>
                  </template>
                  <template v-else>
                    <span class="file-display-name file-display-empty">No file uploaded</span>
                  </template>
                </div>
                <!-- Edit mode -->
                <template v-else>
                  <v-file-upload
                    :key="polygonUploadKey"
                    v-model="polygonFiles"
                    :disabled="isInputDisabled || fileUploadStore.isUploadingPolygon"
                    :loading="fileUploadStore.isUploadingPolygon"
                    show-size
                    clearable
                    accept=".csv"
                    class="bcds-file-upload"
                  />
                  <!-- Uploaded file info -->
                  <div v-if="fileUploadStore.polygonFileInfo" class="uploaded-file-info">
                    <v-icon size="small" class="uploaded-file-icon">mdi-paperclip</v-icon>
                    <span class="uploaded-file-name">{{ fileUploadStore.polygonFileInfo.filename }}</span>
                    <v-btn
                      icon
                      variant="text"
                      size="x-small"
                      class="uploaded-file-delete-btn"
                      :disabled="isInputDisabled"
                      @click="confirmRemovePolygonFile"
                    >
                      <v-icon size="25">mdi-delete-forever</v-icon>
                    </v-btn>
                  </div>
                </template>
              </v-col>
              <v-col cols="6" class="file-upload-col file-upload-col-right">
                <span
                  class="bcds-file-input-label"
                  :class="{ 'bcds-file-input-label--disabled': isReadOnly }"
                >
                  {{ isReadOnly ? 'Layer File' : 'Select Layer File' }}
                </span>
                <!-- View mode -->
                <div v-if="isReadOnly" class="file-display-container file-display-container--disabled">
                  <template v-if="fileUploadStore.layerFileInfo">
                    <v-icon class="file-display-icon">mdi-paperclip</v-icon>
                    <span class="file-display-name">{{ fileUploadStore.layerFileInfo.filename }}</span>
                  </template>
                  <template v-else>
                    <span class="file-display-name file-display-empty">No file uploaded</span>
                  </template>
                </div>
                <!-- Edit mode: Show existing file info and allow replacement -->
                <template v-else>
                  <v-file-upload
                    :key="layerUploadKey"
                    v-model="layerFiles"
                    :disabled="isInputDisabled || fileUploadStore.isUploadingLayer"
                    :loading="fileUploadStore.isUploadingLayer"
                    show-size
                    clearable
                    accept=".csv"
                    class="bcds-file-upload"
                  />
                  <!-- Uploaded file info -->
                  <div v-if="fileUploadStore.layerFileInfo" class="uploaded-file-info">
                    <v-icon size="small" class="uploaded-file-icon">mdi-paperclip</v-icon>
                    <span class="uploaded-file-name">{{ fileUploadStore.layerFileInfo.filename }}</span>
                    <v-btn
                      icon
                      variant="text"
                      size="x-small"
                      class="uploaded-file-delete-btn"
                      :disabled="isInputDisabled"
                      @click="confirmRemoveLayerFile"
                    >
                      <v-icon size="25">mdi-delete-forever</v-icon>
                    </v-btn>
                  </div>
                </template>
              </v-col>
            </v-row>
          </v-form>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { AppMessageDialog } from '@/components'
import { CONSTANTS, MESSAGE } from '@/constants'
import { PROJECTION_ERR, FILE_REMOVAL_DIALOG } from '@/constants/message'
import type { MessageDialog } from '@/interfaces/interfaces'
import { fileUploadValidation } from '@/validation'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import {
  getProjectionById,
  getFileSetFiles,
  deleteFileFromFileSet,
} from '@/services/projectionService'
import { uploadFileToFileSet } from '@/services/apiActions'

const form = ref<HTMLFormElement>()
const fileUploadStore = useFileUploadStore()
const appStore = useAppStore()
const notificationStore = useNotificationStore()
const alertDialogStore = useAlertDialogStore()

// Local file arrays for v-file-upload (it uses array model)
const polygonFiles = ref<File[]>([])
const layerFiles = ref<File[]>([])

// Keys to force v-file-upload re-render after clearing, so re-selecting the same file triggers upload
const polygonUploadKey = ref(0)
const layerUploadKey = ref(0)

// Check if we're in read-only (view) mode
const isReadOnly = computed(() => appStore.isReadOnly)

const messageDialog = ref<MessageDialog>({
  dialog: false,
  title: '',
  message: '',
})

const panelName = CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS

const panelOpenStates = computed(() => fileUploadStore.panelOpenStates)

// File Upload panel is always interactable - only disabled in read-only mode
const isInputDisabled = computed(() => isReadOnly.value)

const MAX_DISPLAY_ERR_ITEMS = 2

const formatErrColumnList = (
  columns: string[],
  maxItems: number,
  errName: string,
): string => {
  if (columns.length <= maxItems) {
    return columns.join(', ')
  } else {
    const displayed = columns.slice(0, maxItems).join(', ')
    const remaining = columns.length - maxItems
    return `${displayed}, and ${remaining} more ${errName} columns`
  }
}

const showErrorDialog = (title: string, message: string) => {
  messageDialog.value = {
    dialog: true,
    title,
    message,
    btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    dialogWidth: 500,
    variant: 'error',
  }
}

const buildHeaderErrorMessage = (details: {
  missing: string[]
  extra: string[]
  mismatches: string[]
}): string => {
  let message = ''

  if (details.missing.length > 0) {
    message += 'Missing columns:\n' +
      formatErrColumnList(details.missing, MAX_DISPLAY_ERR_ITEMS, 'missing') +
      '\n\n'
  }
  if (details.extra.length > 0) {
    message += 'Extra columns:\n' +
      formatErrColumnList(details.extra, MAX_DISPLAY_ERR_ITEMS, 'extra') +
      '\n\n'
  }
  if (details.mismatches.length > 0) {
    message += 'Mismatches:\n' +
      formatErrColumnList(details.mismatches, MAX_DISPLAY_ERR_ITEMS, 'mismatch') +
      '\n\n'
  }

  message += 'Please ensure the headers match the required order.'
  return message
}

const validateDuplicateColumns = async (
  file: File | null,
  validateFn: (file: File) => Promise<{ isValid: boolean; duplicates: string[] }>,
  errorMessage: string,
  dialogTitle: string,
): Promise<boolean> => {
  if (!file) return true

  const result = await validateFn(file)
  if (!result.isValid) {
    let message = errorMessage
    if (result.duplicates.length > 0) {
      message += '\n\nDuplicate columns found:\n' +
        formatErrColumnList(result.duplicates, MAX_DISPLAY_ERR_ITEMS, 'duplicate')
    }
    showErrorDialog(dialogTitle, message)
    return false
  }
  return true
}

const validateHeaderColumns = async (
  file: File | null,
  validateFn: (file: File) => Promise<{ isValid: boolean; details: any }>,
  dialogTitle: string,
): Promise<boolean> => {
  if (!file) return true

  const result = await validateFn(file)
  if (!result.isValid) {
    const message = buildHeaderErrorMessage(result.details)
    showErrorDialog(dialogTitle, message)
    return false
  }
  return true
}

const validatePolygonFile = async (file: File): Promise<boolean> => {
  const duplicatesValid = await validateDuplicateColumns(
    file,
    fileUploadValidation.validatePolygonDuplicateColumns,
    MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_DUPLICATE_COLUMNS,
    MESSAGE.MSG_DIALOG_TITLE.POLYGON_FILE_DUPLICATE_COLUMNS,
  )
  if (!duplicatesValid) return false

  return validateHeaderColumns(
    file,
    fileUploadValidation.validatePolygonHeader,
    MESSAGE.MSG_DIALOG_TITLE.POLYGON_FILE_HEADER_MISMATCH,
  )
}

const validateLayerFile = async (file: File): Promise<boolean> => {
  const duplicatesValid = await validateDuplicateColumns(
    file,
    fileUploadValidation.validateLayerDuplicateColumns,
    MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_DUPLICATE_COLUMNS,
    MESSAGE.MSG_DIALOG_TITLE.LAYER_FILE_DUPLICATE_COLUMNS,
  )
  if (!duplicatesValid) return false

  return validateHeaderColumns(
    file,
    fileUploadValidation.validateLayerHeader,
    MESSAGE.MSG_DIALOG_TITLE.LAYER_FILE_HEADER_MISMATCH,
  )
}

// Upload polygon file immediately when selected
const uploadPolygonFile = async (file: File) => {
  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID) {
    notificationStore.showErrorMessage(PROJECTION_ERR.MISSING_GUID, PROJECTION_ERR.FILE_UPLOAD_FAILED_TITLE)
    return
  }

  // Validate the file first
  const isValid = await validatePolygonFile(file)
  if (!isValid) {
    polygonFiles.value = []
    polygonUploadKey.value++
    return
  }

  fileUploadStore.isUploadingPolygon = true
  try {
    const projectionModel = await getProjectionById(projectionGUID)
    if (!projectionModel.polygonFileSet?.projectionFileSetGUID) {
      throw new Error('Polygon file set not found')
    }

    const polygonFileSetGUID = projectionModel.polygonFileSet.projectionFileSetGUID

    // Delete existing file if any
    if (fileUploadStore.polygonFileInfo) {
      await deleteFileFromFileSet(
        projectionGUID,
        fileUploadStore.polygonFileInfo.fileSetGUID,
        fileUploadStore.polygonFileInfo.fileMappingGUID,
      )
    }

    // Upload new file
    await uploadFileToFileSet(projectionGUID, polygonFileSetGUID, file)

    // Fetch the uploaded file info
    const files = await getFileSetFiles(projectionGUID, polygonFileSetGUID)
    if (files.length > 0) {
      const uploadedFile = files[0]
      fileUploadStore.setPolygonFileInfo({
        filename: uploadedFile.filename || file.name,
        fileMappingGUID: uploadedFile.fileMappingGUID,
        fileSetGUID: polygonFileSetGUID,
      })
    }

    // Clear the file input
    polygonFiles.value = []
    polygonUploadKey.value++
    fileUploadStore.polygonFile = null
  } catch (error) {
    console.error('Error uploading polygon file:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.FILE_UPLOAD_FAILED, PROJECTION_ERR.FILE_UPLOAD_FAILED_TITLE)
    polygonFiles.value = []
    polygonUploadKey.value++
  } finally {
    fileUploadStore.isUploadingPolygon = false
  }
}

// Upload layer file immediately when selected
const uploadLayerFile = async (file: File) => {
  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID) {
    notificationStore.showErrorMessage(PROJECTION_ERR.MISSING_GUID, PROJECTION_ERR.FILE_UPLOAD_FAILED_TITLE)
    return
  }

  // Validate the file first
  const isValid = await validateLayerFile(file)
  if (!isValid) {
    layerFiles.value = []
    layerUploadKey.value++
    return
  }

  fileUploadStore.isUploadingLayer = true
  try {
    const projectionModel = await getProjectionById(projectionGUID)
    if (!projectionModel.layerFileSet?.projectionFileSetGUID) {
      throw new Error('Layer file set not found')
    }

    const layerFileSetGUID = projectionModel.layerFileSet.projectionFileSetGUID

    // Delete existing file if any
    if (fileUploadStore.layerFileInfo) {
      await deleteFileFromFileSet(
        projectionGUID,
        fileUploadStore.layerFileInfo.fileSetGUID,
        fileUploadStore.layerFileInfo.fileMappingGUID,
      )
    }

    // Upload new file
    await uploadFileToFileSet(projectionGUID, layerFileSetGUID, file)

    // Fetch the uploaded file info
    const files = await getFileSetFiles(projectionGUID, layerFileSetGUID)
    if (files.length > 0) {
      const uploadedFile = files[0]
      fileUploadStore.setLayerFileInfo({
        filename: uploadedFile.filename || file.name,
        fileMappingGUID: uploadedFile.fileMappingGUID,
        fileSetGUID: layerFileSetGUID,
      })
    }

    // Clear the file input
    layerFiles.value = []
    layerUploadKey.value++
    fileUploadStore.layerFile = null
  } catch (error) {
    console.error('Error uploading layer file:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.FILE_UPLOAD_FAILED, PROJECTION_ERR.FILE_UPLOAD_FAILED_TITLE)
    layerFiles.value = []
    layerUploadKey.value++
  } finally {
    fileUploadStore.isUploadingLayer = false
  }
}

// Watch for polygon file selection
watch(polygonFiles, (newFiles) => {
  if (newFiles && newFiles.length > 0) {
    uploadPolygonFile(newFiles[0])
  }
})

// Watch for layer file selection
watch(layerFiles, (newFiles) => {
  if (newFiles && newFiles.length > 0) {
    uploadLayerFile(newFiles[0])
  }
})

// Confirm and remove polygon file
const confirmRemovePolygonFile = async () => {
  if (!fileUploadStore.polygonFileInfo) return

  const confirmed = await alertDialogStore.openDialog(
    FILE_REMOVAL_DIALOG.TITLE,
    FILE_REMOVAL_DIALOG.POLYGON_MESSAGE(fileUploadStore.polygonFileInfo.filename),
    { variant: 'confirmation' },
  )

  if (confirmed) {
    await removePolygonFile()
  }
}

// Remove polygon file from server
const removePolygonFile = async () => {
  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID || !fileUploadStore.polygonFileInfo) return

  fileUploadStore.isDeletingFile = true
  try {
    await deleteFileFromFileSet(
      projectionGUID,
      fileUploadStore.polygonFileInfo.fileSetGUID,
      fileUploadStore.polygonFileInfo.fileMappingGUID,
    )
    fileUploadStore.setPolygonFileInfo(null)
  } catch (error) {
    console.error('Error removing polygon file:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.FILE_DELETE_FAILED, PROJECTION_ERR.FILE_DELETE_FAILED_TITLE)
  } finally {
    fileUploadStore.isDeletingFile = false
  }
}

// Confirm and remove layer file
const confirmRemoveLayerFile = async () => {
  if (!fileUploadStore.layerFileInfo) return

  const confirmed = await alertDialogStore.openDialog(
    FILE_REMOVAL_DIALOG.TITLE,
    FILE_REMOVAL_DIALOG.LAYER_MESSAGE(fileUploadStore.layerFileInfo.filename),
    { variant: 'confirmation' },
  )

  if (confirmed) {
    await removeLayerFile()
  }
}

// Remove layer file from server
const removeLayerFile = async () => {
  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID || !fileUploadStore.layerFileInfo) return

  fileUploadStore.isDeletingFile = true
  try {
    await deleteFileFromFileSet(
      projectionGUID,
      fileUploadStore.layerFileInfo.fileSetGUID,
      fileUploadStore.layerFileInfo.fileMappingGUID,
    )
    fileUploadStore.setLayerFileInfo(null)
  } catch (error) {
    console.error('Error removing layer file:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.FILE_DELETE_FAILED, PROJECTION_ERR.FILE_DELETE_FAILED_TITLE)
  } finally {
    fileUploadStore.isDeletingFile = false
  }
}

const handleDialogClose = () => {}
</script>

<style>
/* Attachments Panel Card - BC Government Design Standards */

/* File Input Label - BC Government Design Standards */
.bcds-file-input-label {
  display: block;
  font: var(--typography-regular-small-body);
  color: var(--typography-color-primary);
  font-size: var(--typography-font-size-label);
  font-weight: var(--typography-font-weights-regular);
  line-height: 1.5;
  padding: var(--layout-padding-xsmall) var(--layout-padding-none);
  padding-bottom: 4px;
  margin-bottom: 0;
}

/* File Upload Component - BC Government Design Standards */
/* Main container styling - matching provided design */
.bcds-file-upload {
  background-color: var(--theme-gray-20) !important;
  border: 1px dashed var(--surface-color-border-medium) !important;
  border-radius: var(--layout-border-radius-medium) !important;
  padding: var(--layout-padding-large) !important;
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  min-height: 140px;
  gap: var(--layout-margin-small);
}

.bcds-file-upload:hover {
  border-color: var(--surface-color-border-dark) !important;
  background-color: var(--theme-gray-30) !important;
}

.bcds-file-upload .v-file-upload-items {
  display: none;
}

/* Upload icon styling */
.bcds-file-upload .v-file-upload-icon .v-icon {
  color: var(--icons-color-primary) !important;
  font-size: 32px !important;
}

/* "Drag and drop files here" text */
.bcds-file-upload .v-file-upload-title {
  font: var(--typography-regular-body) !important;
  color: var(--typography-color-primary) !important;
}

/* "or" divider */
.bcds-file-upload .v-file-upload-divider {
  width: 100%;
  max-width: 300px;
}

.bcds-file-upload .v-file-upload-divider .v-divider__content {
  font: var(--typography-regular-small-body) !important;
  color: var(--typography-color-secondary) !important;
}

.bcds-file-upload .v-file-upload-divider .v-divider {
  border-color: var(--surface-color-border-medium) !important;
}

/* "Browse Files" button - outlined style */
.bcds-file-upload button.v-btn.v-btn--variant-tonal {
  background-color: #FFFFFF !important;
  border: 1px solid var(--surface-color-border-dark) !important;
  color: var(--typography-color-primary) !important;
  font: var(--typography-regular-body) !important;
  text-transform: none !important;
  padding: 8px 24px !important;
  min-width: 120px;
  box-shadow: none !important;
  border-radius: var(--layout-border-radius-medium) !important;
}

.bcds-file-upload button.v-btn.v-btn--variant-tonal:hover {
  background-color: var(--surface-color-secondary-button-hover) !important;
}

.bcds-file-upload button.v-btn .v-btn__overlay,
.bcds-file-upload button.v-btn .v-btn__underlay {
  display: none !important;
}

/* File upload row - full width layout */
.file-upload-row {
  margin: 0 !important;
}

.file-upload-col {
  padding-top: 0 !important;
  padding-bottom: 0 !important;
}

.file-upload-col-left {
  padding-left: 0 !important;
  padding-right: 12px !important;
}

.file-upload-col-right {
  padding-left: 12px !important;
  padding-right: 0 !important;
}

/* Responsive stacking below 1000px */
@media (max-width: 999px) {
  .file-upload-col {
    flex: 0 0 100% !important;
    max-width: 100% !important;
  }

  .file-upload-col-left {
    padding-right: 0 !important;
    margin-bottom: 16px;
  }

  .file-upload-col-right {
    padding-left: 0 !important;
  }
}

/* File display for view mode */
.file-display-container {
  display: flex;
  align-items: center;
  gap: var(--layout-margin-small);
  padding: var(--layout-padding-small) 12px;
  background: var(--surface-color-background-light-gray);
  border: var(--layout-border-width-small) solid var(--surface-color-border-default);
  border-radius: var(--layout-border-radius-medium);
  min-height: var(--layout-margin-xxlarge);
}

/* Disabled state - View mode file display container */
.file-display-container--disabled {
  background: var(--surface-color-forms-disabled);
  cursor: default;
}

/* Disabled state - View mode label */
.bcds-file-input-label--disabled {
  color: var(--typography-color-disabled) !important;
}

.file-display-icon {
  color: var(--icons-color-primary);
  font-size: 20px;
}

/* Disabled state - View mode file icon */
.file-display-container--disabled .file-display-icon {
  color: var(--icons-color-disabled);
}

.file-display-name {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Disabled state - View mode file name */
.file-display-container--disabled .file-display-name {
  color: var(--typography-color-disabled);
}

.file-display-empty {
  color: var(--typography-color-placeholder);
  font-style: italic;
}

/* Disabled state - View mode empty text */
.file-display-container--disabled .file-display-empty {
  color: var(--typography-color-disabled);
  font-style: italic;
}

/* Uploaded file info display */
.uploaded-file-info {
  display: flex;
  align-items: center;
  gap: var(--layout-margin-xsmall);
  padding: var(--layout-padding-medium);
  margin-top: var(--layout-margin-medium);
  border: var(--layout-border-width-small) solid var(--surface-color-border-default);
}

.uploaded-file-icon {
  color: var(--icons-color-primary);
}

.uploaded-file-name {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.uploaded-file-delete-btn {
  color: var(--icons-color-primary);
  margin-left: auto;
}

.uploaded-file-delete-btn:hover {
  color: var(--icons-color-danger);
}
</style>
