<template>
  <v-card :class="computedCardClass" elevation="0">
    <v-card-actions :class="computedCardActionsClass">
      <v-spacer></v-spacer>
      <AppButton
        label="Run Model"
        variant="primary"
        class="ml-2"
        :isDisabled="isDisabled"
        @click="runModel"
      />
    </v-card-actions>
  </v-card>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import AppButton from '@/components/core/AppButton.vue'

const props = defineProps<{
  isDisabled: boolean
  cardClass?: string
  cardActionsClass?: string
}>()

const emit = defineEmits(['runModel'])

const computedCardClass = computed(
  () => props.cardClass ?? 'file-upload-run-model-card',
)

const computedCardActionsClass = computed(
  () => props.cardActionsClass ?? 'card-actions',
)

const runModel = () => {
  emit('runModel')
}
</script>
<style scoped>
.input-model-param-run-model-card {
  padding: 16px !important;
  margin-top: 20px !important;
  background-color: #f6f6f6;
  border: 1px solid #0000001f;
  border-top-left-radius: 0px;
  border-top-right-radius: 0px;
  border-bottom-left-radius: 10px;
  border-bottom-right-radius: 10px;
  display: flex;
  justify-content: end;
  align-items: end;
  text-align: end;
}
.file-upload-run-model-card {
  padding: 16px !important;
  margin-top: 20px !important;
  background-color: #f6f6f6;
  border-top: 1px solid #0000001f;
  border-top-left-radius: 0px;
  border-top-right-radius: 0px;
  border-bottom-left-radius: 3px;
  border-bottom-right-radius: 3px;
  display: flex;
  justify-content: end;
  align-items: end;
  text-align: end;
}
.card-actions {
  padding-right: 0px !important;
  margin-right: 8px !important;
}
</style>
