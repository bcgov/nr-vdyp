<template>
  <v-card class="elevation-4">
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
              <span class="text-h6">Report Information</span>
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
            />
            <ActionPanel
              v-if="!isReadOnly"
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
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import {
  AppMessageDialog,
} from '@/components'
import {
  ActionPanel,
  ReportConfiguration,
} from '@/components/projection'
import { CONSTANTS, MESSAGE } from '@/constants'
import { PROJECTION_ERR } from '@/constants/message'
import type { MessageDialog } from '@/interfaces/interfaces'
import { reportInfoValidation } from '@/validation'
import { saveProjectionOnPanelConfirm as saveModelParamProjection } from '@/services/projection/modelParameterService'
import { saveProjectionOnPanelConfirm as saveFileUploadProjection } from '@/services/projection/fileUploadService'
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
  try {
    if (isModelParametersMode.value) {
      await saveModelParamProjection(modelParameterStore, panelName.value)
    } else {
      await saveFileUploadProjection(fileUploadStore, panelName.value)
    }
  } catch (error) {
    console.error('Error saving projection:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, 'Projection Save Failed')
    return
  }

  // this panel is not in a confirmed state
  if (!isConfirmed.value) {
    currentStore.value.confirmPanel(panelName.value)
  }
}

const onEdit = () => {
  // this panel has already been confirmed.
  if (isConfirmed.value) {
    currentStore.value.editPanel(panelName.value)
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
  currentStore.value.projectionType = null
}

const handleDialogClose = () => {}
</script>
<style scoped />
