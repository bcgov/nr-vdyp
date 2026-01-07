<template>
  <v-dialog
    :model-value="computedDialog"
    @update:model-value="updateDialog"
    persistent
    :max-width="computedDialogWidth"
    :scroll-strategy="computedScrollStrategy"
  >
    <v-card :class="['bcds-message-dialog', computedVariant]">
      <div class="bcds-message-dialog--header popup-header">
        <v-icon v-if="computedIcon" class="bcds-message-dialog--icon">{{
          computedIcon
        }}</v-icon>
        <h2 class="bcds-message-dialog--title">{{ computedTitle }}</h2>
        <v-icon class="bcds-message-dialog--close-icon" @click="agree"
          >mdi-close</v-icon
        >
      </div>

      <div v-show="Boolean(computedMessage)" class="bcds-message-dialog--children">
        {{ computedMessage }}
      </div>

      <div class="bcds-message-dialog--actions">
        <v-spacer></v-spacer>
        <AppButton
          :label="computedBtnLabel"
          variant="primary"
          class="ml-2"
          @click="agree"
        />
      </div>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppButton from '@/components/core/AppButton.vue'
import { BUTTON_LABEL } from '@/constants/constants'

const props = defineProps<{
  dialog?: boolean
  title?: string
  message?: string
  dialogWidth?: number
  btnLabel?: string
  scrollStrategy?: any
  variant?: 'info' | 'confirmation' | 'warning' | 'error'
}>()

const emit = defineEmits(['update:dialog', 'close'])

const computedDialog = computed(() => props.dialog ?? false)
const computedTitle = computed(() => props.title ?? '')
const computedMessage = computed(() => props.message ?? '')
const computedDialogWidth = computed(() => props.dialogWidth ?? 400)
const computedBtnLabel = computed(
  () => props.btnLabel ?? BUTTON_LABEL.CONT_EDIT,
)

const computedVariant = computed(() => props.variant || 'info')

const computedIcon = computed(() => {
  const variant = computedVariant.value
  const iconMap = {
    info: 'mdi-information',
    confirmation: 'mdi-check-circle',
    warning: 'mdi-alert',
    error: 'mdi-alert-circle',
  }
  return iconMap[variant]
})

const computedScrollStrategy = computed(() => {
  return props.scrollStrategy ?? 'block'
})

// Emit updates for dialog visibility
const updateDialog = (value: boolean) => {
  emit('update:dialog', value)
}

// Emit close event and close the dialog
const agree = () => {
  emit('update:dialog', false)
  emit('close')
}
</script>

<style scoped>
/*
 * Styling based on BC Gov Design Standards - Alert Dialog Component
 */
.bcds-message-dialog {
  display: flex;
  flex-direction: column;
}

/* Variant icon colors */
.bcds-message-dialog.info .bcds-message-dialog--icon {
  color: var(--icons-color-primary);
}

.bcds-message-dialog.confirmation .bcds-message-dialog--icon {
  color: var(--icons-color-success);
}

.bcds-message-dialog.warning .bcds-message-dialog--icon {
  color: var(--icons-color-warning);
}

.bcds-message-dialog.error .bcds-message-dialog--icon {
  color: var(--icons-color-danger);
}

.bcds-message-dialog--header {
  display: inline-flex;
  flex-direction: row;
  gap: var(--layout-padding-small);
  justify-content: space-between;
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  border-bottom: var(--layout-border-width-small) solid
    var(--surface-color-border-default);
}

.bcds-message-dialog--title {
  flex-grow: 1;
  font: var(--typography-bold-h5);
  color: var(--typography-color-primary);
  margin: 0;
}

.bcds-message-dialog--icon {
  justify-self: flex-start;
  align-self: center;
  padding-top: var(--layout-padding-xsmall);
}

.bcds-message-dialog--close-icon {
  justify-self: flex-end;
  color: var(--icons-color-primary);
  cursor: pointer;
}

.bcds-message-dialog--children {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  border-bottom: var(--layout-border-width-small) solid
    var(--surface-color-border-default);
  white-space: pre-line;
}

.bcds-message-dialog--actions {
  display: flex;
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  gap: var(--layout-padding-small);
}
</style>
