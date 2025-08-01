<template>
  <div style="position: relative; width: 100%">
    <v-text-field
      type="text"
      v-model="localValue"
      :max="max"
      :min="min"
      :step="step"
      :persistent-placeholder="persistentPlaceholder"
      :placeholder="placeholder"
      :hide-details="hideDetails"
      :density="density"
      :dense="dense"
      :style="customStyle"
      :disabled="disabled"
      @update:modelValue="handleUpdateModelValue"
    >
      <template v-slot:label>
        <span v-html="label"></span>
      </template>
    </v-text-field>
    <!-- Spin Buttons -->
    <div class="spin-box">
      <div
        class="spin-up-arrow-button"
        @mousedown="startIncrement"
        @mouseup="stopIncrement"
        @mouseleave="stopIncrement"
        :class="{ disabled: disabled }"
      >
        {{ CONSTANTS.SPIN_BUTTON.UP }}
      </div>
      <div
        class="spin-down-arrow-button"
        @mousedown="startDecrement"
        @mouseup="stopDecrement"
        @mouseleave="stopDecrement"
        :class="{ disabled: disabled }"
      >
        {{ CONSTANTS.SPIN_BUTTON.DOWN }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, type PropType } from 'vue'
import type { Density, Variant } from '@/types/types'
import { CONSTANTS } from '@/constants'
import {
  increaseItemBySpinButton,
  decrementItemBySpinButton,
} from '@/utils/util'

const props = defineProps({
  label: String,
  modelValue: {
    type: [String, null] as PropType<string | null>,
    default: null,
  },
  max: { type: Number, default: 60 },
  min: { type: Number, default: 0 },
  step: {
    type: Number,
    default: 1,
  },
  persistentPlaceholder: Boolean,
  placeholder: String,
  hideDetails: Boolean,
  density: {
    type: String as PropType<Density>,
    default: 'default',
  },
  dense: Boolean,
  customStyle: String,
  variant: {
    type: String as PropType<Variant>,
    default: 'filled',
  },
  disabled: Boolean,
  interval: {
    type: Number,
    default: CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL,
  },
  decimalAllowNumber: {
    type: Number,
    default: 2,
  },
})

const emit = defineEmits(['update:modelValue'])

const localValue = ref<string | null>(props.modelValue)

let incrementInterval: number | null = null
let decrementInterval: number | null = null

// Watch for external modelValue changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (localValue.value !== newValue) {
      localValue.value = newValue
    }
  },
)

// Emit changes back to parent
watch(localValue, (newValue) => {
  emit('update:modelValue', newValue)
})

const startIncrement = () => {
  updateValue('increment')
  incrementInterval = window.setInterval(
    () => updateValue('increment'),
    props.interval,
  )
}

const stopIncrement = () => {
  if (incrementInterval !== null) {
    clearInterval(incrementInterval)
    incrementInterval = null
  }
}

const startDecrement = () => {
  updateValue('decrement')
  decrementInterval = window.setInterval(
    () => updateValue('decrement'),
    props.interval,
  )
}

const stopDecrement = () => {
  if (decrementInterval !== null) {
    clearInterval(decrementInterval)
    decrementInterval = null
  }
}

const updateValue = (action: 'increment' | 'decrement') => {
  let newValue =
    action === 'increment'
      ? increaseItemBySpinButton(
          localValue.value,
          props.max,
          props.min,
          props.step,
        )
      : decrementItemBySpinButton(
          localValue.value,
          props.max,
          props.min,
          props.step,
        )

  localValue.value = newValue.toFixed(props.decimalAllowNumber)

  emit('update:modelValue', localValue.value)
}

const handleUpdateModelValue = (newValue: string) => {
  emit('update:modelValue', newValue)
}
</script>

<style scoped>
/* custom spin box and spin button beside text field */
.spin-box {
  position: absolute;
  right: 15px;
  top: 17px;
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
  color: #8a8a8a !important;
}

.spin-up-arrow-button {
  cursor: default;
  font-size: 7px;
  width: 10px;
  height: 10px;
  color: #898989 !important;
  background-color: #fbfbfb;
  display: flex;
  justify-content: center;
  align-items: center;
  transform: scaleX(1.5);
  padding-top: 3px;
  padding-bottom: 2px;
}

.spin-down-arrow-button {
  cursor: default;
  font-size: 7px;
  width: 10px;
  height: 10px;
  color: #898989 !important;
  background-color: #fbfbfb;
  display: flex;
  justify-content: center;
  align-items: center;
  transform: scaleX(1.5);
  padding-top: 3px;
  padding-bottom: 2px;
}

.spin-box .disabled {
  cursor: not-allowed;
  opacity: 0.5; /* Makes the button look visually disabled */
  pointer-events: none; /* Prevents clicking */
}
</style>
