import { defineStore } from 'pinia'
import { ref, computed, watch, nextTick } from 'vue'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS } from '@/constants'
import type { PanelName, PanelState } from '@/types/types'
import type { SpeciesList, SpeciesGroup, ParsedProjectionParameters, SiteIndexSpeciesRow } from '@/interfaces/interfaces'
import { isEmptyOrZero } from '@/utils/util'
import { ExecutionOptionsEnum, UtilizationClassSetEnum } from '@/services/vdyp-api'
import type { ModelParameters } from '@/services/vdyp-api'

export const useModelParameterStore = defineStore('modelParameter', () => {
  // panel open
  const panelOpenStates = ref<Record<PanelName, PanelState>>({
    reportDetails: CONSTANTS.PANEL.OPEN,
    speciesInfo: CONSTANTS.PANEL.CLOSE,
    siteInfo: CONSTANTS.PANEL.CLOSE,
    standInfo: CONSTANTS.PANEL.CLOSE,
    reportSettings: CONSTANTS.PANEL.CLOSE,
  })

  // Panel states for confirming and editing
  const panelState = ref<
    Record<PanelName, { confirmed: boolean; editable: boolean }>
  >({
    reportDetails: { confirmed: false, editable: true },
    speciesInfo: { confirmed: false, editable: false },
    siteInfo: { confirmed: false, editable: false },
    standInfo: { confirmed: false, editable: false },
    reportSettings: { confirmed: false, editable: false },
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
      CONSTANTS.MANUAL_INPUT_PANEL.REPORT_DETAILS,
      CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.SITE_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.STAND_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.REPORT_SETTINGS,
    ]
    const currentIndex = panelOrder.indexOf(panelName)
    if (currentIndex !== -1 && currentIndex < panelOrder.length - 1) {
      // The current panel closes and the next panel opens automatically, switching to the editable.
      panelOpenStates.value[panelName] = CONSTANTS.PANEL.CLOSE
      const nextPanel = panelOrder[currentIndex + 1]
      panelOpenStates.value[nextPanel] = CONSTANTS.PANEL.OPEN
      panelState.value[nextPanel].editable = true
    }

    // reportInfo (the last panel) has no Next button - only the first 4 panels are
    // required to be confirmed before enabling Run Model. reportInfo is validated at run time.
    const panelsForRunCheck: PanelName[] = [
      CONSTANTS.MANUAL_INPUT_PANEL.REPORT_DETAILS,
      CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.SITE_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.STAND_INFO,
    ]
    runModelEnabled.value = panelsForRunCheck.every(
      (panel) => panelState.value[panel].confirmed,
    )
  }

  // Method to handle edit action for each panel
  const editPanel = (panelName: PanelName) => {
    panelState.value[panelName].confirmed = false
    panelState.value[panelName].editable = true
    panelOpenStates.value[panelName] = CONSTANTS.PANEL.OPEN

    // Disable all subsequent panels
    const panelOrder: PanelName[] = [
      CONSTANTS.MANUAL_INPUT_PANEL.REPORT_DETAILS,
      CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.SITE_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.STAND_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.REPORT_SETTINGS,
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

    // Recalculate Run Model button state - only first 4 panels required
    const panelsForRunCheck: PanelName[] = [
      CONSTANTS.MANUAL_INPUT_PANEL.REPORT_DETAILS,
      CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.SITE_INFO,
      CONSTANTS.MANUAL_INPUT_PANEL.STAND_INFO,
    ]
    runModelEnabled.value = panelsForRunCheck.every(
      (panel) => panelState.value[panel].confirmed,
    )
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

    // Sync siteIndexRows with updated speciesGroups, preserving existing row data
    const existingRows = new Map(siteIndexRows.value.map((r) => [r.speciesCode, r]))
    siteIndexRows.value = speciesGroups.value.map((group) => {
      const existing = existingRows.get(group.siteSpecies)
      if (existing) return existing
      return {
        speciesCode: group.siteSpecies,
        computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX,
        ageType: CONSTANTS.AGE_TYPE.TOTAL,
        age: DEFAULTS.DEFAULT_VALUES.SPZ_AGE,
        height: DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT,
        bhaSiteIndex: null,
      }
    })

    // Watchers fire only on isVolumeComputed/isSupplied transitions, not when rows are added
    // while the mode is already active. Apply constraints here to cover that gap.
    if (isVolumeComputed.value) {
      siteIndexRows.value.slice(1).forEach((row) => {
        row.computedValue = null
        row.ageType = CONSTANTS.AGE_TYPE.TOTAL
        row.age = null
        row.height = null
        row.bhaSiteIndex = null
      })
    } else if (isSupplied.value) {
      siteIndexRows.value.forEach((row) => {
        row.computedValue = null
        row.ageType = CONSTANTS.AGE_TYPE.TOTAL
        row.age = null
        row.height = null
        row.bhaSiteIndex = DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
      })
    }
  }

  // site info
  const becZone = ref<string | null>(null)
  const ecoZone = ref<string | null>(null)
  const siteSpeciesValues = ref<string | null>(CONSTANTS.SITE_SPECIES_VALUES.COMPUTED)
  const ageType = ref<string | null>(null)
  const spzAge = ref<string | null>(null)
  const spzHeight = ref<string | null>(null)
  const bha50SiteIndex = ref<string | null>(null)
  const siteIndexRows = ref<SiteIndexSpeciesRow[]>([])

  // VDYP-880: Cell enable/disable rules for the new Site Indices table (showNewSiteIndicesFeature).
  //
  // Derived By \ Site Index |  Computed                                    |  Supplied
  // ----------------------- | -------------------------------------------- | ----------------------------------
  // Volume                  | Primary row only; the field matching         | All rows: BHA Site Index only.
  //                         | Computed Value is disabled (calc.), the      | Age, Height, Computed Value: N/A.
  //                         | other two (Age/Height/BHA SI) are enabled.   |
  //                         | Non-primary rows: entirely disabled (N/A).   |
  // ----------------------- | -------------------------------------------- | ----------------------------------
  // Basal Area              | All rows active; same Computed Value rule    | All rows: BHA Site Index only.
  //                         | applies per row as above.                    | Age, Height, Computed Value: N/A.
  const isVolumeComputed = computed(
    () =>
      siteSpeciesValues.value !== CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED &&
      derivedBy.value === CONSTANTS.DERIVED_BY.VOLUME,
  )

  const isSupplied = computed(
    () => siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED,
  )

  // Why isVolumeComputed and isSupplied are watched here (not in SiteIndicesTable): transitions
  // must be tracked across component remounts. Vuetify expansion panels destroy and recreate
  // their content when collapsed/expanded, which would reset a component-level watcher's oldValue
  // to undefined and miss transitions that occurred while the panel was closed.
  watch(isVolumeComputed, (nowVC, wasVC) => {
    // The `if (isRestoringFromDB.value) return` below (also in the isSupplied watcher) prevents overwriting rows that
    // restoreFromModelParameters() just restored from DB (e.g. Cancel button). Changing
    // derivedBy/siteSpeciesValues during restore triggers these watchers before
    // restorePerSpeciesRows() sets the correct values. isRestoringFromDB resets via nextTick.
    if (isRestoringFromDB.value) return
    if (nowVC) {
      // Volume+Computed: non-primary rows must be N/A - null out all their fields except ageType.
      siteIndexRows.value.slice(1).forEach((row) => {
        row.computedValue = null
        row.ageType = CONSTANTS.AGE_TYPE.TOTAL
        row.age = null
        row.height = null
        row.bhaSiteIndex = null
      })
    } else if (wasVC) {
      // Leaving Volume+Computed -> restore defaults so non-primary rows are editable again.
      // If the destination is Supplied, the isSupplied watcher below will overwrite these
      // immediately after, which is intentional - each watcher owns its own transition.
      siteIndexRows.value.slice(1).forEach((row) => {
        row.computedValue = CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX
        row.ageType = CONSTANTS.AGE_TYPE.TOTAL
        row.age = DEFAULTS.DEFAULT_VALUES.SPZ_AGE
        row.height = DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
        row.bhaSiteIndex = null
      })
    }
  })

  watch(isSupplied, (supplied) => {
    // Same guard as above - skip during DB restore to preserve the restored row values.
    if (isRestoringFromDB.value) return
    if (supplied) {
      // Supplied: only BHA Site Index is editable - null out all other fields for every row.
      siteIndexRows.value.forEach((row) => {
        row.computedValue = null
        row.ageType = CONSTANTS.AGE_TYPE.TOTAL
        row.age = null
        row.height = null
        row.bhaSiteIndex = DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
      })
    } else {
      // Leaving Supplied -> restore Computed defaults.
      // When also entering Volume+Computed, isVolumeComputed watcher (registered before this one)
      // has already nulled non-primary rows. Skip them here so we don't overwrite those nulls
      // with defaults.
      siteIndexRows.value.forEach((row, index) => {
        if (isVolumeComputed.value && index > 0) return
        row.computedValue = CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX
        row.ageType = CONSTANTS.AGE_TYPE.TOTAL
        row.age = DEFAULTS.DEFAULT_VALUES.SPZ_AGE
        row.height = DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
        row.bhaSiteIndex = null
      })
    }
  })

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

  const projectionType = ref<string | null>(DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE)
  const reportTitle = ref<string | null>(null)
  const copyTitle = ref<string | null>(null)
  const reportDescription = ref<string | null>(null)

  //
  const referenceYear = ref<number | null>(null)

  // Reset store to initial state
  const resetStore = () => {
    // Reset panel states
    panelOpenStates.value = {
      reportDetails: CONSTANTS.PANEL.OPEN,
      speciesInfo: CONSTANTS.PANEL.CLOSE,
      siteInfo: CONSTANTS.PANEL.CLOSE,
      standInfo: CONSTANTS.PANEL.CLOSE,
      reportSettings: CONSTANTS.PANEL.CLOSE,
    }
    panelState.value = {
      reportDetails: { confirmed: false, editable: true },
      speciesInfo: { confirmed: false, editable: false },
      siteInfo: { confirmed: false, editable: false },
      standInfo: { confirmed: false, editable: false },
      reportSettings: { confirmed: false, editable: false },
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
    siteSpeciesValues.value = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
    ageType.value = DEFAULTS.DEFAULT_VALUES.AGE_TYPE
    spzAge.value = null
    spzHeight.value = null
    bha50SiteIndex.value = null
    siteIndexRows.value = []

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
    projectionType.value = DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE
    reportTitle.value = DEFAULTS.DEFAULT_VALUES.REPORT_TITLE
    copyTitle.value = null
    reportDescription.value = null
    referenceYear.value = null
  }

  /**
   * Restore execution options from the selected options array
   */
  const restoreExecutionOptions = (options: string[]) => {
    isForwardGrowEnabled.value = options.includes(ExecutionOptionsEnum.ForwardGrowEnabled)
    isBackwardGrowEnabled.value = options.includes(ExecutionOptionsEnum.BackGrowEnabled)
    isBySpeciesEnabled.value = options.includes(ExecutionOptionsEnum.DoIncludeSpeciesProjection)
    incSecondaryHeight.value = options.includes(ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable)
    isComputedMAIEnabled.value = options.includes(ExecutionOptionsEnum.ReportIncludeVolumeMAI)
    isCulminationValuesEnabled.value = options.includes(ExecutionOptionsEnum.ReportIncludeCulminationValues)

    if (options.includes(ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass)) {
      projectionType.value = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
    } else if (options.includes(ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes)) {
      projectionType.value = CONSTANTS.PROJECTION_TYPE.VOLUME
    }
  }

  /**
   * Restore utilization levels from utils array to species groups
   */
  const restoreUtilizationLevels = (utils: unknown[] | undefined) => {
    if (!utils || !Array.isArray(utils) || utils.length === 0) {
      return
    }

    const utilsMap: Record<string, UtilizationClassSetEnum> = {}
    for (const util of utils) {
      const utilObj = util as { s: string; u: string }
      if (utilObj.s && utilObj.u) {
        utilsMap[utilObj.s] = utilObj.u as UtilizationClassSetEnum
      }
    }

    for (const group of speciesGroups.value) {
      if (utilsMap[group.group]) {
        group.minimumDBHLimit = utilsMap[group.group]
      }
    }
  }

  /**
   * Infer and apply panel state for edit mode from currently populated store values.
   * Each panel is confirmed only if its required field is set AND all prior panels are confirmed.
   */
  const restoreEditModePanelState = () => {
    const det = reportTitle.value !== null && reportTitle.value.trim() !== ''
    const spe = det && speciesGroups.value.length > 0
    const sit = spe && becZone.value !== null
    const sta = sit && percentStockableArea.value !== null
    const rep = sta && startingAge.value !== null
    const detOpen = !det
    const speOpen = det && !spe
    const sitOpen = spe && !sit
    const staOpen = sit && !sta
    const repOpen = sta && !rep
    panelOpenStates.value = {
      reportDetails: detOpen ? CONSTANTS.PANEL.OPEN : CONSTANTS.PANEL.CLOSE,
      speciesInfo: speOpen ? CONSTANTS.PANEL.OPEN : CONSTANTS.PANEL.CLOSE,
      siteInfo: sitOpen ? CONSTANTS.PANEL.OPEN : CONSTANTS.PANEL.CLOSE,
      standInfo: staOpen ? CONSTANTS.PANEL.OPEN : CONSTANTS.PANEL.CLOSE,
      reportSettings: repOpen ? CONSTANTS.PANEL.OPEN : CONSTANTS.PANEL.CLOSE,
    }
    panelState.value = {
      reportDetails: { confirmed: det, editable: detOpen },
      speciesInfo: { confirmed: spe, editable: speOpen },
      siteInfo: { confirmed: sit, editable: sitOpen },
      standInfo: { confirmed: sta, editable: staOpen },
      reportSettings: { confirmed: rep, editable: repOpen },
    }
    runModelEnabled.value = sta
  }

  /**
   * Restore store state from parsed projection parameters.
   * In view mode, all panels open and confirmed.
   * In edit mode, infers confirmed status from populated data and opens only the first incomplete panel.
   * @param params Parsed projection parameters from the backend
   * @param isViewMode If true, sets all panels to confirmed and non-editable
   */
  const restoreFromProjectionParams = (
    params: ParsedProjectionParameters,
    isViewMode: boolean = false,
  ) => {
    reportTitle.value = params.reportTitle
    copyTitle.value = params.copyTitle
    startingAge.value = params.ageStart
    finishingAge.value = params.ageEnd
    ageIncrement.value = params.ageIncrement

    restoreExecutionOptions(params.selectedExecutionOptions)
    restoreUtilizationLevels(params.utils)

    if (!referenceYear.value) {
      referenceYear.value = new Date().getFullYear()
    }

    if (isViewMode) {
      panelOpenStates.value = {
        reportDetails: CONSTANTS.PANEL.OPEN,
        speciesInfo: CONSTANTS.PANEL.OPEN,
        siteInfo: CONSTANTS.PANEL.OPEN,
        standInfo: CONSTANTS.PANEL.OPEN,
        reportSettings: CONSTANTS.PANEL.OPEN,
      }
      panelState.value = {
        reportDetails: { confirmed: true, editable: false },
        speciesInfo: { confirmed: true, editable: false },
        siteInfo: { confirmed: true, editable: false },
        standInfo: { confirmed: true, editable: false },
        reportSettings: { confirmed: true, editable: false },
      }
      // In view mode all first 4 panels are confirmed - run model is enabled
      runModelEnabled.value = true
    } else {
      restoreEditModePanelState()
    }
  }

  /**
   * Convert a nullable number to string or null
   */
  const toStringOrNull = (value: number | null | undefined): string | null => {
    return value !== null && value !== undefined ? value.toString() : null
  }

  /**
   * Format percent value to fixed decimal string
   */
  const formatPercentValue = (percentValue: number | string | null | undefined): string | null => {
    if (percentValue === null || percentValue === undefined) {
      return null
    }
    const numValue = typeof percentValue === 'string' ? Number.parseFloat(percentValue) : percentValue
    return numValue.toFixed(CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_DECIMAL_NUM)
  }

  /**
   * Restore species list from model parameters
   */
  const restoreSpeciesListFromParams = (species: ModelParameters['species']) => {
    speciesList.value = []
    for (let i = 0; i < 6; i++) {
      if (species && i < species.length) {
        speciesList.value.push({
          species: species[i].code,
          percent: formatPercentValue(species[i].percent),
        })
      } else {
        speciesList.value.push({ species: null, percent: null })
      }
    }
    updateSpeciesGroup()
  }

  // Old data (pre-VDYP-1076): infer computedValue from which field is null
  const inferPrimaryRowFromOldData = (
    primaryRow: SiteIndexSpeciesRow,
    age: string | null,
    height: string | null,
    si: string | null
  ) => {
    if (!si) {
      primaryRow.computedValue = CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX
      primaryRow.age = age ?? DEFAULTS.DEFAULT_VALUES.SPZ_AGE
      primaryRow.height = height ?? DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
      primaryRow.bhaSiteIndex = null
    } else if (!height) {
      primaryRow.computedValue = CONSTANTS.COMPUTED_VALUE.HEIGHT
      primaryRow.age = age ?? DEFAULTS.DEFAULT_VALUES.SPZ_AGE
      primaryRow.height = null
      primaryRow.bhaSiteIndex = si
    } else if (age) {
      // All three values present - default to BHA_SITE_INDEX computed
      primaryRow.computedValue = CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX
      primaryRow.age = age
      primaryRow.height = height
      primaryRow.bhaSiteIndex = null
    } else {
      // age is null -> TOTAL_AGE computed
      primaryRow.computedValue = CONSTANTS.COMPUTED_VALUE.TOTAL_AGE
      primaryRow.age = null
      primaryRow.height = height
      primaryRow.bhaSiteIndex = si
    }
  }

  // Restore SPP2~6 rows from VDYP-1076 fields
  const restorePerSpeciesRows = (params: ModelParameters) => {
    const perSpeciesData = [
      { compute: params.compute2, ageYears: params.ageYears2, age: params.age2, height: params.height2, si: params.si2 },
      { compute: params.compute3, ageYears: params.ageYears3, age: params.age3, height: params.height3, si: params.si3 },
      { compute: params.compute4, ageYears: params.ageYears4, age: params.age4, height: params.height4, si: params.si4 },
      { compute: params.compute5, ageYears: params.ageYears5, age: params.age5, height: params.height5, si: params.si5 },
      { compute: params.compute6, ageYears: params.ageYears6, age: params.age6, height: params.height6, si: params.si6 },
    ]
    for (let i = 1; i < siteIndexRows.value.length; i++) {
      const row = siteIndexRows.value[i]
      const data = perSpeciesData[i - 1]
      if (siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED) {
        // Supplied mode: null out all fields except BHA site index and ageType (mirrors the isSupplied watcher).
        row.computedValue = null
        row.ageType = CONSTANTS.AGE_TYPE.TOTAL
        row.age = null
        row.height = null
        row.bhaSiteIndex = data.si ?? DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
      } else if (data.compute) {
        // New data (VDYP-1076): restore all fields; null means that field is computed
        row.computedValue = data.compute
        row.ageType = data.ageYears ?? CONSTANTS.AGE_TYPE.TOTAL
        row.age = data.age
        row.height = data.height
        row.bhaSiteIndex = data.si
      }
      // Old data (no compute stored): leave row with defaults set by updateSpeciesGroup()
    }
  }

  /**
   * Restore site info from model parameters
   */
  const restoreSiteInfoFromParams = (params: ModelParameters) => {
    becZone.value = params.becZone
    ecoZone.value = params.ecoZone
    siteSpeciesValues.value = params.siteIndex
    ageType.value = params.ageYears
    spzAge.value = params.speciesAge
    spzHeight.value = params.speciesHeight
    bha50SiteIndex.value = params.bha50SiteIndex

    if (siteIndexRows.value.length > 0) {
      const primaryRow = siteIndexRows.value[0]

      if (siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED) {
        // Supplied mode: only BHA site index is editable - null out all other fields explicitly
        // so that row state stays consistent even when the isSupplied watcher was skipped (DB restore).
        primaryRow.computedValue = null
        primaryRow.ageType = CONSTANTS.AGE_TYPE.TOTAL
        primaryRow.age = null
        primaryRow.height = null
        primaryRow.bhaSiteIndex = bha50SiteIndex.value
      } else if (params.compute) {
        primaryRow.ageType = ageType.value ?? CONSTANTS.AGE_TYPE.TOTAL
        // New data (VDYP-1076): restore compute directly from stored value
        primaryRow.computedValue = params.compute
        primaryRow.age = spzAge.value ?? DEFAULTS.DEFAULT_VALUES.SPZ_AGE
        primaryRow.height = spzHeight.value ?? DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
        primaryRow.bhaSiteIndex = bha50SiteIndex.value
        if (params.compute === CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX) {
          primaryRow.bhaSiteIndex = null
        } else if (params.compute === CONSTANTS.COMPUTED_VALUE.HEIGHT) {
          primaryRow.height = null
        } else if (params.compute === CONSTANTS.COMPUTED_VALUE.TOTAL_AGE) {
          primaryRow.age = null
        }
      } else {
        primaryRow.ageType = ageType.value ?? CONSTANTS.AGE_TYPE.TOTAL
        inferPrimaryRowFromOldData(primaryRow, spzAge.value, spzHeight.value, bha50SiteIndex.value)
      }
    }

    restorePerSpeciesRows(params)
  }

  /**
   * Restore stand info from model parameters
   */
  const restoreStandInfoFromParams = (params: ModelParameters) => {
    percentStockableArea.value = toStringOrNull(params.stockable)
    crownClosure.value = toStringOrNull(params.cc)
    basalArea.value = toStringOrNull(params.BA)
    treesPerHectare.value = toStringOrNull(params.TPH)
    minDBHLimit.value = params.minDBHLimit
    currentDiameter.value = params.currentDiameter
  }

  /**
   * Restore species, site, and stand info from model parameters JSON
   * @param params The ModelParameters object from the backend
   */
  const isRestoringFromDB = ref(false)

  const restoreFromModelParameters = (params: ModelParameters) => {
    isRestoringFromDB.value = true
    derivedBy.value = params.derivedBy
    restoreSpeciesListFromParams(params.species)

    if (params.siteSpecies) {
      highestPercentSpecies.value = params.siteSpecies
      selectedSiteSpecies.value = params.siteSpecies
    }

    restoreSiteInfoFromParams(params)
    restoreStandInfoFromParams(params)

    if (!referenceYear.value) {
      referenceYear.value = new Date().getFullYear()
    }

    nextTick(() => {
      isRestoringFromDB.value = false
    })
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
    isVolumeComputed,
    isSupplied,
    becZone,
    ecoZone,
    siteSpeciesValues,
    ageType,
    spzAge,
    spzHeight,
    bha50SiteIndex,
    siteIndexRows,
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
    copyTitle,
    reportDescription,
    //
    referenceYear,
    // restore functions
    isRestoringFromDB,
    resetStore,
    restoreFromProjectionParams,
    restoreFromModelParameters,
  }
})
