<template>
  <v-container fluid class="bcds-reporting-container">
    <ReportingOutput :data="data" :tabname="tabname" />
  </v-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { ReportingOutput } from '@/components/projection'
import { useProjectionStore } from '@/stores/projection/projectionStore'
import { CONSTANTS } from '@/constants'
import type { ReportingTab } from '@/types/types'

const props = defineProps({
  tabname: {
    type: String as PropType<ReportingTab>,
    required: true,
  },
})
const projectionStore = useProjectionStore()

const data = computed(() => {
  switch (props.tabname) {
    case CONSTANTS.REPORTING_TAB.MODEL_REPORT:
      return [...projectionStore.txtYieldLines]
    case CONSTANTS.REPORTING_TAB.VIEW_ERR_MSG:
      return [...projectionStore.errorMessages]
    case CONSTANTS.REPORTING_TAB.VIEW_LOG_FILE:
      return [...projectionStore.logMessages]
    default:
      return []
  }
})
</script>
<style scoped>
/* BC Gov Design Standards - Full-width container for reporting tabs */
.bcds-reporting-container {
  /* Override Vuetify's default container max-width and padding */
  max-width: 100% !important;
  width: 100%;
  padding-left: var(--layout-padding-none) !important;
  padding-right: var(--layout-padding-none) !important;
  padding-top: 0px;
  padding-bottom: var(--layout-padding-medium);
  margin: var(--layout-margin-none);
}

/* Ensure container uses full available space */
.bcds-reporting-container :deep(.v-container) {
  max-width: 100% !important;
}
</style>
