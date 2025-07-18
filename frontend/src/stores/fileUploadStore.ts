import { defineStore } from 'pinia'
import { ref } from 'vue'
import { CONSTANTS, DEFAULTS } from '@/constants'
import type { FileUploadPanelName, PanelState } from '@/types/types'

export const useFileUploadStore = defineStore('fileUploadStore', () => {
  // panel open
  const panelOpenStates = ref<Record<FileUploadPanelName, PanelState>>({
    reportInfo: CONSTANTS.PANEL.OPEN,
    attachments: CONSTANTS.PANEL.CLOSE,
  })

  // Panel states for confirming and editing
  const panelState = ref<
    Record<FileUploadPanelName, { confirmed: boolean; editable: boolean }>
  >({
    reportInfo: { confirmed: false, editable: true },
    attachments: { confirmed: false, editable: false },
  })

  const runModelEnabled = ref(false)

  // Method to handle confirm action for each panel
  const confirmPanel = (panelName: FileUploadPanelName) => {
    panelState.value[panelName].confirmed = true
    panelState.value[panelName].editable = false

    // Enable the next panel's confirm and clear buttons
    const panelOrder: FileUploadPanelName[] = [
      CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO,
      CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS,
    ]
    const currentIndex = panelOrder.indexOf(panelName)
    if (currentIndex !== -1 && currentIndex < panelOrder.length - 1) {
      // The next panel opens automatically, switching to the editable.
      const nextPanel = panelOrder[currentIndex + 1]
      panelOpenStates.value[nextPanel] = CONSTANTS.PANEL.OPEN
      panelState.value[nextPanel].editable = true
    }

    // Check if all panels are confirmed to enable the 'Run Model' button
    runModelEnabled.value = panelOrder.every(
      (panel) => panelState.value[panel].confirmed,
    )
  }

  // Method to handle edit action for each panel
  const editPanel = (panelName: FileUploadPanelName) => {
    panelState.value[panelName].confirmed = false
    panelState.value[panelName].editable = true

    // Disable all subsequent panels
    const panelOrder: FileUploadPanelName[] = [
      CONSTANTS.FILE_UPLOAD_PANEL.REPORT_INFO,
      CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS,
    ]
    const currentIndex = panelOrder.indexOf(panelName)
    if (currentIndex !== -1) {
      for (let i = currentIndex + 1; i < panelOrder.length; i++) {
        // All of the next panels are automatically closed, uneditable, and unconfirmed
        const nextPanel = panelOrder[i]
        panelState.value[nextPanel].confirmed = false
        panelState.value[nextPanel].editable = false
        panelOpenStates.value[nextPanel] = CONSTANTS.PANEL.CLOSE
      }
    }

    // Disable 'Run Model' button
    runModelEnabled.value = false
  }

  // report info
  const selectedAgeYearRange = ref<string | null>(null)
  const startingAge = ref<number | null>(null)
  const finishingAge = ref<number | null>(null)
  const ageIncrement = ref<number | null>(null)

  const startYear = ref<number | null>(null)
  const endYear = ref<number | null>(null)
  const yearIncrement = ref<number | null>(null)

  const isForwardGrowEnabled = ref<boolean>(true)
  const isBackwardGrowEnabled = ref<boolean>(true)

  const volumeReported = ref<string[]>([])
  const includeInReport = ref<string[]>([])
  const projectionType = ref<string | null>(null)
  const reportTitle = ref<string | null>(null)

  // attachments
  const polygonFile = ref<File | null>(null)
  const layerFile = ref<File | null>(null)

  // set default values
  const setDefaultValues = () => {
    selectedAgeYearRange.value = DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
    startingAge.value = DEFAULTS.DEFAULT_VALUES.STARTING_AGE
    finishingAge.value = DEFAULTS.DEFAULT_VALUES.FINISHING_AGE
    ageIncrement.value = DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT
    startYear.value = DEFAULTS.DEFAULT_VALUES.START_YEAR
    endYear.value = DEFAULTS.DEFAULT_VALUES.END_YEAR
    yearIncrement.value = DEFAULTS.DEFAULT_VALUES.YEAR_INCREMENT
    isForwardGrowEnabled.value = DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED
    isBackwardGrowEnabled.value =
      DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED
    volumeReported.value = DEFAULTS.DEFAULT_VALUES.VOLUME_REPORTED
    projectionType.value = DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE
    reportTitle.value = DEFAULTS.DEFAULT_VALUES.REPORT_TITLE
  }

  return {
    // panel open
    panelOpenStates,
    // Panel state
    panelState,
    runModelEnabled,
    confirmPanel,
    editPanel,
    // report info
    selectedAgeYearRange,
    startingAge,
    finishingAge,
    ageIncrement,
    startYear,
    endYear,
    yearIncrement,
    isForwardGrowEnabled,
    isBackwardGrowEnabled,
    volumeReported,
    includeInReport,
    projectionType,
    reportTitle,
    // attachments
    polygonFile,
    layerFile,
    // set default values
    setDefaultValues,
  }
})
