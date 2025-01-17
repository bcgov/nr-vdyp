<template>
  <v-card-actions class="mt-5 pr-0">
    <v-spacer></v-spacer>
    <AppButton
      label="Clear"
      customClass="white-btn"
      :isDisabled="!isConfirmEnabled"
      @click="onClear"
    />
    <AppButton
      label="Confirm"
      v-show="!isConfirmed"
      customClass="blue-btn ml-2"
      :isDisabled="!isConfirmEnabled"
      @click="onConfirm"
    />
    <AppButton
      label="Edit"
      v-show="isConfirmed"
      customClass="blue-btn ml-2"
      @click="onEdit"
    />
  </v-card-actions>
</template>

<script setup lang="ts">
import { AppButton } from '@/components'

defineProps({
  isConfirmEnabled: {
    type: Boolean,
    required: true,
  },
  isConfirmed: {
    type: Boolean,
    required: true,
  },
})

const emit = defineEmits(['clear', 'confirm', 'edit'])

const onClear = () => emit('clear')
const onConfirm = () => emit('confirm')
const onEdit = () => emit('edit')
</script>

<style scoped></style>
