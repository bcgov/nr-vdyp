<template>
  <v-container fluid>
    <div class="top-project-year mt-3">
      <h1 class="top-project">Projects</h1>
      <span class="top-year">Year: 2024/2025</span>
    </div>
    <div class="mt-n1 mb-3">
      <h3>Model Parameter Selection</h3>
    </div>

    <v-card class="pa-4 job-type-sel-card" elevation="0">
      <JobTypeSelection />
    </v-card>
    <v-spacer class="space"></v-spacer>
    <div class="hr-line mb-5"></div>
    <v-spacer class="space"></v-spacer>

    <template v-if="modelSelection === MODEL_SELECTION.INPUT_MODEL_PARAMETERS">
      <v-tabs
        v-model="currentTab"
        :hideSlider="true"
        :centerActive="true"
        :showArrows="true"
        height="60px"
      >
        <v-tab
          v-for="(tab, index) in tabs"
          :key="index"
          :class="{ 'first-tab': index === 0 }"
          >{{ tab.label }}</v-tab
        >
      </v-tabs>
      <v-tabs-window v-model="currentTab">
        <v-tabs-window-item
          v-for="(tab, index) in tabs"
          :key="index"
          :value="index"
        >
          <component :is="tab.component"></component>
        </v-tabs-window-item>
      </v-tabs-window>

      <template v-if="isModelParameterPanelsVisible">
        <v-spacer class="space"></v-spacer>
        <SiteInfo />
        <v-spacer class="space"></v-spacer>
        <StandDensity />
        <v-spacer class="space"></v-spacer>
        <ReportInfo />

        <v-card class="mt-5 pa-4 run-model-card" elevation="0">
          <v-card-actions class="pr-0 mr-2">
            <v-spacer></v-spacer>
            <v-btn
              class="blue-btn ml-2"
              :disabled="!modelParameterStore.runModelEnabled"
              @click="runModel"
              >Run Model</v-btn
            >
          </v-card-actions>
        </v-card>
      </template>
    </template>
    <template v-else>
      <FileUpload />
    </template>
  </v-container>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useAppStore } from '@/stores/appStore'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { storeToRefs } from 'pinia'

import JobTypeSelection from '@/components/JobTypeSelection.vue'

import ModelParameterSelection from '@/views/input-model-parameters/ModelParameterSelection.vue'
import ModelReport from '@/views/input-model-parameters/ModelReport.vue'
import ViewLogFile from '@/views/input-model-parameters/ViewLogFile.vue'
import ViewErrorMessages from '@/views/input-model-parameters/ViewErrorMessages.vue'

import SiteInfo from '@/components/model-param-selection-panes/SiteInfo.vue'
import StandDensity from '@/components/model-param-selection-panes/StandDensity.vue'
import ReportInfo from '@/components/model-param-selection-panes/ReportInfo.vue'

import FileUpload from '@/views/input-model-parameters/FileUpload.vue'

import { MODEL_SELECTION, MODEL_PARAM_TAB_NAME } from '@/constants/constants'

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()

const { currentTab, modelSelection } = storeToRefs(appStore)

const tabs = [
  {
    label: MODEL_PARAM_TAB_NAME.MODEL_PARAM_SELECTION,
    component: ModelParameterSelection,
  },
  { label: MODEL_PARAM_TAB_NAME.MODEL_REPORT, component: ModelReport },
  { label: MODEL_PARAM_TAB_NAME.VIEW_LOG_FILE, component: ViewLogFile },
  {
    label: MODEL_PARAM_TAB_NAME.VIEW_ERROR_MESSAGES,
    component: ViewErrorMessages,
  },
]

const isModelParameterPanelsVisible = computed(() => {
  return (
    modelSelection.value === MODEL_SELECTION.INPUT_MODEL_PARAMETERS &&
    currentTab.value === 0
  )
})

onMounted(() => {
  modelParameterStore.setDefaultValues()
})

const runModel = () => {}
</script>

<style scoped>
.space {
  margin-top: 10px;
}

.run-model-card {
  padding-bottom: 16px !important;
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

.job-type-sel-card {
  padding-bottom: 16px !important;
  background-color: #f6f6f6;
  border-top: 1px solid #0000001f;
  border-bottom: 1px solid #0000001f;
  border-radius: 0px;
}
</style>
