import { defineStore } from 'pinia'
import { ref } from 'vue'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS } from '@/constants'
import type { FileUploadPanelName, PanelState } from '@/types/types'
import type { FileUploadSpeciesGroup } from '@/interfaces/interfaces'

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
  const startingAge = ref<string | null>(null)
  const finishingAge = ref<string | null>(null)
  const ageIncrement = ref<string | null>(null)
  const startYear = ref<string | null>(null)
  const endYear = ref<string | null>(null)
  const yearIncrement = ref<string | null>(null)
  const isForwardGrowEnabled = ref<boolean>(true)
  const isBackwardGrowEnabled = ref<boolean>(true)

  const isComputedMAIEnabled = ref<boolean>(false)
  const isCulminationValuesEnabled = ref<boolean>(false)
  const isBySpeciesEnabled = ref<boolean>(false)
  const isByLayerEnabled = ref<boolean>(false)
  const isProjectionModeEnabled = ref<boolean>(false)
  const isPolygonIDEnabled = ref<boolean>(false)
  const isCurrentYearEnabled = ref<boolean>(false)
  const isReferenceYearEnabled = ref<boolean>(false)
  const incSecondaryHeight = ref<boolean>(false)
  const specificYear = ref<string | null>(null)

  const projectionType = ref<string | null>(null)
  const reportTitle = ref<string | null>(null)

  // species groups for utilization levels
  const fileUploadSpeciesGroup = ref<FileUploadSpeciesGroup[]>([])

  // initialize species groups with all 16 species
  const initializeSpeciesGroups = () => {
    fileUploadSpeciesGroup.value = BIZCONSTANTS.SPECIES_GROUPS.map((group) => ({
      group,
      minimumDBHLimit: DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group],
    }))
  }

  // update species groups based on projection type
  const updateSpeciesGroupsForProjectionType = (type: string | null) => {
    if (type === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS) {
      // CFO Biomass: use specific values
      fileUploadSpeciesGroup.value.forEach((group) => {
        group.minimumDBHLimit =
          DEFAULTS.SPECIES_GROUP_CFO_BIOMASS_UTILIZATION_MAP[group.group]
      })
    } else {
      // Volume: use default values
      fileUploadSpeciesGroup.value.forEach((group) => {
        group.minimumDBHLimit =
          DEFAULTS.SPECIES_GROUP_VOLUME_UTILIZATION_MAP[group.group]
      })
    }
  }

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
    isByLayerEnabled.value = DEFAULTS.DEFAULT_VALUES.IS_BY_LAYER_ENABLED
    isPolygonIDEnabled.value = DEFAULTS.DEFAULT_VALUES.IS_POLYGON_ID_ENABLED
    projectionType.value = DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE
    reportTitle.value = DEFAULTS.DEFAULT_VALUES.REPORT_TITLE

    // initialize species groups
    initializeSpeciesGroups()
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
    isComputedMAIEnabled,
    isCulminationValuesEnabled,
    isBySpeciesEnabled,
    isByLayerEnabled,
    isProjectionModeEnabled,
    isPolygonIDEnabled,
    isCurrentYearEnabled,
    isReferenceYearEnabled,
    incSecondaryHeight,
    specificYear,
    projectionType,
    reportTitle,
    // species groups
    fileUploadSpeciesGroup,
    initializeSpeciesGroups,
    updateSpeciesGroupsForProjectionType,
    // attachments
    polygonFile,
    layerFile,
    // set default values
    setDefaultValues,
  }
})
