<template>
  <div>
    <v-row class="mt-n5">
      <v-col cols="3" sm="3" md="3">
        <span class="search-label">Model</span>
        <v-select
          :items="modelSelectionOptions"
          v-model="selectedModel"
          item-title="label"
          item-value="value"
          hide-details
          dense
          density="compact"
          variant="outlined"
          style="background-color: white !important"
        ></v-select>
      </v-col>
    </v-row>
  </div>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import { modelSelectionOptions } from '@/constants/options'
import { MODEL_SELECTION } from '@/constants/constants'

const emit = defineEmits(['update:modelSelection'])

const selectedModel = ref<string>(MODEL_SELECTION.FILE_UPLOAD)

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
