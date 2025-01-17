<template>
  <div id="modelSelectionCard" class="mt-n1 mb-3">
    <h3
      v-if="internalModelSelection === MODEL_SELECTION.INPUT_MODEL_PARAMETERS"
    >
      {{ HEADER_SELECTION.MODEL_PARAMETER_SELECTION }}
    </h3>
    <h3 v-else>{{ HEADER_SELECTION.FILE_UPLOAD }}</h3>
  </div>

  <v-card class="pa-4 job-type-sel-card" elevation="0">
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
.job-type-sel-card {
  padding-bottom: 16px !important;
  background-color: #f6f6f6;
  border-top: 1px solid #0000001f;
  border-bottom: 1px solid #0000001f;
  border-radius: 0px;
}
</style>
