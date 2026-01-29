import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS } from '@/constants'
import type { PanelName, PanelState } from '@/types/types'
import type { SpeciesList, SpeciesGroup, ParsedProjectionParameters, ParsedCsvFileContent } from '@/interfaces/interfaces'
import { isEmptyOrZero } from '@/utils/util'
import { ExecutionOptionsEnum } from '@/services/vdyp-api'
import type { ModelParameters } from '@/services/vdyp-api'

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

  // Reset store to initial state
  const resetStore = () => {
    // Reset panel states
    panelOpenStates.value = {
      speciesInfo: CONSTANTS.PANEL.OPEN,
      siteInfo: CONSTANTS.PANEL.CLOSE,
      standInfo: CONSTANTS.PANEL.CLOSE,
      reportInfo: CONSTANTS.PANEL.CLOSE,
    }
    panelState.value = {
      speciesInfo: { confirmed: false, editable: true },
      siteInfo: { confirmed: false, editable: false },
      standInfo: { confirmed: false, editable: false },
      reportInfo: { confirmed: false, editable: false },
    }
    runModelEnabled.value = false

    // Reset species info
    derivedBy.value = null
    speciesList.value = [
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
    ]
    speciesGroups.value = []
    highestPercentSpecies.value = null
    selectedSiteSpecies.value = null

    // Reset site info
    becZone.value = null
    ecoZone.value = null
    siteSpeciesValues.value = null
    ageType.value = DEFAULTS.DEFAULT_VALUES.AGE_TYPE
    spzAge.value = null
    spzHeight.value = null
    bha50SiteIndex.value = null

    // Reset stand info
    percentStockableArea.value = null
    basalArea.value = null
    treesPerHectare.value = null
    minDBHLimit.value = null
    currentDiameter.value = null
    crownClosure.value = null

    // Reset report info
    selectedAgeYearRange.value = DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
    startingAge.value = null
    finishingAge.value = null
    ageIncrement.value = null
    startYear.value = null
    endYear.value = null
    yearIncrement.value = null
    isForwardGrowEnabled.value = true
    isBackwardGrowEnabled.value = true
    isComputedMAIEnabled.value = false
    isCulminationValuesEnabled.value = false
    isBySpeciesEnabled.value = false
    isByLayerEnabled.value = true
    isProjectionModeEnabled.value = false
    isPolygonIDEnabled.value = false
    isCurrentYearEnabled.value = false
    isReferenceYearEnabled.value = false
    incSecondaryHeight.value = false
    specificYear.value = null
    projectionType.value = null
    reportTitle.value = DEFAULTS.DEFAULT_VALUES.REPORT_TITLE
    referenceYear.value = null
  }

  /**
   * Restore store state from parsed projection parameters
   * @param params Parsed projection parameters from the backend
   * @param isViewMode If true, sets all panels to confirmed and non-editable
   */
  const restoreFromProjectionParams = (
    params: ParsedProjectionParameters,
    isViewMode: boolean = false,
  ) => {
    // Restore report info from parameters
    reportTitle.value = params.reportTitle

    // Restore age/year range settings
    startingAge.value = params.ageStart
    finishingAge.value = params.ageEnd
    ageIncrement.value = params.ageIncrement
    startYear.value = params.yearStart
    endYear.value = params.yearEnd

    // Determine selectedAgeYearRange based on what values exist
    const hasAge = params.ageStart !== null || params.ageEnd !== null
    const hasYear = params.yearStart !== null || params.yearEnd !== null
    if (hasAge && hasYear) {
      selectedAgeYearRange.value = params.combineAgeYearRange || 'intersect'
    } else if (hasYear) {
      selectedAgeYearRange.value = CONSTANTS.AGE_YEAR_RANGE.YEAR
    } else {
      selectedAgeYearRange.value = CONSTANTS.AGE_YEAR_RANGE.AGE
    }

    // Restore execution options
    const options = params.selectedExecutionOptions

    isForwardGrowEnabled.value = options.includes(ExecutionOptionsEnum.ForwardGrowEnabled)
    isBackwardGrowEnabled.value = options.includes(ExecutionOptionsEnum.BackGrowEnabled)
    isByLayerEnabled.value = options.includes(ExecutionOptionsEnum.DoSummarizeProjectionByLayer)
    isBySpeciesEnabled.value = options.includes(ExecutionOptionsEnum.DoIncludeSpeciesProjection)
    isPolygonIDEnabled.value = options.includes(ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable)
    isProjectionModeEnabled.value = options.includes(ExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable)
    isCurrentYearEnabled.value = options.includes(ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables)
    isReferenceYearEnabled.value = options.includes(ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables)
    incSecondaryHeight.value = options.includes(ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable)
    isComputedMAIEnabled.value = options.includes(ExecutionOptionsEnum.ReportIncludeVolumeMAI)
    isCulminationValuesEnabled.value = options.includes(ExecutionOptionsEnum.ReportIncludeCulminationValues)

    // Determine projection type
    if (options.includes(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)) {
      projectionType.value = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
    } else if (options.includes(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)) {
      projectionType.value = CONSTANTS.PROJECTION_TYPE.VOLUME
    }

    // Set reference year to current year if not set
    if (!referenceYear.value) {
      referenceYear.value = new Date().getFullYear()
    }

    // Set panel states based on view/edit mode
    if (isViewMode) {
      // In view mode, all panels are confirmed and not editable
      panelOpenStates.value = {
        speciesInfo: CONSTANTS.PANEL.OPEN,
        siteInfo: CONSTANTS.PANEL.OPEN,
        standInfo: CONSTANTS.PANEL.OPEN,
        reportInfo: CONSTANTS.PANEL.OPEN,
      }
      panelState.value = {
        speciesInfo: { confirmed: true, editable: false },
        siteInfo: { confirmed: true, editable: false },
        standInfo: { confirmed: true, editable: false },
        reportInfo: { confirmed: true, editable: false },
      }
      runModelEnabled.value = false
    } else {
      // In edit mode, only first panel is open and editable (same as new projection)
      panelOpenStates.value = {
        speciesInfo: CONSTANTS.PANEL.OPEN,
        siteInfo: CONSTANTS.PANEL.CLOSE,
        standInfo: CONSTANTS.PANEL.CLOSE,
        reportInfo: CONSTANTS.PANEL.CLOSE,
      }
      panelState.value = {
        speciesInfo: { confirmed: false, editable: true },
        siteInfo: { confirmed: false, editable: false },
        standInfo: { confirmed: false, editable: false },
        reportInfo: { confirmed: false, editable: false },
      }
      runModelEnabled.value = false
    }
  }

  /**
   * Restore species, site, and stand info from parsed CSV file content
   * @param parsed The parsed CSV file content from parseCsvFileContent()
   */
  const restoreFromFileContent = (parsed: ParsedCsvFileContent) => {
    // Restore species info
    derivedBy.value = parsed.derivedBy
    // Format percent values with proper decimal places
    speciesList.value = parsed.speciesList.map((item) => ({
      species: item.species,
      percent: item.percent !== null && item.percent !== undefined && item.percent !== ''
        ? Number.parseFloat(item.percent as string).toFixed(CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM)
        : null,
    }))
    updateSpeciesGroup()

    if (parsed.highestPercentSpecies) {
      highestPercentSpecies.value = parsed.highestPercentSpecies
      selectedSiteSpecies.value = parsed.highestPercentSpecies
    }

    // Restore site info
    becZone.value = parsed.becZone
    ecoZone.value = parsed.ecoZone
    siteSpeciesValues.value = parsed.siteSpeciesValues
    spzAge.value = parsed.spzAge
    spzHeight.value = parsed.spzHeight
    bha50SiteIndex.value = parsed.bha50SiteIndex

    // Restore stand info
    percentStockableArea.value = parsed.percentStockableArea
    crownClosure.value = parsed.crownClosure
    basalArea.value = parsed.basalArea
    treesPerHectare.value = parsed.treesPerHectare

    // Restore reference year
    if (parsed.referenceYear) {
      referenceYear.value = parsed.referenceYear
    }
  }

  /**
   * Restore species, site, and stand info from model parameters JSON
   * @param params The ModelParameters object from the backend
   */
  const restoreFromModelParameters = (params: ModelParameters) => {
    // Restore species info
    derivedBy.value = params.derivedBy

    // Restore species list from params.species
    if (params.species && params.species.length > 0) {
      speciesList.value = []
      for (let i = 0; i < 6; i++) {
        if (i < params.species.length) {
          const percentValue = params.species[i].percent
          let formattedPercent: string | null = null
          if (percentValue !== null && percentValue !== undefined) {
            // Convert to number (handles both number and string from JSON) and format with decimal
            const numValue = typeof percentValue === 'string' ? Number.parseFloat(percentValue) : percentValue
            formattedPercent = numValue.toFixed(CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM)
          }
          speciesList.value.push({
            species: params.species[i].code,
            percent: formattedPercent,
          })
        } else {
          speciesList.value.push({ species: null, percent: null })
        }
      }
      updateSpeciesGroup()
    }

    // Restore site species
    if (params.siteSpecies) {
      highestPercentSpecies.value = params.siteSpecies
      selectedSiteSpecies.value = params.siteSpecies
    }

    // Restore site info
    becZone.value = params.becZone
    ecoZone.value = params.ecoZone
    siteSpeciesValues.value = params.siteIndex
    ageType.value = params.ageYears
    spzAge.value = params.speciesAge !== null ? params.speciesAge.toString() : null
    spzHeight.value = params.speciesHeight !== null ? params.speciesHeight.toString() : null
    bha50SiteIndex.value = params.bha50SiteIndex !== null ? params.bha50SiteIndex.toString() : null

    // Restore stand info
    percentStockableArea.value = params.stockable !== null ? params.stockable.toString() : null
    crownClosure.value = params.cc !== null ? params.cc.toString() : null
    basalArea.value = params.BA !== null ? params.BA.toString() : null
    treesPerHectare.value = params.TPH !== null ? params.TPH.toString() : null
    minDBHLimit.value = params.minDBHLimit
    currentDiameter.value = params.currentDiameter

    // Set reference year to current year if not set
    if (!referenceYear.value) {
      referenceYear.value = new Date().getFullYear()
    }
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
    // restore functions
    resetStore,
    restoreFromProjectionParams,
    restoreFromFileContent,
    restoreFromModelParameters,
  }
})
