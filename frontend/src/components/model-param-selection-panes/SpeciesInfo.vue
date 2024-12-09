<template>
  <v-card class="elevation-4">
    <v-expansion-panels v-model="panelOpenStates.speciesInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">
                {{
                  panelOpenStates.speciesInfo === PANEL.OPEN
                    ? 'mdi-chevron-up'
                    : 'mdi-chevron-down'
                }}
              </v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Species Information</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text mt-n4">
          <v-form ref="form">
            <div class="mt-1">
              <v-row style="display: inline-flex; align-items: center">
                <v-col cols="auto" style="margin-bottom: 20px">
                  <div>Species % derived by:</div>
                </v-col>
                <v-col cols="auto">
                  <div>
                    <v-radio-group
                      v-model="derivedBy"
                      inline
                      :disabled="!isConfirmEnabled"
                    >
                      <v-radio
                        v-for="option in derivedByOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      ></v-radio>
                    </v-radio-group>
                  </div>
                </v-col>
              </v-row>
            </div>
            <div class="mt-n3">
              <v-row>
                <v-col cols="5">
                  <div v-for="(item, index) in speciesList" :key="index">
                    <v-row>
                      <v-col cols="6">
                        <v-select
                          :label="`Species #${index + 1}`"
                          :items="computedSpeciesOptions"
                          v-model="item.species"
                          item-title="label"
                          item-value="value"
                          clearable
                          hide-details="auto"
                          persistent-placeholder
                          placeholder="Select..."
                          density="compact"
                          dense
                          :disabled="!isConfirmEnabled"
                        ></v-select>
                      </v-col>
                      <v-col cols="6">
                        <div style="position: relative; width: 100%">
                          <v-text-field
                            :label="`Species #${index + 1} Percent`"
                            type="text"
                            v-model="item.percent"
                            :max="NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX"
                            :min="NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN"
                            :step="NUM_INPUT_LIMITS.SPECIES_PERCENT_STEP"
                            :rules="[validatePercent]"
                            persistent-placeholder
                            placeholder="Select..."
                            density="compact"
                            dense
                            @blur="triggerSpeciesSortByPercent"
                            @input="handlePercentInput($event, index)"
                            :disabled="!isConfirmEnabled"
                          ></v-text-field>
                          <!-- Custom spin buttons -->
                          <div class="spin-box">
                            <div
                              class="spin-up-arrow-button"
                              @mousedown="startIncrementPercent(index)"
                              @mouseup="stopIncrementPercent"
                              @mouseleave="stopIncrementPercent"
                              :class="{ disabled: !isConfirmEnabled }"
                            >
                              {{ SPIN_BUTTON.UP }}
                            </div>
                            <div
                              class="spin-down-arrow-button"
                              @mousedown="startDecrementPercent(index)"
                              @mouseup="stopDecrementPercent"
                              @mouseleave="stopDecrementPercent"
                              :class="{ disabled: !isConfirmEnabled }"
                            >
                              {{ SPIN_BUTTON.DOWN }}
                            </div>
                          </div>
                        </div>
                      </v-col>
                    </v-row>
                    <div class="hr-line mb-1"></div>
                  </div>
                </v-col>
                <v-col class="vertical-line pb-0" />
                <!-- output -->
                <v-col cols="6" v-if="speciesGroups.length > 0">
                  <div
                    v-for="(group, index) in speciesGroups"
                    :key="index"
                    class="mt-2"
                  >
                    <v-row>
                      <v-col cols="4" sm="4" md="4">
                        <v-text-field
                          label="Species Group"
                          :model-value="group.group"
                          variant="underlined"
                          disabled
                          density="compact"
                          dense
                        ></v-text-field>
                      </v-col>
                      <v-col cols="4" sm="4" md="4">
                        <v-text-field
                          label="Species Group Percent"
                          :model-value="group.percent"
                          variant="underlined"
                          disabled
                          density="compact"
                          dense
                        ></v-text-field>
                      </v-col>
                      <v-col cols="4" sm="4" md="4">
                        <v-text-field
                          label="Site Species"
                          :model-value="group.siteSpecies"
                          variant="underlined"
                          disabled
                          density="compact"
                          dense
                        ></v-text-field>
                      </v-col>
                    </v-row>
                    <div class="hr-line mb-3"></div>
                  </div>
                </v-col>
                <v-col cols="6" v-else>
                  <div class="mt-2">
                    <v-row
                      ><v-col cols="4" sm="4" md="4">
                        <v-text-field
                          label="Species Group"
                          variant="underlined"
                          disabled
                          persistent-placeholder
                          placeholder=""
                          density="compact"
                          dense
                        ></v-text-field></v-col
                      ><v-col cols="4" sm="4" md="4">
                        <v-text-field
                          label="Species Group Percent"
                          variant="underlined"
                          disabled
                          persistent-placeholder
                          placeholder=""
                          density="compact"
                          dense
                        ></v-text-field></v-col
                      ><v-col cols="4" sm="4" md="4">
                        <v-text-field
                          label="Site Species"
                          variant="underlined"
                          disabled
                          persistent-placeholder
                          placeholder=""
                          density="compact"
                          dense
                        ></v-text-field
                      ></v-col>
                    </v-row>
                  </div>
                </v-col>
              </v-row>
            </div>
            <div>
              <v-row>
                <v-col cols="5">
                  <div>
                    <v-row>
                      <v-col cols="6"></v-col>
                      <v-col cols="6">
                        <v-text-field
                          label="Total Species Percent"
                          :model-value="totalSpeciesPercent"
                          variant="underlined"
                          disabled
                          density="compact"
                          dense
                        ></v-text-field>
                      </v-col>
                    </v-row>
                  </div>
                </v-col>
                <v-col class="vertical-line" />
                <v-col cols="6" />
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
import { ref, watch, computed } from 'vue'
import { Util } from '@/utils/util'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useMessageDialogStore } from '@/stores/common/messageDialogStore'
import { storeToRefs } from 'pinia'
import { derivedByOptions } from '@/constants/options'
import { SPECIES_MAP } from '@/constants/mappings'
import {
  PANEL,
  MODEL_PARAMETER_PANEL,
  NUM_INPUT_LIMITS,
  CONTINUOUS_INC_DEC,
  SPIN_BUTTON,
} from '@/constants/constants'
import { DEFAULT_VALUES } from '@/constants/defaults'
import { MDL_PRM_INPUT_ERR, MSG_DIALOG_TITLE } from '@/constants/message'
import { SpeciesInfoValidation } from '@/validation/speciesInfoValidation'

const form = ref<HTMLFormElement>()

const speciesInfoValidator = new SpeciesInfoValidation()

const modelParameterStore = useModelParameterStore()
const messageDialogStore = useMessageDialogStore()

const {
  panelOpenStates,
  derivedBy,
  speciesList,
  speciesGroups,
  totalSpeciesPercent,
  totalSpeciesGroupPercent,
} = storeToRefs(modelParameterStore)

const panelName = MODEL_PARAMETER_PANEL.SPECIES_INFO
const isConfirmEnabled = computed(
  () => modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)

const computedSpeciesOptions = computed(() =>
  (Object.keys(SPECIES_MAP) as Array<keyof typeof SPECIES_MAP>).map((code) => ({
    label: `${code} - ${SPECIES_MAP[code]}`,
    value: code,
  })),
)

const updateSpeciesGroup = modelParameterStore.updateSpeciesGroup

// Interval references for continuous increment/decrement
let percentIncrementInterval: number | null = null
let percentDecrementInterval: number | null = null

watch(
  speciesList,
  (newSpeciesList) => {
    // Sort only if all items contain species
    const shouldSort = newSpeciesList.every((item) => item.species)
    if (shouldSort) {
      triggerSpeciesSortByPercent()
    }

    updateSpeciesGroup()
  },
  { deep: true },
)

const incrementPercent = (index: number) => {
  if (speciesList.value[index]) {
    const newValue = Util.increaseItemBySpinButton(
      speciesList.value[index].percent,
      NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX,
      NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN,
      NUM_INPUT_LIMITS.SPECIES_PERCENT_STEP,
    )
    speciesList.value[index].percent = newValue.toFixed(
      NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM,
    )

    // Sort only when species exists
    if (speciesList.value[index].species) {
      triggerSpeciesSortByPercent()
    }
  }
}

const decrementPercent = (index: number) => {
  if (speciesList.value[index]) {
    const newValue = Util.decrementItemBySpinButton(
      speciesList.value[index].percent,
      NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX,
      NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN,
      NUM_INPUT_LIMITS.SPECIES_PERCENT_STEP,
    )
    speciesList.value[index].percent = newValue.toFixed(
      NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM,
    )

    // sort only when species is present, remove species if value is 0
    if (Util.isEmptyOrZero(newValue)) {
      speciesList.value[index].species = null
    } else if (speciesList.value[index].species) {
      triggerSpeciesSortByPercent()
    }
  }
}

// Continuous increment/decrement functions
const startIncrementPercent = (index: number) => {
  incrementPercent(index)
  percentIncrementInterval = window.setInterval(
    () => incrementPercent(index),
    CONTINUOUS_INC_DEC.INTERVAL,
  )
}

const stopIncrementPercent = () => {
  if (percentIncrementInterval !== null) {
    clearInterval(percentIncrementInterval)
    percentIncrementInterval = null
  }
}

const startDecrementPercent = (index: number) => {
  decrementPercent(index)
  percentDecrementInterval = window.setInterval(
    () => decrementPercent(index),
    CONTINUOUS_INC_DEC.INTERVAL,
  )
}

const stopDecrementPercent = () => {
  if (percentDecrementInterval !== null) {
    clearInterval(percentDecrementInterval)
    percentDecrementInterval = null
  }
}

const triggerSpeciesSortByPercent = () => {
  speciesList.value.sort((a, b) => {
    const percentA = parseFloat(a.percent || '0')
    const percentB = parseFloat(b.percent || '0')

    // Empty species are sent backward in the sort
    if (!a.species) return 1
    if (!b.species) return -1

    // Sort by percent in descending order
    return percentB - percentA
  })
}

// allow only up to the first decimal place
const handlePercentInput = (event: Event, index: number) => {
  const input = event.target as HTMLInputElement
  let value = input.value

  if (value.includes('.')) {
    const [integerPart, decimalPart] = value.split('.')
    if (decimalPart.length > 1) {
      value = `${integerPart}.${decimalPart.slice(0, 1)}`
    }
  }

  speciesList.value[index].percent = value

  // sort only when species is present, remove species if value is 0
  if (Util.isEmptyOrZero(value)) {
    speciesList.value[index].species = null
  } else if (speciesList.value[index].species) {
    triggerSpeciesSortByPercent()
  }
}

const validatePercent = (percent: any) => {
  const isValid = speciesInfoValidator.validatePercent(percent)
  if (!isValid) {
    return MDL_PRM_INPUT_ERR.SPCZ_VLD_INPUT_RANGE(
      NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN,
      NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX,
    )
  }
  return true
}

const validateDuplicateSpecies = () => {
  const duplicateSpecies = speciesInfoValidator.validateDuplicateSpecies(
    speciesList.value,
  )
  if (duplicateSpecies) {
    const speciesLabel = (
      Object.keys(SPECIES_MAP) as Array<keyof typeof SPECIES_MAP>
    ).find((key) => key === duplicateSpecies)
      ? SPECIES_MAP[duplicateSpecies as keyof typeof SPECIES_MAP]
      : ''

    const message = speciesLabel
      ? MDL_PRM_INPUT_ERR.SPCZ_VLD_DUP_W_LABEL(duplicateSpecies, speciesLabel)
      : MDL_PRM_INPUT_ERR.SPCZ_VLD_DUP_WO_LABEL(duplicateSpecies)

    messageDialogStore.openDialog(MSG_DIALOG_TITLE.DATA_DUPLICATED, message)
    return false
  }

  return true
}

const validateTotalSpeciesPercent = () => {
  if (
    !speciesInfoValidator.validateTotalSpeciesPercent(
      totalSpeciesPercent.value,
      totalSpeciesGroupPercent.value,
    )
  ) {
    messageDialogStore.openDialog(
      MSG_DIALOG_TITLE.DATA_INCOMPLETE,
      MDL_PRM_INPUT_ERR.SPCZ_VLD_TOTAL_PCT,
      { width: 400 },
    )
    return false
  }
  return true
}

const validateRequired = () => {
  if (!speciesInfoValidator.validateRequired(derivedBy.value)) {
    messageDialogStore.openDialog(
      MSG_DIALOG_TITLE.MISSING_INFO,
      MDL_PRM_INPUT_ERR.SPCZ_VLD_MISSING_DERIVED_BY,
      { width: 400 },
    )
    return false
  }
  return true
}

const onConfirm = () => {
  if (
    validateDuplicateSpecies() &&
    validateTotalSpeciesPercent() &&
    validateRequired()
  ) {
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
  for (const item of speciesList.value) {
    item.species = null
    item.percent = null
  }

  if (form.value) {
    form.value.reset()
  }

  derivedBy.value = DEFAULT_VALUES.DERIVED_BY
}
</script>

<style scoped>
.vertical-line {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 1px;
}

.vertical-line::before {
  content: '';
  display: block;
  border-left: 1px dashed rgba(0, 0, 0, 0.12);
  height: 100%;
}

/* custom spin box and spin button beside text field */
.spin-box {
  top: 16px;
}
</style>
