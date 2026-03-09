<template>
  <v-card class="elevation-0">
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
              <span class="text-h6">Report Details</span>
            </v-col>
            <v-col cols="auto" v-if="!isReadOnly" class="edit-button-col">
              <v-tooltip :text="editTooltipText" :disabled="!editTooltipText" location="top">
                <template #activator="{ props: tooltipProps }">
                  <span v-bind="tooltipProps">
                    <AppButton
                      label="Edit"
                      variant="tertiary"
                      mdi-name="mdi-pencil-outline"
                      iconPosition="top"
                      :isDisabled="!isHeaderEditActive"
                      @click="onHeaderEdit"
                    />
                  </span>
                </template>
              </v-tooltip>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text">
          <v-form ref="form">
            <ReportConfiguration
              ref="reportConfigRef"
              :selectedAgeYearRange="fileUploadStore.selectedAgeYearRange"
              :startingAge="fileUploadStore.startingAge"
              :finishingAge="fileUploadStore.finishingAge"
              :ageIncrement="fileUploadStore.ageIncrement"
              :startYear="fileUploadStore.startYear"
              :endYear="fileUploadStore.endYear"
              :yearIncrement="fileUploadStore.yearIncrement"
              :isBySpeciesEnabled="fileUploadStore.isBySpeciesEnabled"
              :isByLayerEnabled="fileUploadStore.isByLayerEnabled"
              :isProjectionModeEnabled="fileUploadStore.isProjectionModeEnabled"
              :isPolygonIDEnabled="fileUploadStore.isPolygonIDEnabled"
              :isCurrentYearEnabled="fileUploadStore.isCurrentYearEnabled"
              :isReferenceYearEnabled="fileUploadStore.isReferenceYearEnabled"
              :incSecondaryHeight="fileUploadStore.incSecondaryHeight"
              :specificYear="fileUploadStore.specificYear"
              :projectionType="fileUploadStore.projectionType"
              :reportTitle="fileUploadStore.reportTitle"
              :reportDescription="fileUploadStore.reportDescription"
              :isDisabled="isInputDisabled"
              @update:selectedAgeYearRange="handleSelectedAgeYearRangeUpdate"
              @update:startingAge="handleStartingAgeUpdate"
              @update:finishingAge="handleFinishingAgeUpdate"
              @update:ageIncrement="handleAgeIncrementUpdate"
              @update:startYear="handleStartYearUpdate"
              @update:endYear="handleEndYearUpdate"
              @update:yearIncrement="handleYearIncrementUpdate"
              @update:isBySpeciesEnabled="handleIsBySpeciesEnabledUpdate"
              @update:isByLayerEnabled="handleIsByLayerEnabledUpdate"
              @update:isProjectionModeEnabled="handleIsProjectionModeEnabledUpdate"
              @update:isPolygonIDEnabled="handleIsPolygonIDEnabledUpdate"
              @update:isCurrentYearEnabled="handleIsCurrentYearEnabledUpdate"
              @update:isReferenceYearEnabled="handleIsReferenceYearEnabledUpdate"
              @update:incSecondaryHeight="handleIncSecondaryHeightUpdate"
              @update:specificYear="handleSpecificYearUpdate"
              @update:projectionType="handleProjectionTypeUpdate"
              @update:reportTitle="handleReportTitleUpdate"
              @update:reportDescription="handleReportDescriptionUpdate"
            />
            <ActionPanel
              v-if="!isReadOnly"
              :isConfirmEnabled="isConfirmEnabled"
              :isConfirmed="isConfirmed"
              :hideClearButton="true"
              :hideEditButton="true"
              :showCancelButton="true"
              @confirm="onConfirm"
              @cancel="onCancel"
            />
          </v-form>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>
<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAppStore } from '@/stores/projection/appStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { AppButton } from '@/components'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import {
  ActionPanel,
  ReportConfiguration,
} from '@/components/projection'
import { CONSTANTS, MESSAGE } from '@/constants'
import { PROJECTION_ERR } from '@/constants/message'
import type { FileUploadPanelName } from '@/types/types'
import { reportInfoValidation } from '@/validation'
import { saveProjectionOnPanelConfirm as saveFileUploadProjection, revertPanelToSaved, hasMinimumDBHUnsavedChanges } from '@/services/projection/fileUploadService'
import { useNotificationStore } from '@/stores/common/notificationStore'

const form = ref<HTMLFormElement>()
const reportConfigRef = ref<{ validateTitle: () => boolean; validateFields: () => boolean } | null>(null)

const appStore = useAppStore()
const fileUploadStore = useFileUploadStore()
const notificationStore = useNotificationStore()
const alertDialogStore = useAlertDialogStore()

const panelName = CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO
const panelOpenStates = computed(() => fileUploadStore.panelOpenStates)

// Check if we're in read-only (view) mode
const isReadOnly = computed(() => appStore.isReadOnly)

const isConfirmEnabled = computed(
  () => !isReadOnly.value && fileUploadStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => fileUploadStore.panelState[panelName].confirmed,
)

// Determine if inputs should be disabled (read-only mode or not editable)
const isInputDisabled = computed(
  () => isReadOnly.value || !fileUploadStore.panelState[panelName].editable,
)

const isHeaderEditActive = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) return false
  return isConfirmed.value && !fileUploadStore.panelState[panelName].editable
})

const editTooltipText = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) {
    return `This section may not be edited with a status of ${status}`
  }
  if (isConfirmed.value && !fileUploadStore.panelState[panelName].editable) {
    return 'Click Edit to make changes to this section'
  }
  return ''
})

const handleMinimumDBHRevert = async (): Promise<boolean> => {
  const minDBHState = fileUploadStore.panelState[CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH]
  if (!minDBHState.editable) return true

  const hasChanges = await hasMinimumDBHUnsavedChanges(fileUploadStore)
  if (!hasChanges) return true

  const proceed = await alertDialogStore.openDialog(
    MESSAGE.UNSAVED_CHANGES_DIALOG.TITLE,
    MESSAGE.UNSAVED_CHANGES_DIALOG.MESSAGE,
    { variant: 'warning' },
  )
  if (!proceed) return false

  appStore.isSavingProjection = true
  try {
    await revertPanelToSaved(CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH)
  } catch (error) {
    console.error('Error reverting MinimumDBH panel to saved state:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
    return false
  } finally {
    appStore.isSavingProjection = false
  }
  return true
}

const onHeaderEdit = async () => {
  if (isConfirmed.value) {
    if (!(await handleMinimumDBHRevert())) return
    fileUploadStore.editPanel(panelName)
  }
}

// Common update handlers
const handleSelectedAgeYearRangeUpdate = (value: string) => {
  fileUploadStore.selectedAgeYearRange = value
}

const handleStartingAgeUpdate = (value: string | null) => {
  fileUploadStore.startingAge = value
}

const handleFinishingAgeUpdate = (value: string | null) => {
  fileUploadStore.finishingAge = value
}

const handleAgeIncrementUpdate = (value: string | null) => {
  fileUploadStore.ageIncrement = value
}

const handleStartYearUpdate = (value: string | null) => {
  fileUploadStore.startYear = value
}

const handleEndYearUpdate = (value: string | null) => {
  fileUploadStore.endYear = value
}

const handleYearIncrementUpdate = (value: string | null) => {
  fileUploadStore.yearIncrement = value
}

// Common update handlers (shared checkboxes)
const handleIsBySpeciesEnabledUpdate = (value: boolean) => {
  fileUploadStore.isBySpeciesEnabled = value
}

const handleIncSecondaryHeightUpdate = (value: boolean) => {
  fileUploadStore.incSecondaryHeight = value
}

// File Upload specific update handlers
const handleIsByLayerEnabledUpdate = (value: boolean) => {
  fileUploadStore.isByLayerEnabled = value
}

const handleIsProjectionModeEnabledUpdate = (value: boolean) => {
  fileUploadStore.isProjectionModeEnabled = value
}

const handleIsPolygonIDEnabledUpdate = (value: boolean) => {
  fileUploadStore.isPolygonIDEnabled = value
}

const handleIsCurrentYearEnabledUpdate = (value: boolean) => {
  fileUploadStore.isCurrentYearEnabled = value
}

const handleIsReferenceYearEnabledUpdate = (value: boolean) => {
  fileUploadStore.isReferenceYearEnabled = value
}

const handleSpecificYearUpdate = (value: string | null) => {
  fileUploadStore.specificYear = value
}

const handleProjectionTypeUpdate = (value: string | null) => {
  fileUploadStore.projectionType = value
}

const handleReportTitleUpdate = (value: string | null) => {
  fileUploadStore.reportTitle = value
}

const handleReportDescriptionUpdate = (value: string | null) => {
  fileUploadStore.reportDescription = value
}

const validateReportTitleField = (): boolean => {
  if (reportConfigRef.value) {
    return reportConfigRef.value.validateTitle()
  }
  return reportInfoValidation.validateReportTitle(fileUploadStore.reportTitle).isValid
}

const onConfirm = async () => {
  if (!validateReportTitleField()) return
  if (reportConfigRef.value && !reportConfigRef.value.validateFields()) return

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
  }

  // Save projection (create or update) before confirming the panel
  appStore.isSavingProjection = true
  try {
    await saveFileUploadProjection(fileUploadStore, panelName)
  } catch (error) {
    console.error('Error saving projection:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, PROJECTION_ERR.SAVE_FAILED_TITLE)
    return
  } finally {
    appStore.isSavingProjection = false
  }

  // this panel is not in a confirmed state
  if (!isConfirmed.value) {
    fileUploadStore.confirmPanel(panelName)
  }
}

const onCancel = async () => {
  appStore.isSavingProjection = true
  try {
    await revertPanelToSaved(panelName as FileUploadPanelName)
  } catch (error) {
    console.error('Error reverting panel to saved state:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
  } finally {
    appStore.isSavingProjection = false
  }
}
</script>
<style scoped>
.edit-button-col {
  display: flex;
  align-items: center;
}

.edit-button-col :deep(.bcds-button.icon-top) {
  padding: 2px 4px;
  gap: 2px;
}

.edit-button-col :deep(.bcds-button.icon-top .v-icon) {
  font-size: 18px;
}

.edit-button-col :deep(.bcds-button.icon-top .button-label) {
  font-size: 11px;
}
</style>
