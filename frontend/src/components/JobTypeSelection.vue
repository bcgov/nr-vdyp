<template>
  <div>
    <v-row>
      <v-col cols="3" sm="3" md="3">
        <span class="search-label">Model</span>
        <v-select
          v-model="modelType"
          :items="modelTypes"
          hide-details
          dense
          density="compact"
          variant="outlined"
          style="background-color: white !important"
        ></v-select>
      </v-col>
      <v-col cols="2" sm="2" md="2">
        <span class="search-label">Version</span>
        <v-select
          v-model="modelName"
          :items="modelNames"
          hide-details
          dense
          density="compact"
          variant="outlined"
          style="background-color: white !important"
        ></v-select>
      </v-col>
      <v-col cols="2" sm="2" md="2">
        <span class="search-label">Job ID</span>
        <v-text-field
          v-model="jobName"
          hide-details
          placeholder="Search Job Name..."
          density="compact"
          dense
          variant="outlined"
          style="background-color: white !important"
        ></v-text-field>
      </v-col>
    </v-row>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useJobTypeSelectionStore } from '@/stores/jobTypeSelectionStore'

const jobTypeSelectionStore = useJobTypeSelectionStore()
const modelType = ref(jobTypeSelectionStore.modelType)
const modelName = ref(jobTypeSelectionStore.modelName)
const jobName = ref(jobTypeSelectionStore.jobName)

const modelTypes = ['File Upload', 'Input Model Parameters']
const modelNames = ['VDYP 8', 'VDYP 9']
</script>

<style scoped>
.no-padding {
  padding: 0px;
}
.no-padding-bottom {
  padding-bottom: 0px;
}
</style>
