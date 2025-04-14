import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useReportingStore = defineStore('reportingStore', () => {
  const modelParamReportingTabsEnabled = ref(false)
  const fileUploadReportingTabsEnabled = ref(false)

  const modelParamEnableTabs = () => {
    modelParamReportingTabsEnabled.value = true
  }

  const modelParamDisableTabs = () => {
    modelParamReportingTabsEnabled.value = false
  }

  const fileUploadEnableTabs = () => {
    fileUploadReportingTabsEnabled.value = true
  }

  const fileUploadDisableTabs = () => {
    fileUploadReportingTabsEnabled.value = false
  }

  return {
    modelParamReportingTabsEnabled,
    fileUploadReportingTabsEnabled,
    modelParamEnableTabs,
    modelParamDisableTabs,
    fileUploadEnableTabs,
    fileUploadDisableTabs,
  }
})
