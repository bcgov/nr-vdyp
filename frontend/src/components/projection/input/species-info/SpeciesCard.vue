<template>
  <v-card class="species-card" variant="outlined">
    <div class="species-card-body">
      <!-- Left: species info -->
      <div class="species-left">
        <div class="species-name-text">{{ speciesName }}</div>
        <div class="species-details-row">
          <span>Species Group:<strong class="ml-1">{{ speciesGroupCode }}</strong></span>
          <span class="ml-3">Site Species:<strong class="ml-1">{{ speciesCode }}</strong></span>
        </div>
      </div>

      <!-- Divider 1: vertical on desktop, horizontal on mobile -->
      <div class="card-divider card-divider--main" />

      <!-- Right section: percentage + delete (same row on all sizes) -->
      <div class="card-right-section">
        <div class="percent-section">
          <label class="bcds-text-field-label" :for="`species-percent-${speciesCode}`">Percentage</label>
          <div class="percent-field-wrapper" ref="wrapperRef">
            <span
              v-if="localPercent"
              class="percent-sign-overlay"
              :style="percentOverlayStyle"
              aria-hidden="true"
            >%</span>
            <v-text-field
              ref="textFieldRef"
              :id="`species-percent-${speciesCode}`"
              type="text"
              v-model="localPercent"
              :rules="[validatePercent]"
              :disabled="isDisabled"
              hide-details="auto"
              density="compact"
              variant="underlined"
              class="percent-field"
              @update:focused="handleBlur"
              @update:modelValue="handleInput"
              @keydown="handleKeyDown"
            >
              <template #append-inner>
                <div class="spin-box">
                  <div
                    class="spin-up-arrow-button"
                    @mousedown="startIncrement"
                    @mouseup="stopIncrement"
                    @mouseout="handleIncMouseout"
                    :class="{ disabled: isDisabled }"
                  />
                  <div
                    class="spin-down-arrow-button"
                    @mousedown="startDecrement"
                    @mouseup="stopDecrement"
                    @mouseout="handleDecMouseout"
                    :class="{ disabled: isDisabled }"
                  />
                </div>
              </template>
            </v-text-field>
          </div>
        </div>

        <!-- Divider 2: always vertical -->
        <div class="card-divider card-divider--inner" />

        <!-- Delete button -->
        <div class="delete-section">
          <button
            type="button"
            class="delete-btn"
            :class="{ 'delete-btn--disabled': isDisabled }"
            :disabled="isDisabled"
            @click="!isDisabled && $emit('delete')"
            aria-label="Delete species"
          >
            <v-icon size="28">mdi-delete-outline</v-icon>
            <span class="delete-label">Delete</span>
          </button>
        </div>
      </div>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { BIZCONSTANTS, CONSTANTS, MESSAGE } from '@/constants'
import { speciesInfoValidation } from '@/validation'
import { increaseItemBySpinButton, decrementItemBySpinButton } from '@/utils/util'

const props = defineProps<{
  speciesCode: string
  percent: string | null
  isDisabled: boolean
}>()

const emit = defineEmits<{
  'update:percent': [value: string]
  'percent-blur': []
  'delete': []
}>()

const speciesName = computed(
  () =>
    BIZCONSTANTS.SPECIES_MAP[props.speciesCode as keyof typeof BIZCONSTANTS.SPECIES_MAP] ??
    props.speciesCode,
)

const speciesGroupCode = computed(
  () => BIZCONSTANTS.SPECIES_GROUP_MAP[props.speciesCode] ?? props.speciesCode,
)

const localPercent = ref(props.percent ?? '0.0')

const wrapperRef = ref<HTMLElement | null>(null)
const textFieldRef = ref()
const percentOverlayStyle = ref<Record<string, string>>({})

const _measureCanvas = document.createElement('canvas')

const updatePercentOverlay = async () => {
  if (!localPercent.value || !wrapperRef.value || !textFieldRef.value) {
    percentOverlayStyle.value = {}
    return
  }

  await nextTick()

  const inputEl = textFieldRef.value.$el?.querySelector('input') as HTMLInputElement | null
  if (!inputEl) return

  const wrapperRect = wrapperRef.value.getBoundingClientRect()
  const inputRect = inputEl.getBoundingClientRect()
  const style = globalThis.getComputedStyle(inputEl)
  const paddingLeft = Number.parseFloat(style.paddingLeft) || 16

  const ctx = _measureCanvas.getContext('2d')
  let textWidth = 0
  if (ctx) {
    ctx.font = style.font
    textWidth = ctx.measureText(localPercent.value).width
  }

  percentOverlayStyle.value = {
    left: `${inputRect.left - wrapperRect.left + paddingLeft + textWidth}px`,
    top: `${inputRect.top - wrapperRect.top}px`,
    height: `${inputRect.height}px`,
    color: style.color,
    fontSize: style.fontSize,
    fontFamily: style.fontFamily,
  }
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (wrapperRef.value) {
    resizeObserver = new ResizeObserver(() => {
      if (localPercent.value) updatePercentOverlay()
    })
    resizeObserver.observe(wrapperRef.value)
  }
  updatePercentOverlay()
})

watch(
  () => props.percent,
  (val) => {
    const v = val ?? '0.0' // NOSONAR S7760 - val is string|null; default param only handles undefined
    if (v !== localPercent.value) localPercent.value = v
  },
)

watch(localPercent, (val) => {
  emit('update:percent', val)
  updatePercentOverlay()
})

const validatePercent = (v: string | null): boolean | string => {
  const result = speciesInfoValidation.validatePercent(v)
  if (!result.isValid) {
    return MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_INPUT_RANGE(
      CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX,
    )
  }
  return true
}

const updateValue = (action: 'increment' | 'decrement') => {
  const max = CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX
  const min = CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN
  const step = CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_STEP
  const newVal =
    action === 'increment'
      ? increaseItemBySpinButton(localPercent.value, max, min, step)
      : decrementItemBySpinButton(localPercent.value, max, min, step)
  localPercent.value = newVal.toFixed(CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM)
}

let incId: ReturnType<typeof globalThis.setInterval> | null = null
let decId: ReturnType<typeof globalThis.setInterval> | null = null

const startIncrement = () => {
  updateValue('increment')
  incId = globalThis.setInterval(
    () => updateValue('increment'),
    CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL,
  )
}
const stopIncrement = () => {
  if (incId !== null) {
    clearInterval(incId)
    incId = null
  }
}
const startDecrement = () => {
  updateValue('decrement')
  decId = globalThis.setInterval(
    () => updateValue('decrement'),
    CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL,
  )
}
const stopDecrement = () => {
  if (decId !== null) {
    clearInterval(decId)
    decId = null
  }
}

const handleIncMouseout = () => {
  stopIncrement()
  emit('percent-blur')
}

const handleDecMouseout = () => {
  stopDecrement()
  emit('percent-blur')
}

const handleBlur = (focused: boolean) => {
  if (focused) return
  if (!localPercent.value) return
  if (localPercent.value.startsWith('.')) {
    localPercent.value = `0${localPercent.value}`
  }
  if (!localPercent.value.includes('.') || localPercent.value.endsWith('.')) {
    localPercent.value = Number.parseFloat(localPercent.value).toFixed(
      CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM,
    )
  }
  localPercent.value = Number.parseFloat(localPercent.value).toFixed(
    CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM,
  )
  emit('percent-blur')
}

const handleInput = () => {
  let v = localPercent.value || ''
  v = v.replaceAll(/[^0-9.]/g, '')
  const dotIdx = v.indexOf('.')
  if (dotIdx !== -1) {
    v = v.slice(0, dotIdx + 1) + v.slice(dotIdx + 1).replaceAll('.', '')
  }
  localPercent.value = v
}

const handleKeyDown = (event: KeyboardEvent) => {
  if (props.isDisabled) return
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    updateValue('increment')
  } else if (event.key === 'ArrowDown') {
    event.preventDefault()
    updateValue('decrement')
  }
}

onBeforeUnmount(() => {
  stopIncrement()
  stopDecrement()
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.species-card {
  background-color: #d8eafd !important;
  border-color: #757575 !important;
  border-radius: 4px;
}

.species-card-body {
  display: flex;
  align-items: stretch;
  min-height: 64px;
}

/* White divider */
.card-divider {
  background-color: white;
  flex-shrink: 0;
}

.card-divider--main {
  width: 1px;
  align-self: stretch;
  margin: 10px 0;
}

.card-divider--inner {
  width: 1px;
  align-self: stretch;
  margin: 10px 0;
}

/* Right section: percentage + delete always on same row */
.card-right-section {
  display: flex;
  flex-direction: row;
  align-items: stretch;
}

/* Left: species info */
.species-left {
  flex: 1;
  min-width: 0;
  padding: 10px 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.species-name-text {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.species-details-row {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.7);
  white-space: nowrap;
}

@media (min-width: 600px) and (max-width: 1280px) {
  .species-details-row {
    display: flex;
    flex-direction: column;
    gap: 2px;
    white-space: normal;
  }
  .species-details-row span:first-child {
    margin-left: 12px;
  }
}

@media (min-width: 960px) and (max-width: 1279px) {
  .species-details-row {
    flex-direction: row;
    gap: 0;
    white-space: nowrap;
  }
  .species-details-row span:first-child {
    margin-left: 0;
  }
}

/* Center: percentage */
.percent-section {
  flex-shrink: 0;
  width: 127px;
  padding: 8px 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.percent-field-wrapper {
  position: relative;
  width: 100%;
}

.percent-field {
  width: 100%;
}

.percent-field :deep(.v-field__append-inner) {
  padding-inline-start: 0px;
}

.percent-sign-overlay {
  position: absolute;
  display: flex;
  align-items: center;
  pointer-events: none;
  z-index: 2;
  white-space: nowrap;
  user-select: none;
}

/* Right: delete button */
.delete-section {
  flex-shrink: 0;
  width: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 8px;
}

.delete-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: rgba(0, 0, 0, 0.55);
}

.delete-btn:hover:not(.delete-btn--disabled) {
  color: rgba(0, 0, 0, 0.87);
}

.delete-btn--disabled {
  color: rgba(0, 0, 0, 0.26);
  cursor: not-allowed;
}

.delete-label {
  font-size: 11px;
  line-height: 1;
}

@media (max-width: 599px) {
  /* Card body becomes vertical on mobile */
  .species-card-body {
    flex-direction: column;
  }

  /* Main divider becomes horizontal */
  .card-divider--main {
    width: auto;
    height: 1px;
    margin: 0 10px;
    align-self: auto;
  }

  /* Right section stays as a row (percent + delete side by side) */
  .card-right-section {
    flex: 1;
  }

  /* Percent section grows to fill available space */
  .percent-section {
    flex: 1;
    width: auto;
  }
}
</style>
