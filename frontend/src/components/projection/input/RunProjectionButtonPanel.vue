<template>
  <v-card :class="computedCardClass" elevation="0">
    <v-card-actions :class="computedCardActionsClass">
      <v-spacer></v-spacer>
      <AppButton
        v-if="!showCancelButton"
        label="Run Projection"
        variant="primary"
        class="ml-2"
        :isDisabled="isDisabled"
        @click="runModel"
      />
      <AppButton
        v-else
        label="Cancel Run"
        variant="danger"
        mdi-name="mdi-stop-circle-outline"
        class="ml-2"
        @click="cancelRun"
      />
    </v-card-actions>
  </v-card>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import AppButton from '@/components/core/AppButton.vue'

const props = withDefaults(defineProps<{
  isDisabled: boolean
  showCancelButton?: boolean
  cardClass?: string
  cardActionsClass?: string
}>(), {
  showCancelButton: false,
})

const emit = defineEmits(['runModel', 'cancelRun'])

const computedCardClass = computed(
  () => props.cardClass ?? 'file-upload-run-model-card',
)

const computedCardActionsClass = computed(
  () => props.cardActionsClass ?? 'card-actions',
)

const runModel = () => {
  emit('runModel')
}

const cancelRun = () => {
  emit('cancelRun')
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
