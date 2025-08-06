<template>
  <div class="ml-4 mt-5">
    <div class="ml-n4 mt-n5">
      <span class="text-h7">Report Title</span>
    </div>
    <v-row>
      <v-col cols="7">
        <v-text-field
          id="reportTitle"
          type="string"
          v-model="localReportTitle"
          hide-details="auto"
          persistent-placeholder
          placeholder="Enter a report title..."
          density="compact"
          dense
          style="max-width: 95% !important"
          :disabled="isDisabled"
        ></v-text-field>
      </v-col>
      <v-col class="col-space-2" />
      <v-col cols="2">
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
          :disabled="isDisabled"
        ></v-select>
      </v-col>
    </v-row>
  </div>
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
              :disabled="isForwardGrowDeactivated"
              data-testid="is-forward-grow-enabled"
            ></v-checkbox>
          </v-col>
          <v-col cols="auto">
            <v-checkbox
              v-model="localIsBackwardGrowEnabled"
              label="Backward"
              hide-details
              :disabled="isBackwardGrowDeactivated"
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
          <v-col style="max-width: 20%">
            <v-checkbox
              v-model="localIsWholeStemEnabled"
              label="Whole Stem"
              hide-details
              :disabled="isWholeStemDeactivated"
              data-testid="is-whole-stem-enabled"
            ></v-checkbox>
          </v-col>
          <v-col style="max-width: 20%">
            <v-checkbox
              v-model="localIsCloseUtilEnabled"
              label="Close Utilization"
              hide-details
              :disabled="isCloseUtilDeactivated"
              data-testid="is-close-util-enabled"
            ></v-checkbox>
          </v-col>
          <v-col style="max-width: 20%">
            <v-checkbox
              v-model="localIsNetDecayEnabled"
              label="Net Decay"
              hide-details
              :disabled="isNetDecayDeactivated"
              data-testid="is-net-decay-enabled"
            ></v-checkbox>
          </v-col>
          <v-col style="max-width: 20%">
            <v-checkbox
              v-model="localIsNetDecayWasteEnabled"
              label="Net Decay and Waste"
              hide-details
              :disabled="isNetDecayWasteDeactivated"
              data-testid="is-net-decay-waste-enabled"
            ></v-checkbox>
          </v-col>
          <v-col style="max-width: 20%">
            <v-checkbox
              v-model="localIsNetDecayWasteBreakageEnabled"
              label="Net Decay, Waste and Breakage"
              hide-details
              :disabled="isNetDecayWasteBreakageDeactivated"
              data-testid="is-net-decay-waste-breakage-enabled"
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
        <template
          v-if="
            appStore.modelSelection ===
            CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
          "
        >
          <v-row>
            <v-col style="max-width: 20%">
              <v-checkbox
                v-model="localIsComputedMAIEnabled"
                label="Computed MAI"
                hide-details
                :disabled="isComputedMAIDeactivated"
                data-testid="is-computed-mai-enabled"
              ></v-checkbox>
            </v-col>
            <v-col style="max-width: 20%">
              <v-checkbox
                v-model="localIsCulminationValuesEnabled"
                label="Culmination Values"
                hide-details
                :disabled="isCulminationValuesDeactivated"
                data-testid="is-colmination-values-enabled"
              ></v-checkbox>
            </v-col>
            <v-col style="max-width: 20%">
              <v-checkbox
                v-model="localIsBySpeciesEnabled"
                label="By Species"
                hide-details
                :disabled="isBySpeciesDeactivated"
                data-testid="is-by-species-enabled"
              ></v-checkbox>
            </v-col>
          </v-row>
        </template>
        <template v-else>
          <v-row>
            <v-col style="max-width: 20%">
              <v-checkbox
                v-model="localIsByLayerEnabled"
                label="By Layer"
                hide-details
                :disabled="isByLayerDeactivated"
                data-testid="is-by-layer-enabled"
              ></v-checkbox>
            </v-col>
            <v-col style="max-width: 20%">
              <v-checkbox
                v-model="localIsProjectionModeEnabled"
                label="Projection Mode"
                hide-details
                :disabled="isProjectionModeDeactivated"
                data-testid="is-projection-mode-enabled"
              ></v-checkbox>
            </v-col>
            <v-col style="max-width: 20%">
              <v-checkbox
                v-model="localIsPolygonIDEnabled"
                label="Polygon ID"
                hide-details
                :disabled="isPolygonIDDeactivated"
                data-testid="is-polygon-id-enabled"
              ></v-checkbox>
            </v-col>
            <v-col style="max-width: 20%">
              <v-checkbox
                v-model="localIsCurrentYearEnabled"
                label="Current Year"
                hide-details
                :disabled="isCurrentYearDeactivated"
                data-testid="is-current-year-enabled"
              ></v-checkbox>
            </v-col>
            <v-col style="max-width: 20%">
              <v-checkbox
                v-model="localIsReferenceYearEnabled"
                label="Reference Year"
                hide-details
                :disabled="isReferenceYearDeactivated"
                data-testid="is-reference-year-enabled"
              ></v-checkbox>
            </v-col>
          </v-row>
          <v-row class="mt-n7">
            <v-col style="max-width: 20%">
              <v-checkbox
                v-model="localIncSecondaryHeight"
                label="Secondary Species Height"
                hide-details
                :disabled="isincSecondaryHeightDeactivated"
                data-testid="inc-secondary-height"
              ></v-checkbox>
            </v-col>
          </v-row>
          <v-row class="mt-n5">
            <v-col cols="2">
              <v-text-field
                id="specificYear"
                label="Specific Year"
                type="number"
                v-model.number="localSpecificYear"
                :min="CONSTANTS.NUM_INPUT_LIMITS.SPECIFIC_YEAR_MIN"
                :max="CONSTANTS.NUM_INPUT_LIMITS.SPECIFIC_YEAR_MAX"
                :step="CONSTANTS.NUM_INPUT_LIMITS.SPECIFIC_YEAR_STEP"
                persistent-placeholder
                placeholder=""
                density="compact"
                dense
                :disabled="isSpecificYearDeactivated"
                @update:model-value="handleSpecificYearInput"
              ></v-text-field>
            </v-col>
          </v-row>
        </template>
      </v-col>
    </v-row>
  </div>

  <div class="ml-4 mt-10" v-if="isModelParametersMode">
    <div class="ml-n4 mt-n5">
      <span class="text-h7">Minimum DBH Limit by Species Group</span>
    </div>
    <v-container fluid class="ml-n6 mt-5">
      <v-row v-for="(group, index) in speciesGroups" :key="index">
        <v-col style="max-width: 5%; padding-top: 0px; padding-left: 20px">
          {{ `${group.group}` }}
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
import { CONSTANTS, DEFAULTS, OPTIONS } from '@/constants'
import { parseNumberOrNull } from '@/utils/util'
import { useAppStore } from '@/stores/appStore'
import { useModelParameterStore } from '@/stores/modelParameterStore'

const appStore = useAppStore()
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
  isWholeStemEnabled: boolean
  isCloseUtilEnabled: boolean
  isNetDecayEnabled: boolean
  isNetDecayWasteEnabled: boolean
  isNetDecayWasteBreakageEnabled: boolean
  isComputedMAIEnabled: boolean
  isCulminationValuesEnabled: boolean
  isBySpeciesEnabled: boolean
  isByLayerEnabled: boolean
  isProjectionModeEnabled: boolean
  isPolygonIDEnabled: boolean
  isCurrentYearEnabled: boolean
  isReferenceYearEnabled: boolean
  incSecondaryHeight: boolean
  specificYear: number | null
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
  'update:isWholeStemEnabled',
  'update:isCloseUtilEnabled',
  'update:isNetDecayEnabled',
  'update:isNetDecayWasteEnabled',
  'update:isNetDecayWasteBreakageEnabled',
  'update:isComputedMAIEnabled',
  'update:isCulminationValuesEnabled',
  'update:isBySpeciesEnabled',
  'update:isByLayerEnabled',
  'update:isProjectionModeEnabled',
  'update:isPolygonIDEnabled',
  'update:isCurrentYearEnabled',
  'update:isReferenceYearEnabled',
  'update:incSecondaryHeight',
  'update:specificYear',
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
const localIsWholeStemEnabled = ref<boolean>(props.isWholeStemEnabled)
const localIsCloseUtilEnabled = ref<boolean>(props.isCloseUtilEnabled)
const localIsNetDecayEnabled = ref<boolean>(props.isNetDecayEnabled)
const localIsNetDecayWasteEnabled = ref<boolean>(props.isNetDecayWasteEnabled)
const localIsNetDecayWasteBreakageEnabled = ref<boolean>(
  props.isNetDecayWasteBreakageEnabled,
)
const localIsComputedMAIEnabled = ref<boolean>(props.isComputedMAIEnabled)
const localIsCulminationValuesEnabled = ref<boolean>(
  props.isCulminationValuesEnabled,
)
const localIsBySpeciesEnabled = ref<boolean>(props.isBySpeciesEnabled)
const localIsByLayerEnabled = ref<boolean>(props.isByLayerEnabled)
const localIsProjectionModeEnabled = ref<boolean>(props.isProjectionModeEnabled)
const localIsPolygonIDEnabled = ref<boolean>(props.isPolygonIDEnabled)
const localIsCurrentYearEnabled = ref<boolean>(props.isCurrentYearEnabled)
const localIsReferenceYearEnabled = ref<boolean>(props.isReferenceYearEnabled)
const localIncSecondaryHeight = ref<boolean>(props.incSecondaryHeight)
const localSpecificYear = ref<number | null>(props.specificYear)
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
  () => props.isWholeStemEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIsWholeStemEnabled.value)
    ) {
      localIsWholeStemEnabled.value = newVal
    }
  },
)
watch(
  () => props.isCloseUtilEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIsCloseUtilEnabled.value)
    ) {
      localIsCloseUtilEnabled.value = newVal
    }
  },
)
watch(
  () => props.isNetDecayEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIsNetDecayEnabled.value)
    ) {
      localIsNetDecayEnabled.value = newVal
    }
  },
)
watch(
  () => props.isNetDecayWasteEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !==
      JSON.stringify(localIsNetDecayWasteEnabled.value)
    ) {
      localIsNetDecayWasteEnabled.value = newVal
    }
  },
)
watch(
  () => props.isNetDecayWasteBreakageEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !==
      JSON.stringify(localIsNetDecayWasteBreakageEnabled.value)
    ) {
      localIsNetDecayWasteBreakageEnabled.value = newVal
    }
  },
)
watch(
  () => props.isComputedMAIEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIsComputedMAIEnabled.value)
    ) {
      localIsComputedMAIEnabled.value = newVal
    }
  },
)
watch(
  () => props.isCulminationValuesEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !==
      JSON.stringify(localIsCulminationValuesEnabled.value)
    ) {
      localIsCulminationValuesEnabled.value = newVal
    }
  },
)
watch(
  () => props.isBySpeciesEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIsBySpeciesEnabled.value)
    ) {
      localIsBySpeciesEnabled.value = newVal
    }
  },
)
watch(
  () => props.isByLayerEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIsByLayerEnabled.value)
    ) {
      localIsByLayerEnabled.value = newVal
    }
  },
)
watch(
  () => props.isProjectionModeEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !==
      JSON.stringify(localIsProjectionModeEnabled.value)
    ) {
      localIsProjectionModeEnabled.value = newVal
    }
  },
)
watch(
  () => props.isPolygonIDEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIsPolygonIDEnabled.value)
    ) {
      localIsPolygonIDEnabled.value = newVal
    }
  },
)
watch(
  () => props.isCurrentYearEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIsCurrentYearEnabled.value)
    ) {
      localIsCurrentYearEnabled.value = newVal
    }
  },
)
watch(
  () => props.isReferenceYearEnabled,
  (newVal) => {
    if (
      JSON.stringify(newVal) !==
      JSON.stringify(localIsReferenceYearEnabled.value)
    ) {
      localIsReferenceYearEnabled.value = newVal
    }
  },
)
watch(
  () => props.incSecondaryHeight,
  (newVal) => {
    if (
      JSON.stringify(newVal) !== JSON.stringify(localIncSecondaryHeight.value)
    ) {
      localIncSecondaryHeight.value = newVal
    }
  },
)
watch(
  () => props.specificYear,
  (newVal) => {
    localSpecificYear.value = newVal
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
const canCulminationValuesSelectionActivated = computed(() => {
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
  if (!canCulminationValuesSelectionActivated.value) {
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
watch(localIsWholeStemEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isWholeStemEnabled)) {
    emit('update:isWholeStemEnabled', newVal)
  }
})
watch(localIsCloseUtilEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isCloseUtilEnabled)) {
    emit('update:isCloseUtilEnabled', newVal)
  }
})
watch(localIsNetDecayEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isNetDecayEnabled)) {
    emit('update:isNetDecayEnabled', newVal)
  }
})
watch(localIsNetDecayWasteEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isNetDecayWasteEnabled)) {
    emit('update:isNetDecayWasteEnabled', newVal)
  }
})
watch(localIsNetDecayWasteBreakageEnabled, (newVal) => {
  if (
    JSON.stringify(newVal) !==
    JSON.stringify(props.isNetDecayWasteBreakageEnabled)
  ) {
    emit('update:isNetDecayWasteBreakageEnabled', newVal)
  }
})
watch(localIsComputedMAIEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isComputedMAIEnabled)) {
    emit('update:isComputedMAIEnabled', newVal)
  }
})
watch(localIsCulminationValuesEnabled, (newVal) => {
  if (
    JSON.stringify(newVal) !== JSON.stringify(props.isCulminationValuesEnabled)
  ) {
    emit('update:isCulminationValuesEnabled', newVal)
  }
})
watch(localIsBySpeciesEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isBySpeciesEnabled)) {
    emit('update:isBySpeciesEnabled', newVal)
  }
})
watch(localIsByLayerEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isByLayerEnabled)) {
    emit('update:isByLayerEnabled', newVal)
  }
})
watch(localIsProjectionModeEnabled, (newVal) => {
  if (
    JSON.stringify(newVal) !== JSON.stringify(props.isProjectionModeEnabled)
  ) {
    emit('update:isProjectionModeEnabled', newVal)
  }
})
watch(localIsPolygonIDEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isPolygonIDEnabled)) {
    emit('update:isPolygonIDEnabled', newVal)
  }
})
watch(localIsCurrentYearEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isCurrentYearEnabled)) {
    emit('update:isCurrentYearEnabled', newVal)
  }
})
watch(localIsReferenceYearEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isReferenceYearEnabled)) {
    emit('update:isReferenceYearEnabled', newVal)
  }
})
watch(localIncSecondaryHeight, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.incSecondaryHeight)) {
    emit('update:incSecondaryHeight', newVal)
  }
})
watch(localSpecificYear, (newVal) => emit('update:specificYear', newVal))
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
const isForwardGrowDeactivated = computed(() => {
  return props.isDisabled
})
const isBackwardGrowDeactivated = computed(() => {
  return props.isDisabled
})
const isWholeStemDeactivated = computed(() => {
  return props.isDisabled
})
const isCloseUtilDeactivated = computed(() => {
  return props.isDisabled
})
const isNetDecayDeactivated = computed(() => {
  return props.isDisabled
})
const isNetDecayWasteDeactivated = computed(() => {
  return props.isDisabled
})
const isNetDecayWasteBreakageDeactivated = computed(() => {
  return props.isDisabled
})
const isComputedMAIDeactivated = computed(() => {
  return props.isDisabled
})
const isCulminationValuesDeactivated = computed(() => {
  return props.isDisabled
})
const isBySpeciesDeactivated = computed(() => {
  return props.isDisabled
})
const isByLayerDeactivated = computed(() => {
  return props.isDisabled
})
const isProjectionModeDeactivated = computed(() => {
  return props.isDisabled
})
const isPolygonIDDeactivated = computed(() => {
  return props.isDisabled
})
const isCurrentYearDeactivated = computed(() => {
  return props.isDisabled
})
const isReferenceYearDeactivated = computed(() => {
  return props.isDisabled
})
const isincSecondaryHeightDeactivated = computed(() => {
  return props.isDisabled
})
const isSpecificYearDeactivated = computed(() => {
  return props.isDisabled
})

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

const handleSpecificYearInput = (value: string) => {
  localSpecificYear.value = parseNumberOrNull(value)
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
