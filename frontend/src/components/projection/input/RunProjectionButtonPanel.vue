<template>
  <v-card :class="computedCardClass" elevation="0">
    <v-card-actions :class="computedCardActionsClass">
      <v-spacer></v-spacer>
      <AppButton
        label="Run Projection"
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
/*
 * Styling based on BC Gov Design Standards - Using Design Tokens
 */
.input-model-param-run-model-card {
  padding: var(--layout-padding-medium) !important;
  margin-top: var(--layout-margin-medium) !important;
  background-color: var(--surface-color-background-light-gray);
  border: var(--layout-border-width-small) solid
    var(--surface-color-border-default);
  border-top-left-radius: 0px;
  border-top-right-radius: 0px;
  border-bottom-left-radius: var(--layout-border-radius-large);
  border-bottom-right-radius: var(--layout-border-radius-large);
  display: flex;
  justify-content: end;
  align-items: end;
  text-align: end;
}

.file-upload-run-model-card {
  padding: var(--layout-padding-medium) !important;
  margin-top: var(--layout-margin-medium) !important;
  background-color: var(--surface-color-background-light-gray);
  border-top: var(--layout-border-width-small) solid
    var(--surface-color-border-default);
  border-top-left-radius: 0px;
  border-top-right-radius: 0px;
  border-bottom-left-radius: var(--layout-border-radius-small);
  border-bottom-right-radius: var(--layout-border-radius-small);
  display: flex;
  justify-content: end;
  align-items: end;
  text-align: end;
}

.card-actions {
  padding-right: 0px !important;
  margin-right: var(--layout-margin-xsmall) !important;
}
</style>
