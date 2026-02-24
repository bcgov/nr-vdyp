<template>
  <v-card class="elevation-0">
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
              <span class="text-h6">{{ panelTitle }}</span>
            </v-col>
            <v-col cols="auto" v-if="isFileUploadMode && !isReadOnly" class="edit-button-col">
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
        <v-expansion-panel-text class="expansion-panel-text mt-n2">
          <v-form ref="form">
            <ReportConfiguration
              :selectedAgeYearRange="currentStore.selectedAgeYearRange"
              :startingAge="currentStore.startingAge"
              :finishingAge="currentStore.finishingAge"
              :ageIncrement="currentStore.ageIncrement"
              :startYear="currentStore.startYear"
              :endYear="currentStore.endYear"
              :yearIncrement="currentStore.yearIncrement"
              :isForwardGrowEnabled="currentStore.isForwardGrowEnabled"
              :isBackwardGrowEnabled="currentStore.isBackwardGrowEnabled"
              :isComputedMAIEnabled="currentStore.isComputedMAIEnabled"
              :isCulminationValuesEnabled="
                currentStore.isCulminationValuesEnabled
              "
              :isBySpeciesEnabled="currentStore.isBySpeciesEnabled"
              :isByLayerEnabled="currentStore.isByLayerEnabled"
              :isProjectionModeEnabled="currentStore.isProjectionModeEnabled"
              :isPolygonIDEnabled="currentStore.isPolygonIDEnabled"
              :isCurrentYearEnabled="currentStore.isCurrentYearEnabled"
              :isReferenceYearEnabled="currentStore.isReferenceYearEnabled"
              :incSecondaryHeight="currentStore.incSecondaryHeight"
              :specificYear="currentStore.specificYear"
              :projectionType="currentStore.projectionType"
              :reportTitle="currentStore.reportTitle"
              :reportDescription="currentStore.reportDescription"
              :isDisabled="isInputDisabled"
              :isModelParametersMode="isModelParametersMode"
              @update:selectedAgeYearRange="handleSelectedAgeYearRangeUpdate"
              @update:startingAge="handleStartingAgeUpdate"
              @update:finishingAge="handleFinishingAgeUpdate"
              @update:ageIncrement="handleAgeIncrementUpdate"
              @update:startYear="handleStartYearUpdate"
              @update:endYear="handleEndYearUpdate"
              @update:yearIncrement="handleYearIncrementUpdate"
              @update:isForwardGrowEnabled="handleIsForwardGrowEnabledUpdate"
              @update:isBackwardGrowEnabled="handleIsBackwardGrowEnabledUpdate"
              @update:isComputedMAIEnabled="handleIsComputedMAIEnabledUpdate"
              @update:isCulminationValuesEnabled="
                handleIsCulminationValuesEnabledUpdate
              "
              @update:isBySpeciesEnabled="handleIsBySpeciesEnabledUpdate"
              @update:isByLayerEnabled="handleIsByLayerEnabledUpdate"
              @update:isProjectionModeEnabled="
                handleIsProjectionModeEnabledUpdate
              "
              @update:isPolygonIDEnabled="handleIsPolygonIDEnabledUpdate"
              @update:isCurrentYearEnabled="handleIsCurrentYearEnabledUpdate"
              @update:isReferenceYearEnabled="
                handleIsReferenceYearEnabledUpdate
              "
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
              :hideClearButton="isFileUploadMode"
              :hideEditButton="isFileUploadMode"
              :showCancelButton="isFileUploadMode"
              @clear="onClear"
              @confirm="onConfirm"
              @edit="onEdit"
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
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import {
  AppMessageDialog,
  AppButton,
} from '@/components'
import {
  ActionPanel,
  ReportConfiguration,
} from '@/components/projection'
import { CONSTANTS, MESSAGE } from '@/constants'
import { PROJECTION_ERR } from '@/constants/message'
import type { MessageDialog } from '@/interfaces/interfaces'
import type { FileUploadPanelName } from '@/types/types'
import { reportInfoValidation } from '@/validation'
import { saveProjectionOnPanelConfirm as saveModelParamProjection } from '@/services/projection/modelParameterService'
import { saveProjectionOnPanelConfirm as saveFileUploadProjection, revertPanelToSaved } from '@/services/projection/fileUploadService'
import { useNotificationStore } from '@/stores/common/notificationStore'

const form = ref<HTMLFormElement>()

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const fileUploadStore = useFileUploadStore()
const notificationStore = useNotificationStore()

const messageDialog = ref<MessageDialog>({
  dialog: false,
  title: '',
  message: '',
})

const currentStore = computed(() => {
  return appStore.modelSelection ===
    CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
    ? modelParameterStore
    : fileUploadStore
})

const panelName = computed(() => {
  return appStore.modelSelection ===
    CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
    ? CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO
    : CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO
})

const panelOpenStates = computed(() => currentStore.value.panelOpenStates)

// Check if we're in read-only (view) mode
const isReadOnly = computed(() => appStore.isReadOnly)

const isConfirmEnabled = computed(
  () => !isReadOnly.value && currentStore.value.panelState[panelName.value].editable,
)
const isConfirmed = computed(
  () => currentStore.value.panelState[panelName.value].confirmed,
)

// Determine if inputs should be disabled (read-only mode or not editable)
const isInputDisabled = computed(
  () => isReadOnly.value || !currentStore.value.panelState[panelName.value].editable,
)

const isModelParametersMode = computed(
  () =>
    appStore.modelSelection ===
    CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS,
)

const isFileUploadMode = computed(
  () => appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD,
)

const panelTitle = computed(() => {
  return isFileUploadMode.value ? 'Report Details' : 'Report Information'
})

const isHeaderEditActive = computed(() => {
  if (!isFileUploadMode.value) return false
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) return false
  return isConfirmed.value && !currentStore.value.panelState[panelName.value].editable
})

const editTooltipText = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) {
    return `This section may not be edited with a status of ${status}`
  }
  if (isConfirmed.value && !currentStore.value.panelState[panelName.value].editable) {
    return 'Click Edit to make changes to this section'
  }
  return ''
})

const onHeaderEdit = () => {
  if (isConfirmed.value) {
    currentStore.value.editPanel(panelName.value)
  }
}

const handleSelectedAgeYearRangeUpdate = (value: string) => {
  currentStore.value.selectedAgeYearRange = value
}

const handleStartingAgeUpdate = (value: string | null) => {
  currentStore.value.startingAge = value
}

const handleFinishingAgeUpdate = (value: string | null) => {
  currentStore.value.finishingAge = value
}

const handleAgeIncrementUpdate = (value: string | null) => {
  currentStore.value.ageIncrement = value
}

const handleStartYearUpdate = (value: string | null) => {
  currentStore.value.startYear = value
}

const handleEndYearUpdate = (value: string | null) => {
  currentStore.value.endYear = value
}

const handleYearIncrementUpdate = (value: string | null) => {
  currentStore.value.yearIncrement = value
}

const handleIsForwardGrowEnabledUpdate = (value: boolean) => {
  currentStore.value.isForwardGrowEnabled = value
}

const handleIsBackwardGrowEnabledUpdate = (value: boolean) => {
  currentStore.value.isBackwardGrowEnabled = value
}

const handleIsComputedMAIEnabledUpdate = (value: boolean) => {
  currentStore.value.isComputedMAIEnabled = value
}

const handleIsCulminationValuesEnabledUpdate = (value: boolean) => {
  currentStore.value.isCulminationValuesEnabled = value
}

const handleIsBySpeciesEnabledUpdate = (value: boolean) => {
  currentStore.value.isBySpeciesEnabled = value
}

const handleIsByLayerEnabledUpdate = (value: boolean) => {
  currentStore.value.isByLayerEnabled = value
}

const handleIsProjectionModeEnabledUpdate = (value: boolean) => {
  currentStore.value.isProjectionModeEnabled = value
}

const handleIsPolygonIDEnabledUpdate = (value: boolean) => {
  currentStore.value.isPolygonIDEnabled = value
}

const handleIsCurrentYearEnabledUpdate = (value: boolean) => {
  currentStore.value.isCurrentYearEnabled = value
}

const handleIsReferenceYearEnabledUpdate = (value: boolean) => {
  currentStore.value.isReferenceYearEnabled = value
}

const handleIncSecondaryHeightUpdate = (value: boolean) => {
  currentStore.value.incSecondaryHeight = value
}

const handleSpecificYearUpdate = (value: string | null) => {
  currentStore.value.specificYear = value
}

const handleProjectionTypeUpdate = (value: string | null) => {
  currentStore.value.projectionType = value
}

const handleReportTitleUpdate = (value: string | null) => {
  currentStore.value.reportTitle = value
}

const handleReportDescriptionUpdate = (value: string | null) => {
  currentStore.value.reportDescription = value
}

const validateComparison = (): boolean => {
  if (
    currentStore.value.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE
  ) {
    const result = reportInfoValidation.validateComparison(
      currentStore.value.startingAge,
      currentStore.value.finishingAge,
    )
    if (!result.isValid) {
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
        message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_FNSH_AGE,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        variant: 'error',
      }
    }
    return result.isValid
  } else {
    const result = reportInfoValidation.validateComparison(
      currentStore.value.startYear,
      currentStore.value.endYear,
    )
    if (!result.isValid) {
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
        message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_END_YEAR,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        variant: 'error',
      }
    }
    return result.isValid
  }
}

const validateRequiredFields = (): boolean => {
  if (
    currentStore.value.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE
  ) {
    const result = reportInfoValidation.validateRequiredFields(
      currentStore.value.startingAge,
      currentStore.value.finishingAge,
      currentStore.value.ageIncrement,
    )
    if (!result.isValid) {
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
        message: MESSAGE.FILE_UPLOAD_ERR.RPT_VLD_REQUIRED_FIELDS_AGE,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        variant: 'error',
      }
    }
    return result.isValid
  } else {
    const result = reportInfoValidation.validateRequiredFields(
      currentStore.value.startYear,
      currentStore.value.endYear,
      currentStore.value.yearIncrement,
    )
    if (!result.isValid) {
      const message = MESSAGE.FILE_UPLOAD_ERR.RPT_VLD_REQUIRED_FIELDS_YEAR
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
        message: message,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        variant: 'error',
      }
    }
    return result.isValid
  }
}

const validateReportTitleField = (): boolean => {
  const result = reportInfoValidation.validateReportTitle(
    currentStore.value.reportTitle,
  )
  if (!result.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.MISSING_INFO,
      message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_REPORT_TITLE_REQ,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      variant: 'error',
    }
  }
  return result.isValid
}

const validateProjectionTypeField = (): boolean => {
  const result = reportInfoValidation.validateProjectionType(
    currentStore.value.projectionType,
  )
  if (!result.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.MISSING_INFO,
      message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_PROJECTION_TYPE_REQ,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      variant: 'error',
    }
  }
  return result.isValid
}

const validateRange = (): boolean => {
  if (
    currentStore.value.selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE
  ) {
    const result = reportInfoValidation.validateAgeRange(
      currentStore.value.startingAge,
      currentStore.value.finishingAge,
      currentStore.value.ageIncrement,
    )
    if (!result.isValid) {
      let message = ''
      switch (result.errorType) {
        case 'startingAge':
          message = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_AGE_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX,
          )
          break
        case 'finishingAge':
          message = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_FNSH_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
          )
          break
        case 'ageIncrement':
          message = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_AGE_INC_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX,
          )
          break
      }
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
        message: message,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        variant: 'error',
      }
    }
    return result.isValid
  } else {
    const result = reportInfoValidation.validateYearRange(
      currentStore.value.startYear,
      currentStore.value.endYear,
      currentStore.value.yearIncrement,
    )

    if (!result.isValid) {
      let message = ''

      switch (result.errorType) {
        case 'startYear':
          message = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_YEAR_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MAX,
          )
          break
        case 'endYear':
          message = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_END_YEAR_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX,
          )
          break
        case 'yearIncrement':
          message = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_YEAR_INC_RNG(
            CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN,
            CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX,
          )
          break
      }

      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
        message: message,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
        variant: 'error',
      }
    }
    return result.isValid
  }
}

const onConfirm = async () => {
  if (!validateReportTitleField()) return
  if (!validateProjectionTypeField()) return
  if (!validateComparison()) return
  if (!validateRequiredFields()) return
  if (!validateRange()) return

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
  }

  // Save projection (create or update) before confirming the panel
  appStore.isSavingProjection = true
  try {
    if (isModelParametersMode.value) {
      await saveModelParamProjection(modelParameterStore, panelName.value)
    } else {
      await saveFileUploadProjection(fileUploadStore, panelName.value)
    }
  } catch (error) {
    console.error('Error saving projection:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, PROJECTION_ERR.SAVE_FAILED_TITLE)
    return
  } finally {
    appStore.isSavingProjection = false
  }

  // this panel is not in a confirmed state
  if (!isConfirmed.value) {
    currentStore.value.confirmPanel(panelName.value)
  }
}

const onEdit = () => {
  onHeaderEdit()
}

const onCancel = async () => {
  appStore.isSavingProjection = true
  try {
    await revertPanelToSaved(panelName.value as FileUploadPanelName)
  } catch (error) {
    console.error('Error reverting panel to saved state:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
  } finally {
    appStore.isSavingProjection = false
  }
}

const onClear = () => {
  currentStore.value.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
  currentStore.value.startingAge = null
  currentStore.value.finishingAge = null
  currentStore.value.ageIncrement = null
  currentStore.value.startYear = null
  currentStore.value.endYear = null
  currentStore.value.yearIncrement = null
  currentStore.value.isForwardGrowEnabled = false
  currentStore.value.isBackwardGrowEnabled = false
  currentStore.value.isComputedMAIEnabled = false
  currentStore.value.isCulminationValuesEnabled = false
  currentStore.value.isBySpeciesEnabled = false
  currentStore.value.isByLayerEnabled = false
  currentStore.value.isProjectionModeEnabled = false
  currentStore.value.isPolygonIDEnabled = false
  currentStore.value.isCurrentYearEnabled = false
  currentStore.value.isReferenceYearEnabled = false
  currentStore.value.incSecondaryHeight = false
  currentStore.value.specificYear = null
  currentStore.value.reportTitle = null
  currentStore.value.reportDescription = null
  currentStore.value.projectionType = null
}

const handleDialogClose = () => {}
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
