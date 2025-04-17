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
      v-if="
        appStore.modelSelection ===
        CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
      "
    >
      <AppTabs
        v-model:currentTab="modelParamActiveTab"
        :tabs="modelParamTabs"
      />
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
      <AppTabs
        v-model:currentTab="fileUploadActiveTab"
        :tabs="fileUploadTabs"
      />
      <template v-if="isFileUploadPanelsVisible">
        <v-spacer class="space"></v-spacer>
        <AttachmentsPanel />
        <AppRunModelButton
          :isDisabled="!fileUploadStore.runModelEnabled"
          cardClass="input-model-param-run-model-card"
          cardActionsClass="card-actions"
          @runModel="runModelHandler"
        />
      </template>
    </template>
  </v-container>
</template>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAppStore } from '@/stores/appStore'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import { useProjectionStore } from '@/stores/projectionStore'
import { useReportingStore } from '@/stores/reportingStore'
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
  AttachmentsPanel,
} from '@/components'
import type { Tab } from '@/interfaces/interfaces'
import { CONSTANTS, MESSAGE } from '@/constants'
import { handleApiError } from '@/services/apiErrorHandler'
import { runModel } from '@/services/modelParameterService'
import { runModelFileUpload } from '@/services/fileUploadService'
import {
  checkZipForErrors,
  delay,
  downloadFile,
  extractZipFileName,
} from '@/utils/util'
import { logSuccessMessage, logErrorMessage } from '@/utils/messageHandler'

const isProgressVisible = ref(false)
const progressMessage = ref('')
const modelParamActiveTab = ref(0)
const fileUploadActiveTab = ref(0)

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const fileUploadStore = useFileUploadStore()
const projectionStore = useProjectionStore()
const reportingStore = useReportingStore()

const modelParamTabs = computed<Tab[]>(() => [
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.MODEL_PARAM_SELECTION,
    component: SpeciesInfoPanel,
    tabname: null,
    disabled: false,
  },
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.MODEL_REPORT,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.MODEL_REPORT,
    disabled: !reportingStore.modelParamReportingTabsEnabled,
  },
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.VIEW_LOG_FILE,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.VIEW_LOG_FILE,
    disabled: !reportingStore.modelParamReportingTabsEnabled,
  },
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.VIEW_ERROR_MESSAGES,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.VIEW_ERR_MSG,
    disabled: !reportingStore.modelParamReportingTabsEnabled,
  },
])

const fileUploadTabs = computed<Tab[]>(() => [
  {
    label: CONSTANTS.FILE_UPLOAD_TAB_NAME.FILE_UPLOAD,
    component: ReportInfoPanel,
    tabname: null,
    disabled: false,
  },
  {
    label: CONSTANTS.FILE_UPLOAD_TAB_NAME.MODEL_REPORT,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.MODEL_REPORT,
    disabled: !reportingStore.fileUploadReportingTabsEnabled,
  },
  {
    label: CONSTANTS.FILE_UPLOAD_TAB_NAME.VIEW_LOG_FILE,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.VIEW_LOG_FILE,
    disabled: !reportingStore.fileUploadReportingTabsEnabled,
  },
  {
    label: CONSTANTS.FILE_UPLOAD_TAB_NAME.VIEW_ERROR_MESSAGES,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.VIEW_ERR_MSG,
    disabled: !reportingStore.fileUploadReportingTabsEnabled,
  },
])

const isModelParameterPanelsVisible = computed(() => {
  return (
    appStore.modelSelection ===
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS &&
    modelParamActiveTab.value === 0
  )
})

const isFileUploadPanelsVisible = computed(() => {
  return (
    appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD &&
    fileUploadActiveTab.value === 0
  )
})

const updateModelSelection = (newSelection: string) => {
  appStore.setModelSelection(newSelection)
}

onMounted(() => {
  modelParameterStore.setDefaultValues()
  fileUploadStore.setDefaultValues()
})

const processResponse = async (response: any) => {
  const zipFileName =
    extractZipFileName(response.headers) ||
    CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP

  const resultBlob = response.data

  console.debug('resultBlob:', resultBlob, 'type:', resultBlob?.type)
  console.debug('resultBlob size:', resultBlob.size)

  if (!resultBlob || !(resultBlob instanceof Blob)) {
    throw new Error('Invalid response data')
  }

  const hasErrors = await checkZipForErrors(resultBlob)
  await projectionStore.handleZipResponse(resultBlob, zipFileName)

  if (appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD) {
    downloadFile(resultBlob, zipFileName)
  }

  if (hasErrors) {
    logErrorMessage(
      appStore.modelSelection ===
        CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
        ? MESSAGE.SUCCESS_MSG.INPUT_MODEL_PARAM_RUN_RESULT_W_ERR
        : MESSAGE.SUCCESS_MSG.FILE_UPLOAD_RUN_RESULT_W_ERR,
    )
  } else {
    logSuccessMessage(
      appStore.modelSelection ===
        CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
        ? MESSAGE.SUCCESS_MSG.INPUT_MODEL_PARAM_RUN_RESULT
        : MESSAGE.SUCCESS_MSG.FILE_UPLOAD_RUN_RESULT,
    )
  }
}

const handleError = (error: any) => {
  if (error.response && error.response.data) {
    try {
      const validationMessages = JSON.parse(error.response.data)
      console.log('Validation Messages:', validationMessages)
      alert(`Validation Error: ${JSON.stringify(validationMessages, null, 2)}`)
    } catch (parseError) {
      console.error('Failed to parse error response:', parseError)
      handleApiError(
        error,
        appStore.modelSelection ===
          CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
          ? MESSAGE.MODEL_PARAM_INPUT_ERR.FAIL_RUN_MODEL
          : MESSAGE.FILE_UPLOAD_ERR.FAIL_RUN_MODEL,
      )
    }
  } else {
    handleApiError(
      error,
      appStore.modelSelection ===
        CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
        ? MESSAGE.MODEL_PARAM_INPUT_ERR.FAIL_RUN_MODEL
        : MESSAGE.FILE_UPLOAD_ERR.FAIL_RUN_MODEL,
    )
  }
}

const runModelHandler = async () => {
  try {
    isProgressVisible.value = true
    progressMessage.value = MESSAGE.PROGRESS_MSG.RUNNING_MODEL

    await delay(500)

    if (
      appStore.modelSelection ===
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
    ) {
      reportingStore.modelParamDisableTabs()

      const response = await runModel(modelParameterStore)
      console.debug('Full response:', response)

      await processResponse(response)

      reportingStore.modelParamEnableTabs()
    } else if (
      appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD
    ) {
      reportingStore.fileUploadDisableTabs()

      const response = await runModelFileUpload(fileUploadStore)
      console.debug('Full response:', response)

      await processResponse(response)

      reportingStore.fileUploadEnableTabs()
    }
  } catch (error) {
    handleError(error)
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
