<template>
  <v-card class="elevation-4">
    <AppMessageDialog
      :dialog="messageDialog.dialog"
      :title="messageDialog.title"
      :message="messageDialog.message"
      :dialogWidth="messageDialog.dialogWidth"
      :btnLabel="messageDialog.btnLabel"
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
        <v-expansion-panel-text class="expansion-panel-text mt-n2">
          <v-form ref="form">
            <div class="ml-2 mt-10 mb-10">
              <v-row>
                <v-col cols="5">
                  <v-file-input
                    id="polygon-file-input"
                    :label="
                      fileUploadStore.polygonFile
                        ? 'Polygon File'
                        : 'Select Polygon File...'
                    "
                    v-model="fileUploadStore.polygonFile"
                    show-size
                    chips
                    clearable
                    density="compact"
                    accept=".csv"
                    :disabled="!isConfirmEnabled"
                  />
                </v-col>
                <v-col class="col-space-3" />
                <v-col cols="5">
                  <v-file-input
                    id="layer-file-input"
                    :label="
                      fileUploadStore.layerFile
                        ? 'Layer File'
                        : 'Select Layer File...'
                    "
                    v-model="fileUploadStore.layerFile"
                    show-size
                    chips
                    clearable
                    density="compact"
                    accept=".csv"
                    :disabled="!isConfirmEnabled"
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

const validateFiles = async (): Promise<boolean> => {
  const result = await fileUploadValidation.validateFiles(
    fileUploadStore.polygonFile,
    fileUploadStore.layerFile,
  )
  if (!result.isValid) {
    let message = ''
    switch (result.errorType) {
      case 'polygonFileMissing':
        message = MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_MISSING
        break
      case 'layerFileMissing':
        message = MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_MISSING
        break
      case 'polygonFileNotCSVFormat':
        message = MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_NOT_CSV_FORMAT
        break
      case 'layerFileNotCSVFormat':
        message = MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_NOT_CSV_FORMAT
        break
    }
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_FILE,
      message: message,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }

    return false
  }

  let message = ''
  const MAX_DISPLAY_ERR_ITEMS = 2

  // Polygon file duplicate column validation
  if (fileUploadStore.polygonFile) {
    const polygonDuplicateResult = await fileUploadValidation.validatePolygonDuplicateColumns(
      fileUploadStore.polygonFile,
    )
    if (!polygonDuplicateResult.isValid) {
      message = MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_DUPLICATE_COLUMNS
      if (polygonDuplicateResult.duplicates.length > 0) {
        message += '\n\nDuplicate columns found:\n' +
          formatErrColumnList(
            polygonDuplicateResult.duplicates,
            MAX_DISPLAY_ERR_ITEMS,
            'duplicate',
          )
      }
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.POLYGON_FILE_DUPLICATE_COLUMNS,
        message,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        dialogWidth: 500,
      }
      return false
    }
  }

  // Polygon file header validation with details
  if (fileUploadStore.polygonFile) {
    const polygonResult = await fileUploadValidation.validatePolygonHeader(
      fileUploadStore.polygonFile,
    )
    if (!polygonResult.isValid) {
      const { details } = polygonResult
      if (details.missing.length > 0) {
        message +=
          'Missing columns:\n' +
          formatErrColumnList(
            details.missing,
            MAX_DISPLAY_ERR_ITEMS,
            'missing',
          ) +
          '\n\n'
      }
      if (details.extra.length > 0) {
        message +=
          'Extra columns:\n' +
          formatErrColumnList(details.extra, MAX_DISPLAY_ERR_ITEMS, 'extra') +
          '\n\n'
      }
      if (details.mismatches.length > 0) {
        message +=
          'Mismatches:\n' +
          formatErrColumnList(
            details.mismatches,
            MAX_DISPLAY_ERR_ITEMS,
            'mismatch',
          ) +
          '\n\n'
      }
      message += 'Please ensure the headers match the required order.'
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.POLYGON_FILE_HEADER_MISMATCH,
        message,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        dialogWidth: 500,
      }
      return false
    }
  }

  // Layer file duplicate column validation
  if (fileUploadStore.layerFile) {
    const layerDuplicateResult = await fileUploadValidation.validateLayerDuplicateColumns(
      fileUploadStore.layerFile,
    )
    if (!layerDuplicateResult.isValid) {
      message = MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_DUPLICATE_COLUMNS
      if (layerDuplicateResult.duplicates.length > 0) {
        message += '\n\nDuplicate columns found:\n' +
          formatErrColumnList(
            layerDuplicateResult.duplicates,
            MAX_DISPLAY_ERR_ITEMS,
            'duplicate',
          )
      }
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.LAYER_FILE_DUPLICATE_COLUMNS,
        message,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        dialogWidth: 500,
      }
      return false
    }
  }

  // Layer file header validation with details
  if (fileUploadStore.layerFile) {
    const layerResult = await fileUploadValidation.validateLayerHeader(
      fileUploadStore.layerFile,
    )
    if (!layerResult.isValid) {
      const { details } = layerResult
      if (details.missing.length > 0) {
        message +=
          'Missing columns:\n' +
          formatErrColumnList(
            details.missing,
            MAX_DISPLAY_ERR_ITEMS,
            'missing',
          ) +
          '\n\n'
      }
      if (details.extra.length > 0) {
        message +=
          'Extra columns:\n' +
          formatErrColumnList(details.extra, MAX_DISPLAY_ERR_ITEMS, 'extra') +
          '\n\n'
      }
      if (details.mismatches.length > 0) {
        message +=
          'Mismatches:\n' +
          formatErrColumnList(
            details.mismatches,
            MAX_DISPLAY_ERR_ITEMS,
            'mismatch',
          ) +
          '\n\n'
      }
      message += 'Please ensure the headers match the required order.'
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.LAYER_FILE_HEADER_MISMATCH,
        message,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        dialogWidth: 500,
      }
      return false
    }
  }

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
.col-space-3 {
  flex: 0 0 3rem;
}
</style>
