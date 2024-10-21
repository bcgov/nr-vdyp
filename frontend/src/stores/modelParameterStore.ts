import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useModelParameterStore = defineStore('modelParameter', () => {
  // species info
  const derivedBy = ref(null)

  const speciesList = ref<{ species: string | null; percent: number | null }[]>(
    [
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
      { species: null, percent: null },
    ],
  )

  const speciesGroups = ref<
    {
      group: string
      percent: number
      siteSpecies: string
      minimumDBHLimit: string
    }[]
  >([])

  // determined in Species Information
  const highestPercentSpecies = ref<string | null>(null)

  // auto-populated once highestPercentSpecies is determined, but could be changed in Site Information
  const selectedSiteSpecies = ref<string | null>(null)

  const totalSpeciesPercent = computed(() => {
    const totalPercent = speciesList.value.reduce((acc, item) => {
      return acc + (parseFloat(item.percent as any) || 0)
    }, 0)
    // preserve to the first decimal place and truncate after that
    return Math.floor(totalPercent * 10) / 10
  })

  const totalSpeciesGroupPercent = computed(() => {
    return speciesGroups.value.reduce((acc, group) => {
      return acc + group.percent
    }, 0)
  })

  const isOverTotalPercent = computed(() => {
    return totalSpeciesPercent.value > 100
  })

  const updateSpeciesGroup = () => {
    const groupMap: { [key: string]: number } = {}

    speciesList.value.forEach((item) => {
      if (item.species && item.percent !== null) {
        if (!groupMap[item.species]) {
          groupMap[item.species] = 0
        }
        groupMap[item.species] += parseFloat(item.percent as any) || 0
      }
    })

    speciesGroups.value = Object.keys(groupMap).map((key) => ({
      group: key,
      percent: groupMap[key],
      siteSpecies: key,
      minimumDBHLimit: '4.0 cm+', // TODO -dealing with defualt value
    }))

    speciesGroups.value.sort((a, b) => b.percent - a.percent)

    // update highestPercentSpecies and selectedSiteSpecies
    highestPercentSpecies.value = selectedSiteSpecies.value =
      speciesGroups.value.length > 0 ? speciesGroups.value[0].siteSpecies : null
  }

  // site info
  const becZone = ref(null)
  const ecoZone = ref(null)
  const incSecondaryHeight = ref(false)
  const siteIndexCurve = ref<string | null>(null)
  const siteSpeciesValues = ref<string | null>(null)
  const ageType = ref(null)
  const age = ref<number | null>(null)
  const height = ref<number | null>(null)
  const bha50SiteIndex = ref<number | null>(null)
  const floating = ref<string | null>(null)

  // stand density
  const percentStockableArea = ref<number | null>(0)
  const basalArea = ref<number | null>(null)
  const treesPerHectare = ref<number | null>(null)
  const minimumDBHLimit = ref(null)
  const percentCrownClosure = ref<number | null>(null)

  // additional stand attributes
  const computedValues = ref<string | null>(null)
  const loreyHeight = ref<number | null>(null)
  const basalArea125cm = ref<number | null>(null)
  const closeUtilVolume = ref<number | null>(null)
  const closeUtilNetDecayWasteVolume = ref<number | null>(null)
  const wholeStemVolume75cm = ref<number | null>(null)
  const wholeStemVolume125cm = ref<number | null>(null)
  const closeUtilNetDecayVolume = ref<number | null>(null)

  // report info
  const startingAge = ref(null)
  const finishingAge = ref(null)
  const ageIncrement = ref(null)
  const selectedVolumeReported = ref([])
  const projectionType = ref(null)
  const reportTitle = ref(null)

  return {
    // species info
    derivedBy,
    speciesList,
    speciesGroups,
    highestPercentSpecies,
    selectedSiteSpecies,
    totalSpeciesPercent,
    totalSpeciesGroupPercent,
    isOverTotalPercent,
    updateSpeciesGroup,
    // site info
    becZone,
    ecoZone,
    incSecondaryHeight,
    siteIndexCurve,
    siteSpeciesValues,
    ageType,
    age,
    height,
    bha50SiteIndex,
    floating,
    // stand density
    percentStockableArea,
    basalArea,
    treesPerHectare,
    minimumDBHLimit,
    percentCrownClosure,
    // additional stand attributes
    computedValues,
    loreyHeight,
    basalArea125cm,
    closeUtilVolume,
    closeUtilNetDecayWasteVolume,
    wholeStemVolume75cm,
    wholeStemVolume125cm,
    closeUtilNetDecayVolume,
    // report info
    startingAge,
    finishingAge,
    ageIncrement,
    selectedVolumeReported,
    projectionType,
    reportTitle,
  }
})
