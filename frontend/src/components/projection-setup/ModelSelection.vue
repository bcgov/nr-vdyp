<template>
  <v-row class="model-selection-row">
    <v-col cols="3" sm="3" md="3" class="model-selection-col">
      <label class="bcds-select-label" for="model-select">Model</label>
      <v-select
        id="model-select"
        :items="OPTIONS.modelSelectionOptions"
        v-model="selectedModel"
        item-title="label"
        item-value="value"
        hide-details
        append-inner-icon="mdi-chevron-down"
      ></v-select>
    </v-col>
  </v-row>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import { DEFAULTS, OPTIONS } from '@/constants'

const emit = defineEmits(['update:modelSelection'])

const selectedModel = ref<string>(DEFAULTS.DEFAULT_VALUES.MODEL_SELECTION)

watch(selectedModel, (newValue) => {
  emit('update:modelSelection', newValue)
})
</script>

<style scoped>
.model-selection-row {
  margin-top: var(--layout-margin-none);
  margin-bottom: var(--layout-margin-none);
}

.model-selection-col {
  padding-top: var(--layout-padding-none);
  padding-bottom: var(--layout-padding-none);
}

.bcds-select-label {
  font: var(--typography-regular-label);
  color: var(--typography-color-primary);
  display: block;
  margin-bottom: var(--layout-margin-hair);
}
</style>
