<template>
  <v-card-actions class="mt-5 pr-0">
    <v-spacer></v-spacer>
    <AppButton
      v-if="!hideClearButton"
      label="Clear"
      variant="tertiary"
      :isDisabled="!isConfirmEnabled"
      @click="onClear"
    />
    <AppButton
      v-if="showCancelButton"
      label="Cancel"
      variant="tertiary"
      class="ml-2"
      :isDisabled="!isConfirmEnabled"
      @click="onCancel"
    />
    <AppButton
      label="Next"
      v-show="!isConfirmed || hideEditButton"
      variant="primary"
      class="ml-2"
      :isDisabled="!isConfirmEnabled"
      @click="onConfirm"
    />
    <AppButton
      label="Edit"
      v-show="isConfirmed && !hideEditButton"
      variant="primary"
      class="ml-2"
      @click="onEdit"
    />
  </v-card-actions>
</template>

<script setup lang="ts">
import { AppButton } from '@/components'
import { PANEL_ACTION } from '@/constants/constants'

defineProps({
  isConfirmEnabled: {
    type: Boolean,
    required: true,
  },
  isConfirmed: {
    type: Boolean,
    required: true,
  },
  hideClearButton: {
    type: Boolean,
    default: false,
  },
  hideEditButton: {
    type: Boolean,
    default: false,
  },
  showCancelButton: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([PANEL_ACTION.CLEAR, PANEL_ACTION.CONFIRM, PANEL_ACTION.EDIT, PANEL_ACTION.CANCEL])

const onClear = () => emit(PANEL_ACTION.CLEAR)
const onConfirm = () => emit(PANEL_ACTION.CONFIRM)
const onEdit = () => emit(PANEL_ACTION.EDIT)
const onCancel = () => emit(PANEL_ACTION.CANCEL)
</script>

<style scoped />
