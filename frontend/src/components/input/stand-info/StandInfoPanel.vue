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
    <v-expansion-panels v-model="panelOpenStates.standInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <!-- Place an arrow icon to the left of the title -->
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.standInfo === CONSTANTS.PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Stand Information</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text mt-n2">
          <v-form ref="form">
            <v-row style="height: 70px !important">
              <v-col cols="3">
                <v-text-field
                  label="% Stockable Area"
                  type="number"
                  v-model.number="percentStockableArea"
                  :max="CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX"
                  :min="CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN"
                  :step="CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_STEP"
                  placeholder=""
                  persistent-placeholder
                  hide-details
                  density="compact"
                  dense
                  :disabled="!isConfirmEnabled"
                  data-testid="percent-stockable-area"
                ></v-text-field>
                <v-label
                  v-show="isEmptyOrZero(percentStockableArea)"
                  style="font-size: 12px"
                  >{{ MESSAGE.MDL_PRM_INPUT_HINT.SITE_DFT_COMPUTED }}</v-label
                >
              </v-col>
            </v-row>
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
import { AppMessageDialog, AppPanelActions } from '@/components'
import { CONSTANTS, MESSAGE } from '@/constants'
import type { MessageDialog } from '@/interfaces/interfaces'
import { standInfoValidation } from '@/validation'
import { isEmptyOrZero } from '@/utils/util'

const form = ref<HTMLFormElement>()

const modelParameterStore = useModelParameterStore()

const messageDialog = ref<MessageDialog>({
  dialog: false,
  title: '',
  message: '',
})

const { panelOpenStates, percentStockableArea } =
  storeToRefs(modelParameterStore)

const panelName = CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO
const isConfirmEnabled = computed(
  () => modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)

const onConfirm = () => {
  // validation - range
  const rangeResult = standInfoValidation.validateRange(
    percentStockableArea.value,
  )
  if (!rangeResult.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_PCT_STCB_AREA_RNG,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
    return
  }

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
  }

  if (isEmptyOrZero(percentStockableArea.value)) {
    percentStockableArea.value = 0
  }

  // this panel is not in a confirmed state
  if (!isConfirmed.value) {
    modelParameterStore.confirmPanel(panelName)
  }
}

const onEdit = () => {
  // this panel has already been confirmed.
  if (isConfirmed.value) {
    modelParameterStore.editPanel(panelName)
  }
}

const onClear = () => {
  if (form.value) {
    form.value.reset()
  }
}

const handleDialogClose = () => {}
</script>

<style scoped></style>
