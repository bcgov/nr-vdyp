<template>
  <div class="debug-mode-section">
    <label
      class="bcds-radio-label"
      :class="{ 'bcds-radio-label--disabled': disabled }"
      for="debugMode"
    >Debug Mode</label>
    <v-radio-group
      id="debugMode"
      :model-value="modelValue"
      inline
      hide-details
      :disabled="disabled"
      class="mt-0 pt-0"
      data-testid="debug-mode"
      @update:model-value="emit('update:modelValue', $event === true)"
    >
      <v-radio label="On" :value="true"></v-radio>
      <v-radio label="Off" :value="false"></v-radio>
    </v-radio-group>
    <div v-if="warningMessage" class="debug-mode-message debug-mode-warning" data-testid="debug-mode-warning">
      <v-icon size="small" class="debug-mode-warning-icon">mdi-alert-outline</v-icon>
      <span>{{ warningMessage }}</span>
    </div>
    <div v-else class="debug-mode-message" data-testid="debug-mode-hint">
      {{ MESSAGE.DEBUG_MODE.PROCESSING_TIME_HINT }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { MESSAGE } from '@/constants'

withDefaults(
  defineProps<{
    modelValue: boolean
    disabled?: boolean
    // Replaces the processing-time hint when Debug Mode is unavailable
    warningMessage?: string
  }>(),
  {
    disabled: false,
    warningMessage: '',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()
</script>

<style scoped>
.debug-mode-section {
  display: flex;
  flex-direction: column;
}

.debug-mode-message {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-xsmall);
  font: var(--typography-regular-small-body);
  color: var(--typography-color-primary);
}

.debug-mode-warning-icon {
  color: var(--typography-color-primary);
}
</style>
