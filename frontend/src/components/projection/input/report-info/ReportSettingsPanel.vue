<template>
  <v-card class="elevation-0">
    <v-expansion-panels v-model="panelOpenStates[panelName]">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title class="settings-panel-title">
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates[panelName] === CONSTANTS.PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Report Settings</span>
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
            <!-- Top row: age/year fields + include-in-report section -->
            <div class="report-top-row">
              <div class="age-year-fields">
                <template v-if="selectedAgeYearRange === CONSTANTS.AGE_YEAR_RANGE.AGE">
                  <div class="field-col">
                    <AppSpinField
                      label="Starting Age (Required)"
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
                  </div>
                  <div class="field-col">
                    <AppSpinField
                      label="Finishing Age (Required)"
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
                  </div>
                  <div class="field-col">
                    <AppSpinField
                      label="Increment (Required)"
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
                  </div>
                </template>
                <template v-else>
                  <div class="field-col">
                    <AppSpinField
                      label="Start Year (Required)"
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
                  </div>
                  <div class="field-col">
                    <AppSpinField
                      label="End Year (Required)"
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
                  </div>
                  <div class="field-col">
                    <AppSpinField
                      label="Increment (Required)"
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
                  </div>
                </template>
              </div>
              <!-- Include the following values in the Report -->
              <div class="include-in-report-section">
                <span class="include-in-report-label include-in-report-label-mobile" :class="{ 'include-in-report-disabled': isInputDisabled }">
                  Include the following values in the Report
                </span>
                <v-row no-gutters class="form-fields-row report-settings-checkboxes-row">
                  <v-col cols="auto">
                    <v-checkbox
                      v-model="localIsComputedMAIEnabled"
                      :label="CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI"
                      hide-details
                      :disabled="isComputedMAIDeactivated"
                      data-testid="is-computed-mai-enabled"
                    ></v-checkbox>
                  </v-col>
                  <v-col cols="auto">
                    <v-checkbox
                      v-model="localIsCulminationValuesEnabled"
                      :label="CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES"
                      hide-details
                      :disabled="isCulminationValuesDeactivated"
                      data-testid="is-culmination-values-enabled"
                    ></v-checkbox>
                  </v-col>
                  <v-col cols="auto">
                    <v-checkbox
                      v-model="localIsBySpeciesEnabled"
                      :label="CONSTANTS.INCLUDE_IN_REPORT.BY_SPECIES"
                      hide-details
                      :disabled="isBySpeciesDeactivated"
                      data-testid="is-by-species-enabled"
                    ></v-checkbox>
                  </v-col>
                  <v-col cols="auto">
                    <v-checkbox
                      v-model="localIncSecondaryHeight"
                      :label="CONSTANTS.INCLUDE_IN_REPORT.SECD_SPCZ_HEIGHT"
                      hide-details
                      :disabled="isIncSecondaryHeightDeactivated"
                      data-testid="inc-secondary-height"
                    ></v-checkbox>
                  </v-col>
                </v-row>
              </div>
            </div>

            <!-- Minimum DBH Limit section -->
            <div class="min-dbh-section">
              <div class="min-dbh-header-row">
                <span class="min-dbh-limit-species-group-label" :class="{ 'min-dbh-disabled': isMinDBHDeactivated }">
                  {{ mobile ? 'Min DBH by Species Group (cm+)' : 'Minimum DBH Limit by Species Group' }}
                </span>
              </div>
              <v-container fluid class="min-dbh-container">
                <v-row v-for="(group, index) in speciesGroups" :key="index" no-gutters class="form-fields-row min-dbh-row">
                  <v-col v-bind="mobile ? { cols: 'auto' } : {}" class="min-dbh-species-group-label" :class="{ 'min-dbh-disabled': isMinDBHDeactivated }">
                    {{ group.group }}
                  </v-col>
                  <v-col cols="10" sm="8" :class="mobile ? 'min-dbh-slider-col-mobile' : 'min-dbh-slider-col'">
                    <!-- Labels rendered outside Vuetify's slider DOM to avoid non-visibility on a mobile device web browser -->
                    <div class="min-dbh-label-row" :class="{ 'min-dbh-labels-muted': isMinDBHDeactivated }">
                      <span
                        v-for="opt in utilizationClassOptions"
                        :key="opt.index"
                        class="min-dbh-label-item"
                      >{{ mobile ? opt.label.replace(' cm+', '') : opt.label }}</span>
                    </div>
                    <v-slider
                      v-model="utilizationSliderValues[index]"
                      :min="0"
                      :max="4"
                      show-ticks="always"
                      step="1"
                      thumb-size="12"
                      track-size="7"
                      track-color="transparent"
                      hide-details
                      :disabled="isMinDBHDeactivated"
                      @update:model-value="updateMinDBH(index, $event)"
                    ></v-slider>
                  </v-col>
                </v-row>
              </v-container>
            </div>
          </v-form>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>
<script setup lang="ts">
import { ref, watch, computed, type Ref } from 'vue'
import { useDisplay } from 'vuetify'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS, MESSAGE, OPTIONS } from '@/constants'
import { reportInfoValidation } from '@/validation'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { AppButton, AppSpinField } from '@/components'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { saveProjectionOnPanelConfirm as saveModelParamProjection, revertPanelToSaved, hasPanelUnsavedChanges } from '@/services/projection/modelParameterService'
import { PROJECTION_ERR } from '@/constants/message'
import type { PanelName } from '@/types/types'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'

const { mobile } = useDisplay()

const form = ref<HTMLFormElement>()

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const notificationStore = useNotificationStore()
const alertDialogStore = useAlertDialogStore()

const panelName = CONSTANTS.MANUAL_INPUT_PANEL.REPORT_SETTINGS
const panelOpenStates = computed(() => modelParameterStore.panelOpenStates)

const isReadOnly = computed(() => appStore.isReadOnly)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)
const isInputDisabled = computed(
  () => isReadOnly.value || !modelParameterStore.panelState[panelName].editable,
)

// Edit button in header
const isHeaderEditActive = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) return false
  return isConfirmed.value && !modelParameterStore.panelState[panelName].editable
})

const editTooltipText = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) {
    return `This section may not be edited with a status of ${status}`
  }
  if (isConfirmed.value && !modelParameterStore.panelState[panelName].editable) {
    return 'Click Edit to make changes to this section'
  }
  return ''
})

const getEditablePanel = (): string | null => {
  const panelsToCheck = [
    CONSTANTS.MANUAL_INPUT_PANEL.REPORT_DETAILS,
    CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO,
    CONSTANTS.MANUAL_INPUT_PANEL.SITE_INFO,
    CONSTANTS.MANUAL_INPUT_PANEL.STAND_INFO,
  ]
  return panelsToCheck.find((p) => modelParameterStore.panelState[p].editable) ?? null
}

const onHeaderEdit = async () => {
  if (isConfirmed.value) {
    const editablePanel = getEditablePanel()
    if (editablePanel) {
      const hasChanges = await hasPanelUnsavedChanges(editablePanel, modelParameterStore)
      if (hasChanges) {
        const proceed = await alertDialogStore.openDialog(
          MESSAGE.UNSAVED_CHANGES_DIALOG.TITLE,
          MESSAGE.UNSAVED_CHANGES_DIALOG.MESSAGE,
          { variant: 'warning' },
        )
        if (!proceed) return

        appStore.isSavingProjection = true
        try {
          await revertPanelToSaved(editablePanel as PanelName)
        } catch (error) {
          console.error('Error reverting panel to saved state:', error)
          notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
          return
        } finally {
          appStore.isSavingProjection = false
        }
      }
    }
    modelParameterStore.editPanel(panelName)
  }
}

// Local state
const selectedAgeYearRange = ref<string>(
  modelParameterStore.selectedAgeYearRange || DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE,
)
const localStartingAge = ref<string | null>(modelParameterStore.startingAge)
const localFinishingAge = ref<string | null>(modelParameterStore.finishingAge)
const localAgeIncrement = ref<string | null>(modelParameterStore.ageIncrement)
const localStartYear = ref<string | null>(modelParameterStore.startYear)
const localEndYear = ref<string | null>(modelParameterStore.endYear)
const localYearIncrement = ref<string | null>(modelParameterStore.yearIncrement)
const localIsComputedMAIEnabled = ref<boolean>(modelParameterStore.isComputedMAIEnabled)
const localIsCulminationValuesEnabled = ref<boolean>(modelParameterStore.isCulminationValuesEnabled)
const localIsBySpeciesEnabled = ref<boolean>(modelParameterStore.isBySpeciesEnabled)
const localIncSecondaryHeight = ref<boolean>(modelParameterStore.incSecondaryHeight)

// Validation error refs
const startingAgeError = ref<string>('')
const finishingAgeError = ref<string>('')
const ageIncrementError = ref<string>('')
const startYearError = ref<string>('')
const endYearError = ref<string>('')
const yearIncrementError = ref<string>('')

// MinimumDBH
const utilizationSliderValues = ref<number[]>([])
const utilizationClassOptions = OPTIONS.utilizationClassOptions
const speciesGroups = computed(() => modelParameterStore.speciesGroups)


const isCFOBiomassSelected = computed(() => {
  return modelParameterStore.projectionType === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
})

// Computed deactivated states
const isComputedMAIDeactivated = computed(() => {
  return isInputDisabled.value || isCFOBiomassSelected.value
})

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

const isCulminationValuesDeactivated = computed(() => {
  return (
    isInputDisabled.value ||
    !isCulminationValuesEligible(localStartingAge.value, localFinishingAge.value)
  )
})

const isBySpeciesDeactivated = computed(() => {
  return isInputDisabled.value || isCFOBiomassSelected.value
})

const isIncSecondaryHeightDeactivated = computed(() => {
  return (
    isInputDisabled.value ||
    modelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.VOLUME
  )
})

const isMinDBHDeactivated = computed(() => {
  return isInputDisabled.value || isCFOBiomassSelected.value
})

// Watch speciesGroups for changes and sync utilization sliderValues
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

// Watch projectionType for CFS Biomass to update Min DBH limits
watch(
  () => modelParameterStore.projectionType,
  (newVal) => {
    if (newVal === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS) {
      speciesGroups.value.forEach((group) => {
        if (BIZCONSTANTS.CFS_BIOMASS_SPECIES_GROUP_UTILIZATION_MAP[group.group]) {
          group.minimumDBHLimit =
            BIZCONSTANTS.CFS_BIOMASS_SPECIES_GROUP_UTILIZATION_MAP[group.group]
        }
      })
      utilizationSliderValues.value = speciesGroups.value.map((group) =>
        utilizationClassOptions.findIndex(
          (opt) => opt.value === group.minimumDBHLimit,
        ),
      )
    }
  },
)

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

// Watch store -> local
watch(() => modelParameterStore.selectedAgeYearRange, (newVal) => {
  selectedAgeYearRange.value = newVal || DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
})
watch(() => modelParameterStore.startingAge, (newVal) => { localStartingAge.value = newVal })
watch(() => modelParameterStore.finishingAge, (newVal) => { localFinishingAge.value = newVal })
watch(() => modelParameterStore.ageIncrement, (newVal) => { localAgeIncrement.value = newVal })
watch(() => modelParameterStore.startYear, (newVal) => { localStartYear.value = newVal })
watch(() => modelParameterStore.endYear, (newVal) => { localEndYear.value = newVal })
watch(() => modelParameterStore.yearIncrement, (newVal) => { localYearIncrement.value = newVal })

watch(() => modelParameterStore.isComputedMAIEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsComputedMAIEnabled.value)) {
    localIsComputedMAIEnabled.value = newVal
  }
})
watch(() => modelParameterStore.isCulminationValuesEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsCulminationValuesEnabled.value)) {
    localIsCulminationValuesEnabled.value = newVal
  }
})
watch(() => modelParameterStore.isBySpeciesEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsBySpeciesEnabled.value)) {
    localIsBySpeciesEnabled.value = newVal
  }
})
watch(() => modelParameterStore.incSecondaryHeight, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIncSecondaryHeight.value)) {
    localIncSecondaryHeight.value = newVal
  }
})

watch(selectedAgeYearRange, (newVal) => { modelParameterStore.selectedAgeYearRange = newVal })
watch(localStartingAge, (newVal) => { modelParameterStore.startingAge = newVal })
watch(localFinishingAge, (newVal) => { modelParameterStore.finishingAge = newVal })
watch(localAgeIncrement, (newVal) => { modelParameterStore.ageIncrement = newVal })
watch(localStartYear, (newVal) => { modelParameterStore.startYear = newVal })
watch(localEndYear, (newVal) => { modelParameterStore.endYear = newVal })
watch(localYearIncrement, (newVal) => { modelParameterStore.yearIncrement = newVal })

watch(localIsComputedMAIEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(modelParameterStore.isComputedMAIEnabled)) {
    modelParameterStore.isComputedMAIEnabled = newVal
  }
})
watch(localIsCulminationValuesEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(modelParameterStore.isCulminationValuesEnabled)) {
    modelParameterStore.isCulminationValuesEnabled = newVal
  }
})
watch(localIsBySpeciesEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(modelParameterStore.isBySpeciesEnabled)) {
    modelParameterStore.isBySpeciesEnabled = newVal
  }
})
watch(localIncSecondaryHeight, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(modelParameterStore.incSecondaryHeight)) {
    modelParameterStore.incSecondaryHeight = newVal
  }
})

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

const updateMinDBH = (index: number, value: number) => {
  if (speciesGroups.value[index]) {
    const enumValue = utilizationClassOptions[value]?.value
    if (enumValue !== undefined) {
      speciesGroups.value[index].minimumDBHLimit = enumValue
    }
  }
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

const validateTitle = (): boolean => {
  if (!modelParameterStore.reportTitle || modelParameterStore.reportTitle.trim() === '') {
    return false
  }
  return true
}

const validateFields = (): boolean => {
  startingAgeError.value = ''
  finishingAgeError.value = ''
  ageIncrementError.value = ''
  startYearError.value = ''
  endYearError.value = ''
  yearIncrementError.value = ''

  let isValid = true

  if (!modelParameterStore.projectionType || modelParameterStore.projectionType.trim() === '') {
    isValid = false
  }

  const rangeValid =
    selectedAgeYearRange.value === CONSTANTS.AGE_YEAR_RANGE.AGE ? validateAgeFields() : validateYearFields()

  return isValid && rangeValid
}

const onConfirm = async (): Promise<boolean> => {
  if (!validateTitle()) return false
  if (!validateFields()) return false

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
  }

  appStore.isSavingProjection = true
  try {
    await saveModelParamProjection(modelParameterStore, panelName)
  } catch (error) {
    console.error('Error saving projection:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, PROJECTION_ERR.SAVE_FAILED_TITLE)
    return false
  } finally {
    appStore.isSavingProjection = false
  }

  if (!isConfirmed.value) {
    modelParameterStore.confirmPanel(panelName)
  }
  return true
}

defineExpose({ onConfirm })
</script>
<style scoped>
/* Top row: age/year fields + include in report section */
.report-top-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 16px;
  margin-top: 0px;
}

.age-year-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-start;
}

.field-col {
  min-width: 170px;
  max-width: 170px;
  flex: 0 0 auto;
}

.include-in-report-section {
  display: flex;
  flex-direction: column;
  flex: 0 0 auto;
}

.include-in-report-label {
  display: block;
  color: var(--typography-color-secondary);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-label);
  line-height: 1.5;
  padding-bottom: 4px;
  margin-bottom: 7px;
}

.include-in-report-disabled {
  color: var(--typography-color-disabled) !important;
}

/* Minimum DBH section */
.min-dbh-section {
  margin-top: 16px;
}

.min-dbh-header-row {
  margin-bottom: 8px;
}

.min-dbh-limit-species-group-label {
  display: block;
  color: var(--typography-color-secondary);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-label);
  line-height: 1.5;
}

.min-dbh-container {
  padding: 0px 0px 0px 0px;
}

.min-dbh-species-group-label {
  flex: 0 0 1.5rem;
  width: 3rem;
  padding-top: 15px !important;
}

.min-dbh-row + .min-dbh-row {
  margin-top: 6px;
}

.min-dbh-label-row {
  position: relative;
  height: 1.2rem;
  margin-top: -4px;
  margin-bottom: -4px;
  overflow: visible;
}

.min-dbh-label-item {
  position: absolute;
  font-size: var(--typography-font-size-label);
  color: rgba(0, 0, 0, 0.87);
  white-space: nowrap;
}

.min-dbh-label-item:nth-child(1) { left: 0; }
.min-dbh-label-item:nth-child(2) { left: 25%;  transform: translateX(-50%); }
.min-dbh-label-item:nth-child(3) { left: 50%;  transform: translateX(-50%); }
.min-dbh-label-item:nth-child(4) { left: 75%;  transform: translateX(-50%); }
.min-dbh-label-item:nth-child(5) { right: 0; }

.min-dbh-labels-muted .min-dbh-label-item {
  opacity: 0.6;
}

.min-dbh-disabled {
  color: var(--typography-color-disabled) !important;
}

/* Edit button in header */
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

.expansion-panel-icon-col {
  display: flex;
  align-items: center;
  padding-right: 8px;
}

.expansion-panel-icon {
  color: var(--typography-color-primary);
}

.expander-header {
  align-items: center;
}

/* Desktop (>=960px): slider col makes group+slider total 50% of row width */
.min-dbh-slider-col {
  flex: 0 0 calc(50% - 3rem) !important;
  width: calc(50% - 3rem) !important;
  max-width: calc(50% - 3rem) !important;
}

@media (max-width: 1279px) {
  .min-dbh-row {
    flex-wrap: nowrap !important;
  }

  /* Mobile (<960px): full width by default */
  .min-dbh-slider-col-mobile {
    flex: 1 1 0 !important;
    width: auto !important;
    max-width: 100% !important;
  }
}

/* Tablet/large mobile (768-1024px): slider col makes group+slider total 50% */
@media (min-width: 768px) and (max-width: 1024px) {
  .include-in-report-label-mobile {
    padding-bottom: 0px;
    margin-bottom: 0px;
  }

  .min-dbh-slider-col-mobile {
    flex: 0 0 calc(50% - 3rem) !important;
    width: calc(50% - 3rem) !important;
    max-width: calc(50% - 3rem) !important;
  }
}

@media (min-width: 768px) and (max-width: 912px) {
  .report-settings-checkboxes-row {
    row-gap: 16px;
  }
}

@media (max-width: 600px) {
  .field-col {
    min-width: 170px;
    max-width: calc(50% - 8px);
  }

  .include-in-report-label-mobile {
    padding-bottom: 0px;
    margin-bottom: 0px;
  }

  /* Allow include-in-report section to take full width so checkboxes wrap */
  .include-in-report-section {
    flex-shrink: 1;
    min-width: 0;
    flex-basis: 100%;
  }

  .min-dbh-species-group-label {
    padding-left: 0px !important;
  }

  .min-dbh-slider-col-mobile {
    margin-left: 0;
    padding-left: 4px;
    padding-right: 4px;
  }

  .min-dbh-row + .min-dbh-row {
    margin-top: 6px;
  }
}
</style>
