<template>
  <div class="ml-4 mt-5 mb-5">
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
          style="max-width: 97% !important"
          :disabled="isDisabled"
        ></v-text-field>
      </v-col>
      <v-col class="col-space-2" />
      <v-col cols="3">
        <label class="bcds-select-label" for="projection-type-select">Projection Type</label>
        <v-select
          id="projection-type-select"
          :items="OPTIONS.projectionTypeOptions"
          v-model="localProjectionType"
          item-title="label"
          item-value="value"
          hide-details="auto"
          persistent-placeholder
          placeholder="Select..."
          :disabled="isDisabled"
          append-inner-icon="mdi-chevron-down"
        ></v-select>
      </v-col>
    </v-row>
  </div>
  <div
    class="ml-n2"
    v-if="appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD"
  >
    <v-row>
      <v-col cols="12" class="mb-n8">
        <v-radio-group
          v-model="selectedAgeYearRange"
          inline
          hide-details
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
  <div class="ml-4">
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
        <v-col cols="2" class="ml-2">
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
        <v-col cols="2" class="ml-2">
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
        <v-col cols="2" class="ml-2">
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
        <v-col cols="2" class="ml-2">
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
      <v-col class="col-space-3" />
      <v-col cols="2">
        <v-checkbox
          v-model="localIsForwardGrowEnabled"
          label="Forward"
          hide-details
          :disabled="isForwardGrowDeactivated"
          data-testid="is-forward-grow-enabled"
        ></v-checkbox>
      </v-col>
      <v-col class="col-space-3" />
      <v-col cols="2" class="ml-2">
        <v-checkbox
          v-model="localIsBackwardGrowEnabled"
          label="Backward"
          hide-details
          :disabled="isBackwardGrowDeactivated"
          data-testid="is-backward-grow-enabled"
        ></v-checkbox>
      </v-col>
    </v-row>
  </div>
  <div class="ml-4 mt-5">
    <div class="ml-n4 mt-n5">
      <span class="text-h7">Include in Report</span>
    </div>
    <v-row class="ml-n6">
      <v-col cols="12" style="padding-top: 1px">
        <template
          v-if="
            appStore.modelSelection ===
            CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
          "
        >
          <v-row>
            <v-col cols="2">
              <v-checkbox
                v-model="localIsComputedMAIEnabled"
                label="Computed MAI"
                hide-details
                :disabled="isComputedMAIDeactivated"
                data-testid="is-computed-mai-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="ml-2">
              <v-checkbox
                v-model="localIsCulminationValuesEnabled"
                label="Culmination Values"
                hide-details
                :disabled="isCulminationValuesDeactivated"
                data-testid="is-culmination-values-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="ml-1">
              <v-checkbox
                v-model="localIsBySpeciesEnabled"
                label="By Species"
                hide-details
                :disabled="isBySpeciesDeactivated"
                data-testid="is-by-species-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="ml-2">
              <v-checkbox
                v-model="localIncSecondaryHeight"
                label="Secondary Species Height"
                hide-details
                :disabled="isincSecondaryHeightDeactivated"
                data-testid="inc-secondary-height"
              ></v-checkbox>
            </v-col>
          </v-row>
        </template>
        <template v-else>
          <v-row>
            <v-col cols="2">
              <v-checkbox
                v-model="localIsByLayerEnabled"
                label="By Layer"
                hide-details
                :disabled="isByLayerDeactivated"
                data-testid="is-by-layer-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="ml-2">
              <v-checkbox
                v-model="localIsBySpeciesEnabled"
                label="By Species"
                hide-details
                :disabled="isBySpeciesDeactivated"
                data-testid="is-by-species-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="ml-1">
              <v-checkbox
                v-model="localIsProjectionModeEnabled"
                label="Projection Mode"
                hide-details
                :disabled="isProjectionModeDeactivated"
                data-testid="is-projection-mode-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="ml-2">
              <v-checkbox
                v-model="localIsPolygonIDEnabled"
                label="Polygon ID"
                hide-details
                :disabled="isPolygonIDDeactivated"
                data-testid="is-polygon-id-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="ml-1">
              <v-checkbox
                v-model="localIsCurrentYearEnabled"
                label="Current Year"
                hide-details
                :disabled="isCurrentYearDeactivated"
                data-testid="is-current-year-enabled"
              ></v-checkbox>
            </v-col>
          </v-row>
          <v-row class="mt-n7">
            <v-col cols="2">
              <v-checkbox
                v-model="localIsReferenceYearEnabled"
                label="Reference Year"
                hide-details
                :disabled="isReferenceYearDeactivated"
                data-testid="is-reference-year-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="ml-4">
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
            <v-col class="col-space-3" />
            <v-col cols="2" class="ml-2" style="padding-left: 0px;">
              <v-checkbox
                v-model="localIncSecondaryHeight"
                label="Secondary Species Height"
                hide-details
                :disabled="isincSecondaryHeightDeactivated"
                data-testid="inc-secondary-height"
              ></v-checkbox>
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
            :disabled="isMinDBHDeactivated"
            @update:model-value="updateMinDBH(index, $event)"
          ></v-slider>
        </v-col>
      </v-row>
    </v-container>
  </div>
  <div
    class="ml-4 mt-10"
    v-else-if="
      !isModelParametersMode &&
      appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD
    "
  >
    <div class="ml-n4 mt-n5">
      <span class="text-h7">Minimum DBH Limit by Species Group</span>
    </div>
    <v-container fluid class="ml-n6 mt-5">
      <v-row v-for="(group, index) in fileUploadSpeciesGroups" :key="index">
        <v-col style="max-width: 5%; padding-top: 0px; padding-left: 20px">
          {{ `${group.group}` }}
        </v-col>
        <v-col cols="8">
          <v-slider
            v-model="fileUploadUtilizationSliderValues[index]"
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
            :disabled="isMinDBHDeactivated"
            @update:model-value="updateFileUploadMinDBH(index, $event)"
          ></v-slider>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS, OPTIONS } from '@/constants'
import { parseNumberOrNull } from '@/utils/util'
import { useAppStore } from '@/stores/appStore'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useFileUploadStore } from '@/stores/fileUploadStore'

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const fileUploadStore = useFileUploadStore()

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
  'update:projectionType',
  'update:reportTitle',
])

const utilizationSliderValues = ref<number[]>([]) // in Model Parameter mode
const fileUploadUtilizationSliderValues = ref<number[]>([])

const utilizationClassOptions = OPTIONS.utilizationClassOptions

const speciesGroups = computed(() => modelParameterStore.speciesGroups)
const fileUploadSpeciesGroups = computed(
  () => fileUploadStore.fileUploadSpeciesGroup,
)

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

// Watch local state for changes (Local State -> Parent Emit)
watch(selectedAgeYearRange, (newVal) =>
  emit('update:selectedAgeYearRange', newVal),
)
watch(localStartingAge, (newVal) => emit('update:startingAge', newVal))
watch(localFinishingAge, (newVal) => emit('update:finishingAge', newVal))
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
watch(localProjectionType, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.projectionType)) {
    emit('update:projectionType', newVal)
  }
})
watch(localReportTitle, (newVal) => emit('update:reportTitle', newVal))

const isCulminationValuesEligible = (
  _startingAge: number | null,
  _finishingAge: number | null,
) => {
  return (
    _startingAge !== null &&
    _startingAge <= 10 &&
    _finishingAge !== null &&
    _finishingAge >= 300
  )
}

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

// Watch fileUploadSpeciesGroups for changes and sync utilization sliderValues (with immediate: true for initial load)
watch(
  fileUploadSpeciesGroups,
  (newGroups) => {
    fileUploadUtilizationSliderValues.value = newGroups.map((group) =>
      utilizationClassOptions.findIndex(
        (opt) => opt.value === group.minimumDBHLimit,
      ),
    )
  },
  { immediate: true, deep: true },
)

// Watch fileUploadStore projectionType to update species groups and slider values when projection type changes
watch(
  () => fileUploadStore.projectionType,
  (newType) => {
    // Update species groups based on projection type
    fileUploadStore.updateSpeciesGroupsForProjectionType(newType)
    // Update slider values when projection type changes
    fileUploadUtilizationSliderValues.value = fileUploadSpeciesGroups.value.map(
      (group) =>
        utilizationClassOptions.findIndex(
          (opt) => opt.value === group.minimumDBHLimit,
        ),
    )
  },
)

// Watch for projectionType to manage objects in the 'Volumes Reported' and 'Mimimum DBH Limit by Species Group' states
watch(localProjectionType, (newVal) => {
  if (newVal === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS) {
    // Update minimum DBH limits for CFS Biomass in Model Parameter mode
    speciesGroups.value.forEach((group) => {
      if (BIZCONSTANTS.CFS_BIOMASS_SPECIES_GROUP_UTILIZATION_MAP[group.group]) {
        group.minimumDBHLimit =
          BIZCONSTANTS.CFS_BIOMASS_SPECIES_GROUP_UTILIZATION_MAP[group.group]
      }
    })
    // Sync slider values with updated minimum DBH limits
    utilizationSliderValues.value = speciesGroups.value.map((group) =>
      utilizationClassOptions.findIndex(
        (opt) => opt.value === group.minimumDBHLimit,
      ),
    )
  }

  // Update File Upload store projection type (this will trigger automatic species group updates)
  if (fileUploadStore.projectionType !== newVal) {
    fileUploadStore.projectionType = newVal
  }
})
// Watch startingAge and finishingAge to manage CulminationValues state
watch(
  [localStartingAge, localFinishingAge],
  ([newStartingAge, newFinishingAge]) => {
    if (!isCulminationValuesEligible(newStartingAge, newFinishingAge)) {
      localIsCulminationValuesEnabled.value = false
    }
  },
)
// Watch derivedBy to manage Secondary Species Height state
watch(
  () => modelParameterStore.derivedBy,
  (newDerivedBy) => {
    if (newDerivedBy === CONSTANTS.DERIVED_BY.VOLUME) {
      localIncSecondaryHeight.value = false
    }
  },
  { immediate: true },
)

const isCFOBiomassSelected = computed(() => {
  return localProjectionType.value === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
})

const isForwardGrowDeactivated = computed(() => {
  return props.isDisabled
})
const isBackwardGrowDeactivated = computed(() => {
  return props.isDisabled
})
const isComputedMAIDeactivated = computed(() => {
  return props.isDisabled || isCFOBiomassSelected.value
})

const isCulminationValuesDeactivated = computed(() => {
  return (
    props.isDisabled ||
    !isCulminationValuesEligible(
      localStartingAge.value,
      localFinishingAge.value,
    )
  )
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
  return (
    props.isDisabled ||
    (appStore.modelSelection === CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS && modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.VOLUME)
  )
})
const isSpecificYearDeactivated = computed(() => {
  return props.isDisabled
})
const isMinDBHDeactivated = computed(() => {
  return props.isDisabled || isCFOBiomassSelected.value
})

const utilizationSliderTickLabels = utilizationClassOptions.reduce(
  (acc, opt) => {
    acc[opt.index] = opt.label
    return acc
  },
  {} as Record<number, string>,
)

const handleStartingAgeInput = (value: string) => {
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

// Update minimum DBH limit in file upload store based on slider value
const updateFileUploadMinDBH = (index: number, value: number) => {
  if (fileUploadSpeciesGroups.value[index]) {
    const enumValue = utilizationClassOptions[value]?.value
    if (enumValue !== undefined) {
      fileUploadSpeciesGroups.value[index].minimumDBHLimit = enumValue
    }
  }
}
</script>
<style scoped />
