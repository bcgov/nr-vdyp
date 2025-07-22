<template>
  <div>
    <v-row>
      <v-col cols="12">
        <v-radio-group
          v-model="selectedAgeYearRange"
          inline
          :disabled="isDisabled"
        >
          <v-radio
            v-for="option in OPTIONS.ageYearRangeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          ></v-radio>
        </v-radio-group>
      </v-col>
    </v-row>
  </div>
  <div>
    <v-row>
      <template v-if="selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE">
        <v-col cols="2">
          <v-text-field
            id="startingAge"
            label="Starting Age"
            type="number"
            v-model.number="localStartingAge"
            :min="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_STEP"
            persistent-placeholder
            placeholder=""
            density="compact"
            dense
            :disabled="isDisabled"
            @update:model-value="handleStartingAgeInput"
          ></v-text-field>
        </v-col>
        <v-col class="col-space-3" />
        <v-col cols="2">
          <v-text-field
            id="finishingAge"
            label="Finishing Age"
            type="number"
            v-model.number="localFinishingAge"
            :min="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_STEP"
            persistent-placeholder
            placeholder=""
            density="compact"
            dense
            :disabled="isDisabled"
            @update:model-value="handleFinishingAgeInput"
          ></v-text-field>
        </v-col>
        <v-col class="col-space-3" />
        <v-col cols="2">
          <v-text-field
            id="ageIncrement"
            label="Increment"
            type="number"
            v-model.number="localAgeIncrement"
            :min="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_STEP"
            persistent-placeholder
            placeholder=""
            density="compact"
            dense
            :disabled="isDisabled"
            @update:model-value="handleAgeIncrementInput"
          ></v-text-field>
        </v-col>
      </template>
      <template v-else>
        <v-col cols="2">
          <v-text-field
            id="startYear"
            label="Start Year"
            type="number"
            v-model.number="localStartYear"
            :min="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_STEP"
            persistent-placeholder
            placeholder=""
            density="compact"
            dense
            :disabled="isDisabled"
            @update:model-value="handleStartYearInput"
          ></v-text-field>
        </v-col>
        <v-col class="col-space-3" />
        <v-col cols="2">
          <v-text-field
            id="endYear"
            label="End Year"
            type="number"
            v-model.number="localEndYear"
            :min="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_STEP"
            persistent-placeholder
            placeholder=""
            density="compact"
            dense
            :disabled="isDisabled"
            @update:model-value="handleEndYearInput"
          ></v-text-field>
        </v-col>
        <v-col class="col-space-3" />
        <v-col cols="2">
          <v-text-field
            id="yearIncrement"
            label="Increment"
            type="number"
            v-model.number="localYearIncrement"
            :min="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_STEP"
            persistent-placeholder
            placeholder=""
            density="compact"
            dense
            :disabled="isDisabled"
            @update:model-value="handleYearIncrementInput"
          ></v-text-field>
        </v-col>
      </template>
      <v-col class="col-space-4" />
      <v-col cols="auto">
        <v-row>
          <v-col cols="auto">
            <v-checkbox
              v-model="localIsForwardGrowEnabled"
              label="Forward"
              hide-details
              :disabled="isForwardGrowDisabled"
              data-testid="is-forward-grow-enabled"
            ></v-checkbox>
          </v-col>
          <v-col cols="auto">
            <v-checkbox
              v-model="localIsBackwardGrowEnabled"
              label="Backward"
              hide-details
              :disabled="isBackwardGrowDisabled"
              data-testid="is-backward-grow-enabled"
            ></v-checkbox>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </div>
  <div class="ml-4 mt-5">
    <div class="ml-n4 mt-n5">
      <span class="text-h7">Volumes Reported</span>
    </div>
    <v-row class="ml-n6">
      <v-col cols="12" style="padding-top: 0px">
        <v-row>
          <v-col
            v-for="(option, index) in OPTIONS.volumeReportedOptions"
            :key="index"
            :style="{ 'max-width': index < 4 ? '20%' : 'auto' }"
          >
            <v-checkbox
              v-model="localVolumeReported"
              :label="option.label"
              :value="option.value"
              hide-details
              :disabled="isVolumeReportedDisabled"
              data-testid="volume-reported"
            ></v-checkbox>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </div>
  <div class="ml-4 mt-5">
    <div class="ml-n4 mt-n5">
      <span class="text-h7">Include in Report</span>
    </div>
    <v-row class="ml-n6">
      <v-col cols="12" style="padding-top: 0px">
        <v-row>
          <v-col
            v-for="(option, index) in OPTIONS.includeInReportOptions"
            :key="index"
            :style="{ 'max-width': index < 4 ? '20%' : 'auto' }"
          >
            <v-checkbox
              v-model="localIncludeInReport"
              :label="option.label"
              :value="option.value"
              hide-details
              :disabled="getIncludeInReportDisabled(option.value)"
            ></v-checkbox>
          </v-col>
          <v-col style="max-width: 20% !important">
            <v-select
              label="Projection Type"
              :items="OPTIONS.projectionTypeOptions"
              v-model="localProjectionType"
              item-title="label"
              item-value="value"
              hide-details="auto"
              persistent-placeholder
              placeholder="Select..."
              density="compact"
              dense
              style="max-width: 70% !important"
              :disabled="isDisabled"
            ></v-select>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </div>
  <div class="ml-4 mt-5">
    <div class="ml-n4 mt-n5">
      <span class="text-h7">Report Title</span>
    </div>
    <v-row>
      <v-col cols="6">
        <v-text-field
          id="reportTitle"
          type="string"
          v-model="localReportTitle"
          hide-details="auto"
          persistent-placeholder
          placeholder="Enter a report title..."
          density="compact"
          dense
          style="max-width: 50% !important"
          :disabled="isDisabled"
        ></v-text-field>
      </v-col>
    </v-row>
  </div>
  <div class="ml-4 mt-10" v-if="isModelParametersMode">
    <div class="ml-n4 mt-n5">
      <span class="text-h7">Minimum DBH Limit by Species Group</span>
    </div>
    <v-container fluid class="ml-n6 mt-5">
      <v-row v-for="(group, index) in speciesGroups" :key="index">
        <v-col cols="2" style="padding-top: 0px">
          {{ `${group.group} - ${getSpeciesFullName(group.group)}` }}
        </v-col>
        <v-col cols="8">
          <v-slider
            v-model="utilizationSliderValues[index]"
            :min="0"
            :max="4"
            :ticks="utilizationSliderTickLabels"
            show-ticks="always"
            step="1"
            tick-size="0"
            color="#e0e0e0"
            thumb-color="#767676"
            thumb-size="12"
            track-size="7"
            track-color="transparent"
            :disabled="isDisabled"
            @update:model-value="updateMinDBH(index, $event)"
          ></v-slider>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { CONSTANTS, BIZCONSTANTS, DEFAULTS, OPTIONS } from '@/constants'
import { parseNumberOrNull } from '@/utils/util'
import { useModelParameterStore } from '@/stores/modelParameterStore'

const modelParameterStore = useModelParameterStore()

const props = defineProps<{
  selectedAgeYearRange: string | null
  startingAge: number | null
  finishingAge: number | null
  ageIncrement: number | null
  startYear: number | null
  endYear: number | null
  yearIncrement: number | null
  isForwardGrowEnabled: boolean
  isBackwardGrowEnabled: boolean
  volumeReported: string[]
  includeInReport: string[]
  projectionType: string | null
  reportTitle: string | null
  isDisabled: boolean
  isModelParametersMode: boolean
}>()

const emit = defineEmits([
  'update:selectedAgeYearRange',
  'update:startingAge',
  'update:finishingAge',
  'update:ageIncrement',
  'update:startYear',
  'update:endYear',
  'update:yearIncrement',
  'update:isForwardGrowEnabled',
  'update:isBackwardGrowEnabled',
  'update:volumeReported',
  'update:includeInReport',
  'update:projectionType',
  'update:reportTitle',
])

const utilizationSliderValues = ref<number[]>([])

const utilizationClassOptions = OPTIONS.utilizationClassOptions

const speciesGroups = computed(() => modelParameterStore.speciesGroups)

const selectedAgeYearRange = ref<string>(
  props.selectedAgeYearRange || DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE,
)
const localStartingAge = ref<number | null>(props.startingAge)
const localFinishingAge = ref<number | null>(props.finishingAge)
const localAgeIncrement = ref<number | null>(props.ageIncrement)
const localStartYear = ref<number | null>(props.startYear)
const localEndYear = ref<number | null>(props.endYear)
const localYearIncrement = ref<number | null>(props.yearIncrement)
const localIsForwardGrowEnabled = ref<boolean>(props.isForwardGrowEnabled)
const localIsBackwardGrowEnabled = ref<boolean>(props.isBackwardGrowEnabled)
const localVolumeReported = ref<string[]>([...props.volumeReported])
const localIncludeInReport = ref<string[]>([...props.includeInReport])
const localProjectionType = ref<string | null>(props.projectionType)
const localReportTitle = ref<string | null>(props.reportTitle)

// Watch props for changes (Prop -> Local State)
watch(
  () => props.selectedAgeYearRange,
  (newVal) => {
    selectedAgeYearRange.value =
      newVal || DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
  },
)
watch(
  () => props.startingAge,
  (newVal) => {
    localStartingAge.value = newVal
  },
)
watch(
  () => props.finishingAge,
  (newVal) => {
    localFinishingAge.value = newVal
  },
)
watch(
  () => props.ageIncrement,
  (newVal) => {
    localAgeIncrement.value = newVal
  },
)
watch(
  () => props.startYear,
  (newVal) => {
    localStartYear.value = newVal
  },
)
watch(
  () => props.endYear,
  (newVal) => {
    localEndYear.value = newVal
  },
)
watch(
  () => props.yearIncrement,
  (newVal) => {
    localYearIncrement.value = newVal
  },
)
watch(
  () => props.isForwardGrowEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIsForwardGrowEnabled.value)
    ) {
      localIsForwardGrowEnabled.value = newVal
    }
  },
)
watch(
  () => props.isBackwardGrowEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !==
      JSON.stringify(localIsBackwardGrowEnabled.value)
    ) {
      localIsBackwardGrowEnabled.value = newVal
    }
  },
)
watch(
  () => props.volumeReported,
  (newVal) => {
    if (JSON.stringify(newVal) !== JSON.stringify(localVolumeReported.value)) {
      localVolumeReported.value = [...newVal]
    }
  },
)
watch(
  () => props.includeInReport,
  (newVal) => {
    if (JSON.stringify(newVal) !== JSON.stringify(localIncludeInReport.value)) {
      localIncludeInReport.value = [...newVal]
    }
  },
)
watch(
  () => props.projectionType,
  (newVal) => {
    localProjectionType.value = newVal
  },
)
watch(
  () => props.reportTitle,
  (newVal) => {
    localReportTitle.value = newVal
  },
)

// Calculating whether the "Culmination Values" checkbox is enabled
const isCulminationValuesEnabled = computed(() => {
  return (
    localStartingAge.value !== null &&
    localStartingAge.value <= 10 &&
    localFinishingAge.value !== null &&
    localFinishingAge.value >= 300
  )
})

// Watch local state for changes (Local State -> Parent Emit)
watch(selectedAgeYearRange, (newVal) =>
  emit('update:selectedAgeYearRange', newVal),
)
watch(localStartingAge, (newVal) => emit('update:startingAge', newVal))
watch(localFinishingAge, (newVal) => emit('update:finishingAge', newVal))

watch([localStartingAge, localFinishingAge], () => {
  if (!isCulminationValuesEnabled.value) {
    // Remove the selection if "Culmination Values" is disabled
    localIncludeInReport.value = localIncludeInReport.value.filter(
      (item) => item !== CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES,
    )
  }
})

watch(localAgeIncrement, (newVal) => emit('update:ageIncrement', newVal))
watch(localStartYear, (newVal) => emit('update:startYear', newVal))
watch(localEndYear, (newVal) => emit('update:endYear', newVal))
watch(localYearIncrement, (newVal) => emit('update:yearIncrement', newVal))
watch(localIsForwardGrowEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isForwardGrowEnabled)) {
    emit('update:isForwardGrowEnabled', newVal)
  }
})
watch(localIsBackwardGrowEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isBackwardGrowEnabled)) {
    emit('update:isBackwardGrowEnabled', newVal)
  }
})
watch(localVolumeReported, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.volumeReported)) {
    emit('update:volumeReported', [...newVal])
  }
})
watch(localIncludeInReport, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.includeInReport)) {
    emit('update:includeInReport', [...newVal])
  }
})

watch(localProjectionType, (newVal) => {
  if (newVal === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS) {
    // Reset the "Volumes Reported" checkbox and force select only 'Close Utilization' checkbox
    localVolumeReported.value = [CONSTANTS.VOLUME_REPORTED.CLOSE_UTIL]
    emit('update:volumeReported', [...localVolumeReported.value])
  }

  emit('update:projectionType', newVal)
})

watch(localReportTitle, (newVal) => emit('update:reportTitle', newVal))

// Watch speciesGroups for changes and sync utilization sliderValues (with immediate: true for initial load)
watch(
  speciesGroups,
  (newGroups) => {
    utilizationSliderValues.value = newGroups.map((group) =>
      utilizationClassOptions.findIndex(
        (opt) => opt.value === group.minimumDBHLimit,
      ),
    )
  },
  { immediate: true },
)

// Decide whether to disable the "Backward / Forward" checkbox
const isForwardGrowDisabled = computed(() => {
  return props.isDisabled
})
const isBackwardGrowDisabled = computed(() => {
  return props.isDisabled
})

// Decide whether to disable the "Volumes Reported" checkbox
const isVolumeReportedDisabled = computed(() => {
  return (
    localProjectionType.value === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS ||
    props.isDisabled
  )
})

// Decide to enable/disable the "Include in Report" checkbox
const getIncludeInReportDisabled = (value: string) => {
  if (value === CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES) {
    return !isCulminationValuesEnabled.value
  }
  return props.isDisabled
}

const getSpeciesFullName = (code: string) => {
  const validCodes = Object.keys(BIZCONSTANTS.SPECIES_MAP) as Array<
    keyof typeof BIZCONSTANTS.SPECIES_MAP
  >
  return validCodes.includes(code as keyof typeof BIZCONSTANTS.SPECIES_MAP)
    ? `${BIZCONSTANTS.SPECIES_MAP[code as keyof typeof BIZCONSTANTS.SPECIES_MAP]}`
    : ''
}

const utilizationSliderTickLabels = utilizationClassOptions.reduce(
  (acc, opt) => {
    acc[opt.index] = opt.label
    return acc
  },
  {} as Record<number, string>,
)

const handleStartingAgeInput = (value: string) => {
  // Convert an empty string to null
  localStartingAge.value = parseNumberOrNull(value)
}

const handleFinishingAgeInput = (value: string) => {
  localFinishingAge.value = parseNumberOrNull(value)
}

const handleAgeIncrementInput = (value: string) => {
  localAgeIncrement.value = parseNumberOrNull(value)
}

const handleStartYearInput = (value: string) => {
  localStartYear.value = parseNumberOrNull(value)
}

const handleEndYearInput = (value: string) => {
  localEndYear.value = parseNumberOrNull(value)
}

const handleYearIncrementInput = (value: string) => {
  localYearIncrement.value = parseNumberOrNull(value)
}

// Update minimum DBH limit in the store based on slider value
const updateMinDBH = (index: number, value: number) => {
  if (speciesGroups.value[index]) {
    const enumValue = utilizationClassOptions[value]?.value
    if (enumValue !== undefined) {
      speciesGroups.value[index].minimumDBHLimit = enumValue
    }
  }
}
</script>
<style scoped />
