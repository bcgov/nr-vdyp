<template>
  <v-snackbar
    v-model="isVisibleLocal"
    :timeout="computedAutoTimeout ? computedTimeout : -1"
    :color="computedColor"
    :location="computedLocation"
    :show-timer="showTimer"
    class="app-snackbar"
    :class="computedType"
    variant="flat"
    @update:model-value="onClose"
    @click:outside="onClose"
  >
    <v-icon class="app-snackbar__icon">{{ getIcon(computedType) }}</v-icon>
    <div class="app-snackbar__container">
      <span>{{ computedMessage }}</span>
    </div>

    <template #actions>
      <div class="app-snackbar__close-icon">
        <v-btn icon variant="text" density="compact" @click="onClose">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </div>
    </template>
  </v-snackbar>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  isVisible?: boolean
  message?: string
  type?: 'info' | 'success' | 'error' | 'warning' | ''
  color?: string
  location?: 'top' | 'center' | 'bottom' | 'right' | 'left'
  timeout?: number
  autoTimeout?: boolean
  showTimer?: boolean
}>()

const emit = defineEmits(['update:isVisible', 'close'])

const isVisibleLocal = ref(props.isVisible)

watch(
  () => props.isVisible,
  (newVal) => {
    isVisibleLocal.value = newVal
  },
)

const computedMessage = computed(() => props.message ?? 'Notification message')
const computedType = computed(() => props.type ?? 'info')
const computedColor = computed(() => props.color ?? 'info')
const computedLocation = computed(() => props.location ?? 'top')
const computedTimeout = computed(() => props.timeout ?? 5000)
const computedAutoTimeout = computed(() => props.autoTimeout ?? true)
const showTimer = computed(() => props.showTimer ?? false)

const onClose = () => {
  isVisibleLocal.value = false
  emit('update:isVisible', false)
  emit('close')
}

const getIcon = (type: string): string => {
  const iconMap: { [key: string]: string } = {
    info: 'mdi-information-outline',
    success: 'mdi-check-circle-outline',
    error: 'mdi-alert-circle-outline',
    warning: 'mdi-alert-outline',
    '': 'mdi-information',
  }
  return iconMap[type] ?? 'mdi-information'
}
</script>

<style scoped>
/*
 * Styling based on BC Gov Design Standards - Inline Alert Component
 */

/* Base wrapper styles */
.app-snackbar :deep(.v-snackbar__wrapper) {
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  gap: var(--layout-margin-small) !important;
  padding: var(--layout-padding-medium) var(--layout-padding-large) !important;
  min-height: 48px !important;
}

/* Snackbar content area - make it inline with actions */
.app-snackbar :deep(.v-snackbar__content) {
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  gap: var(--layout-margin-small) !important;
  flex-grow: 1 !important;
  padding: 0 !important;
}

/* Snackbar actions area - inline with content */
.app-snackbar :deep(.v-snackbar__actions) {
  display: flex !important;
  align-items: center !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* Content container */
.app-snackbar__container {
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  flex-grow: 1 !important;
  font: var(--typography-regular-body) !important;
  color: var(--typography-color-primary) !important;
}

/* Left icon */
.app-snackbar__icon {
  display: inline-flex !important;
  align-self: center !important;
  min-width: var(--icons-size-medium) !important;
  height: var(--icons-size-medium) !important;
  font-size: var(--icons-size-medium) !important;
}

/* Close icon positioning */
.app-snackbar__close-icon {
  display: flex !important;
  align-items: center !important;
  color: var(--icons-color-primary) !important;
}

.app-snackbar__close-icon :deep(.v-btn) {
  color: var(--icons-color-primary) !important;
}

.app-snackbar__close-icon :deep(.v-btn) svg {
  min-width: var(--icons-size-medium) !important;
  height: var(--icons-size-medium) !important;
  color: var(--icons-color-primary) !important;
}

/* Variants */

/* Info variant */
.app-snackbar.info :deep(.v-snackbar__wrapper) {
  background-color: var(--support-surface-color-info) !important;
  border: var(--layout-border-width-small) solid
    var(--support-border-color-info) !important;
  border-radius: var(--layout-border-radius-medium) !important;
}

.app-snackbar.info .app-snackbar__icon {
  color: var(--icons-color-info) !important;
}

/* Success variant */
.app-snackbar.success :deep(.v-snackbar__wrapper) {
  background-color: var(--support-surface-color-success) !important;
  border: var(--layout-border-width-small) solid
    var(--support-border-color-success) !important;
  border-radius: var(--layout-border-radius-medium) !important;
}

.app-snackbar.success .app-snackbar__icon {
  color: var(--icons-color-success) !important;
}

/* Warning variant */
.app-snackbar.warning :deep(.v-snackbar__wrapper) {
  background-color: var(--support-surface-color-warning) !important;
  border: var(--layout-border-width-small) solid
    var(--support-border-color-warning) !important;
  border-radius: var(--layout-border-radius-medium) !important;
}

.app-snackbar.warning .app-snackbar__icon {
  color: var(--icons-color-warning) !important;
}

/* Error/Danger variant */
.app-snackbar.error :deep(.v-snackbar__wrapper) {
  background-color: var(--support-surface-color-danger) !important;
  border: var(--layout-border-width-small) solid
    var(--support-border-color-danger) !important;
  border-radius: var(--layout-border-radius-medium) !important;
}

.app-snackbar.error .app-snackbar__icon {
  color: var(--icons-color-danger) !important;
}
</style>
