import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS } from '@/constants'
import type { PanelName, PanelState } from '@/types/types'
import type { SpeciesList, SpeciesGroup } from '@/interfaces/interfaces'
import { isEmptyOrZero } from '@/utils/util'

export const useModelParameterStore = defineStore('modelParameter', () => {
  // panel open
  const panelOpenStates = ref<Record<PanelName, PanelState>>({
    speciesInfo: CONSTANTS.PANEL.OPEN,
    siteInfo: CONSTANTS.PANEL.CLOSE,
    standInfo: CONSTANTS.PANEL.CLOSE,
    reportInfo: CONSTANTS.PANEL.CLOSE,
  })

  // Panel states for confirming and editing
  const panelState = ref<
    Record<PanelName, { confirmed: boolean; editable: boolean }>
  >({
    speciesInfo: { confirmed: false, editable: true },
    siteInfo: { confirmed: false, editable: false },
    standInfo: { confirmed: false, editable: false },
    reportInfo: { confirmed: false, editable: false },
  })

  const runModelEnabled = ref(false)

  // <confirmed === true>
  // Indicates that the panel has validated and confirmed the user's input.
  // When the user completes all of the input within the panel and clicks the confirm button to pass validation, confirmed is set to true.
  // This state means that the data in that panel is currently valid and does not need to be modified.
  // When this happens, the confirm button on the current panel changes to "Edit" and the confirm button on the next panel becomes active.

  // <editable === true>
  // Indicates that the panel can be modified by the user.
  // editable becomes true when a panel is active and allows users to enter or modify data.
  // On initial loading, only the first panel (SpeciesInfo) is set to editable to allow modification;
  // the other panels start with editable false because the previous panel has not been confirmed.
  // In this state, the user can modify the input fields within the panel, and the confirm button is enabled to confirm with validation.

  // Method to handle confirm action for each panel
  const confirmPanel = (panelName: PanelName) => {
    panelState.value[panelName].confirmed = true
    panelState.value[panelName].editable = false

    // Enable the next panel's confirm and clear buttons
    const panelOrder: PanelName[] = [
      CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
      CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO,
      CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO,
      CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO,
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
  const editPanel = (panelName: PanelName) => {
    panelState.value[panelName].confirmed = false
    panelState.value[panelName].editable = true

    // Disable all subsequent panels
    const panelOrder: PanelName[] = [
      CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
      CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO,
      CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO,
      CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO,
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

  // species info
  const derivedBy = ref<string | null>(null)

  const speciesList = ref<SpeciesList[]>([
    { species: null, percent: null },
    { species: null, percent: null },
    { species: null, percent: null },
    { species: null, percent: null },
    { species: null, percent: null },
    { species: null, percent: null },
  ])

  const speciesGroups = ref<SpeciesGroup[]>([])

  // determined in Species Information
  const highestPercentSpecies = ref<string | null>(null)

  // auto-populated once highestPercentSpecies is determined, but could be changed in Site Information
  const selectedSiteSpecies = ref<string | null>(null)

  const totalSpeciesPercent = computed(() => {
    const totalPercent = speciesList.value.reduce((acc, item) => {
      return acc + (Number.parseFloat(item.percent as any) || 0)
    }, 0)

    // Preserve to the first decimal place and convert to string in '##0.0' format
    const formattedPercent = (Math.floor(totalPercent * 10) / 10).toFixed(
      CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM,
    )

    return formattedPercent
  })

  const totalSpeciesGroupPercent = computed(() => {
    return speciesGroups.value.reduce((acc, group) => {
      return acc + (Number.parseFloat(group.percent) || 0)
    }, 0)
  })

  const updateSpeciesGroup = () => {
    const groupMap: { [key: string]: number } = {}

    // Iterate through speciesList and build a group map
    for (const item of speciesList.value) {
      if (!item.species || isEmptyOrZero(item.percent)) {
        continue
      }

      // Initialize group if it doesn't exist in groupMap
      if (!groupMap[item.species]) {
        groupMap[item.species] = 0
      }

      // Add percent to the group
      groupMap[item.species] += Number.parseFloat(item.percent as any) || 0
    }

    // Convert groupMap to speciesGroups array
    speciesGroups.value = Object.keys(groupMap).map((key) => {
      const group = BIZCONSTANTS.SPECIES_GROUP_MAP[key] || key
      return {
        group,
        percent: groupMap[key].toFixed(
          CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM,
        ),
        siteSpecies: key,
        minimumDBHLimit: DEFAULTS.SPECIES_GROUP_DEFAULT_UTILIZATION_MAP[group],
      }
    })

    speciesGroups.value.sort(
      (a, b) => Number.parseFloat(b.percent) - Number.parseFloat(a.percent),
    )

    // Update highestPercentSpecies and selectedSiteSpecies with the first siteSpecies in speciesGroups
    highestPercentSpecies.value = selectedSiteSpecies.value =
      speciesGroups.value.length > 0 ? speciesGroups.value[0].siteSpecies : null
  }

  // site info
  const becZone = ref<string | null>(null)
  const ecoZone = ref<string | null>(null)
  const siteSpeciesValues = ref<string | null>(null)
  const ageType = ref<string | null>(null)
  const spzAge = ref<string | null>(null)
  const spzHeight = ref<string | null>(null)
  const bha50SiteIndex = ref<string | null>(null)

  // stand information
  const percentStockableArea = ref<string | null>(null)
  const basalArea = ref<string | null>(null)
  const treesPerHectare = ref<string | null>(null)
  const minDBHLimit = ref<string | null>(null)
  const currentDiameter = ref<string | null>(null)
  const crownClosure = ref<string | null>(null)

  // report info
  const selectedAgeYearRange = ref<string | null>(DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE)
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
  const isByLayerEnabled = ref<boolean>(true)
  const isProjectionModeEnabled = ref<boolean>(false)
  const isPolygonIDEnabled = ref<boolean>(false)
  const isCurrentYearEnabled = ref<boolean>(false)
  const isReferenceYearEnabled = ref<boolean>(false)
  const incSecondaryHeight = ref<boolean>(false)
  const specificYear = ref<string | null>(null)

  const projectionType = ref<string | null>(null)
  const reportTitle = ref<string | null>(null)

  //
  const referenceYear = ref<number | null>(null)

  // set default values
  const setDefaultValues = () => {
    derivedBy.value = DEFAULTS.DEFAULT_VALUES.DERIVED_BY
    speciesList.value = [
      { species: 'PL', percent: '30.0' },
      { species: 'AC', percent: '30.0' },
      { species: 'H', percent: '30.0' },
      { species: 'S', percent: '10.0' },
      { species: null, percent: '0.0' },
      { species: null, percent: '0.0' },
    ]

    updateSpeciesGroup()

    becZone.value = DEFAULTS.DEFAULT_VALUES.BEC_ZONE
    siteSpeciesValues.value = DEFAULTS.DEFAULT_VALUES.SITE_SPECIES_VALUES
    ageType.value = DEFAULTS.DEFAULT_VALUES.AGE_TYPE
    spzAge.value = DEFAULTS.DEFAULT_VALUES.SPZ_AGE
    spzHeight.value = DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
    bha50SiteIndex.value = DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
    percentStockableArea.value = DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA
    crownClosure.value = DEFAULTS.DEFAULT_VALUES.CROWN_CLOSURE
    minDBHLimit.value = DEFAULTS.DEFAULT_VALUES.MIN_DBH_LIMIT
    currentDiameter.value = DEFAULTS.DEFAULT_VALUES.CURRENT_DIAMETER
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
    projectionType.value = DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE
    isByLayerEnabled.value = DEFAULTS.DEFAULT_VALUES.IS_BY_LAYER_ENABLED
    reportTitle.value = DEFAULTS.DEFAULT_VALUES.REPORT_TITLE

    referenceYear.value = new Date().getFullYear()
  }

  return {
    // panel open
    panelOpenStates,
    // Panel state
    panelState,
    runModelEnabled,
    confirmPanel,
    editPanel,
    // species info
    derivedBy,
    speciesList,
    speciesGroups,
    highestPercentSpecies,
    selectedSiteSpecies,
    totalSpeciesPercent,
    totalSpeciesGroupPercent,
    updateSpeciesGroup,
    // site info
    becZone,
    ecoZone,
    siteSpeciesValues,
    ageType,
    spzAge,
    spzHeight,
    bha50SiteIndex,
    // stand info
    percentStockableArea,
    basalArea,
    treesPerHectare,
    minDBHLimit,
    currentDiameter,
    crownClosure,

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
    //
    referenceYear,
    // set default values
    setDefaultValues,
  }
})
