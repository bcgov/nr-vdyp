<template>
  <div>
    <v-row class="mt-n5">
      <v-col cols="3" sm="3" md="3">
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
  </div>
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
.no-padding {
  padding: 0px;
}
.no-padding-bottom {
  padding-bottom: 0px;
}
</style>
