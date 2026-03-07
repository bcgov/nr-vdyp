<template>
  <div style="width: 100%">
    <span class="bcds-text-field-label" v-html="label"></span>
    <div class="spin-field-wrapper">
      <v-text-field
        type="text"
        v-model="localValue"
        :max="max"
        :min="min"
        :step="step"
        :persistent-placeholder="persistentPlaceholder"
        :placeholder="placeholder"
        :hide-details="hasError ? 'auto' : hideDetails"
        :error="hasError"
        :error-messages="errorMessages"
        :style="customStyle"
        :disabled="disabled"
        @update:modelValue="handleUpdateModelValue"
        @keydown="handleKeyDown"
      >
        <template #append-inner>
          <!-- Spin Buttons -->
          <div class="spin-box">
            <div
              class="spin-up-arrow-button"
              @mousedown="startIncrement"
              @mouseup="stopIncrement"
              @mouseleave="stopIncrement"
              :class="{ disabled: disabled }"
            >
              <!-- CSS triangle instead of text -->
            </div>
            <div
              class="spin-down-arrow-button"
              @mousedown="startDecrement"
              @mouseup="stopDecrement"
              @mouseleave="stopDecrement"
              :class="{ disabled: disabled }"
            >
              <!-- CSS triangle instead of text -->
            </div>
          </div>
        </template>
      </v-text-field>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, type PropType } from 'vue'
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
  errorMessages: {
    type: [String, Array] as PropType<string | string[]>,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const hasError = computed(() => {
  const msgs = props.errorMessages
  if (Array.isArray(msgs)) return msgs.some((m) => !!m)
  return !!msgs
})

const localValue = ref<string | null>(props.modelValue)

let incrementInterval: ReturnType<typeof setInterval> | null = null
let decrementInterval: ReturnType<typeof setInterval> | null = null

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
  incrementInterval = globalThis.setInterval(
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
  decrementInterval = globalThis.setInterval(
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

const filterNumeric = (value: string): string => {
  const allowDecimal = props.decimalAllowNumber > 0
  let filtered = allowDecimal ? value.replaceAll(/[^\d.]/g, '') : value.replaceAll(/\D/g, '')
  if (allowDecimal) {
    const parts = filtered.split('.')
    if (parts.length > 2) {
      filtered = parts[0] + '.' + parts.slice(1).join('')
    }
  }
  return filtered
}

const handleUpdateModelValue = (newValue: string) => {
  const filtered = filterNumeric(newValue)
  if (filtered !== newValue) {
    localValue.value = filtered
  }
  emit('update:modelValue', filtered)
}

const handleKeyDown = (event: KeyboardEvent) => {
  if (props.disabled) return

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    updateValue('increment')
  } else if (event.key === 'ArrowDown') {
    event.preventDefault()
    updateValue('decrement')
  } else {
    const allowedKeys = ['Backspace', 'Delete', 'Tab', 'Escape', 'Enter', 'Home', 'End', 'ArrowLeft', 'ArrowRight']
    if (allowedKeys.includes(event.key)) return
    if (event.ctrlKey || event.metaKey) return
    if (/^\d$/.test(event.key)) return
    if (event.key === '.' && props.decimalAllowNumber > 0) return
    event.preventDefault()
  }
}
</script>

<style scoped>
/* Spin box styles are defined in src/styles/_spin-field.scss */

.spin-field-wrapper {
  position: relative;
}
</style>
