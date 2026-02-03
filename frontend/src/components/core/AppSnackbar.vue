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
      <span v-if="computedTitle" class="app-snackbar__title">{{
        computedTitle
      }}</span>
      <span class="app-snackbar__message">{{ computedMessage }}</span>
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
  title?: string
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

const computedTitle = computed(() => props.title ?? '')
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
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: var(--layout-margin-small);
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  min-height: 48px;
}

/* Snackbar content area - top aligned with actions */
.app-snackbar :deep(.v-snackbar__content) {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: var(--layout-margin-small);
  flex-grow: 1;
  padding: 0;
}

/* Snackbar actions area - top aligned */
.app-snackbar :deep(.v-snackbar__actions) {
  display: flex;
  align-items: flex-start;
  align-self: flex-start;
  margin: 0;
  padding: 0;
}

/* Content container */
.app-snackbar__container {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  flex-grow: 1;
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
}

/* Title/Header text */
.app-snackbar__title {
  font: var(--typography-bold-body);
  color: var(--typography-color-primary);
}

/* Message text */
.app-snackbar__message {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
}

/* Left icon - aligned with title line */
.app-snackbar__icon {
  display: inline-flex;
  align-self: flex-start;
  min-width: var(--icons-size-medium);
  height: var(--icons-size-medium);
  font-size: var(--icons-size-medium);
  margin-top: 4px;
}

/* Close icon positioning - aligned with title line */
.app-snackbar__close-icon {
  display: flex;
  align-items: flex-start;
  color: var(--icons-color-primary);
}

.app-snackbar__close-icon :deep(.v-btn) {
  color: var(--icons-color-primary);
  width: 24px;
  height: 24px;
  min-width: 24px;
}

.app-snackbar__close-icon :deep(.v-btn) .v-icon {
  font-size: 18px;
}

.app-snackbar__close-icon :deep(.v-btn) svg {
  min-width: 18px;
  height: 18px;
  color: var(--icons-color-primary);
}

/* Variants */

/* Info variant */
.app-snackbar.info :deep(.v-snackbar__wrapper) {
  background-color: var(--support-surface-color-info) !important;
  border: var(--layout-border-width-small) solid
    var(--support-border-color-info);
  border-radius: var(--layout-border-radius-medium);
}

.app-snackbar.info .app-snackbar__icon {
  color: var(--icons-color-info);
}

/* Success variant */
.app-snackbar.success :deep(.v-snackbar__wrapper) {
  background-color: var(--support-surface-color-success) !important;
  border: var(--layout-border-width-small) solid
    var(--support-border-color-success);
  border-radius: var(--layout-border-radius-medium);
}

.app-snackbar.success .app-snackbar__icon {
  color: var(--icons-color-success);
}

/* Warning variant */
.app-snackbar.warning :deep(.v-snackbar__wrapper) {
  background-color: var(--support-surface-color-warning) !important;
  border: var(--layout-border-width-small) solid
    var(--support-border-color-warning);
  border-radius: var(--layout-border-radius-medium);
}

.app-snackbar.warning .app-snackbar__icon {
  color: var(--icons-color-warning);
}

/* Error/Danger variant */
.app-snackbar.error :deep(.v-snackbar__wrapper) {
  background-color: var(--support-surface-color-danger) !important;
  border: var(--layout-border-width-small) solid
    var(--support-border-color-danger);
  border-radius: var(--layout-border-radius-medium);
}

.app-snackbar.error .app-snackbar__icon {
  color: var(--icons-color-danger);
}
</style>
