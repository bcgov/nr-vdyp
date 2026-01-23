<template>
  <AppProgressCircular
    :isShow="isProgressVisible"
    :showMessage="true"
    :message="progressMessage"
    :hasBackground="true"
  />
  <v-container fluid>
    <div>
      <div id="modelSelectionCard" class="model-selection-header">
        <h3
          v-if="appStore.modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS"
        >
          {{ CONSTANTS.HEADER_SELECTION.MODEL_PARAMETER_SELECTION }}
        </h3>
        <h3 v-else>{{ CONSTANTS.HEADER_SELECTION.FILE_UPLOAD }}</h3>
      </div>
    </div>
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
        <SiteInfoPanel class="panel-spacing" />
        <StandInfoPanel class="panel-spacing" />
        <ReportInfoPanel class="panel-spacing" />
        <AppRunModelButtonPanel
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
        <AttachmentsPanel class="panel-spacing" />
        <AppRunModelButtonPanel
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
import { ref, onMounted, computed, nextTick } from 'vue'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useProjectionStore } from '@/stores/projectionStore'
import { useReportingStore } from '@/stores/reportingStore'
import {
  AppProgressCircular,
  AppTabs,
  AppRunModelButtonPanel,
} from '@/components'
import {
  ReportingContainer,
  SpeciesInfoPanel,
  SiteInfoPanel,
  StandInfoPanel,
  ReportInfoPanel,
  AttachmentsPanel,
} from '@/components/projection'
import type { Tab } from '@/interfaces/interfaces'
import { CONSTANTS, DEFAULTS, MESSAGE } from '@/constants'
import { handleApiError } from '@/services/apiErrorHandler'
import { runModel } from '@/services/projection/modelParameterService'
import { runModelFileUpload } from '@/services/projection/fileUploadService'
import {
  checkZipForErrors,
  delay,
  extractZipFileName,
  sanitizeFileName,
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
    modelParamActiveTab.value ===
      CONSTANTS.MODEL_PARAM_TAB_INDEX.PARAM_SELECTION
  )
})

const isFileUploadPanelsVisible = computed(() => {
  return (
    appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD &&
    fileUploadActiveTab.value ===
      CONSTANTS.FILE_UPLOAD_TAB_INDEX.PARAM_SELECTION
  )
})

onMounted(() => {
  console.debug(`appStore.modelSelection: ${appStore.modelSelection}`)
  fileUploadStore.initializeSpeciesGroups()
})

const processResponse = async (response: any): Promise<boolean> => {
  const zipFileName =
    extractZipFileName(response.headers) ||
    CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP

  const resultBlob = response.data

  console.debug('resultBlob:', resultBlob, 'type:', resultBlob?.type)
  console.debug('resultBlob size:', resultBlob.size)

  if (!resultBlob || !(resultBlob instanceof Blob)) {
    throw new Error('Invalid response data')
  }

  const reportTitle =
    appStore.modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
      ? modelParameterStore.reportTitle
      : fileUploadStore.reportTitle

  let finalFileName = zipFileName

  if (
    reportTitle &&
    reportTitle !== DEFAULTS.DEFAULT_VALUES.REPORT_TITLE &&
    zipFileName.startsWith('vdyp-output')
  ) {
    const sanitizedTitle = sanitizeFileName(reportTitle)
    const timestampPart = zipFileName.substring('vdyp-output'.length)
    // Limit the total file name to 200 characters to account for operating system file name length restrictions
    const maxTitleLength = 200 - timestampPart.length
    const truncatedTitle = sanitizedTitle.substring(0, maxTitleLength)
    finalFileName = truncatedTitle + timestampPart
  }

  const hasErrors = await checkZipForErrors(resultBlob)
  await projectionStore.handleZipResponse(resultBlob, finalFileName)

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
  return hasErrors
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

      const hasErrors = await processResponse(response)

      reportingStore.modelParamEnableTabs()

      await nextTick() // Ensure UI updates before switching tabs
      setTimeout(() => {
        modelParamActiveTab.value = hasErrors
          ? CONSTANTS.MODEL_PARAM_TAB_INDEX.VIEW_ERROR_MESSAGES
          : CONSTANTS.MODEL_PARAM_TAB_INDEX.MODEL_REPORT
        console.log(
          'After tab switch (Input Model):',
          modelParamActiveTab.value,
        )
      }, 100) // Switching tabs after a small delay
    } else if (
      appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD
    ) {
      reportingStore.fileUploadDisableTabs()

      const response = await runModelFileUpload(fileUploadStore)
      console.debug('Full response:', response)

      const hasErrors = await processResponse(response)

      reportingStore.fileUploadEnableTabs()

      await nextTick() // Ensure UI updates before switching tabs
      setTimeout(() => {
        fileUploadActiveTab.value = hasErrors
          ? CONSTANTS.FILE_UPLOAD_TAB_INDEX.VIEW_ERROR_MESSAGES
          : CONSTANTS.FILE_UPLOAD_TAB_INDEX.MODEL_REPORT
        console.log(
          'After tab switch (File Upload):',
          fileUploadActiveTab.value,
        )
      }, 100) // Switching tabs after a small delay
    }
  } catch (error) {
    handleError(error)
    console.error('Error during model execution:', error)
  } finally {
    isProgressVisible.value = false
  }
}
</script>

<style scoped>
.model-selection-header {
  margin-bottom: var(--layout-margin-medium);
}

h3 {
  font: var(--typography-bold-h3);
  color: var(--typography-color-primary);
}

.panel-spacing {
  margin-top: var(--layout-margin-medium);
}
</style>
