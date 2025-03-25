<template>
  <v-card
    elevation="0"
    style="
      display: flex;
      justify-content: end;
      align-items: end;
      text-align: end;
    "
    class="mr-2"
  >
    <v-card-actions class="pr-0">
      <v-spacer></v-spacer>
      <AppButton
        label="Print"
        :isDisabled="isButtonDisabled"
        customClass="blue-btn"
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
        customClass="white-btn"
        @click="handleDownload"
      />
      <AppButton
        v-if="tabname === REPORTING_TAB.MODEL_REPORT"
        label="Download Raw Results"
        :isDisabled="isRawResultsButtonDisabled"
        customClass="white-btn"
        @click="handleDownloadRawResult"
      />
    </v-card-actions>
  </v-card>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import AppButton from '@/components/core/AppButton.vue'
import type { ReportingTab } from '@/types/types'
import { REPORTING_TAB } from '@/constants/constants'

defineProps({
  isButtonDisabled: {
    type: Boolean,
    required: true,
  },
  isRawResultsButtonDisabled: {
    type: Boolean,
    required: true,
  },
  tabname: {
    type: String as PropType<ReportingTab>,
    required: true,
  },
})

const emit =
  defineEmits<(e: 'print' | 'download' | 'downloadrawresult') => void>()

const handlePrint = () => {
  emit('print')
}

const handleDownload = () => {
  emit('download')
}

const handleDownloadRawResult = () => {
  emit('downloadrawresult')
}
</script>
