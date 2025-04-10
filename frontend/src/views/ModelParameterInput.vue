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
    </template>
  </v-container>
</template>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
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
  FileUpload,
} from '@/components'
import type { Tab } from '@/interfaces/interfaces'
import { CONSTANTS, MESSAGE, DEFAULTS } from '@/constants'
import { handleApiError } from '@/services/apiErrorHandler'
import { runModel } from '@/services/modelParameterService'
import { checkZipForErrors, delay, extractZipFileName } from '@/utils/util'
import { logSuccessMessage, logErrorMessage } from '@/utils/messageHandler'

const modelSelection = ref<string>(DEFAULTS.DEFAULT_VALUES.MODEL_SELECTION)
const isProgressVisible = ref(false)
const progressMessage = ref('')
const modelParamActiveTab = ref(0)
const fileUploadActiveTab = ref(0)

const modelParameterStore = useModelParameterStore()
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
    component: FileUpload,
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

/**
 * Computes whether the model parameter panels should be visible.
 * Panels are visible when the model selection equals the constant for input model parameters and the active tab is 0.
 */
const isModelParameterPanelsVisible = computed(() => {
  return (
    modelSelection.value === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS &&
    modelParamActiveTab.value === 0
  )
})

/**
 * Updates the model selection value.
 * @param newSelection - The new model selection string.
 */
const updateModelSelection = (newSelection: string) => {
  modelSelection.value = newSelection
}

/**
 * Sets default values in the model parameter store when the component is mounted.
 */
onMounted(() => {
  modelParameterStore.setDefaultValues()
})

/**
 * Handles the run model process.
 * This function shows a progress indicator, waits briefly, then runs the model using the modelParameterStore.
 * It processes the returned zip response, logs a success message, and handles any errors.
 */
const runModelHandler = async () => {
  try {
    isProgressVisible.value = true
    progressMessage.value = MESSAGE.PROGRESS_MSG.RUNNING_MODEL

    await delay(1000)

    reportingStore.modelParamDisableTabs()
    console.log(
      'modelParamReportingTabsDisabled:',
      reportingStore.modelParamReportingTabsEnabled,
    )

    const response = await runModel(modelParameterStore)

    console.debug('Full response:', response)

    const zipFileName =
      extractZipFileName(response.headers) ||
      CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP
    console.debug('download zip file name:', zipFileName)

    const resultBlob = response.data

    console.debug('resultBlob:', resultBlob, 'type:', resultBlob?.type)
    console.debug('resultBlob size:', resultBlob.size)

    if (!resultBlob) {
      throw new Error('Response data is undefined')
    }

    if (!(resultBlob instanceof Blob)) {
      throw new Error('Response data is not a Blob')
    }

    const hasErrors = await checkZipForErrors(resultBlob)

    await projectionStore.handleZipResponse(resultBlob, zipFileName)

    if (hasErrors) {
      logErrorMessage(MESSAGE.SUCCESS_MSG.INPUT_MODEL_PARAM_RUN_RESULT_W_ERR)
    } else {
      logSuccessMessage(MESSAGE.SUCCESS_MSG.INPUT_MODEL_PARAM_RUN_RESULT)
    }

    reportingStore.modelParamEnableTabs()
  } catch (error) {
    if ((error as any).response && (error as any).response.data) {
      try {
        const validationMessages = JSON.parse((error as any).response.data)
        console.log('Validation Messages:', validationMessages)
        alert(
          `Validation Error: ${JSON.stringify(validationMessages, null, 2)}`,
        )
      } catch (parseError) {
        handleApiError(error, MESSAGE.MODEL_PARAM_INPUT_ERR.FAIL_RUN_MODEL)
      }
    } else {
      handleApiError(error, MESSAGE.MODEL_PARAM_INPUT_ERR.FAIL_RUN_MODEL)
    }
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
