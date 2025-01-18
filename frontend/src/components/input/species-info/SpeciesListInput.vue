<template>
  <div>
    <div v-for="(item, index) in localSpeciesList" :key="index">
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
            data-testid="species-select"
            @update:model-value="handleUpdateSpecies"
          ></v-select>
        </v-col>
        <v-col cols="6">
          <div style="position: relative; width: 100%">
            <v-text-field
              :label="`Species #${index + 1} Percent`"
              type="text"
              v-model="item.percent"
              :max="max"
              :min="min"
              :step="step"
              :rules="[validatePercent]"
              persistent-placeholder
              placeholder="Select..."
              density="compact"
              dense
              data-testid="species-percent"
              @update:focused="handlePercentBlur"
              @update:modelValue="handlePercentInput(index)"
              :disabled="!isConfirmEnabled"
            ></v-text-field>
            <div class="spin-box">
              <div
                class="spin-up-arrow-button"
                @mousedown="startIncrement(index)"
                @mouseup="stopIncrement"
                @mouseout="handleIncMouseout"
                :class="{ disabled: !isConfirmEnabled }"
              >
                {{ CONSTANTS.SPIN_BUTTON.UP }}
              </div>
              <div
                class="spin-down-arrow-button"
                @mousedown="startDecrement(index)"
                @mouseup="stopDecrement"
                @mouseout="handleDecMouseout"
                :class="{ disabled: !isConfirmEnabled }"
              >
                {{ CONSTANTS.SPIN_BUTTON.DOWN }}
              </div>
            </div>
          </div>
        </v-col>
      </v-row>
      <div class="hr-line mb-1"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, watch, type PropType } from 'vue'
import { CONSTANTS, MESSAGE } from '@/constants'
import type { SpeciesList } from '@/interfaces/interfaces'
import { speciesInfoValidation } from '@/validation'
import { Util } from '@/utils/util'
import { cloneDeep } from 'lodash'

const props = defineProps({
  speciesList: {
    type: Array as PropType<SpeciesList[]>,
    required: true,
  },
  computedSpeciesOptions: {
    type: Array as PropType<{ label: string; value: string }[]>,
    required: true,
  },
  isConfirmEnabled: {
    type: Boolean,
    required: true,
  },
  max: {
    type: Number,
    default: CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX,
  },
  min: {
    type: Number,
    default: CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN,
  },
  step: {
    type: Number,
    default: CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_STEP,
  },
})

const emit = defineEmits(['update:speciesList'])

const localSpeciesList = ref(cloneDeep(props.speciesList))

// Watch for external speciesList changes
watch(
  () => props.speciesList,
  (newList) => {
    const isDifferent =
      newList.some(
        (item) =>
          !localSpeciesList.value.some(
            (localItem) =>
              localItem.species === item.species &&
              localItem.percent === item.percent,
          ),
      ) ||
      localSpeciesList.value.some(
        (localItem) =>
          !newList.some(
            (item) =>
              item.species === localItem.species &&
              item.percent === localItem.percent,
          ),
      )

    if (isDifferent) {
      localSpeciesList.value = cloneDeep(newList)
    }
  },
  { deep: true },
)

// Emit changes back to parent
watch(
  localSpeciesList,
  (newSpeciesList) => {
    emit('update:speciesList', newSpeciesList)
  },
  { deep: true },
)

let incrementIntervalId: number | null = null
let decrementIntervalId: number | null = null

const updateValue = (action: 'increment' | 'decrement', index: number) => {
  const localPercent = localSpeciesList.value[index].percent
  let newValue =
    action === 'increment'
      ? Util.increaseItemBySpinButton(
          localPercent,
          props.max,
          props.min,
          props.step,
        )
      : Util.decrementItemBySpinButton(
          localPercent,
          props.max,
          props.min,
          props.step,
        )

  localSpeciesList.value[index].percent = newValue.toFixed(
    CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM,
  )

  if (Util.isEmptyOrZero(localSpeciesList.value[index].percent)) {
    localSpeciesList.value[index].species = null
  }
}

const startIncrement = (index: number) => {
  updateValue('increment', index)
  incrementIntervalId = window.setInterval(
    () => updateValue('increment', index),
    CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL,
  )
}

const stopIncrement = () => {
  if (incrementIntervalId !== null) {
    clearInterval(incrementIntervalId)
    incrementIntervalId = null
  }
}

const startDecrement = (index: number) => {
  updateValue('decrement', index)
  decrementIntervalId = window.setInterval(
    () => updateValue('decrement', index),
    CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL,
  )
}

const stopDecrement = () => {
  if (decrementIntervalId !== null) {
    clearInterval(decrementIntervalId)
    decrementIntervalId = null
  }
}

const triggerSpeciesSortByPercent = () => {
  localSpeciesList.value.sort((a: SpeciesList, b: SpeciesList) => {
    const percentA = parseFloat(a.percent || '0')
    const percentB = parseFloat(b.percent || '0')

    // Empty species are sent backward in the sort
    if (!a.species) return 1
    if (!b.species) return -1

    // Sort by percent in descending order
    return percentB - percentA
  })
}

const validatePercent = (percent: string | null): boolean | string => {
  const validationResult = speciesInfoValidation.validatePercent(percent)
  if (!validationResult.isValid) {
    return MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_INPUT_RANGE(
      CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX,
    )
  }
  return true
}

// Loop through speciesList to set percent to 0.0 for empty species
const handleUpdateSpecies = () => {
  for (const item of localSpeciesList.value) {
    if (!item.species) {
      item.percent = '0.0'
    }
  }

  triggerSpeciesSortByPercent()
}

// NOTE: "update:focused" event fired when the input is Focused or Blurred
const handlePercentBlur = () => {
  for (const item of localSpeciesList.value) {
    if (item.percent) {
      // Add a leading zero if the value starts with a decimal point (e.g., '.1' -> '0.1')
      if (item.percent.startsWith('.')) {
        item.percent = `0${item.percent}`
      }

      // Format percent value to fixed decimal places if it lacks a decimal or ends with a decimal point
      if (!item.percent.includes('.') || item.percent.endsWith('.')) {
        item.percent = parseFloat(item.percent).toFixed(
          CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM,
        )
      }

      // Parse the percent value, round it to the nearest allowed decimal place, and convert back to string
      const roundedValue = parseFloat(item.percent).toFixed(
        CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM,
      )

      item.percent = roundedValue
    }
  }

  triggerSpeciesSortByPercent()
}

const handlePercentInput = (index: number) => {
  let localPercent = localSpeciesList.value[index].percent || ''

  // Allow only numbers and a single '.'
  let cleanedValue = localPercent.replace(/[^0-9.]/g, '')

  // If there are multiple '.', keep only the first one
  const dotIndex = cleanedValue.indexOf('.')
  if (dotIndex !== -1) {
    cleanedValue =
      cleanedValue.slice(0, dotIndex + 1) +
      cleanedValue.slice(dotIndex + 1).replace(/\./g, '')
  }
  // Update the percent value in the list
  localSpeciesList.value[index].percent = cleanedValue

  // If the value is 0.0, set the species to null
  if (Util.isEmptyOrZero(localSpeciesList.value[index].percent)) {
    localSpeciesList.value[index].species = null
  }
}

const handleIncMouseout = () => {
  stopIncrement()
  triggerSpeciesSortByPercent()
}

const handleDecMouseout = () => {
  stopDecrement()
  triggerSpeciesSortByPercent()
}

onBeforeUnmount(() => {
  stopIncrement()
  stopDecrement()
})
</script>

<style scoped>
/* custom spin box and spin button beside text field */
.spin-box {
  position: absolute;
  right: 15px;
  top: 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 16px;
  height: 20px;
  background-color: transparent;
  padding: 2px;
}

/* mouse over */
.spin-box div:hover {
  background-color: #d3d3d3 !important;
}

.spin-up-arrow-button {
  cursor: pointer;
  font-size: 7px;
  width: 10px;
  height: 10px;
  background-color: #f2f0f0;
  display: flex;
  justify-content: center;
  align-items: center;
  transform: scaleX(1.5);
  padding-top: 3px;
  padding-bottom: 2px;
}

.spin-down-arrow-button {
  cursor: pointer;
  font-size: 7px;
  width: 10px;
  height: 10px;
  background-color: #f2f0f0;
  display: flex;
  justify-content: center;
  align-items: center;
  transform: scaleX(1.5);
  padding-top: 3px;
  padding-bottom: 2px;
}

/* bottom line under text field */
.spin-text-field-bottom-line {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background-color: #ababab;
}

.spin-box .disabled {
  cursor: not-allowed;
  opacity: 0.5; /* Makes the button look visually disabled */
  pointer-events: none; /* Prevents clicking */
}
</style>
