<template>
  <AppProgressCircular
    :isShow="showProgress"
    :showMessage="true"
    :message="progressMessage || fileOperationMessage"
    :hasBackground="true"
  />
  <v-container fluid>
    <div>
      <router-link to="/" class="return-to-list-link">
        <img :src="MenuIcon" alt="" class="return-to-list-icon" />
        <span>Return to Projections List</span>
      </router-link>
      <div id="modelSelectionCard" class="model-selection-header">
        <h3
          v-if="appStore.modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS"
        >
          {{ CONSTANTS.HEADER_SELECTION.MODEL_PARAMETER_SELECTION }}
        </h3>
        <h3 v-else>{{ CONSTANTS.HEADER_SELECTION.FILE_UPLOAD }}</h3>
        <v-menu v-if="isRunning">
          <template #activator="{ props }">
            <button v-bind="props" class="running-status-menu-button">
              <img
                :src="getStatusIcon(CONSTANTS.PROJECTION_STATUS.RUNNING)"
                alt="Running"
                class="running-status-icon"
              />
              <span class="running-status-text">Running</span>
              <v-icon size="small">mdi-chevron-down</v-icon>
            </button>
          </template>
          <v-list class="running-status-menu-list">
            <v-list-item
              class="running-status-menu-item"
              @click="cancelRunHandler"
            >
              <div class="running-menu-item-content">
                <img
                  src="@/assets/icons/Cancel_Icon_Menu.png"
                  alt="Cancel"
                  class="running-menu-icon"
                />
                <span class="running-menu-text">Cancel</span>
              </div>
            </v-list-item>
          </v-list>
        </v-menu>
        <div v-else-if="isReady" class="ready-status-container">
          <img
            :src="getStatusIcon(CONSTANTS.PROJECTION_STATUS.READY)"
            alt="Ready"
            class="ready-status-icon"
          />
          <span class="ready-status-text">Ready</span>
        </div>
      </div>
    </div>
    <template
      v-if="
        appStore.modelSelection ===
        CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
      "
    >
      <div class="tabs-with-download">
        <AppTabs
          v-model:currentTab="modelParamActiveTab"
          :tabs="modelParamTabs"
        />
        <AppButton
          label="Download Report"
          :icon-src="DownloadIcon"
          variant="primary"
          :is-disabled="!isDownloadReady"
          class="download-report-button"
          @click="handleDownloadReport"
        />
      </div>
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
      <div class="file-upload-header">
        <AppButton
          label="Download Report"
          :icon-src="DownloadIcon"
          variant="primary"
          :is-disabled="!isDownloadReady"
          @click="handleDownloadReport"
        />
      </div>
      <template v-if="isFileUploadPanelsVisible">
        <ParameterSelectionProgressBar
          :sections="fileUploadProgressSections"
          :percentage="fileUploadPercentage"
          :completedCount="fileUploadCompletedCount"
          :projectionStatus="appStore.currentProjectionStatus"
          class="panel-spacing"
        />
        <ReportInfoPanel class="panel-spacing" />
        <MinimumDBHPanel class="panel-spacing" />
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
import { ref, onMounted, computed, nextTick } from 'vue'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useReportingStore } from '@/stores/reportingStore'
import { useProjectionStore } from '@/stores/projection/projectionStore'
import { useNotificationStore } from '@/stores/common/notificationStore'
import {
  AppProgressCircular,
  AppTabs,
  AppButton,
} from '@/components'
import {
  ReportingContainer,
  SpeciesInfoPanel,
  SiteInfoPanel,
  StandInfoPanel,
  ReportInfoPanel,
  AttachmentsPanel,
  MinimumDBHPanel,
  ParameterSelectionProgressBar,
  RunProjectionButtonPanel
} from '@/components/projection'
import type { Tab } from '@/interfaces/interfaces'
import { CONSTANTS, MESSAGE } from '@/constants'
import { mapProjectionStatus, cancelProjection, getProjectionById, streamResultsZip } from '@/services/projectionService'
import { handleApiError } from '@/services/apiErrorHandler'
import { runProjection } from '@/services/projection/modelParameterService'
import { runProjectionFileUpload } from '@/services/projection/fileUploadService'
import {
  delay,
  getStatusIcon,
  downloadFile,
  sanitizeFileName,
  checkZipForErrors,
} from '@/utils/util'
import { logSuccessMessage, logErrorMessage } from '@/utils/messageHandler'
import { DownloadIcon, MenuIcon } from '@/assets/'

const isProgressVisible = ref(false)
const progressMessage = ref('')
const isFileUploading = computed(() => fileUploadStore.isUploadingPolygon || fileUploadStore.isUploadingLayer)
const isDeletingFile = computed(() => fileUploadStore.isDeletingFile)
const isSaving = computed(() => appStore.isSavingProjection)
const showProgress = computed(() => isProgressVisible.value || isFileUploading.value || isDeletingFile.value || isSaving.value)
const fileOperationMessage = computed(() => {
  if (isSaving.value) return MESSAGE.PROGRESS_MSG.SAVING_PROJECTION
  if (isDeletingFile.value) return MESSAGE.PROGRESS_MSG.DELETING_FILE
  if (isFileUploading.value) return MESSAGE.PROGRESS_MSG.UPLOADING_FILE
  return ''
})
const modelParamActiveTab = ref(0)

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const fileUploadStore = useFileUploadStore()
const reportingStore = useReportingStore()
const projectionStore = useProjectionStore()
const notificationStore = useNotificationStore()

const isRunning = computed(() => appStore.currentProjectionStatus === CONSTANTS.PROJECTION_STATUS.RUNNING)
const isReady = computed(() => appStore.currentProjectionStatus === CONSTANTS.PROJECTION_STATUS.READY)
const isDownloadReady = computed(() => appStore.currentProjectionStatus === CONSTANTS.PROJECTION_STATUS.READY)

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
    disabled: !isReady.value || !reportingStore.modelParamReportingTabsEnabled,
  },
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.VIEW_LOG_FILE,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.VIEW_LOG_FILE,
    disabled: !isReady.value || !reportingStore.modelParamReportingTabsEnabled,
  },
  {
    label: CONSTANTS.MODEL_PARAM_TAB_NAME.VIEW_ERROR_MESSAGES,
    component: ReportingContainer,
    tabname: CONSTANTS.REPORTING_TAB.VIEW_ERR_MSG,
    disabled: !isReady.value || !reportingStore.modelParamReportingTabsEnabled,
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
  return appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD
})

const fileUploadPrerequisitesDone = computed(
  () =>
    fileUploadStore.panelState.reportInfo.confirmed &&
    fileUploadStore.panelState.minimumDBH.confirmed,
)

const uploadedFilesCount = computed(() => {
  let count = 0
  if (fileUploadStore.polygonFileInfo !== null) count++
  if (fileUploadStore.layerFileInfo !== null) count++
  return count
})

const fileUploadProgressSections = computed(() => [
  {
    label: 'Report Details',
    completed: fileUploadStore.panelState.reportInfo.confirmed,
  },
  {
    label: 'Minimum DBH',
    completed: fileUploadStore.panelState.minimumDBH.confirmed,
  },
  {
    label: 'File Upload',
    completed:
      fileUploadPrerequisitesDone.value && uploadedFilesCount.value === 2,
  },
])

// Each pre-section contributes 33%, file upload contributes 17% per file (total 34%)
// but only when both prerequisites (Report Details + Minimum DBH) are confirmed.
const fileUploadPercentage = computed(() => {
  let pct = 0
  if (fileUploadStore.panelState.reportInfo.confirmed) pct += 33
  if (fileUploadStore.panelState.minimumDBH.confirmed) pct += 33
  if (fileUploadPrerequisitesDone.value) {
    pct += uploadedFilesCount.value * 17
  }
  return pct
})

const fileUploadCompletedCount = computed(() => {
  let count = 0
  if (fileUploadStore.panelState.reportInfo.confirmed) count++
  if (fileUploadStore.panelState.minimumDBH.confirmed) count++
  if (fileUploadPrerequisitesDone.value && uploadedFilesCount.value === 2) count++
  return count
})

/**
 * Enables reporting tabs, navigates to the appropriate tab, and shows a result message.
 * Navigates to Error Messages tab if errors are found, otherwise Model Report tab.
 */
const enableTabsAndNavigate = async (hasErrors: boolean, showMessage: boolean = true) => {
  reportingStore.modelParamEnableTabs()
  await nextTick()
  setTimeout(() => {
    modelParamActiveTab.value = hasErrors
      ? CONSTANTS.MODEL_PARAM_TAB_INDEX.VIEW_ERROR_MESSAGES
      : CONSTANTS.MODEL_PARAM_TAB_INDEX.MODEL_REPORT
  }, 100)

  if (showMessage) {
    if (hasErrors) {
      logErrorMessage(MESSAGE.SUCCESS_MSG.INPUT_MODEL_PARAM_RUN_RESULT_W_ERR, null, false, false, MESSAGE.SUCCESS_MSG.PROJECTION_RUN_RESULT_W_ERR_TITLE)
    } else {
      logSuccessMessage(MESSAGE.SUCCESS_MSG.INPUT_MODEL_PARAM_RUN_RESULT, null, false, false, MESSAGE.SUCCESS_MSG.PROJECTION_RUN_RESULT_TITLE)
    }
  }
}

/**
 * Fetches the result zip file for a READY projection, parses it,
 * populates the report tabs, and navigates to the appropriate tab.
 * Navigates to Error Messages tab if errors are found, otherwise Model Report tab.
 */
const fetchAndPopulateResults = async (showMessage: boolean = true) => {
  const projectionGUID = appStore.currentProjectionGUID
  if (!projectionGUID) return

  try {
    isProgressVisible.value = true
    progressMessage.value = MESSAGE.PROGRESS_MSG.LOADING_RESULTS

    // Stream the results zip via backend proxy (avoids CORS issues with direct S3 access)
    const { zipBlob, zipFileName } = await streamResultsZip(projectionGUID)

    const hasErrors = await checkZipForErrors(zipBlob)
    await projectionStore.handleZipResponse(zipBlob, zipFileName)

    await enableTabsAndNavigate(hasErrors, showMessage)
  } catch (error) {
    console.error('Error fetching and populating results:', error)
    notificationStore.showErrorMessage(
      MESSAGE.PROJECTION_ERR.RESULTS_LOAD_FAILED,
      MESSAGE.PROJECTION_ERR.RESULTS_LOAD_FAILED_TITLE,
    )
  } finally {
    isProgressVisible.value = false
  }
}

onMounted(() => {
  // Only initialize species groups for new projections
  // For existing projections (view/edit), the values are already restored from the backend
  if (appStore.viewMode === CONSTANTS.PROJECTION_VIEW_MODE.CREATE && appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD) {
    fileUploadStore.initializeSpeciesGroups()
  }

  // For READY Input Model Parameters projections, automatically fetch and display results
  // File Upload projections do not have reporting tabs; users download the ZIP instead
  // Skip showing result messages when navigating from the list view
  if (isReady.value && appStore.modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS) {
    fetchAndPopulateResults(false)
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
      // Validate files are uploaded before running
      if (!fileUploadStore.polygonFileInfo) {
        notificationStore.showErrorMessage(MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_MISSING)
        return
      }
      if (!fileUploadStore.layerFileInfo) {
        notificationStore.showErrorMessage(MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_MISSING)
        return
      }

      const response = await runProjectionFileUpload()
      console.debug('Full response:', response)

      // Update status to RUNNING and switch to VIEW mode
      if (response && response.projectionStatusCode) {
        appStore.setCurrentProjectionStatus(
          mapProjectionStatus(response.projectionStatusCode.code),
        )
        appStore.setViewMode(CONSTANTS.PROJECTION_VIEW_MODE.VIEW)
        fileUploadStore.panelOpenStates.attachments = CONSTANTS.PANEL.CLOSE
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
        isProgressVisible.value = false
        if (appStore.modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS) {
          await fetchAndPopulateResults()
        }
        return
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

const handleDownloadReport = async () => {
  const projectionGUID = appStore.currentProjectionGUID
  if (!projectionGUID) {
    notificationStore.showErrorMessage(MESSAGE.PROJECTION_ERR.MISSING_GUID)
    return
  }

  const reportTitle =
    appStore.modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
      ? modelParameterStore.reportTitle
      : fileUploadStore.reportTitle
  const zipFileName = sanitizeFileName(`${reportTitle || 'Projection'}_All Files`) + '.zip'

  isProgressVisible.value = true
  progressMessage.value = MESSAGE.PROGRESS_MSG.DOWNLOADING_PROJECTION

  try {
    // Stream the results zip via backend proxy and download with custom filename
    const { zipBlob } = await streamResultsZip(projectionGUID)
    downloadFile(zipBlob, zipFileName)

    notificationStore.showSuccessMessage(
      MESSAGE.SUCCESS_MSG.DOWNLOAD_SUCCESS(zipFileName),
      MESSAGE.SUCCESS_MSG.DOWNLOAD_SUCCESS_TITLE,
    )
  } catch (err) {
    console.error('Error downloading projection files:', err)
    notificationStore.showErrorMessage(
      MESSAGE.PROJECTION_ERR.DOWNLOAD_FAILED(zipFileName),
      MESSAGE.PROJECTION_ERR.DOWNLOAD_FAILED_TITLE,
    )
  } finally {
    isProgressVisible.value = false
  }
}
</script>

<style scoped>
.return-to-list-link {
  display: inline-flex;
  align-items: center;
  gap: var(--layout-padding-small);
  text-decoration: none;
  color: var(--typography-color-link);
  font: var(--typography-regular-body);
  margin-bottom: var(--layout-margin-small);
}

.return-to-list-link:hover {
  text-decoration: underline;
}

.return-to-list-icon {
  width: 20px;
  height: 16px;
  flex-shrink: 0;
}

.model-selection-header {
  margin-bottom: var(--layout-margin-medium);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

h3 {
  font: var(--typography-bold-h3);
  color: var(--typography-color-primary);
}

.running-status-menu-button {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-xsmall);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: var(--layout-padding-xsmall) var(--layout-padding-small);
  border-radius: var(--layout-border-radius-small);
  transition: background-color 0.2s;
}

.running-status-menu-button:hover {
  background-color: var(--surface-color-background-light-gray);
}

.running-status-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
}

.running-status-text {
  font: var(--typography-bold-h5);
  color: var(--support-border-color-warning);
}

.running-status-menu-list {
  min-width: 120px;
}

.running-status-menu-item {
  cursor: pointer;
  min-height: 32px;
}

.running-status-menu-item:hover {
  background-color: #eceae8;
}

.running-menu-item-content {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-medium);
}

.running-menu-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  object-fit: contain;
}

.running-menu-text {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
}

.ready-status-container {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-xsmall);
  padding: var(--layout-padding-xsmall) var(--layout-padding-small);
}

.ready-status-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
}

.ready-status-text {
  font: var(--typography-bold-h5);
  color: var(--support-border-color-success);
}

.panel-spacing {
  margin-top: var(--layout-margin-medium);
}

.file-upload-header {
  display: flex;
  justify-content: flex-end;
}

.file-upload-header :deep(.button-icon-img) {
  filter: brightness(0) invert(1);
}

.tabs-with-download {
  position: relative;
}

.download-report-button {
  position: absolute;
  top: 0;
  right: 0;
}

.download-report-button :deep(.button-icon-img) {
  filter: brightness(0) invert(1);
}
</style>
