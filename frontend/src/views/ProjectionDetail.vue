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
        <RunProjectionButtonPanel
          v-if="!appStore.isReadOnly || isRunning"
          :isDisabled="!modelParameterStore.runModelEnabled || !appStore.isDraft"
          :showCancelButton="isRunning"
          cardClass="input-model-param-run-model-card"
          cardActionsClass="card-actions"
          @runModel="runModelHandler"
          @cancelRun="cancelRunHandler"
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
        <RunProjectionButtonPanel
          v-if="!appStore.isReadOnly || isRunning"
          :isDisabled="!fileUploadStore.runModelEnabled || !appStore.isDraft"
          :showCancelButton="isRunning"
          cardClass="input-model-param-run-model-card"
          cardActionsClass="card-actions"
          @runModel="runModelHandler"
          @cancelRun="cancelRunHandler"
        />
      </template>
    </template>
  </v-container>
</template>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useReportingStore } from '@/stores/reportingStore'
import { useNotificationStore } from '@/stores/common/notificationStore'
import {
  AppProgressCircular,
  AppTabs,
} from '@/components'
import {
  ReportingContainer,
  SpeciesInfoPanel,
  SiteInfoPanel,
  StandInfoPanel,
  ReportInfoPanel,
  AttachmentsPanel,
  RunProjectionButtonPanel
} from '@/components/projection'
import type { Tab } from '@/interfaces/interfaces'
import { CONSTANTS, MESSAGE } from '@/constants'
import { mapProjectionStatus, cancelProjection, getProjectionById } from '@/services/projectionService'
import { handleApiError } from '@/services/apiErrorHandler'
import { runProjection } from '@/services/projection/modelParameterService'
import { runProjectionFileUpload } from '@/services/projection/fileUploadService'
import {
  delay,
} from '@/utils/util'
import { logSuccessMessage } from '@/utils/messageHandler'

const isProgressVisible = ref(false)
const progressMessage = ref('')
const modelParamActiveTab = ref(0)
const fileUploadActiveTab = ref(0)

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const fileUploadStore = useFileUploadStore()
const reportingStore = useReportingStore()
const notificationStore = useNotificationStore()

const isRunning = computed(() => appStore.currentProjectionStatus === CONSTANTS.PROJECTION_STATUS.RUNNING)

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
  // Only initialize species groups for new projections
  // For existing projections (view/edit), the values are already restored from the backend
  if (appStore.viewMode === CONSTANTS.PROJECTION_VIEW_MODE.CREATE && appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD) {
    fileUploadStore.initializeSpeciesGroups()
  }
})

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
    progressMessage.value = MESSAGE.PROGRESS_MSG.RUNNING_PROJECTION

    await delay(500)

    if (
      appStore.modelSelection ===
      CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
    ) {
      const response = await runProjection()
      console.debug('Full response:', response)

      // Update status to RUNNING and switch to VIEW mode
      if (response && response.projectionStatusCode) {
        appStore.setCurrentProjectionStatus(
          mapProjectionStatus(response.projectionStatusCode.code),
        )
        appStore.setViewMode(CONSTANTS.PROJECTION_VIEW_MODE.VIEW)
      }

      // Show success notification
      logSuccessMessage(
        MESSAGE.SUCCESS_MSG.BATCH_PROJECTION_STARTED,
        null,
        false,
        false,
        MESSAGE.SUCCESS_MSG.BATCH_PROJECTION_STARTED_TITLE,
      )
    } else if (
      appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD
    ) {
      const response = await runProjectionFileUpload()
      console.debug('Full response:', response)

      // Update status to RUNNING and switch to VIEW mode
      if (response && response.projectionStatusCode) {
        appStore.setCurrentProjectionStatus(
          mapProjectionStatus(response.projectionStatusCode.code),
        )
        appStore.setViewMode(CONSTANTS.PROJECTION_VIEW_MODE.VIEW)
      }

      // Show success notification
      logSuccessMessage(
        MESSAGE.SUCCESS_MSG.BATCH_PROJECTION_STARTED,
        null,
        false,
        false,
        MESSAGE.SUCCESS_MSG.BATCH_PROJECTION_STARTED_TITLE,
      )
    }
  } catch (error) {
    handleError(error)
    console.error('Error during model execution:', error)
  } finally {
    isProgressVisible.value = false
  }
}

const updateProjectionState = (status: string) => {
  const mappedStatus = mapProjectionStatus(status)
  appStore.setCurrentProjectionStatus(mappedStatus)

  if (mappedStatus === CONSTANTS.PROJECTION_STATUS.READY || mappedStatus === CONSTANTS.PROJECTION_STATUS.FAILED) {
    appStore.setViewMode(CONSTANTS.PROJECTION_VIEW_MODE.VIEW)
  } else if (mappedStatus === CONSTANTS.PROJECTION_STATUS.DRAFT) {
    appStore.setViewMode(CONSTANTS.PROJECTION_VIEW_MODE.EDIT)
  }
}

const cancelRunHandler = async () => {
  const projectionGUID = appStore.currentProjectionGUID
  if (!projectionGUID) {
    notificationStore.showErrorMessage(MESSAGE.PROJECTION_ERR.CANCEL_FAILED, MESSAGE.PROJECTION_ERR.CANCEL_FAILED_TITLE)
    return
  }

  try {
    isProgressVisible.value = true
    progressMessage.value = MESSAGE.PROGRESS_MSG.CANCELLING_PROJECTION

    // Pre-check: fetch the latest projection status from the backend
    const latestProjection = await getProjectionById(projectionGUID)
    const latestStatus = mapProjectionStatus(latestProjection.projectionStatusCode?.code || CONSTANTS.PROJECTION_STATUS.DRAFT)

    if (latestStatus !== CONSTANTS.PROJECTION_STATUS.RUNNING) {
      // Projection is no longer running — update state and show appropriate message
      updateProjectionState(latestProjection.projectionStatusCode?.code || CONSTANTS.PROJECTION_STATUS.DRAFT)

      if (latestStatus === CONSTANTS.PROJECTION_STATUS.READY) {
        notificationStore.showInfoMessage(MESSAGE.PROJECTION_ERR.CANCEL_ALREADY_COMPLETED, MESSAGE.PROJECTION_ERR.CANCEL_ALREADY_COMPLETED_TITLE)
      } else if (latestStatus === CONSTANTS.PROJECTION_STATUS.FAILED) {
        notificationStore.showWarningMessage(MESSAGE.PROJECTION_ERR.CANCEL_ALREADY_FAILED, MESSAGE.PROJECTION_ERR.CANCEL_ALREADY_FAILED_TITLE)
      } else {
        notificationStore.showInfoMessage(MESSAGE.PROJECTION_ERR.CANCEL_NOT_RUNNING, MESSAGE.PROJECTION_ERR.CANCEL_NOT_RUNNING_TITLE)
      }
      return
    }

    await cancelProjection(projectionGUID)

    appStore.setCurrentProjectionStatus(CONSTANTS.PROJECTION_STATUS.DRAFT)
    appStore.setViewMode(CONSTANTS.PROJECTION_VIEW_MODE.EDIT)

    notificationStore.showSuccessMessage(MESSAGE.SUCCESS_MSG.PROJECTION_CANCELLED, MESSAGE.SUCCESS_MSG.PROJECTION_CANCELLED_TITLE)
  } catch (error) {
    console.error('Error cancelling projection:', error)
    notificationStore.showErrorMessage(MESSAGE.PROJECTION_ERR.CANCEL_FAILED, MESSAGE.PROJECTION_ERR.CANCEL_FAILED_TITLE)
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
