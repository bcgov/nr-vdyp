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
              :volumeReported="currentStore.volumeReported"
              :includeInReport="currentStore.includeInReport"
              :projectionType="currentStore.projectionType"
              :reportTitle="currentStore.reportTitle"
              :isDisabled="!isConfirmEnabled"
              @update:selectedAgeYearRange="handleSelectedAgeYearRangeUpdate"
              @update:startingAge="handleStartingAgeUpdate"
              @update:finishingAge="handleFinishingAgeUpdate"
              @update:ageIncrement="handleAgeIncrementUpdate"
              @update:startYear="handleStartYearUpdate"
              @update:endYear="handleEndYearUpdate"
              @update:yearIncrement="handleYearIncrementUpdate"
              @update:volumeReported="handleVolumeReportedUpdate"
              @update:includeInReport="handleIncludeInReportUpdate"
              @update:projectionType="handleProjectionTypeUpdate"
              @update:reportTitle="handleReportTitleUpdate"
            />
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
import { useAppStore } from '@/stores/appStore'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import {
  AppMessageDialog,
  AppPanelActions,
  ReportConfiguration,
} from '@/components'
import { CONSTANTS, DEFAULTS, MESSAGE } from '@/constants'
import type { MessageDialog } from '@/interfaces/interfaces'
import { reportInfoValidation } from '@/validation'

const form = ref<HTMLFormElement>()

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const fileUploadStore = useFileUploadStore()

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
const isConfirmEnabled = computed(
  () => currentStore.value.panelState[panelName.value].editable,
)
const isConfirmed = computed(
  () => currentStore.value.panelState[panelName.value].confirmed,
)

const handleSelectedAgeYearRangeUpdate = (value: string) => {
  currentStore.value.selectedAgeYearRange = value
}

const handleStartingAgeUpdate = (value: number | null) => {
  currentStore.value.startingAge = value
}

const handleFinishingAgeUpdate = (value: number | null) => {
  currentStore.value.finishingAge = value
}

const handleAgeIncrementUpdate = (value: number | null) => {
  currentStore.value.ageIncrement = value
}

const handleStartYearUpdate = (value: number | null) => {
  currentStore.value.startYear = value
}

const handleEndYearUpdate = (value: number | null) => {
  currentStore.value.endYear = value
}

const handleYearIncrementUpdate = (value: number | null) => {
  currentStore.value.yearIncrement = value
}

const handleVolumeReportedUpdate = (value: string[]) => {
  currentStore.value.volumeReported = [...value]
}

const handleIncludeInReportUpdate = (value: string[]) => {
  currentStore.value.includeInReport = [...value]
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
      }
    }
    return result.isValid
  }
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
      }
    }
    return result.isValid
  }
}

const onConfirm = () => {
  if (!validateComparison()) return
  if (!validateRequiredFields()) return
  if (!validateRange()) return

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
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
  currentStore.value.volumeReported = []
  currentStore.value.includeInReport = []
  currentStore.value.reportTitle = null
  currentStore.value.projectionType = DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE
}

const handleDialogClose = () => {}
</script>
<style scoped></style>
