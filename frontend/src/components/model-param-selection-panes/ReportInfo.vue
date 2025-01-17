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
    <v-expansion-panels v-model="panelOpenStates.reportInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.reportInfo === CONSTANTS.PANEL.OPEN
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
              :startingAge="startingAge"
              :finishingAge="finishingAge"
              :ageIncrement="ageIncrement"
              :volumeReported="volumeReported"
              :includeInReport="includeInReport"
              :projectionType="projectionType"
              :reportTitle="reportTitle"
              :isDisabled="!isConfirmEnabled"
              @update:startingAge="handleStartingAgeUpdate"
              @update:finishingAge="handleFinishingAgeUpdate"
              @update:ageIncrement="handleAgeIncrementUpdate"
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
import { storeToRefs } from 'pinia'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import {
  AppMessageDialog,
  AppPanelActions,
  ReportConfiguration,
} from '@/components'
import { CONSTANTS, DEFAULTS, MESSAGE } from '@/constants'
import type { MessageDialog } from '@/interfaces/interfaces'
import { ReportInfoValidation } from '@/validation/reportInfoValidation'

const form = ref<HTMLFormElement>()

const reportInfoValidator = new ReportInfoValidation()

const modelParameterStore = useModelParameterStore()

const messageDialog = ref<MessageDialog>({
  dialog: false,
  title: '',
  message: '',
})

const {
  panelOpenStates,
  startingAge,
  finishingAge,
  ageIncrement,
  volumeReported,
  includeInReport,
  projectionType,
  reportTitle,
} = storeToRefs(modelParameterStore)

const panelName = CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO
const isConfirmEnabled = computed(
  () => modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)

const handleStartingAgeUpdate = (value: number | null) => {
  startingAge.value = value
}

const handleFinishingAgeUpdate = (value: number | null) => {
  finishingAge.value = value
}

const handleAgeIncrementUpdate = (value: number | null) => {
  ageIncrement.value = value
}

const handleVolumeReportedUpdate = (value: string[]) => {
  volumeReported.value = [...value]
}

const handleIncludeInReportUpdate = (value: string[]) => {
  includeInReport.value = [...value]
}

const handleProjectionTypeUpdate = (value: string | null) => {
  projectionType.value = value
}

const handleReportTitleUpdate = (value: string | null) => {
  reportTitle.value = value
}

const validateComparison = (): boolean => {
  if (
    !reportInfoValidator.validateAgeComparison(
      finishingAge.value,
      startingAge.value,
    )
  ) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_FNSH_AGE,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }

    return false
  }

  return true
}

const validateRange = (): boolean => {
  if (!reportInfoValidator.validateStartingAgeRange(startingAge.value)) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_AGE_RNG(
        CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
        CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX,
      ),
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
    return false
  }

  if (!reportInfoValidator.validateFinishingAgeRange(finishingAge.value)) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_FNSH_RNG(
        CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN,
        CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
      ),
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
    return false
  }

  if (!reportInfoValidator.validateAgeIncrementRange(ageIncrement.value)) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_AGE_INC_RNG(
        CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
        CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX,
      ),
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
    return false
  }

  return true
}

const onConfirm = () => {
  if (validateComparison() && validateRange()) {
    if (form.value) {
      form.value.validate()
    } else {
      console.warn('Form reference is null. Validation skipped.')
    }

    // this panel is not in a confirmed state
    if (!isConfirmed.value) {
      modelParameterStore.confirmPanel(panelName)
    }
  }
}

const onEdit = () => {
  // this panel has already been confirmed.
  if (isConfirmed.value) {
    modelParameterStore.editPanel(panelName)
  }
}

const onClear = () => {
  startingAge.value = null
  finishingAge.value = null
  ageIncrement.value = null
  volumeReported.value = []
  includeInReport.value = []
  reportTitle.value = null
  projectionType.value = DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE
}

const handleDialogClose = () => {}
</script>
<style scoped></style>
