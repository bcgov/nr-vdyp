<template>
  <v-container>
    <ReportingActions
      :isButtonDisabled="isButtonDisabled"
      :isRawResultsButtonDisabled="isRawResultsButtonDisabled"
      :tabname="tabname"
      @print="handlePrint"
      @download="handleDownload"
      @downloadrawresult="handleDownloadRawResult"
    />
    <ReportingOutput :data="data" :tabname="tabname" />
  </v-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import ReportingActions from '@/components/reporting/ReportingActions.vue'
import ReportingOutput from '@/components/reporting/ReportingOutput.vue'
import {
  downloadTextFile,
  downloadCSVFile,
  printReport,
} from '@/services/reportService'
import { useAppStore } from '@/stores/appStore'
import { useProjectionStore } from '@/stores/projectionStore'
import { CONSTANTS, MESSAGE } from '@/constants'
import type { ReportingTab } from '@/types/types'
import * as messageHandler from '@/utils/messageHandler'
import { downloadFile } from '@/utils/util'

const props = defineProps({
  tabname: {
    type: String as PropType<ReportingTab>,
    required: true,
  },
})
const appStore = useAppStore()
const projectionStore = useProjectionStore()

const data = computed(() => {
  switch (props.tabname) {
    case CONSTANTS.REPORTING_TAB.MODEL_REPORT:
      if (
        appStore.modelSelection ===
        CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
      ) {
        return [...projectionStore.txtYieldLines]
      } else {
        return [...projectionStore.csvYieldLines]
      }
    case CONSTANTS.REPORTING_TAB.VIEW_ERR_MSG:
      return [...projectionStore.errorMessages]
    case CONSTANTS.REPORTING_TAB.VIEW_LOG_FILE:
      return [...projectionStore.logMessages]
    default:
      return []
  }
})

// For downloads, always use CSV format for MODEL_REPORT
const downloadData = computed(() => {
  if (props.tabname === CONSTANTS.REPORTING_TAB.MODEL_REPORT)
    return [...projectionStore.csvYieldLines]
  else return data.value
})

const printData = computed(() => {
  if (props.tabname === CONSTANTS.REPORTING_TAB.MODEL_REPORT) {
    if (appStore.modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS) {
        return [...projectionStore.txtYieldLines]
    } else {
        return [...projectionStore.csvYieldLines]
    }
  }
  else return data.value
})

const rawResults = computed(() => {
  if (props.tabname === CONSTANTS.REPORTING_TAB.MODEL_REPORT) {
    return projectionStore.rawResultZipFile
  }
  return null
})

const isButtonDisabled = computed(
  () => !downloadData.value || downloadData.value.length === 0,
)
const isRawResultsButtonDisabled = computed(() => !rawResults.value)

const handleDownload = () => {
  if (!downloadData.value || downloadData.value.length === 0) {
    messageHandler.logErrorMessage(MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA)
    return
  }
  switch (props.tabname) {
    case CONSTANTS.REPORTING_TAB.MODEL_REPORT:
      if (!downloadData.value || downloadData.value.length === 0) {
        messageHandler.logErrorMessage(MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA)
        return
      }
      downloadCSVFile(downloadData.value, CONSTANTS.FILE_NAME.YIELD_TABLE_CSV)
      break
    case CONSTANTS.REPORTING_TAB.VIEW_ERR_MSG:
      downloadTextFile(downloadData.value, CONSTANTS.FILE_NAME.ERROR_TXT)
      break
    case CONSTANTS.REPORTING_TAB.VIEW_LOG_FILE:
      downloadTextFile(downloadData.value, CONSTANTS.FILE_NAME.LOG_TXT)
      break
  }
}

const handleDownloadRawResult = () => {
  if (
    !projectionStore.rawResultZipFile ||
    !projectionStore.rawResultZipFileName
  ) {
    messageHandler.logErrorMessage(MESSAGE.FILE_DOWNLOAD_ERR.NO_DATA)
    return
  }

  downloadFile(
    projectionStore.rawResultZipFile,
    projectionStore.rawResultZipFileName,
  )
}

const handlePrint = () => {
  printReport(printData.value)
}
</script>
<style scoped>
/** */
</style>
