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
              <span class="text-h6">Attachments</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text">
          <v-form ref="form">
            <div>
              <v-row>
                <v-col cols="5">
                  <label class="bcds-file-input-label" for="polygon-file-input">
                    Polygon File
                  </label>
                  <v-file-input
                    id="polygon-file-input"
                    v-model="fileUploadStore.polygonFile"
                    label="Select Polygon File..."
                    show-size
                    chips
                    clearable
                    accept=".csv"
                    :disabled="!isConfirmEnabled"
                    persistent-placeholder
                    class="bcds-file-input"
                  />
                </v-col>
                <v-col class="col-space-3" />
                <v-col cols="5">
                  <label class="bcds-file-input-label" for="layer-file-input">
                    Layer File
                  </label>
                  <v-file-input
                    id="layer-file-input"
                    v-model="fileUploadStore.layerFile"
                    label="Select Layer File..."
                    show-size
                    chips
                    clearable
                    accept=".csv"
                    :disabled="!isConfirmEnabled"
                    persistent-placeholder
                    class="bcds-file-input"
                  />
                </v-col>
              </v-row>
            </div>
            <AppPanelActions
              :isConfirmEnabled="isConfirmEnabled"
              :isConfirmed="isConfirmed"
              @clear="onClear"
              @confirm="onConfirm"
              @edit="onEdit"
            />
          </v-form>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import { AppMessageDialog, AppPanelActions } from '@/components'
import { CONSTANTS, MESSAGE } from '@/constants'
import type { MessageDialog } from '@/interfaces/interfaces'
import { fileUploadValidation } from '@/validation'

const form = ref<HTMLFormElement>()
const fileUploadStore = useFileUploadStore()

const messageDialog = ref<MessageDialog>({
  dialog: false,
  title: '',
  message: '',
})

const panelName = CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS

const panelOpenStates = computed(() => fileUploadStore.panelOpenStates)
const isConfirmEnabled = computed(
  () => fileUploadStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => fileUploadStore.panelState[panelName].confirmed,
)

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

const handleBasicFileValidationError = (errorType: string): string => {
  const errorMessages: Record<string, string> = {
    polygonFileMissing: MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_MISSING,
    layerFileMissing: MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_MISSING,
    polygonFileNotCSVFormat: MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_NOT_CSV_FORMAT,
    layerFileNotCSVFormat: MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_NOT_CSV_FORMAT,
  }
  return errorMessages[errorType] || ''
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

const validateFiles = async (): Promise<boolean> => {
  const result = await fileUploadValidation.validateFiles(
    fileUploadStore.polygonFile,
    fileUploadStore.layerFile,
  )

  if (!result.isValid) {
    const message = handleBasicFileValidationError(result.errorType || '')
    showErrorDialog(MESSAGE.MSG_DIALOG_TITLE.INVALID_FILE, message)
    return false
  }

  // Validate polygon file duplicates
  const polygonDuplicatesValid = await validateDuplicateColumns(
    fileUploadStore.polygonFile,
    fileUploadValidation.validatePolygonDuplicateColumns,
    MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_DUPLICATE_COLUMNS,
    MESSAGE.MSG_DIALOG_TITLE.POLYGON_FILE_DUPLICATE_COLUMNS,
  )
  if (!polygonDuplicatesValid) return false

  // Validate polygon file headers
  const polygonHeaderValid = await validateHeaderColumns(
    fileUploadStore.polygonFile,
    fileUploadValidation.validatePolygonHeader,
    MESSAGE.MSG_DIALOG_TITLE.POLYGON_FILE_HEADER_MISMATCH,
  )
  if (!polygonHeaderValid) return false

  // Validate layer file duplicates
  const layerDuplicatesValid = await validateDuplicateColumns(
    fileUploadStore.layerFile,
    fileUploadValidation.validateLayerDuplicateColumns,
    MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_DUPLICATE_COLUMNS,
    MESSAGE.MSG_DIALOG_TITLE.LAYER_FILE_DUPLICATE_COLUMNS,
  )
  if (!layerDuplicatesValid) return false

  // Validate layer file headers
  const layerHeaderValid = await validateHeaderColumns(
    fileUploadStore.layerFile,
    fileUploadValidation.validateLayerHeader,
    MESSAGE.MSG_DIALOG_TITLE.LAYER_FILE_HEADER_MISMATCH,
  )
  if (!layerHeaderValid) return false

  return true
}

const onConfirm = async () => {
  if (!(await validateFiles())) return

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
  }

  if (!isConfirmed.value) {
    fileUploadStore.confirmPanel(panelName)
  }
}

const onEdit = () => {
  if (isConfirmed.value) {
    fileUploadStore.editPanel(panelName)
  }
}

const onClear = () => {
  fileUploadStore.polygonFile = null
  fileUploadStore.layerFile = null
}

const handleDialogClose = () => {}
</script>

<style scoped>
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

/* File Input Component - BC Government Design Standards */
.bcds-file-input {
  flex-direction: column;
  align-self: stretch;
}

/* Style Vuetify's label to look like placeholder */
.bcds-file-input :deep(.v-label) {
  font: var(--typography-regular-body);
  font-size: var(--typography-font-size-body);
  color: var(--typography-color-placeholder);
  opacity: 1 !important;
  position: absolute !important;
  left: 0 !important;
  top: 50% !important;
  transform: translateY(-50%) !important;
  max-width: none !important;
  width: auto !important;
  white-space: nowrap !important;
  pointer-events: none;
}

/* Hide label when file is selected */
.bcds-file-input :deep(.v-field--dirty .v-label),
.bcds-file-input :deep(.v-field--active .v-label) {
  display: none;
}

/* Hide Vuetify's outline borders */
.bcds-file-input :deep(.v-field__outline) {
  display: none !important;
}

/* File Input Container */
.bcds-file-input :deep(.v-field) {
  color: var(--typography-color-primary);
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--layout-margin-small);
  background: var(--surface-color-forms-default);
  box-sizing: border-box;
  border: var(--layout-border-width-small) solid var(--surface-color-border-default);
  border-radius: var(--layout-border-radius-medium);
  padding: var(--layout-padding-small) 12px;
  cursor: pointer;
  height: var(--layout-margin-xxlarge);
  min-height: var(--layout-margin-xxlarge);
}

/* Override Vuetify's filled variant background */
.bcds-file-input :deep(.v-field--variant-filled .v-field__overlay) {
  background-color: var(--surface-color-forms-default);
  opacity: 1;
}

/* File Input Field Wrapper */
.bcds-file-input :deep(.v-field__field) {
  position: relative;
  padding: 0 !important;
  display: flex;
  align-items: center;
  height: 100%;
  flex-grow: 1;
}

/* File Input Element */
.bcds-file-input :deep(.v-field__input) {
  font: var(--typography-regular-body);
  font-size: var(--typography-font-size-body);
  padding: var(--layout-padding-none);
  color: var(--typography-color-primary);
  border: none;
  flex-grow: 1;
  min-height: auto;
  background-color: transparent !important;
  opacity: 1;
}

/* Focused state */
.bcds-file-input :deep(.v-field:focus-within) {
  border-radius: var(--layout-border-radius-large);
  border: var(--layout-border-width-small) solid var(--surface-color-border-active);
  outline: solid var(--layout-border-width-medium) var(--surface-color-border-active);
  outline-offset: var(--layout-margin-hair);
}

/* Hover state */
.bcds-file-input:not(.v-input--disabled) :deep(.v-field:hover) {
  border-color: var(--surface-color-border-dark);
}

/* Disabled state */
.bcds-file-input.v-input--disabled :deep(.v-field),
.bcds-file-input[disabled] :deep(.v-field) {
  background: var(--surface-color-forms-disabled);
  color: var(--typography-color-placeholder);
  cursor: not-allowed;
  opacity: 1 !important;
}

.bcds-file-input.v-input--disabled :deep(.v-field__overlay) {
  background-color: var(--surface-color-forms-disabled) !important;
  opacity: 1 !important;
}

.bcds-file-input.v-input--disabled :deep(.v-field__input) {
  background: var(--surface-color-forms-disabled);
  color: var(--typography-color-placeholder);
  cursor: not-allowed;
}

/* Disabled label */
.bcds-file-input-label:has(+ .bcds-file-input.v-input--disabled) {
  color: var(--typography-color-disabled) !important;
}

/* File chips styling */
.bcds-file-input :deep(.v-chip) {
  font: var(--typography-regular-small-body);
  background-color: var(--surface-color-secondary-default);
  border: var(--layout-border-width-small) solid var(--surface-color-border-default);
  border-radius: var(--layout-border-radius-medium);
}

/* Clear icon */
.bcds-file-input :deep(.v-field__clearable) {
  color: var(--icons-color-primary);
  padding-top: 0 !important;
  padding-bottom: 0 !important;
}

/* Prepend inner icon (file icon) */
.bcds-file-input :deep(.v-field__prepend-inner) {
  color: var(--icons-color-primary);
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  padding-inline-start: 0 !important;
  align-items: center;
  display: flex;
  height: 100%;
  margin-inline-end: var(--layout-margin-small) !important;
}

/* Append inner icon */
.bcds-file-input :deep(.v-field__append-inner) {
  color: var(--icons-color-primary);
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  padding-inline-end: 0 !important;
  align-items: center;
  display: flex;
  height: 100%;
  margin-inline-start: var(--layout-margin-small) !important;
}

/* Disabled icons */
.bcds-file-input.v-input--disabled :deep(.v-field__prepend-inner),
.bcds-file-input.v-input--disabled :deep(.v-field__append-inner),
.bcds-file-input.v-input--disabled :deep(.v-field__clearable) {
  color: var(--icons-color-disabled);
}

/* Column spacer */
.col-space-3 {
  flex: 0 0 3rem;
}
</style>
