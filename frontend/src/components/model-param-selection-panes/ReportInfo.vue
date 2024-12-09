<template>
  <v-card class="elevation-4">
    <v-expansion-panels v-model="panelOpenStates.reportInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.reportInfo === PANEL.OPEN
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
            <div>
              <v-row>
                <v-col cols="3">
                  <v-text-field
                    label="Starting Age"
                    type="number"
                    v-model.number="startingAge"
                    :min="NUM_INPUT_LIMITS.STARTING_AGE_MIN"
                    :max="NUM_INPUT_LIMITS.STARTING_AGE_MAX"
                    :step="NUM_INPUT_LIMITS.STARTING_AGE_STEP"
                    persistent-placeholder
                    placeholder=""
                    density="compact"
                    dense
                    :disabled="!isConfirmEnabled"
                  ></v-text-field>
                </v-col>
                <v-col class="col-space-3" />
                <v-col cols="3">
                  <v-text-field
                    label="Finishing Age"
                    type="number"
                    v-model.number="finishingAge"
                    :min="NUM_INPUT_LIMITS.FINISHING_AGE_MIN"
                    :max="NUM_INPUT_LIMITS.FINISHING_AGE_MAX"
                    :step="NUM_INPUT_LIMITS.FINISHING_AGE_STEP"
                    persistent-placeholder
                    placeholder=""
                    density="compact"
                    dense
                    :disabled="!isConfirmEnabled"
                  ></v-text-field>
                </v-col>
                <v-col class="col-space-3" />
                <v-col cols="3">
                  <v-text-field
                    label="Age Increment"
                    type="number"
                    v-model.number="ageIncrement"
                    :min="NUM_INPUT_LIMITS.AGE_INC_MIN"
                    :max="NUM_INPUT_LIMITS.AGE_INC_MAX"
                    :step="NUM_INPUT_LIMITS.AGE_INC_STEP"
                    persistent-placeholder
                    placeholder=""
                    density="compact"
                    dense
                    :disabled="!isConfirmEnabled"
                  ></v-text-field>
                </v-col>
              </v-row>
            </div>
            <div class="ml-4 mt-5">
              <div class="ml-n4 mt-n5">
                <span class="text-h7">Volume Reported</span>
              </div>
              <v-row class="ml-n6">
                <v-col cols="12" style="padding-top: 0px">
                  <v-row>
                    <v-col
                      v-for="(option, index) in volumeReportedOptions"
                      :key="index"
                      :style="{ 'max-width': index < 4 ? '20%' : 'auto' }"
                    >
                      <v-checkbox
                        v-model="volumeReported"
                        :label="option.label"
                        :value="option.value"
                        hide-details
                        :disabled="!isConfirmEnabled"
                      ></v-checkbox>
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
            </div>
            <div class="ml-4 mt-5">
              <div class="ml-n4 mt-n5">
                <span class="text-h7">Include in Report</span>
              </div>
              <v-row class="ml-n6">
                <v-col cols="12" style="padding-top: 0px">
                  <v-row>
                    <v-col
                      v-for="(option, index) in includeInReportOptions"
                      :key="index"
                      :style="{ 'max-width': index < 4 ? '20%' : 'auto' }"
                    >
                      <v-checkbox
                        v-model="includeInReport"
                        :label="option.label"
                        :value="option.value"
                        hide-details
                        :disabled="!isConfirmEnabled"
                      ></v-checkbox>
                    </v-col>
                    <v-col style="max-width: 20% !important">
                      <v-select
                        label="Projection Type"
                        :items="projectionTypeOptions"
                        v-model="projectionType"
                        item-title="label"
                        item-value="value"
                        hide-details="auto"
                        persistent-placeholder
                        placeholder="Select..."
                        density="compact"
                        dense
                        style="max-width: 70% !important"
                        :disabled="!isConfirmEnabled"
                      ></v-select>
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
            </div>
            <div class="ml-4 mt-5">
              <div class="ml-n4 mt-n5">
                <span class="text-h7">Report Title</span>
              </div>
              <v-row>
                <v-col cols="6">
                  <v-text-field
                    type="string"
                    v-model="reportTitle"
                    hide-details="auto"
                    persistent-placeholder
                    placeholder="Enter a report title..."
                    density="compact"
                    dense
                    style="max-width: 50% !important"
                    :disabled="!isConfirmEnabled"
                  ></v-text-field>
                </v-col>
              </v-row>
            </div>
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
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useMessageDialogStore } from '@/stores/common/messageDialogStore'
import { storeToRefs } from 'pinia'
import {
  volumeReportedOptions,
  includeInReportOptions,
  projectionTypeOptions,
} from '@/constants/options'
import {
  PANEL,
  MODEL_PARAMETER_PANEL,
  NUM_INPUT_LIMITS,
} from '@/constants/constants'
import { DEFAULT_VALUES } from '@/constants/defaults'
import { MDL_PRM_INPUT_ERR, MSG_DIALOG_TITLE } from '@/constants/message'
import { ReportInfoValidation } from '@/validation/reportInfoValidation'

const form = ref<HTMLFormElement>()

const reportInfoValidator = new ReportInfoValidation()

const modelParameterStore = useModelParameterStore()
const messageDialogStore = useMessageDialogStore()

const {
  panelOpenStates,
  speciesGroups,
  startingAge,
  finishingAge,
  ageIncrement,
  volumeReported,
  includeInReport,
  projectionType,
  reportTitle,
} = storeToRefs(modelParameterStore)

const panelName = MODEL_PARAMETER_PANEL.REPORT_INFO
const isConfirmEnabled = computed(
  () => modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)

const validateComparison = (): boolean => {
  if (
    !reportInfoValidator.validateAgeComparison(
      finishingAge.value,
      startingAge.value,
    )
  ) {
    messageDialogStore.openDialog(
      MSG_DIALOG_TITLE.INVALID_INPUT,
      MDL_PRM_INPUT_ERR.RPT_VLD_COMP_FNSH_AGE,
      { width: 400 },
    )
    return false
  }

  return true
}

const validateRange = (): boolean => {
  if (!reportInfoValidator.validateStartingAgeRange(startingAge.value)) {
    messageDialogStore.openDialog(
      MSG_DIALOG_TITLE.INVALID_INPUT,
      MDL_PRM_INPUT_ERR.RPT_VLD_START_AGE_RNG(
        NUM_INPUT_LIMITS.STARTING_AGE_MIN,
        NUM_INPUT_LIMITS.STARTING_AGE_MAX,
      ),
      { width: 400 },
    )
    return false
  }

  if (!reportInfoValidator.validateFinishingAgeRange(finishingAge.value)) {
    messageDialogStore.openDialog(
      MSG_DIALOG_TITLE.INVALID_INPUT,
      MDL_PRM_INPUT_ERR.RPT_VLD_START_FNSH_RNG(
        NUM_INPUT_LIMITS.FINISHING_AGE_MIN,
        NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
      ),
      { width: 400 },
    )
    return false
  }

  if (!reportInfoValidator.validateAgeIncrementRange(ageIncrement.value)) {
    messageDialogStore.openDialog(
      MSG_DIALOG_TITLE.INVALID_INPUT,
      MDL_PRM_INPUT_ERR.RPT_VLD_AGE_INC_RNG(
        NUM_INPUT_LIMITS.AGE_INC_MIN,
        NUM_INPUT_LIMITS.AGE_INC_MAX,
      ),
      { width: 400 },
    )
    return false
  }

  return true
}

const onConfirm = () => {
  if (validateComparison() && validateRange()) {
    if (form.value) {
      form.value.validate()
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

const clear = () => {
  startingAge.value = null
  finishingAge.value = null
  ageIncrement.value = null
  volumeReported.value = []
  includeInReport.value = []
  reportTitle.value = null

  projectionType.value = DEFAULT_VALUES.PROJECTION_TYPE
  speciesGroups.value = speciesGroups.value.map((group) => ({
    ...group,
  }))
}
</script>
<style scoped></style>
