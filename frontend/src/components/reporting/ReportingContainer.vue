<template>
  <v-container>
    <ReportingActions
      :isButtonDisabled="isButtonDisabled"
      @print="handlePrint"
      @download="handleDownload"
    />
    <ReportingOutput :data="data" />
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
import { useProjectionStore } from '@/stores/projectionStore'
import { FILE_NAME, REPORTING_TAB } from '@/constants/constants'
import { FILE_DOWNLOAD_ERR } from '@/constants/message'
import type { ReportingTab } from '@/types/types'
import * as messageHandler from '@/utils/messageHandler'

const props = defineProps({
  tabname: {
    type: String as PropType<ReportingTab>,
    required: true,
  },
})

const projectionStore = useProjectionStore()
const data = computed(() => {
  switch (props.tabname) {
    case REPORTING_TAB.MODEL_REPORT:
      return [...projectionStore.yieldTableArray]
    case REPORTING_TAB.VIEW_ERR_MSG:
      return [...projectionStore.errorMessages]
    case REPORTING_TAB.VIEW_LOG_FILE:
      return [...projectionStore.logMessages]
    default:
      return []
  }
})

const isButtonDisabled = computed(() => !data.value || data.value.length === 0)

const handleDownload = () => {
  if (!data.value || data.value.length === 0) {
    messageHandler.logErrorMessage(FILE_DOWNLOAD_ERR.NO_DATA)
    return
  }
  switch (props.tabname) {
    case REPORTING_TAB.MODEL_REPORT:
      downloadCSVFile(data.value, FILE_NAME.YIELD_TABLE_CSV)
      break
    case REPORTING_TAB.VIEW_ERR_MSG:
      downloadTextFile(data.value, FILE_NAME.ERROR_TXT)
      break
    case REPORTING_TAB.VIEW_LOG_FILE:
      downloadTextFile(data.value, FILE_NAME.LOG_TXT)
      break
  }
}

const handlePrint = () => {
  printReport(data.value)
}
</script>

<style scoped></style>
