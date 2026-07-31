<template>
  <v-dialog v-model="isOpen" persistent max-width="480">
    <v-card class="cancel-dialog">
      <div class="cancel-dialog--header">
        <img src="@/assets/icons/warning.svg" alt="Warning" class="cancel-dialog--icon" />
        <h2 class="cancel-dialog--title">{{ dialogTitle }}</h2>
        <v-icon class="cancel-dialog--close-icon" @click="handleKeepRunning">mdi-close</v-icon>
      </div>

      <div class="cancel-dialog--body">
        <p class="cancel-dialog--warning">{{ ADMIN_CANCEL_DIALOG.WARNING }}</p>

        <div class="bcds-textarea">
          <label class="bcds-textarea-label" for="adminCancelReason">
            {{ ADMIN_CANCEL_DIALOG.REASON_LABEL }}
          </label>
          <div class="bcds-textarea-container">
            <textarea
              id="adminCancelReason"
              v-model="reason"
              class="bcds-textarea-input"
              placeholder="Start typing"
              :maxlength="ADMIN_CANCEL_REASON.MAX_LENGTH"
              rows="3"
            ></textarea>
          </div>
          <div class="bcds-textarea-description reason-counter">
            <span class="counter">{{ reasonLength }} / {{ ADMIN_CANCEL_REASON.MAX_LENGTH }}</span>
          </div>
        </div>
      </div>

      <div class="cancel-dialog--actions">
        <v-spacer></v-spacer>
        <AppButton
          :label="ADMIN_CANCEL_DIALOG.KEEP_RUNNING"
          variant="tertiary"
          @click="handleKeepRunning"
        />
        <AppButton
          :label="ADMIN_CANCEL_DIALOG.CONFIRM_CANCELLATION"
          variant="danger"
          class="ml-2"
          :is-disabled="!isReasonValid"
          @click="handleConfirmCancellation"
        />
      </div>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { AppButton } from '@/components'
import { ADMIN_CANCEL_REASON } from '@/constants/constants'
import { ADMIN_CANCEL_DIALOG } from '@/constants/message'

const props = defineProps<{
  modelValue: boolean
  projectionTitle: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [reason: string]
}>()

const isOpen = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const dialogTitle = computed(() => ADMIN_CANCEL_DIALOG.TITLE(props.projectionTitle))

const reason = ref(ADMIN_CANCEL_REASON.DEFAULT_TEXT)

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      reason.value = ADMIN_CANCEL_REASON.DEFAULT_TEXT
    }
  },
)

const reasonLength = computed(() => reason.value.length)

const isReasonValid = computed(() => {
  const trimmedLength = reason.value.trim().length
  return trimmedLength >= ADMIN_CANCEL_REASON.MIN_LENGTH && reasonLength.value <= ADMIN_CANCEL_REASON.MAX_LENGTH
})

const handleKeepRunning = () => {
  isOpen.value = false
}

const handleConfirmCancellation = () => {
  if (!isReasonValid.value) return
  emit('confirm', reason.value.trim())
  isOpen.value = false
}
</script>

<style scoped>
.cancel-dialog {
  display: flex;
  flex-direction: column;
}

.cancel-dialog--header {
  display: inline-flex;
  flex-direction: row;
  gap: var(--layout-padding-small);
  justify-content: space-between;
  align-items: flex-start;
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  border-bottom: var(--layout-border-width-small) solid var(--surface-color-border-default);
}

.cancel-dialog--icon {
  justify-self: flex-start;
  align-self: center;
  width: 20px;
  height: 20px;
}

.cancel-dialog--title {
  flex-grow: 1;
  font: var(--typography-bold-h5);
  color: var(--typography-color-primary);
  margin: 0;
}

.cancel-dialog--close-icon {
  justify-self: flex-end;
  color: var(--icons-color-primary);
  cursor: pointer;
}

.cancel-dialog--body {
  padding: var(--layout-padding-medium) var(--layout-padding-large);
}

.cancel-dialog--warning {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  margin: 0 0 var(--layout-margin-medium) 0;
}

.reason-counter {
  padding-bottom: 0px;
}

.cancel-dialog--actions {
  display: flex;
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  gap: var(--layout-padding-small);
}
</style>
