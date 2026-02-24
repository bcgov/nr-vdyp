<template>
  <div class="mb-5">
    <v-row>
      <v-col cols="12" sm="7">
        <label class="bcds-text-field-label" for="reportTitle">Report Title (Required)</label>
        <v-text-field
          id="reportTitle"
          type="string"
          v-model="localReportTitle"
          hide-details="auto"
          persistent-placeholder
          placeholder="Enter a report title..."
          :disabled="isDisabled"
          class="report-title-text-field"
        ></v-text-field>
      </v-col>
      <v-col class="col-space-2 d-none d-sm-flex" />
      <v-col cols="12" sm="3" class="projection-type-container">
        <label class="bcds-radio-label" for="projection-type-select">Projection Type</label>
        <v-radio-group
          id="projection-type-select"
          v-model="localProjectionType"
          inline
          hide-details
          :disabled="isDisabled"
        >
          <v-radio
            :key="OPTIONS.projectionTypeOptions[0].value"
            :label="OPTIONS.projectionTypeOptions[0].label"
            :value="OPTIONS.projectionTypeOptions[0].value"
          ></v-radio>
          <v-radio
            :key="OPTIONS.projectionTypeOptions[1].value"
            :label="OPTIONS.projectionTypeOptions[1].label"
            :value="OPTIONS.projectionTypeOptions[1].value"
          ></v-radio>
        </v-radio-group>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <div class="bcds-textarea" :data-disabled="isDisabled || undefined">
          <label class="bcds-textarea-label" for="reportDescription">Description</label>
          <div class="bcds-textarea-container">
            <textarea
              id="reportDescription"
              class="bcds-textarea-input"
              v-model="localReportDescription"
              placeholder="Provide a description of this Projection..."
              :disabled="isDisabled"
              :maxlength="500"
              rows="3"
            ></textarea>
          </div>
          <div class="bcds-textarea-description">
            <span class="counter">{{ reportDescriptionLength }}/500</span>
          </div>
        </div>
      </v-col>
    </v-row>
  </div>
  <div
    class="ml-n2"
    v-if="appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD"
  >
    <v-row>
      <v-col cols="12" sm="auto" class="age-year-range-container">
        <div class="numeric-range-value-label">Numeric Range Value</div>
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
      <template v-if="selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE">
        <v-col cols="4" sm="2">
          <AppSpinField
            label="Starting Age"
            :model-value="localStartingAge"
            :min="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_DECIMAL_NUM"
            data-testid="starting-age"
            @update:modelValue="handleStartingAgeInput"
          />
        </v-col>
        <v-col class="col-space-3 d-none d-sm-flex" />
        <v-col cols="4" sm="2" class="ml-sm-2">
          <AppSpinField
            label="Finishing Age"
            :model-value="localFinishingAge"
            :min="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_DECIMAL_NUM"
            data-testid="finishing-age"
            @update:modelValue="handleFinishingAgeInput"
          />
        </v-col>
        <v-col class="col-space-3 d-none d-sm-flex" />
        <v-col cols="4" sm="2" class="ml-sm-2">
          <AppSpinField
            label="Increment"
            :model-value="localAgeIncrement"
            :min="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_DECIMAL_NUM"
            data-testid="age-increment"
            @update:modelValue="handleAgeIncrementInput"
          />
        </v-col>
      </template>
      <template v-else>
        <v-col cols="4" sm="2">
          <AppSpinField
            label="Starting Year"
            :model-value="localStartYear"
            :min="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_DECIMAL_NUM"
            data-testid="start-year"
            @update:modelValue="handleStartYearInput"
          />
        </v-col>
        <v-col class="col-space-3 d-none d-sm-flex" />
        <v-col cols="4" sm="2" class="ml-sm-2">
          <AppSpinField
            label="Finishing Year"
            :model-value="localEndYear"
            :min="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_DECIMAL_NUM"
            data-testid="end-year"
            @update:modelValue="handleEndYearInput"
          />
        </v-col>
        <v-col class="col-space-3 d-none d-sm-flex" />
        <v-col cols="4" sm="2" class="ml-sm-2">
          <AppSpinField
            label="Increment"
            :model-value="localYearIncrement"
            :min="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_DECIMAL_NUM"
            data-testid="year-increment"
            @update:modelValue="handleYearIncrementInput"
          />
        </v-col>
      </template>
      <v-col class="col-space-3 d-none d-sm-flex" />
      <v-col cols="4" sm="2">
        <AppSpinField
          label="Include Specific Year"
          :model-value="localSpecificYear"
          :min="CONSTANTS.NUM_INPUT_LIMITS.SPECIFIC_YEAR_MIN"
          :max="CONSTANTS.NUM_INPUT_LIMITS.SPECIFIC_YEAR_MAX"
          :step="CONSTANTS.NUM_INPUT_LIMITS.SPECIFIC_YEAR_STEP"
          :persistent-placeholder="true"
          placeholder=""
          :hideDetails="true"
          :disabled="isSpecificYearDeactivated"
          :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
          :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.SPECIFIC_YEAR_DECIMAL_NUM"
          data-testid="specific-year"
          @update:modelValue="handleSpecificYearInput"
        />
      </v-col>
    </v-row>
  </div>
  <div v-if="appStore.modelSelection !== CONSTANTS.MODEL_SELECTION.FILE_UPLOAD">
    <v-row>
      <template v-if="selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE">
        <v-col cols="2">
          <AppSpinField
            label="Starting Age"
            :model-value="localStartingAge"
            :min="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_DECIMAL_NUM"
            data-testid="starting-age"
            @update:modelValue="handleStartingAgeInput"
          />
        </v-col>
        <v-col class="col-space-3" />
        <v-col cols="2" class="ml-2">
          <AppSpinField
            label="Finishing Age"
            :model-value="localFinishingAge"
            :min="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_DECIMAL_NUM"
            data-testid="finishing-age"
            @update:modelValue="handleFinishingAgeInput"
          />
        </v-col>
        <v-col class="col-space-3" />
        <v-col cols="2" class="ml-2">
          <AppSpinField
            label="Increment"
            :model-value="localAgeIncrement"
            :min="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_DECIMAL_NUM"
            data-testid="age-increment"
            @update:modelValue="handleAgeIncrementInput"
          />
        </v-col>
      </template>
      <template v-else>
        <v-col cols="2">
          <AppSpinField
            label="Start Year"
            :model-value="localStartYear"
            :min="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_DECIMAL_NUM"
            data-testid="start-year"
            @update:modelValue="handleStartYearInput"
          />
        </v-col>
        <v-col class="col-space-3" />
        <v-col cols="2" class="ml-2">
          <AppSpinField
            label="End Year"
            :model-value="localEndYear"
            :min="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_DECIMAL_NUM"
            data-testid="end-year"
            @update:modelValue="handleEndYearInput"
          />
        </v-col>
        <v-col class="col-space-3" />
        <v-col cols="2" class="ml-2">
          <AppSpinField
            label="Increment"
            :model-value="localYearIncrement"
            :min="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MIN"
            :max="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_MAX"
            :step="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_STEP"
            :persistent-placeholder="true"
            placeholder=""
            :hideDetails="true"
            :disabled="isDisabled"
            :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
            :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_DECIMAL_NUM"
            data-testid="year-increment"
            @update:modelValue="handleYearIncrementInput"
          />
        </v-col>
      </template>
      <v-col class="col-space-3" />
      <v-col class="pl-8">
        <v-row class="pt-11">
          <v-checkbox
            v-model="localIsForwardGrowEnabled"
            label="Forward"
            hide-details
            :disabled="isForwardGrowDeactivated"
            data-testid="is-forward-grow-enabled"
          ></v-checkbox>
          <v-col class="col-space-3" />
          <v-checkbox
            v-model="localIsBackwardGrowEnabled"
            label="Backward"
            hide-details
            :disabled="isBackwardGrowDeactivated"
            data-testid="is-backward-grow-enabled"
          ></v-checkbox>
        </v-row>
      </v-col>
    </v-row>
  </div>
  <div class="ml-4 mt-7">
    <div class="ml-n4">
      <span class="include-in-report-label" :class="{ 'include-in-report-disabled': isDisabled }">{{ appStore.modelSelection === CONSTANTS.MODEL_SELECTION.FILE_UPLOAD ? 'Include the following values in the Report' : 'Include in Report' }}</span>
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
            <v-col cols="2" class="computed-mai-container">
              <v-checkbox
                v-model="localIsComputedMAIEnabled"
                :label=CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI
                hide-details
                :disabled="isComputedMAIDeactivated"
                data-testid="is-computed-mai-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="culmination-values-container">
              <v-checkbox
                v-model="localIsCulminationValuesEnabled"
                :label=CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES
                hide-details
                :disabled="isCulminationValuesDeactivated"
                data-testid="is-culmination-values-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="2" class="by-species-container">
              <v-checkbox
                v-model="localIsBySpeciesEnabled"
                :label=CONSTANTS.INCLUDE_IN_REPORT.BY_SPECIES
                hide-details
                :disabled="isBySpeciesDeactivated"
                data-testid="is-by-species-enabled"
              ></v-checkbox>
            </v-col>
            <v-col class="col-space-3" />
            <v-col cols="3" class="secondary-species-height-container">
              <v-checkbox
                v-model="localIncSecondaryHeight"
                :label=CONSTANTS.INCLUDE_IN_REPORT.SECD_SPCZ_HEIGHT
                hide-details
                :disabled="isincSecondaryHeightDeactivated"
                data-testid="inc-secondary-height"
              ></v-checkbox>
            </v-col>
          </v-row>
        </template>
        <template v-else>
          <v-row>
            <v-col cols="auto" class="by-layer-container">
              <v-checkbox
                v-model="localIsByLayerEnabled"
                :label=CONSTANTS.INCLUDE_IN_REPORT.BY_LAYER
                hide-details
                :disabled="isByLayerDeactivated"
                data-testid="is-by-layer-enabled"
              ></v-checkbox>
            </v-col>
            <v-col cols="auto" class="by-species-file-upload-container">
              <v-checkbox
                v-model="localIsBySpeciesEnabled"
                :label=CONSTANTS.INCLUDE_IN_REPORT.BY_SPECIES
                hide-details
                :disabled="isBySpeciesDeactivated"
                data-testid="is-by-species-enabled"
              ></v-checkbox>
            </v-col>
            <v-col cols="auto" class="secondary-species-height-file-upload-container" style="padding-left: 0px;">
              <v-checkbox
                v-model="localIncSecondaryHeight"
                :label=CONSTANTS.INCLUDE_IN_REPORT.SECD_SPCZ_HEIGHT
                hide-details
                :disabled="isincSecondaryHeightDeactivated"
                data-testid="inc-secondary-height"
              ></v-checkbox>
            </v-col>
            <v-col cols="auto" class="projection-mode-container">
              <v-checkbox
                v-model="localIsProjectionModeEnabled"
                :label=CONSTANTS.INCLUDE_IN_REPORT.PRJECTION_MODE
                hide-details
                :disabled="isProjectionModeDeactivated"
                data-testid="is-projection-mode-enabled"
              ></v-checkbox>
            </v-col>
            <v-col cols="auto" class="polygon-id-container">
              <v-checkbox
                v-model="localIsPolygonIDEnabled"
                :label=CONSTANTS.INCLUDE_IN_REPORT.POLYGON_ID
                hide-details
                :disabled="isPolygonIDDeactivated"
                data-testid="is-polygon-id-enabled"
              ></v-checkbox>
            </v-col>
            <v-col cols="auto" class="current-year-container">
              <v-checkbox
                v-model="localIsCurrentYearEnabled"
                :label=CONSTANTS.INCLUDE_IN_REPORT.CURRENT_YEAR
                hide-details
                :disabled="isCurrentYearDeactivated"
                data-testid="is-current-year-enabled"
              ></v-checkbox>
            </v-col>
            <v-col cols="auto" class="reference-year-container">
              <v-checkbox
                v-model="localIsReferenceYearEnabled"
                :label=CONSTANTS.INCLUDE_IN_REPORT.REFERENCE_YEAR
                hide-details
                :disabled="isReferenceYearDeactivated"
                data-testid="is-reference-year-enabled"
              ></v-checkbox>
            </v-col>
          </v-row>
        </template>
      </v-col>
    </v-row>
  </div>
  <div class="ml-4 mt-10" v-if="isModelParametersMode">
    <div class="ml-n4 mt-n5">
      <span class="min-dbh-limit-species-group-label" :class="{ 'min-dbh-disabled': isMinDBHDeactivated }">Minimum DBH Limit by Species Group</span>
    </div>
    <v-container fluid class="ml-n10 mt-5">
      <v-row v-for="(group, index) in speciesGroups" :key="index">
        <v-col class="min-dbh-limit-species-group-list-container" :class="{ 'min-dbh-disabled': isMinDBHDeactivated }">
          {{ `${group.group}` }}
        </v-col>
        <v-col cols="8" class="ml-n5">
          <v-slider
            v-model="utilizationSliderValues[index]"
            :min="0"
            :max="4"
            :ticks="utilizationSliderTickLabels"
            show-ticks="always"
            step="1"
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
</template>
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS, OPTIONS } from '@/constants'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { AppSpinField } from '@/components'

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const fileUploadStore = useFileUploadStore()

const props = defineProps<{
  selectedAgeYearRange: string | null
  startingAge: string | null
  finishingAge: string | null
  ageIncrement: string | null
  startYear: string | null
  endYear: string | null
  yearIncrement: string | null
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
  specificYear: string | null
  projectionType: string | null
  reportTitle: string | null
  reportDescription: string | null
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
  'update:reportDescription',
])

const utilizationSliderValues = ref<number[]>([]) // in Model Parameter mode

const utilizationClassOptions = OPTIONS.utilizationClassOptions

const speciesGroups = computed(() => modelParameterStore.speciesGroups)

const selectedAgeYearRange = ref<string>(
  props.selectedAgeYearRange || DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE,
)
const localStartingAge = ref<string | null>(props.startingAge)
const localFinishingAge = ref<string | null>(props.finishingAge)
const localAgeIncrement = ref<string | null>(props.ageIncrement)
const localStartYear = ref<string | null>(props.startYear)
const localEndYear = ref<string | null>(props.endYear)
const localYearIncrement = ref<string | null>(props.yearIncrement)
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
const localSpecificYear = ref<string | null>(props.specificYear)
const localProjectionType = ref<string | null>(props.projectionType)
const localReportTitle = ref<string | null>(props.reportTitle)
const localReportDescription = ref<string | null>(props.reportDescription)

const reportDescriptionLength = computed(() => {
  return localReportDescription.value ? localReportDescription.value.length : 0
})

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
watch(
  () => props.reportDescription,
  (newVal) => {
    localReportDescription.value = newVal
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
watch(localReportDescription, (newVal) => emit('update:reportDescription', newVal))

const isCulminationValuesEligible = (
  _startingAge: string | null,
  _finishingAge: string | null,
) => {
  if (_startingAge === null || _finishingAge === null) {
    return false
  }
  const startAge = Number.parseFloat(_startingAge)
  const finishAge = Number.parseFloat(_finishingAge)
  return startAge <= 10 && finishAge >= 300
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
  { immediate: true, deep: true, flush: 'sync' },
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

const handleStartingAgeInput = (value: string | null) => {
  localStartingAge.value = value
}

const handleFinishingAgeInput = (value: string | null) => {
  localFinishingAge.value = value
}

const handleAgeIncrementInput = (value: string | null) => {
  localAgeIncrement.value = value
}

const handleStartYearInput = (value: string | null) => {
  localStartYear.value = value
}

const handleEndYearInput = (value: string | null) => {
  localEndYear.value = value
}

const handleYearIncrementInput = (value: string | null) => {
  localYearIncrement.value = value
}

const handleSpecificYearInput = (value: string | null) => {
  localSpecificYear.value = value
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
<style scoped>
.include-in-report-label {
  display: block;
  color: var(--typography-color-secondary);
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-body);
  line-height: 1.5;
  padding-bottom: 2px;
  margin-bottom: 12px;
}

.include-in-report-disabled {
  color: var(--typography-color-disabled) !important;
}

.min-dbh-limit-species-group-label {
  display: block;
  color: var(--typography-color-secondary);
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-body);
  line-height: 1.5;
  padding-bottom: 2px;
  margin-bottom: 12px;
}

.min-dbh-limit-species-group-list-container {
  max-width: 5%;
  padding-top: 0px;
  padding-left: 20px
}

.min-dbh-disabled {
  color: var(--typography-color-disabled) !important;
}

.report-title-text-field {
  max-width: 97.5% !important
}

.projection-type-container {
  padding-top: 16px;
}

@media (max-width: 600px) {
  .projection-type-container {
    padding-top: 0;
  }
}

.computed-mai-container {
  padding-left: 8px;
}

.culmination-values-container {
  padding-left: 17px;
}

.by-species-container {
  padding-left: 26px;
}

.secondary-species-height-container {
  padding-left: 34px;
}

.numeric-range-value-label {
  display: block;
  color: var(--typography-color-secondary);
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-body);
  line-height: 1.5;
  padding-bottom: 2px;
}

.age-year-range-container {
  margin-left: 6px;
}

.by-layer-container {
  padding-left: 8px;
}

.reference-year-container {
  padding-left: 8px;
}

.by-species-file-upload-container {
  padding-left: 17px;
}

.specific-year-container {
  margin-left: 5px;
}

.projection-mode-container {
  margin-left: 14px;
}

.secondary-species-height-file-upload-container {
  margin-left: 21px;
}

.polygon-id-container {
  margin-left: 8px;
}

.current-year-container {
  margin-left: 4px;
}

@media (max-width: 911px) {
  .by-layer-container,
  .by-species-file-upload-container,
  .secondary-species-height-file-upload-container,
  .projection-mode-container,
  .polygon-id-container,
  .current-year-container,
  .reference-year-container {
    padding-left: 0 !important;
    margin-left: 0 !important;
  }
}
</style>
