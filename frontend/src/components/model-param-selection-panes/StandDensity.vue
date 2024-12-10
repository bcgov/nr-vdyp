<template>
  <v-card class="elevation-4">
    <v-expansion-panels v-model="panelOpenStates.standDensity">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <!-- Place an arrow icon to the left of the title -->
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.standDensity === PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Stand Density</span>
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
                  :max="NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX"
                  :min="NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN"
                  :step="NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_STEP"
                  placeholder=""
                  persistent-placeholder
                  hide-details
                  density="compact"
                  dense
                  :disabled="!isConfirmEnabled"
                ></v-text-field>
                <v-label
                  v-show="Util.isZeroValue(percentStockableArea)"
                  style="font-size: 12px"
                  >{{ MDL_PRM_INPUT_HINT.SITE_DFT_COMPUTED }}</v-label
                >
              </v-col>
            </v-row>
            <v-card-actions class="mt-5 pr-0">
              <v-spacer></v-spacer>
              <v-btn
                class="white-btn"
                :disabled="!isConfirmEnabled"
                @click="clear"
                >Clear</v-btn
              >
              <v-btn
                v-show="!isConfirmed"
                class="blue-btn ml-2"
                :disabled="!isConfirmEnabled"
                @click="onConfirm"
                >Confirm</v-btn
              >
              <v-btn v-show="isConfirmed" class="blue-btn ml-2" @click="onEdit"
                >Edit</v-btn
              >
            </v-card-actions>
          </v-form>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Util } from '@/utils/util'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useMessageDialogStore } from '@/stores/common/messageDialogStore'
import { storeToRefs } from 'pinia'
import {
  PANEL,
  MODEL_PARAMETER_PANEL,
  NUM_INPUT_LIMITS,
} from '@/constants/constants'
import {
  MDL_PRM_INPUT_ERR,
  MSG_DIALOG_TITLE,
  MDL_PRM_INPUT_HINT,
} from '@/constants/message'
import { StandDensityValidation } from '@/validation/standDensityValidation'

const form = ref<HTMLFormElement>()

const standDensityValidator = new StandDensityValidation()

const modelParameterStore = useModelParameterStore()
const messageDialogStore = useMessageDialogStore()

const { panelOpenStates, percentStockableArea } =
  storeToRefs(modelParameterStore)

const panelName = MODEL_PARAMETER_PANEL.STAND_DENSITY
const isConfirmEnabled = computed(
  () => modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)

const validateRange = (): boolean => {
  if (
    !standDensityValidator.validatePercentStockableAreaRange(
      percentStockableArea.value,
    )
  ) {
    messageDialogStore.openDialog(
      MSG_DIALOG_TITLE.INVALID_INPUT,
      MDL_PRM_INPUT_ERR.DENSITY_VLD_PCT_STCB_AREA_RNG,
      { width: 400 },
    )
    return false
  }

  return true
}

const validateFormInputs = async (): Promise<boolean> => {
  if (!validateRange()) {
    return false
  }

  return true
}

const onConfirm = async () => {
  const isFormValid = await validateFormInputs()

  if (!isFormValid) {
    return
  }

  if (form.value) {
    form.value.validate()
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

const clear = () => {
  if (form.value) {
    form.value.reset()
  }
}
</script>

<style scoped></style>
