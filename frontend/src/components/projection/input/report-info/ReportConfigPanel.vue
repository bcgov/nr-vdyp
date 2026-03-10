<template>
  <v-card class="elevation-0">
    <v-expansion-panels v-model="panelOpenStates[panelName]">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates[panelName] === CONSTANTS.PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Report Details</span>
            </v-col>
            <v-col cols="auto" v-if="!isReadOnly" class="edit-button-col">
              <v-tooltip :text="editTooltipText" :disabled="!editTooltipText" location="top">
                <template #activator="{ props: tooltipProps }">
                  <span v-bind="tooltipProps">
                    <AppButton
                      label="Edit"
                      variant="tertiary"
                      mdi-name="mdi-pencil-outline"
                      iconPosition="top"
                      :isDisabled="!isHeaderEditActive"
                      @click="onHeaderEdit"
                    />
                  </span>
                </template>
              </v-tooltip>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text">
          <v-form ref="form">
            <!-- Report Title, Projection Type, Description -->
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
                    :disabled="isInputDisabled"
                    :error="!!titleError"
                    :error-messages="titleError"
                    @blur="validateTitle"
                  ></v-text-field>
                </v-col>
                <v-col cols="12" sm="4" class="projection-type-container">
                  <label class="bcds-radio-label" :class="{ 'bcds-radio-label--disabled': isInputDisabled }" for="projection-type-select">Projection Type</label>
                  <v-radio-group
                    id="projection-type-select"
                    v-model="localProjectionType"
                    inline
                    :hide-details="projectionTypeError ? 'auto' : true"
                    :error="!!projectionTypeError"
                    :error-messages="projectionTypeError"
                    :disabled="isInputDisabled"
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
                  <div class="bcds-textarea" :data-disabled="isInputDisabled || undefined">
                    <label class="bcds-textarea-label" for="reportDescription">Description</label>
                    <div class="bcds-textarea-container">
                      <textarea
                        id="reportDescription"
                        class="bcds-textarea-input"
                        v-model="localReportDescription"
                        placeholder="Provide a description of this Projection..."
                        :disabled="isInputDisabled"
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
            <!-- Numeric Range (Age/Year) -->
            <div class="mt-n8 file-upload-numeric-range-section">
              <v-row no-gutters class="form-fields-row">
                <v-col cols="12" sm="auto" class="age-year-range-container">
                  <div class="numeric-range-value-label numeric-range-value-label-mobile" :class="{ 'numeric-range-value-label--disabled': isInputDisabled }">Numeric Range Value</div>
                  <v-radio-group
                    v-model="localSelectedAgeYearRange"
                    inline
                    :disabled="isInputDisabled"
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
                <template v-if="localSelectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE">
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
                      :disabled="isInputDisabled"
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
                      :disabled="isInputDisabled"
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
                      :disabled="isInputDisabled"
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
                      :disabled="isInputDisabled"
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
                      :disabled="isInputDisabled"
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
                      :disabled="isInputDisabled"
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
                    :disabled="isInputDisabled"
                    :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                    :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.SPECIFIC_YEAR_DECIMAL_NUM"
                    data-testid="specific-year"
                    @update:modelValue="handleSpecificYearInput"
                  />
                </v-col>
              </v-row>
            </div>
            <!-- Include the following values in the Report -->
            <div class="ml-4 mb-3 include-in-report-container include-in-report-container-mobile">
              <div class="ml-n4 include-in-report-header">
                <span class="include-in-report-label" :class="{ 'include-in-report-disabled': isInputDisabled }">Include the following values in the Report</span>
              </div>
              <v-row class="ml-n7">
                <v-col cols="12" class="include-in-report-checkboxes">
                  <v-row no-gutters class="form-fields-row file-upload-checkboxes-row">
                    <v-col cols="auto">
                      <v-checkbox
                        v-model="localIsByLayerEnabled"
                        :label=CONSTANTS.INCLUDE_IN_REPORT.BY_LAYER
                        hide-details
                        :disabled="isInputDisabled"
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
                        :disabled="isInputDisabled"
                        data-testid="inc-secondary-height"
                      ></v-checkbox>
                    </v-col>
                    <v-col cols="auto">
                      <v-checkbox
                        v-model="localIsProjectionModeEnabled"
                        :label=CONSTANTS.INCLUDE_IN_REPORT.PRJECTION_MODE
                        hide-details
                        :disabled="isInputDisabled"
                        data-testid="is-projection-mode-enabled"
                      ></v-checkbox>
                    </v-col>
                    <v-col cols="auto">
                      <v-checkbox
                        v-model="localIsPolygonIDEnabled"
                        :label=CONSTANTS.INCLUDE_IN_REPORT.POLYGON_ID
                        hide-details
                        :disabled="isInputDisabled"
                        data-testid="is-polygon-id-enabled"
                      ></v-checkbox>
                    </v-col>
                    <v-col cols="auto">
                      <v-checkbox
                        v-model="localIsCurrentYearEnabled"
                        :label=CONSTANTS.INCLUDE_IN_REPORT.CURRENT_YEAR
                        hide-details
                        :disabled="isInputDisabled"
                        data-testid="is-current-year-enabled"
                      ></v-checkbox>
                    </v-col>
                    <v-col cols="auto">
                      <v-checkbox
                        v-model="localIsReferenceYearEnabled"
                        :label=CONSTANTS.INCLUDE_IN_REPORT.REFERENCE_YEAR
                        hide-details
                        :disabled="isInputDisabled"
                        data-testid="is-reference-year-enabled"
                      ></v-checkbox>
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
            </div>
            <ActionPanel
              v-if="!isReadOnly"
              :isConfirmEnabled="isConfirmEnabled"
              :isConfirmed="isConfirmed"
              :hideClearButton="true"
              :hideEditButton="true"
              :showCancelButton="true"
              @confirm="onConfirm"
              @cancel="onCancel"
            />
          </v-form>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>
<script setup lang="ts">
import { ref, watch, computed, type Ref } from 'vue'
import { useAppStore } from '@/stores/projection/appStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { AppButton, AppSpinField } from '@/components'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import { ActionPanel } from '@/components/projection'
import { CONSTANTS, DEFAULTS, MESSAGE, OPTIONS } from '@/constants'
import { PROJECTION_ERR } from '@/constants/message'
import type { FileUploadPanelName } from '@/types/types'
import { reportInfoValidation } from '@/validation'
import { saveProjectionOnPanelConfirm as saveFileUploadProjection, revertPanelToSaved, hasMinimumDBHUnsavedChanges } from '@/services/projection/fileUploadService'
import { useNotificationStore } from '@/stores/common/notificationStore'

const form = ref<HTMLFormElement>()

const appStore = useAppStore()
const fileUploadStore = useFileUploadStore()
const notificationStore = useNotificationStore()
const alertDialogStore = useAlertDialogStore()

const panelName = CONSTANTS.FILE_UPLOAD_PANEL.REPORT_CONFIG
const panelOpenStates = computed(() => fileUploadStore.panelOpenStates)

const isReadOnly = computed(() => appStore.isReadOnly)
const isConfirmEnabled = computed(
  () => !isReadOnly.value && fileUploadStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => fileUploadStore.panelState[panelName].confirmed,
)
const isInputDisabled = computed(
  () => isReadOnly.value || !fileUploadStore.panelState[panelName].editable,
)

const isHeaderEditActive = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) return false
  return isConfirmed.value && !fileUploadStore.panelState[panelName].editable
})

const editTooltipText = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) {
    return `This section may not be edited with a status of ${status}`
  }
  if (isConfirmed.value && !fileUploadStore.panelState[panelName].editable) {
    return 'Click Edit to make changes to this section'
  }
  return ''
})

// Local state - initialized from store, kept in sync via watchers
const localReportTitle = ref<string | null>(fileUploadStore.reportTitle)
const localReportDescription = ref<string | null>(fileUploadStore.reportDescription)
const localProjectionType = ref<string | null>(fileUploadStore.projectionType)
const localSelectedAgeYearRange = ref<string>(
  fileUploadStore.selectedAgeYearRange || DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE,
)
const localStartingAge = ref<string | null>(fileUploadStore.startingAge)
const localFinishingAge = ref<string | null>(fileUploadStore.finishingAge)
const localAgeIncrement = ref<string | null>(fileUploadStore.ageIncrement)
const localStartYear = ref<string | null>(fileUploadStore.startYear)
const localEndYear = ref<string | null>(fileUploadStore.endYear)
const localYearIncrement = ref<string | null>(fileUploadStore.yearIncrement)
const localSpecificYear = ref<string | null>(fileUploadStore.specificYear)
const localIsByLayerEnabled = ref<boolean>(fileUploadStore.isByLayerEnabled)
const localIsBySpeciesEnabled = ref<boolean>(fileUploadStore.isBySpeciesEnabled)
const localIncSecondaryHeight = ref<boolean>(fileUploadStore.incSecondaryHeight)
const localIsProjectionModeEnabled = ref<boolean>(fileUploadStore.isProjectionModeEnabled)
const localIsPolygonIDEnabled = ref<boolean>(fileUploadStore.isPolygonIDEnabled)
const localIsCurrentYearEnabled = ref<boolean>(fileUploadStore.isCurrentYearEnabled)
const localIsReferenceYearEnabled = ref<boolean>(fileUploadStore.isReferenceYearEnabled)

const reportDescriptionLength = computed(() =>
  localReportDescription.value ? localReportDescription.value.length : 0,
)

const isCFOBiomassSelected = computed(() =>
  localProjectionType.value === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS,
)
const isBySpeciesDeactivated = computed(() =>
  isInputDisabled.value || isCFOBiomassSelected.value,
)

// Validation error refs
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
    localStartingAge.value, STARTING_AGE_MIN, STARTING_AGE_MAX,
    'Starting Age is required.', RPT_VLD_START_AGE_RNG(STARTING_AGE_MIN, STARTING_AGE_MAX), startingAgeError,
  )
  isValid = validateNumericRange(
    localFinishingAge.value, FINISHING_AGE_MIN, FINISHING_AGE_MAX,
    'Finishing Age is required.', RPT_VLD_START_FNSH_RNG(FINISHING_AGE_MIN, FINISHING_AGE_MAX), finishingAgeError,
  ) && isValid
  isValid = validateNumericRange(
    localAgeIncrement.value, AGE_INC_MIN, AGE_INC_MAX,
    'Increment is required.', RPT_VLD_AGE_INC_RNG(AGE_INC_MIN, AGE_INC_MAX), ageIncrementError,
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
    localStartYear.value, START_YEAR_MIN, START_YEAR_MAX,
    'Start Year is required.', RPT_VLD_START_YEAR_RNG(START_YEAR_MIN, START_YEAR_MAX), startYearError,
  )
  isValid = validateNumericRange(
    localEndYear.value, END_YEAR_MIN, END_YEAR_MAX,
    'End Year is required.', RPT_VLD_END_YEAR_RNG(END_YEAR_MIN, END_YEAR_MAX), endYearError,
  ) && isValid
  isValid = validateNumericRange(
    localYearIncrement.value, YEAR_INC_MIN, YEAR_INC_MAX,
    'Increment is required.', RPT_VLD_YEAR_INC_RNG(YEAR_INC_MIN, YEAR_INC_MAX), yearIncrementError,
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
    localSelectedAgeYearRange.value === CONSTANTS.AGE_YEAR_RANGE.AGE
      ? validateAgeFields()
      : validateYearFields()

  return isValid && rangeValid
}

// Watch store -> local (for external changes)
watch(() => fileUploadStore.reportTitle, (v) => { localReportTitle.value = v })
watch(() => fileUploadStore.reportDescription, (v) => { localReportDescription.value = v })
watch(() => fileUploadStore.projectionType, (v) => { localProjectionType.value = v })
watch(() => fileUploadStore.selectedAgeYearRange, (v) => {
  localSelectedAgeYearRange.value = v || DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
})
watch(() => fileUploadStore.startingAge, (v) => { localStartingAge.value = v })
watch(() => fileUploadStore.finishingAge, (v) => { localFinishingAge.value = v })
watch(() => fileUploadStore.ageIncrement, (v) => { localAgeIncrement.value = v })
watch(() => fileUploadStore.startYear, (v) => { localStartYear.value = v })
watch(() => fileUploadStore.endYear, (v) => { localEndYear.value = v })
watch(() => fileUploadStore.yearIncrement, (v) => { localYearIncrement.value = v })
watch(() => fileUploadStore.specificYear, (v) => { localSpecificYear.value = v })
watch(() => fileUploadStore.isByLayerEnabled, (v) => { localIsByLayerEnabled.value = v })
watch(() => fileUploadStore.isBySpeciesEnabled, (v) => { localIsBySpeciesEnabled.value = v })
watch(() => fileUploadStore.incSecondaryHeight, (v) => { localIncSecondaryHeight.value = v })
watch(() => fileUploadStore.isProjectionModeEnabled, (v) => { localIsProjectionModeEnabled.value = v })
watch(() => fileUploadStore.isPolygonIDEnabled, (v) => { localIsPolygonIDEnabled.value = v })
watch(() => fileUploadStore.isCurrentYearEnabled, (v) => { localIsCurrentYearEnabled.value = v })
watch(() => fileUploadStore.isReferenceYearEnabled, (v) => { localIsReferenceYearEnabled.value = v })

// Watch local -> store (for UI changes)
watch(localReportTitle, (v) => { fileUploadStore.reportTitle = v })
watch(localReportDescription, (v) => { fileUploadStore.reportDescription = v })
watch(localProjectionType, (v) => {
  if (v === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS) {
    localIsBySpeciesEnabled.value = false
  }
  fileUploadStore.projectionType = v
})
watch(localSelectedAgeYearRange, (v) => { fileUploadStore.selectedAgeYearRange = v })
watch(localStartingAge, (v) => { fileUploadStore.startingAge = v })
watch(localFinishingAge, (v) => { fileUploadStore.finishingAge = v })
watch(localAgeIncrement, (v) => { fileUploadStore.ageIncrement = v })
watch(localStartYear, (v) => { fileUploadStore.startYear = v })
watch(localEndYear, (v) => { fileUploadStore.endYear = v })
watch(localYearIncrement, (v) => { fileUploadStore.yearIncrement = v })
watch(localSpecificYear, (v) => { fileUploadStore.specificYear = v })
watch(localIsByLayerEnabled, (v) => { fileUploadStore.isByLayerEnabled = v })
watch(localIsBySpeciesEnabled, (v) => { fileUploadStore.isBySpeciesEnabled = v })
watch(localIncSecondaryHeight, (v) => { fileUploadStore.incSecondaryHeight = v })
watch(localIsProjectionModeEnabled, (v) => { fileUploadStore.isProjectionModeEnabled = v })
watch(localIsPolygonIDEnabled, (v) => { fileUploadStore.isPolygonIDEnabled = v })
watch(localIsCurrentYearEnabled, (v) => { fileUploadStore.isCurrentYearEnabled = v })
watch(localIsReferenceYearEnabled, (v) => { fileUploadStore.isReferenceYearEnabled = v })

// Input handlers (clear error on change)
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

const handleMinimumDBHRevert = async (): Promise<boolean> => {
  const minDBHState = fileUploadStore.panelState[CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH]
  if (!minDBHState.editable) return true

  const hasChanges = await hasMinimumDBHUnsavedChanges(fileUploadStore)
  if (!hasChanges) return true

  const proceed = await alertDialogStore.openDialog(
    MESSAGE.UNSAVED_CHANGES_DIALOG.TITLE,
    MESSAGE.UNSAVED_CHANGES_DIALOG.MESSAGE,
    { variant: 'warning' },
  )
  if (!proceed) return false

  appStore.isSavingProjection = true
  try {
    await revertPanelToSaved(CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH)
  } catch (error) {
    console.error('Error reverting MinimumDBH panel to saved state:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
    return false
  } finally {
    appStore.isSavingProjection = false
  }
  return true
}

const onHeaderEdit = async () => {
  if (isConfirmed.value) {
    if (!(await handleMinimumDBHRevert())) return
    fileUploadStore.editPanel(panelName)
  }
}

const onConfirm = async () => {
  if (!validateTitle()) return
  if (!validateFields()) return

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
  }

  appStore.isSavingProjection = true
  try {
    await saveFileUploadProjection(fileUploadStore, panelName)
  } catch (error) {
    console.error('Error saving projection:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, PROJECTION_ERR.SAVE_FAILED_TITLE)
    return
  } finally {
    appStore.isSavingProjection = false
  }

  if (!isConfirmed.value) {
    fileUploadStore.confirmPanel(panelName)
  }
}

const onCancel = async () => {
  appStore.isSavingProjection = true
  try {
    await revertPanelToSaved(panelName as FileUploadPanelName)
  } catch (error) {
    console.error('Error reverting panel to saved state:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
  } finally {
    appStore.isSavingProjection = false
  }
}
</script>
<style scoped>
.edit-button-col {
  display: flex;
  align-items: center;
}

.edit-button-col :deep(.bcds-button.icon-top) {
  padding: 2px 4px;
  gap: 2px;
}

.edit-button-col :deep(.bcds-button.icon-top .v-icon) {
  font-size: 18px;
}

.edit-button-col :deep(.bcds-button.icon-top .button-label) {
  font-size: 11px;
}

.include-in-report-label {
  display: block;
  color: var(--typography-color-secondary);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-label);
  line-height: 1.5;
  padding-bottom: 4px;
  margin-bottom: 10px;
}

.include-in-report-disabled {
  color: var(--typography-color-disabled) !important;
}

.include-in-report-checkboxes {
  padding-top: 1px; padding-bottom: 16px
}

.projection-type-container {
  margin-top: 11px;
}

.numeric-range-value-label {
  display: block;
  color: var(--typography-color-secondary);
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-label);
  line-height: 1.5;
  padding-bottom: 9px;
}

.numeric-range-value-label--disabled {
  color: var(--typography-color-disabled) !important;
}

.age-year-range-container {
  margin-top: 0px;
}

.spin-field-col,
.spin-field-col-isy {
  min-width: 170px;
  max-width: 170px;
}

.mt-fields {
  margin-top: 0;
  padding-bottom: 12px;
}

.report-title-label {
  padding-top: 0px;
}

.include-in-report-container {
  margin-top: 16px !important;
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

  .numeric-range-value-label-mobile {
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
