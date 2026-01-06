<template>
  <v-dialog v-model="getIsOpen" persistent :max-width="getDialogOptions.width">
    <v-card :class="['bcds-alert-dialog', computedVariant]">
      <div class="bcds-alert-dialog--header popup-header">
        <v-icon v-if="computedIcon" class="bcds-alert-dialog--icon">{{
          computedIcon
        }}</v-icon>
        <h2 class="bcds-alert-dialog--title">{{ getDialogTitle }}</h2>
        <v-icon class="bcds-alert-dialog--close-icon" @click="cancel"
          >mdi-close</v-icon
        >
      </div>

      <div v-show="Boolean(getDialogMessage)" class="bcds-alert-dialog--children">
        {{ getDialogMessage }}
      </div>

      <div class="bcds-alert-dialog--actions">
        <v-spacer></v-spacer>
        <AppButton
          :label="computedNoBtnLabel"
          variant="tertiary"
          class="ml-2"
          @click="cancel" />

        <AppButton
          :label="computedYesBtnLabel"
          variant="primary"
          class="ml-2"
          @click="agree" />
      </div>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import AppButton from '@/components/core/AppButton.vue'
import { CONSTANTS } from '@/constants'
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'

const alertDialogStore = useAlertDialogStore()

const { getIsOpen, getDialogTitle, getDialogMessage, getDialogOptions } =
  storeToRefs(alertDialogStore)

const computedYesBtnLabel = computed(() => CONSTANTS.BUTTON_LABEL.ALERT_CONTINUE)
const computedNoBtnLabel = computed(() => CONSTANTS.BUTTON_LABEL.ALERT_CANCEL)

const computedVariant = computed(() => getDialogOptions.value.variant || 'confirmation')

const computedIcon = computed(() => {
  const variant = computedVariant.value
  const iconMap = {
    info: 'mdi-information',
    confirmation: 'mdi-check-circle',
    warning: 'mdi-alert',
    error: 'mdi-alert-circle',
    destructive: 'mdi-alert-circle',
  }
  return iconMap[variant]
})

const agree = () => {
  alertDialogStore.agree()
}

const cancel = () => {
  alertDialogStore.cancel()
}
</script>

<style scoped>
.bcds-alert-dialog {
  display: flex;
  flex-direction: column;
}

/* Variant icon colors */
.bcds-alert-dialog.info .bcds-alert-dialog--icon {
  color: var(--icons-color-primary);
}

.bcds-alert-dialog.confirmation .bcds-alert-dialog--icon {
  color: var(--icons-color-success);
}

.bcds-alert-dialog.warning .bcds-alert-dialog--icon {
  color: var(--icons-color-warning);
}

.bcds-alert-dialog.error .bcds-alert-dialog--icon {
  color: var(--icons-color-danger);
}

.bcds-alert-dialog.destructive .bcds-alert-dialog--icon {
  color: var(--icons-color-danger);
}

.bcds-alert-dialog--header {
  display: inline-flex;
  flex-direction: row;
  gap: var(--layout-padding-small);
  justify-content: space-between;
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  border-bottom: var(--layout-border-width-small) solid
    var(--surface-color-border-default);
}

.bcds-alert-dialog--title {
  flex-grow: 1;
  font: var(--typography-bold-h5);
  color: var(--typography-color-primary);
  margin: 0;
}

.bcds-alert-dialog--icon {
  justify-self: flex-start;
  align-self: center;
  padding-top: var(--layout-padding-xsmall);
}

.bcds-alert-dialog--close-icon {
  justify-self: flex-end;
  color: var(--icons-color-primary);
  cursor: pointer;
}

.bcds-alert-dialog--children {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  border-bottom: var(--layout-border-width-small) solid
    var(--surface-color-border-default);
  white-space: pre-line;
}

.bcds-alert-dialog--actions {
  display: flex;
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  gap: var(--layout-padding-small);
}
</style>
