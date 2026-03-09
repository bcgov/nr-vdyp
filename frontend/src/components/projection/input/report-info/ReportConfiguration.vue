<template>
  <div class="report-top-section file-upload-top-section">
    <v-row no-gutters class="form-fields-row">
      <v-col cols="12" sm="6">
        <label class="bcds-text-field-label report-title-label" for="reportTitle">Report Title (Required)</label>
        <v-text-field
          id="reportTitle"
          type="string"
          v-model="localReportTitle"
          hide-details="auto"
          persistent-placeholder
          placeholder="Enter a report title..."
          :disabled="isDisabled"
          :error="!!titleError"
          :error-messages="titleError"
          @blur="validateTitle"
        ></v-text-field>
      </v-col>
      <v-col cols="12" sm="4" class="projection-type-container">
        <label class="bcds-radio-label" for="projection-type-select">Projection Type</label>
        <v-radio-group
          id="projection-type-select"
          v-model="localProjectionType"
          inline
          :hide-details="projectionTypeError ? 'auto' : true"
          :error="!!projectionTypeError"
          :error-messages="projectionTypeError"
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
    <v-row class="mt-fields">
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
  <div class="mt-n8 file-upload-numeric-range-section">
    <v-row no-gutters class="form-fields-row">
      <v-col cols="12" sm="auto" class="age-year-range-container">
        <div class="numeric-range-value-label">Numeric Range Value</div>
        <v-radio-group
          v-model="selectedAgeYearRange"
          inline
          :disabled="isDisabled"
          hide-details
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
        <v-col cols="6" sm="2" class="spin-field-col starting-age-year-mobile">
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
            :errorMessages="startingAgeError"
            data-testid="starting-age"
            @update:modelValue="handleStartingAgeInput"
          />
        </v-col>
        <v-col cols="6" sm="2" class="spin-field-col">
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
            :errorMessages="finishingAgeError"
            data-testid="finishing-age"
            @update:modelValue="handleFinishingAgeInput"
          />
        </v-col>
        <v-col cols="6" sm="2" class="spin-field-col">
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
            :errorMessages="ageIncrementError"
            data-testid="age-increment"
            @update:modelValue="handleAgeIncrementInput"
          />
        </v-col>
      </template>
      <template v-else>
        <v-col cols="6" sm="2" class="spin-field-col">
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
            :errorMessages="startYearError"
            data-testid="start-year"
            @update:modelValue="handleStartYearInput"
          />
        </v-col>
        <v-col cols="6" sm="2" class="spin-field-col">
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
            :errorMessages="endYearError"
            data-testid="end-year"
            @update:modelValue="handleEndYearInput"
          />
        </v-col>
        <v-col cols="6" sm="2" class="spin-field-col">
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
            :errorMessages="yearIncrementError"
            data-testid="year-increment"
            @update:modelValue="handleYearIncrementInput"
          />
        </v-col>
      </template>
      <v-col cols="6" sm="2" md="2" class="spin-field-col-isy">
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
  <!-- Include following values in Report (File Upload) -->
  <div class="ml-4 mt-5 mb-3 include-in-report-container-mobile">
    <div class="ml-n4 include-in-report-header">
      <span class="include-in-report-label" :class="{ 'include-in-report-disabled': isDisabled }">Include following values in Report</span>
    </div>
    <v-row class="ml-n7">
      <v-col cols="12" class="include-in-report-checkboxes">
        <v-row no-gutters class="form-fields-row file-upload-checkboxes-row">
          <v-col cols="auto">
            <v-checkbox
              v-model="localIsByLayerEnabled"
              :label=CONSTANTS.INCLUDE_IN_REPORT.BY_LAYER
              hide-details
              :disabled="isByLayerDeactivated"
              data-testid="is-by-layer-enabled"
            ></v-checkbox>
          </v-col>
          <v-col cols="auto">
            <v-checkbox
              v-model="localIsBySpeciesEnabled"
              :label=CONSTANTS.INCLUDE_IN_REPORT.BY_SPECIES
              hide-details
              :disabled="isBySpeciesDeactivated"
              data-testid="is-by-species-enabled"
            ></v-checkbox>
          </v-col>
          <v-col cols="auto">
            <v-checkbox
              v-model="localIncSecondaryHeight"
              :label=CONSTANTS.INCLUDE_IN_REPORT.SECD_SPCZ_HEIGHT
              hide-details
              :disabled="isIncSecondaryHeightDeactivated"
              data-testid="inc-secondary-height"
            ></v-checkbox>
          </v-col>
          <v-col cols="auto">
            <v-checkbox
              v-model="localIsProjectionModeEnabled"
              :label=CONSTANTS.INCLUDE_IN_REPORT.PRJECTION_MODE
              hide-details
              :disabled="isProjectionModeDeactivated"
              data-testid="is-projection-mode-enabled"
            ></v-checkbox>
          </v-col>
          <v-col cols="auto">
            <v-checkbox
              v-model="localIsPolygonIDEnabled"
              :label=CONSTANTS.INCLUDE_IN_REPORT.POLYGON_ID
              hide-details
              :disabled="isPolygonIDDeactivated"
              data-testid="is-polygon-id-enabled"
            ></v-checkbox>
          </v-col>
          <v-col cols="auto">
            <v-checkbox
              v-model="localIsCurrentYearEnabled"
              :label=CONSTANTS.INCLUDE_IN_REPORT.CURRENT_YEAR
              hide-details
              :disabled="isCurrentYearDeactivated"
              data-testid="is-current-year-enabled"
            ></v-checkbox>
          </v-col>
          <v-col cols="auto">
            <v-checkbox
              v-model="localIsReferenceYearEnabled"
              :label=CONSTANTS.INCLUDE_IN_REPORT.REFERENCE_YEAR
              hide-details
              :disabled="isReferenceYearDeactivated"
              data-testid="is-reference-year-enabled"
            ></v-checkbox>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </div>
</template>
<script setup lang="ts">
import { ref, watch, computed, type Ref } from 'vue'
import { CONSTANTS, DEFAULTS, MESSAGE, OPTIONS } from '@/constants'
import { reportInfoValidation } from '@/validation'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { AppSpinField } from '@/components'

const fileUploadStore = useFileUploadStore()

const props = defineProps<{
  selectedAgeYearRange: string | null
  startingAge: string | null
  finishingAge: string | null
  ageIncrement: string | null
  startYear: string | null
  endYear: string | null
  yearIncrement: string | null
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
}>()

const emit = defineEmits([
  'update:selectedAgeYearRange',
  'update:startingAge',
  'update:finishingAge',
  'update:ageIncrement',
  'update:startYear',
  'update:endYear',
  'update:yearIncrement',
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

// Common local state (age/year range fields)
const selectedAgeYearRange = ref<string>(
  props.selectedAgeYearRange || DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE,
)
const localStartingAge = ref<string | null>(props.startingAge)
const localFinishingAge = ref<string | null>(props.finishingAge)
const localAgeIncrement = ref<string | null>(props.ageIncrement)
const localStartYear = ref<string | null>(props.startYear)
const localEndYear = ref<string | null>(props.endYear)
const localYearIncrement = ref<string | null>(props.yearIncrement)

// Common local state (shared checkboxes)
const localIsBySpeciesEnabled = ref<boolean>(props.isBySpeciesEnabled)
const localIncSecondaryHeight = ref<boolean>(props.incSecondaryHeight)

// File Upload specific local state
const localIsByLayerEnabled = ref<boolean>(props.isByLayerEnabled)
const localIsProjectionModeEnabled = ref<boolean>(props.isProjectionModeEnabled)
const localIsPolygonIDEnabled = ref<boolean>(props.isPolygonIDEnabled)
const localIsCurrentYearEnabled = ref<boolean>(props.isCurrentYearEnabled)
const localIsReferenceYearEnabled = ref<boolean>(props.isReferenceYearEnabled)
const localSpecificYear = ref<string | null>(props.specificYear)
const localProjectionType = ref<string | null>(props.projectionType)
const localReportTitle = ref<string | null>(props.reportTitle)
const localReportDescription = ref<string | null>(props.reportDescription)

const reportDescriptionLength = computed(() => {
  return localReportDescription.value ? localReportDescription.value.length : 0
})

// Common validation error refs
const titleError = ref<string>('')
const projectionTypeError = ref<string>('')
const startingAgeError = ref<string>('')
const finishingAgeError = ref<string>('')
const ageIncrementError = ref<string>('')
const startYearError = ref<string>('')
const endYearError = ref<string>('')
const yearIncrementError = ref<string>('')

const validateTitle = (): boolean => {
  if (!localReportTitle.value || localReportTitle.value.trim() === '') {
    titleError.value = 'Report Title is required.'
    return false
  }
  titleError.value = ''
  return true
}

// Common validation functions
const validateNumericRange = (
  value: string | null,
  min: number,
  max: number,
  requiredMsg: string,
  rangeMsg: string,
  errorRef: Ref<string>,
): boolean => {
  if (value === null || value.trim() === '') {
    errorRef.value = requiredMsg
    return false
  }
  const num = Number.parseFloat(value)
  if (num < min || num > max) {
    errorRef.value = rangeMsg
    return false
  }
  return true
}

const validateAgeFields = (): boolean => {
  const { STARTING_AGE_MIN, STARTING_AGE_MAX, FINISHING_AGE_MIN, FINISHING_AGE_MAX, AGE_INC_MIN, AGE_INC_MAX } =
    CONSTANTS.NUM_INPUT_LIMITS
  const { RPT_VLD_START_AGE_RNG, RPT_VLD_START_FNSH_RNG, RPT_VLD_AGE_INC_RNG, RPT_VLD_COMP_FNSH_AGE } =
    MESSAGE.MDL_PRM_INPUT_ERR

  let isValid = validateNumericRange(
    localStartingAge.value,
    STARTING_AGE_MIN,
    STARTING_AGE_MAX,
    'Starting Age is required.',
    RPT_VLD_START_AGE_RNG(STARTING_AGE_MIN, STARTING_AGE_MAX),
    startingAgeError,
  )
  isValid =
    validateNumericRange(
      localFinishingAge.value,
      FINISHING_AGE_MIN,
      FINISHING_AGE_MAX,
      'Finishing Age is required.',
      RPT_VLD_START_FNSH_RNG(FINISHING_AGE_MIN, FINISHING_AGE_MAX),
      finishingAgeError,
    ) && isValid
  isValid =
    validateNumericRange(
      localAgeIncrement.value,
      AGE_INC_MIN,
      AGE_INC_MAX,
      'Increment is required.',
      RPT_VLD_AGE_INC_RNG(AGE_INC_MIN, AGE_INC_MAX),
      ageIncrementError,
    ) && isValid

  if (localStartingAge.value !== null && localStartingAge.value.trim() !== '' &&
      localFinishingAge.value !== null && localFinishingAge.value.trim() !== '') {
    if (!reportInfoValidation.validateComparison(localStartingAge.value, localFinishingAge.value).isValid) {
      finishingAgeError.value = RPT_VLD_COMP_FNSH_AGE
      isValid = false
    }
  }

  return isValid
}

const validateYearFields = (): boolean => {
  const { START_YEAR_MIN, START_YEAR_MAX, END_YEAR_MIN, END_YEAR_MAX, YEAR_INC_MIN, YEAR_INC_MAX } =
    CONSTANTS.NUM_INPUT_LIMITS
  const { RPT_VLD_START_YEAR_RNG, RPT_VLD_END_YEAR_RNG, RPT_VLD_YEAR_INC_RNG, RPT_VLD_COMP_END_YEAR } =
    MESSAGE.MDL_PRM_INPUT_ERR

  let isValid = validateNumericRange(
    localStartYear.value,
    START_YEAR_MIN,
    START_YEAR_MAX,
    'Start Year is required.',
    RPT_VLD_START_YEAR_RNG(START_YEAR_MIN, START_YEAR_MAX),
    startYearError,
  )
  isValid =
    validateNumericRange(
      localEndYear.value,
      END_YEAR_MIN,
      END_YEAR_MAX,
      'End Year is required.',
      RPT_VLD_END_YEAR_RNG(END_YEAR_MIN, END_YEAR_MAX),
      endYearError,
    ) && isValid
  isValid =
    validateNumericRange(
      localYearIncrement.value,
      YEAR_INC_MIN,
      YEAR_INC_MAX,
      'Increment is required.',
      RPT_VLD_YEAR_INC_RNG(YEAR_INC_MIN, YEAR_INC_MAX),
      yearIncrementError,
    ) && isValid

  if (localStartYear.value !== null && localStartYear.value.trim() !== '' &&
      localEndYear.value !== null && localEndYear.value.trim() !== '') {
    if (!reportInfoValidation.validateComparison(localStartYear.value, localEndYear.value).isValid) {
      endYearError.value = RPT_VLD_COMP_END_YEAR
      isValid = false
    }
  }

  return isValid
}

const validateFields = (): boolean => {
  projectionTypeError.value = ''
  startingAgeError.value = ''
  finishingAgeError.value = ''
  ageIncrementError.value = ''
  startYearError.value = ''
  endYearError.value = ''
  yearIncrementError.value = ''

  let isValid = true

  if (!localProjectionType.value || localProjectionType.value.trim() === '') {
    projectionTypeError.value = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_PROJECTION_TYPE_REQ
    isValid = false
  }

  const rangeValid =
    selectedAgeYearRange.value === CONSTANTS.AGE_YEAR_RANGE.AGE ? validateAgeFields() : validateYearFields()

  return isValid && rangeValid
}

defineExpose({ validateTitle, validateFields })

// Watch props -> local (common: age/year range fields)
watch(
  () => props.selectedAgeYearRange,
  (newVal) => {
    selectedAgeYearRange.value =
      newVal || DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
  },
)
watch(() => props.startingAge, (newVal) => { localStartingAge.value = newVal })
watch(() => props.finishingAge, (newVal) => { localFinishingAge.value = newVal })
watch(() => props.ageIncrement, (newVal) => { localAgeIncrement.value = newVal })
watch(() => props.startYear, (newVal) => { localStartYear.value = newVal })
watch(() => props.endYear, (newVal) => { localEndYear.value = newVal })
watch(() => props.yearIncrement, (newVal) => { localYearIncrement.value = newVal })

// Watch props -> local (common: shared checkboxes)
watch(() => props.isBySpeciesEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsBySpeciesEnabled.value)) {
    localIsBySpeciesEnabled.value = newVal
  }
})
watch(() => props.incSecondaryHeight, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIncSecondaryHeight.value)) {
    localIncSecondaryHeight.value = newVal
  }
})

// Watch props -> local (File Upload specific)
watch(() => props.isByLayerEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsByLayerEnabled.value)) {
    localIsByLayerEnabled.value = newVal
  }
})
watch(() => props.isProjectionModeEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsProjectionModeEnabled.value)) {
    localIsProjectionModeEnabled.value = newVal
  }
})
watch(() => props.isPolygonIDEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsPolygonIDEnabled.value)) {
    localIsPolygonIDEnabled.value = newVal
  }
})
watch(() => props.isCurrentYearEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsCurrentYearEnabled.value)) {
    localIsCurrentYearEnabled.value = newVal
  }
})
watch(() => props.isReferenceYearEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsReferenceYearEnabled.value)) {
    localIsReferenceYearEnabled.value = newVal
  }
})
watch(() => props.specificYear, (newVal) => { localSpecificYear.value = newVal })
watch(() => props.projectionType, (newVal) => { localProjectionType.value = newVal })
watch(() => props.reportTitle, (newVal) => { localReportTitle.value = newVal })
watch(() => props.reportDescription, (newVal) => { localReportDescription.value = newVal })

// Watch local -> emit (common: age/year range fields)
watch(selectedAgeYearRange, (newVal) =>
  emit('update:selectedAgeYearRange', newVal),
)
watch(localStartingAge, (newVal) => emit('update:startingAge', newVal))
watch(localFinishingAge, (newVal) => emit('update:finishingAge', newVal))
watch(localAgeIncrement, (newVal) => emit('update:ageIncrement', newVal))
watch(localStartYear, (newVal) => emit('update:startYear', newVal))
watch(localEndYear, (newVal) => emit('update:endYear', newVal))
watch(localYearIncrement, (newVal) => emit('update:yearIncrement', newVal))

// Watch local -> emit (common: shared checkboxes)
watch(localIsBySpeciesEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isBySpeciesEnabled)) {
    emit('update:isBySpeciesEnabled', newVal)
  }
})
watch(localIncSecondaryHeight, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.incSecondaryHeight)) {
    emit('update:incSecondaryHeight', newVal)
  }
})

// Watch local -> emit (File Upload specific)
watch(localIsByLayerEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isByLayerEnabled)) {
    emit('update:isByLayerEnabled', newVal)
  }
})
watch(localIsProjectionModeEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.isProjectionModeEnabled)) {
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
watch(localSpecificYear, (newVal) => emit('update:specificYear', newVal))
watch(localProjectionType, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.projectionType)) {
    emit('update:projectionType', newVal)
  }
  // Update File Upload store projection type
  if (fileUploadStore.projectionType !== newVal) {
    fileUploadStore.projectionType = newVal
  }
})
watch(localReportTitle, (newVal) => emit('update:reportTitle', newVal))
watch(localReportDescription, (newVal) => emit('update:reportDescription', newVal))

// File Upload specific computed deactivated states
const isByLayerDeactivated = computed(() => props.isDisabled)
const isProjectionModeDeactivated = computed(() => props.isDisabled)
const isPolygonIDDeactivated = computed(() => props.isDisabled)
const isCurrentYearDeactivated = computed(() => props.isDisabled)
const isReferenceYearDeactivated = computed(() => props.isDisabled)
const isSpecificYearDeactivated = computed(() => props.isDisabled)

// Common computed deactivated states (shared checkboxes)
const isCFOBiomassSelected = computed(() => {
  return localProjectionType.value === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
})
const isBySpeciesDeactivated = computed(() => {
  return props.isDisabled || isCFOBiomassSelected.value
})
const isIncSecondaryHeightDeactivated = computed(() => {
  return props.isDisabled
})

// Common input handlers
const handleStartingAgeInput = (value: string | null) => {
  localStartingAge.value = value
  startingAgeError.value = ''
}

const handleFinishingAgeInput = (value: string | null) => {
  localFinishingAge.value = value
  finishingAgeError.value = ''
}

const handleAgeIncrementInput = (value: string | null) => {
  localAgeIncrement.value = value
  ageIncrementError.value = ''
}

const handleStartYearInput = (value: string | null) => {
  localStartYear.value = value
  startYearError.value = ''
}

const handleEndYearInput = (value: string | null) => {
  localEndYear.value = value
  endYearError.value = ''
}

const handleYearIncrementInput = (value: string | null) => {
  localYearIncrement.value = value
  yearIncrementError.value = ''
}

const handleSpecificYearInput = (value: string | null) => {
  localSpecificYear.value = value
}
</script>
<style scoped>
/* Common styles */
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

.include-in-report-checkboxes {
  padding-top: 1px; padding-bottom: 16px
}

/* File Upload specific styles */
.projection-type-container {
  margin-top: 13px;
}

.numeric-range-value-label {
  display: block;
  color: var(--typography-color-secondary);
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-label);
  line-height: 1.5;
  padding-bottom: 2px;
}

.age-year-range-container {
  margin-top: 2px;
}

.specific-year-container {
  margin-left: 5px;
}

.mt-fields {
  margin-top: 0;
  padding-bottom: 12px;
}

.report-title-label {
  padding-top: 0px;
}

@media (min-width: 768px) and (max-width: 912px) {
  .file-upload-checkboxes-row {
    row-gap: 16px;
  }
}

@media (max-width: 600px) {
  .projection-type-container {
    margin-top: 0;
  }

  .mt-fields {
    margin-top: -8px;
    padding-bottom: 0px;
  }

  .age-year-range-container {
    margin-top: -2px;
  }

  .report-top-section {
    margin-bottom: 8px;
  }

  .file-upload-numeric-range-section {
    padding-top: 4px;
    padding-bottom: 0px;
  }

  .spin-field-col {
    margin-top: -8px;
    padding-top: 0 !important;
  }

  .spin-field-col-isy {
    padding-top: 0 !important;
  }

  .starting-age-year-mobile {
    margin-top: -12px;
  }

  .include-in-report-container-mobile {
    margin-top: 16px !important;
  }
}

@media (min-width: 600px) and (max-width: 900px) {
  .spin-field-col {
    padding-bottom: 0px !important;
  }
  .spin-field-col-isy {
    padding-top: 0px !important;
  }
}
</style>
