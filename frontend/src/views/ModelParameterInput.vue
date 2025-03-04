<template>
  <AppProgressCircular
    :isShow="isProgressVisible"
    :showMessage="true"
    :message="progressMessage"
    :hasBackground="true"
  />
  <v-container fluid>
    <TopProjectYear />
    <div class="hr-line-2 mb-4"></div>
    <v-spacer class="space"></v-spacer>
    <ModelSelectionContainer @update:modelSelection="updateModelSelection" />
    <v-spacer class="space"></v-spacer>
    <div class="hr-line mb-5"></div>
    <v-spacer class="space"></v-spacer>
    <template
      v-if="modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS"
    >
      <AppTabs v-model:currentTab="activeTab" :tabs="tabs" />
      <template v-if="isModelParameterPanelsVisible">
        <v-spacer class="space"></v-spacer>
        <SiteInfoPanel />
        <v-spacer class="space"></v-spacer>
        <StandDensityPanel />
        <v-spacer class="space"></v-spacer>
        <ReportInfoPanel />
        <AppRunModelButton
          :isDisabled="!modelParameterStore.runModelEnabled"
          cardClass="input-model-param-run-model-card"
          cardActionsClass="card-actions"
          @runModel="runModelHandler"
        />
      </template>
    </template>
    <template v-else>
      <FileUpload />
    </template>
  </v-container>
</template>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useProjectionStore } from '@/stores/projectionStore'
import {
  AppProgressCircular,
  AppTabs,
  AppRunModelButton,
  ModelSelectionContainer,
  TopProjectYear,
  ReportingContainer,
  SpeciesInfoPanel,
  SiteInfoPanel,
  StandDensityPanel,
  ReportInfoPanel,
  FileUpload,
} from '@/components'
import type { Tab } from '@/interfaces/interfaces'
import { CONSTANTS, MESSAGE, DEFAULTS } from '@/constants'
import { handleApiError } from '@/services/apiErrorHandler'
import { runModel } from '@/services/modelParameterService'
import { delay } from '@/utils/util'
import { logSuccessMessage } from '@/utils/messageHandler'

const modelSelection = ref<string>(DEFAULTS.DEFAULT_VALUES.MODEL_SELECTION)
const isProgressVisible = ref(false)
const progressMessage = ref('')
const activeTab = ref(0)

const modelParameterStore = useModelParameterStore()
const projectionStore = useProjectionStore()

const tabs: Tab[] = [
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.MODEL_PARAM_SELECTION,
    component: SpeciesInfoPanel,
    tabname: null,
  },
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.MODEL_REPORT,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.MODEL_REPORT,
  },
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.VIEW_LOG_FILE,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.VIEW_LOG_FILE,
  },
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.VIEW_ERROR_MESSAGES,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.VIEW_ERR_MSG,
  },
]

const isModelParameterPanelsVisible = computed(() => {
  return (
    modelSelection.value === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS &&
    activeTab.value === 0
  )
})

const updateModelSelection = (newSelection: string) => {
  modelSelection.value = newSelection
}

onMounted(() => {
  modelParameterStore.setDefaultValues()
})

const runModelHandler = async () => {
  try {
    isProgressVisible.value = true
    progressMessage.value = MESSAGE.PROGRESS_MSG.RUNNING_MODEL

    await delay(1000)

    const result = await runModel(modelParameterStore)
    await projectionStore.handleZipResponse(result)

    logSuccessMessage(MESSAGE.SUCESS_MSG.INPUT_MODEL_PARAM_RUN_RESULT)
  } catch (error) {
    handleApiError(error, MESSAGE.MODEL_PARAM_INPUT_ERR.FAIL_RUN_MODEL)
  } finally {
    isProgressVisible.value = false
  }
}
</script>

<style scoped>
.space {
  margin-top: 10px;
}
</style>
