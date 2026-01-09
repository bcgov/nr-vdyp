<template>
  <div id="modelSelectionCard" class="model-selection-header">
    <h3
      v-if="internalModelSelection === MODEL_SELECTION.INPUT_MODEL_PARAMETERS"
    >
      {{ HEADER_SELECTION.MODEL_PARAMETER_SELECTION }}
    </h3>
    <h3 v-else>{{ HEADER_SELECTION.FILE_UPLOAD }}</h3>
  </div>

  <v-card class="job-type-sel-card" elevation="0">
    <ModelSelection @update:modelSelection="updateModelSelection" />
  </v-card>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import ModelSelection from '@/components/projection-setup/ModelSelection.vue'
import { HEADER_SELECTION, MODEL_SELECTION } from '@/constants/constants'
import { DEFAULT_VALUES } from '@/constants/defaults'

const emit = defineEmits(['update:modelSelection'])

const internalModelSelection = ref<string>(DEFAULT_VALUES.MODEL_SELECTION)

const updateModelSelection = (newSelection: string) => {
  internalModelSelection.value = newSelection
  emit('update:modelSelection', newSelection)
}
</script>
<style scoped>
.model-selection-header {
  padding-top: var(--layout-padding-medium);
  border-top: var(--layout-border-width-medium) solid var(--surface-color-border-default);
  margin-bottom: var(--layout-margin-medium);
}

h3 {
  font: var(--typography-bold-h3);
  color: var(--typography-color-primary);
}

.job-type-sel-card {
  padding: var(--layout-padding-medium);
  background-color: var(--surface-color-background-light-gray);
  border-top: var(--layout-border-width-small) solid var(--surface-color-border-default);
  border-bottom: var(--layout-border-width-small) solid var(--surface-color-border-default);
  border-radius: var(--layout-border-radius-none);
  box-shadow: var(--surface-shadow-none);
  margin-bottom: var(--layout-margin-medium);
}
</style>
