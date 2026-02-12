<template>
  <v-card elevation="0" class="bcds-reporting-actions-card">
    <v-card-actions class="bcds-reporting-actions-container">
      <v-spacer></v-spacer>
      <AppButton
        label="Print"
        :isDisabled="isButtonDisabled"
        variant="primary"
        mdi-name="mdi-printer"
        @click="handlePrint"
      />
      <v-spacer></v-spacer>
      <AppButton
        :label="
          tabname === REPORTING_TAB.MODEL_REPORT
            ? 'Download Yield Table'
            : 'Download'
        "
        :isDisabled="isButtonDisabled"
        variant="secondary"
        mdi-name="mdi-download"
        @click="handleDownload"
      />
    </v-card-actions>
  </v-card>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import { AppButton } from '@/components'
import type { ReportingTab } from '@/types/types'
import { REPORTING_TAB, REPORTING_ACTION } from '@/constants/constants'

defineProps({
  isButtonDisabled: {
    type: Boolean,
    required: true,
  },
  tabname: {
    type: String as PropType<ReportingTab>,
    required: true,
  },
})

const emit =
  defineEmits<(e: typeof REPORTING_ACTION.PRINT | typeof REPORTING_ACTION.DOWNLOAD | typeof REPORTING_ACTION.DOWNLOAD_RAW_RESULT) => void>()

const handlePrint = () => {
  emit(REPORTING_ACTION.PRINT)
}

const handleDownload = () => {
  emit(REPORTING_ACTION.DOWNLOAD)
}
</script>

<style scoped>
/* BC Gov Design Standards - Reporting Actions Container */

/* Card wrapper for action buttons */
.bcds-reporting-actions-card {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
  text-align: end;
  margin-right: var(--layout-margin-small);
  background-color: transparent;
}

/* Actions container - button group */
.bcds-reporting-actions-container {
  padding-right: var(--layout-padding-none) !important;
  padding-left: var(--layout-padding-none);
  padding-top: var(--layout-padding-small);
  padding-bottom: var(--layout-padding-small);
  gap: var(--layout-margin-small);
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
}

/* Responsive layout for smaller screens */
@media (max-width: 768px) {
  .bcds-reporting-actions-card {
    margin-right: var(--layout-margin-xsmall);
  }

  .bcds-reporting-actions-container {
    flex-wrap: wrap;
    gap: var(--layout-margin-xsmall);
  }
}
</style>
