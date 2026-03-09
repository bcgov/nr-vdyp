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
              <span class="text-h6">Report Information</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text">
          <v-form ref="form">
            <div>
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
                      :disabled="isInputDisabled"
                      :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                      :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_DECIMAL_NUM"
                      :errorMessages="startingAgeError"
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
                      :disabled="isInputDisabled"
                      :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                      :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_DECIMAL_NUM"
                      :errorMessages="finishingAgeError"
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
                      :disabled="isInputDisabled"
                      :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                      :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.START_YEAR_DECIMAL_NUM"
                      :errorMessages="startYearError"
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
                      :disabled="isInputDisabled"
                      :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                      :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.END_YEAR_DECIMAL_NUM"
                      :errorMessages="endYearError"
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
                      :disabled="isInputDisabled"
                      :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                      :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.YEAR_INC_DECIMAL_NUM"
                      :errorMessages="yearIncrementError"
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
                      :disabled="isInputDisabled"
                      data-testid="is-forward-grow-enabled"
                    ></v-checkbox>
                    <v-col class="col-space-3" />
                    <v-checkbox
                      v-model="localIsBackwardGrowEnabled"
                      label="Backward"
                      hide-details
                      :disabled="isInputDisabled"
                      data-testid="is-backward-grow-enabled"
                    ></v-checkbox>
                  </v-row>
                </v-col>
              </v-row>
            </div>
            <div class="ml-4 mt-5 mb-3 include-in-report-container-mobile">
              <div class="ml-n4 include-in-report-header">
                <span class="include-in-report-label" :class="{ 'include-in-report-disabled': isInputDisabled }">Include in Report</span>
              </div>
              <v-row class="ml-n7">
                <v-col cols="12" class="include-in-report-checkboxes">
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
                        :disabled="isIncSecondaryHeightDeactivated"
                        data-testid="inc-secondary-height"
                      ></v-checkbox>
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
            </div>
            <div class="ml-4 mt-10">
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
            <ActionPanel
              v-if="!isReadOnly"
              :isConfirmEnabled="isConfirmEnabled"
              :isConfirmed="isConfirmed"
              :hideClearButton="false"
              :hideEditButton="false"
              :showCancelButton="false"
              @clear="onClear"
              @confirm="onConfirm"
              @edit="onEdit"
            />
          </v-form>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>
<script setup lang="ts">
import { ref, watch, computed, type Ref } from 'vue'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS, MESSAGE, OPTIONS } from '@/constants'
import { reportInfoValidation } from '@/validation'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { AppSpinField } from '@/components'
import { ActionPanel } from '@/components/projection'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { saveProjectionOnPanelConfirm as saveModelParamProjection } from '@/services/projection/modelParameterService'
import { PROJECTION_ERR } from '@/constants/message'

const form = ref<HTMLFormElement>()

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const notificationStore = useNotificationStore()

const panelName = CONSTANTS.MODEL_PARAMETER_PANEL.REPORT_INFO
const panelOpenStates = computed(() => modelParameterStore.panelOpenStates)

const isReadOnly = computed(() => appStore.isReadOnly)
const isConfirmEnabled = computed(
  () => !isReadOnly.value && modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)
const isInputDisabled = computed(
  () => isReadOnly.value || !modelParameterStore.panelState[panelName].editable,
)

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
const localIsForwardGrowEnabled = ref<boolean>(modelParameterStore.isForwardGrowEnabled)
const localIsBackwardGrowEnabled = ref<boolean>(modelParameterStore.isBackwardGrowEnabled)
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

const utilizationSliderTickLabels = utilizationClassOptions.reduce(
  (acc, opt) => {
    acc[opt.index] = opt.label
    return acc
  },
  {} as Record<number, string>,
)

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

// Computed deactivated states
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

watch(() => modelParameterStore.isForwardGrowEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsForwardGrowEnabled.value)) {
    localIsForwardGrowEnabled.value = newVal
  }
})
watch(() => modelParameterStore.isBackwardGrowEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localIsBackwardGrowEnabled.value)) {
    localIsBackwardGrowEnabled.value = newVal
  }
})
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

watch(localIsForwardGrowEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(modelParameterStore.isForwardGrowEnabled)) {
    modelParameterStore.isForwardGrowEnabled = newVal
  }
})
watch(localIsBackwardGrowEnabled, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(modelParameterStore.isBackwardGrowEnabled)) {
    modelParameterStore.isBackwardGrowEnabled = newVal
  }
})
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
    await saveModelParamProjection(modelParameterStore, panelName)
  } catch (error) {
    console.error('Error saving projection:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, PROJECTION_ERR.SAVE_FAILED_TITLE)
    return
  } finally {
    appStore.isSavingProjection = false
  }

  if (!isConfirmed.value) {
    modelParameterStore.confirmPanel(panelName)
  }
}

const onEdit = () => {
  if (isConfirmed.value) {
    modelParameterStore.editPanel(panelName)
  }
}

const onClear = () => {
  modelParameterStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
  modelParameterStore.startingAge = null
  modelParameterStore.finishingAge = null
  modelParameterStore.ageIncrement = null
  modelParameterStore.startYear = null
  modelParameterStore.endYear = null
  modelParameterStore.yearIncrement = null
  modelParameterStore.isForwardGrowEnabled = false
  modelParameterStore.isBackwardGrowEnabled = false
  modelParameterStore.isComputedMAIEnabled = false
  modelParameterStore.isCulminationValuesEnabled = false
  modelParameterStore.isBySpeciesEnabled = false
  modelParameterStore.incSecondaryHeight = false
  modelParameterStore.reportTitle = null
  modelParameterStore.reportDescription = null
  modelParameterStore.projectionType = null
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

.include-in-report-checkboxes {
  padding-top: 1px; padding-bottom: 16px
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
</style>
