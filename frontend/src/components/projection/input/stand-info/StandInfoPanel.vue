<template>
  <v-card class="elevation-0">
    <v-expansion-panels v-model="panelOpenStates.standInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.standInfo === CONSTANTS.PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Stand Information</span>
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
            <v-row no-gutters class="form-fields-row stand-fields-row">
              <v-col class="stand-field-col">
                <AppSpinField
                  label="Percent Stockable Area (Required)"
                  :model-value="percentStockableArea"
                  :max="CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX"
                  :min="CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN"
                  :step="CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_STEP"
                  :persistent-placeholder="true"
                  placeholder=""
                  :hideDetails="true"
                  :disabled="isInputDisabled"
                  :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                  :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_DECIMAL_NUM"
                  :errorMessages="percentStockableAreaError"
                  data-testid="percent-stockable-area"
                  @update:modelValue="handlePercentStockableAreaUpdate"
                />
                <v-label
                  v-show="isEmptyOrZero(percentStockableArea) && !percentStockableAreaError"
                  style="font-size: var(--typography-font-size-label)"
                  >{{ MESSAGE.MDL_PRM_INPUT_HINT.SITE_DFT_COMPUTED }}</v-label
                >
              </v-col>
              <v-col class="stand-field-col">
                <AppSpinField
                  label="Crown Closure Percent"
                  :model-value="crownClosure"
                  :max="CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MAX"
                  :min="CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MIN"
                  :step="CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_STEP"
                  :persistent-placeholder="true"
                  :placeholder="crownClosurePlaceholder"
                  :hideDetails="true"
                  :disabled="isCrownClosureDisabled || !isConfirmEnabled"
                  :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                  :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_DECIMAL_NUM"
                  :errorMessages="crownClosureError"
                  data-testid="crown-closure"
                  @update:modelValue="handleCrownClosureUpdate"
                />
                <v-label
                  v-show="isZeroValue(crownClosure) && !isCrownClosureDisabled && !crownClosureError"
                  style="font-size: var(--typography-font-size-label)"
                  >{{ MESSAGE.MDL_PRM_INPUT_HINT.DENSITY_PCC_APPLY_DFT }}</v-label
                >
              </v-col>
              <v-col class="stand-field-col">
                <AppSpinField
                  :label="'Basal Area (m<sup>2</sup>/ha)'"
                  :model-value="basalArea"
                  :max="CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_MAX"
                  :min="CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_MIN"
                  :step="CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_STEP"
                  :persistent-placeholder="true"
                  :placeholder="basalAreaPlaceholder"
                  :hideDetails="true"
                  :disabled="isBasalAreaDisabled || !isConfirmEnabled"
                  :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                  :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_DECIMAL_NUM"
                  :errorMessages="basalAreaError"
                  data-testid="basal-area"
                  @update:modelValue="handleBasalAreaUpdate"
                />
                <v-label
                  v-show="siteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED && !basalAreaError"
                  style="font-size: var(--typography-font-size-label)"
                  >{{ MESSAGE.MDL_PRM_INPUT_HINT.DENSITY_WO_AGE }}</v-label
                >
              </v-col>
              <v-col class="stand-field-col">
                <AppSpinField
                  label="Tree Per Hectare"
                  :model-value="treesPerHectare"
                  :max="CONSTANTS.NUM_INPUT_LIMITS.TPH_MAX"
                  :min="CONSTANTS.NUM_INPUT_LIMITS.TPH_MIN"
                  :step="CONSTANTS.NUM_INPUT_LIMITS.TPH_STEP"
                  :persistent-placeholder="true"
                  :placeholder="treesPerHectarePlaceholder"
                  :hideDetails="true"
                  :disabled="isTreesPerHectareDisabled || !isConfirmEnabled"
                  :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                  :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.TPH_DECIMAL_NUM"
                  :errorMessages="treesPerHectareError"
                  data-testid="trees-per-hectare"
                  @update:modelValue="handleTreesPerHectareUpdate"
                />
              </v-col>
            </v-row>
            <div v-if="minDBHLimit" class="mt-4" data-testid="min-dbh-limit">
              <div class="min-dbh-label">Min DBH Limit</div>
              <div class="min-dbh-value">{{ minDBHLimit }}</div>
            </div>
            <ActionPanel
              v-if="!isReadOnly"
              class="action-panel"
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
import { ref, computed, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import { AppSpinField, AppButton } from '@/components'
import {
  ActionPanel,
} from '@/components/projection'
import { CONSTANTS, DEFAULTS, MESSAGE, OPTIONS } from '@/constants'
import { PROJECTION_ERR, VALIDATION_WARN } from '@/constants/message'
import type { SpeciesGroup } from '@/interfaces/interfaces'
import { standInfoValidation } from '@/validation'
import { isEmptyOrZero, isZeroValue } from '@/utils/util'
import { saveProjectionOnPanelConfirm, revertPanelToSaved, hasPanelUnsavedChanges } from '@/services/projection/modelParameterService'
import { useNotificationStore } from '@/stores/common/notificationStore'
import type { PanelName } from '@/types/types'

const form = ref<HTMLFormElement>()

const modelParameterStore = useModelParameterStore()
const appStore = useAppStore()
const alertDialogStore = useAlertDialogStore()
const notificationStore = useNotificationStore()

// Check if we're in read-only (view) mode
const isReadOnly = computed(() => appStore.isReadOnly)

const percentStockableAreaError = ref<string>('')
const crownClosureError = ref<string>('')
const basalAreaError = ref<string>('')
const treesPerHectareError = ref<string>('')

const {
  panelOpenStates,

  derivedBy,
  becZone,
  speciesGroups,
  selectedSiteSpecies,
  siteSpeciesValues,
  spzHeight,
  percentStockableArea,
  basalArea,
  treesPerHectare,
  minDBHLimit,
  currentDiameter,
  crownClosure,
} = storeToRefs(modelParameterStore)

const panelName = CONSTANTS.MANUAL_INPUT_PANEL.STAND_INFO
const isConfirmEnabled = computed(
  () => !isReadOnly.value && modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)

// Determine if inputs should be disabled (read-only mode or not editable)
const isInputDisabled = computed(
  () => isReadOnly.value || !modelParameterStore.panelState[panelName].editable,
)

const isCrownClosureDisabled = ref(false)
const isBasalAreaDisabled = ref(false)
const isTreesPerHectareDisabled = ref(false)

const basalAreaPlaceholder = ref('')
const treesPerHectarePlaceholder = ref('')
const crownClosurePlaceholder = ref('')

const updateBasalAreaState = (isEnabled: boolean, force: boolean = false) => {
  isBasalAreaDisabled.value = !isEnabled

  if (isBasalAreaDisabled.value) {
    basalAreaPlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.NA
    basalArea.value = null
  } else {
    basalAreaPlaceholder.value = ''
    if (force || basalArea.value === null) {
      basalArea.value = DEFAULTS.DEFAULT_VALUES.BASAL_AREA
    }
  }
}

const updateTreesPerHectareState = (isEnabled: boolean, force: boolean = false) => {
  isTreesPerHectareDisabled.value = !isEnabled

  if (isTreesPerHectareDisabled.value) {
    treesPerHectarePlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.NA
    treesPerHectare.value = null
  } else {
    treesPerHectarePlaceholder.value = ''
    if (force || treesPerHectare.value === null) {
      treesPerHectare.value = DEFAULTS.DEFAULT_VALUES.TPH
    }
  }
}

const updateCrownClosureState = (isVolume: boolean, isComputed: boolean, force: boolean = false) => {
  isCrownClosureDisabled.value = !(isVolume && isComputed)

  if (isCrownClosureDisabled.value) {
    crownClosurePlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.NA
    crownClosure.value = null
  } else {
    crownClosurePlaceholder.value = ''
    if (force || crownClosure.value === null) {
      crownClosure.value = '0'
    }
  }
}

const updateStates = (
  newDerivedBy: string | null,
  newSiteSpeciesValues: string | null,
  force: boolean = false,
) => {
  const isVolume = newDerivedBy === CONSTANTS.DERIVED_BY.VOLUME
  const isBasalArea = newDerivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA
  const isComputed =
    newSiteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED

  updateBasalAreaState(isBasalArea && isComputed, force)
  updateTreesPerHectareState(isBasalArea && isComputed, force)
  updateCrownClosureState(isVolume, isComputed, force)
}

watch(
  [derivedBy, siteSpeciesValues],
  ([newDerivedBy, newSiteSpeciesValues]) => {
    updateStates(newDerivedBy, newSiteSpeciesValues)
  },
  { immediate: true },
)

// Auto-populate minDBHLimit based on selectedSiteSpecies (used in validation)
watch(
  selectedSiteSpecies,
  (newSiteSpecies) => {
    if (!newSiteSpecies) {
      minDBHLimit.value = null
      return
    }

    const group = speciesGroups.value.find(
      (g: SpeciesGroup) => g.siteSpecies === newSiteSpecies
    )

    if (group?.minimumDBHLimit) {
      const option = OPTIONS.utilizationClassOptions.find(
        (opt) => opt.value === group.minimumDBHLimit
      )
      minDBHLimit.value = option?.label ?? null
    }
  },
  { immediate: true },
)

// Compute and store currentDiameter for API use
watch([basalArea, treesPerHectare], ([newBA, newTPH]) => {
  let fDiam = 0

  const fBA = Number.parseFloat(newBA ?? '0') || 0
  const fTPH = Number.parseFloat(newTPH ?? '0') || 0

  try {
    if (fBA > 0 && fTPH > 0) {
      fDiam = Math.sqrt(fBA / fTPH / 0.00007854)
    }

    currentDiameter.value = fDiam.toFixed(1) + ' cm'
  } catch (error) {
    console.warn('Error calculating diameter:', error)
    currentDiameter.value = '0.0 cm'
  }
})

const handlePercentStockableAreaUpdate = (value: string | null) => {
  percentStockableArea.value = value
  percentStockableAreaError.value = ''
}

const handleCrownClosureUpdate = (value: string | null) => {
  crownClosure.value = value
  crownClosureError.value = ''
}

const handleBasalAreaUpdate = (value: string | null) => {
  basalArea.value = value
  basalAreaError.value = ''
}

const handleTreesPerHectareUpdate = (value: string | null) => {
  treesPerHectare.value = value
  treesPerHectareError.value = ''
}

const validateRange = (): boolean => {
  const result = standInfoValidation.validateRange(
    percentStockableArea.value,
    basalArea.value,
    treesPerHectare.value,
    crownClosure.value,
  )

  if (!result.isValid) {
    switch (result.errorType) {
      case 'percentStockableArea':
        percentStockableAreaError.value = MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_PCT_STCB_AREA_RNG
        break
      case 'basalArea':
        basalAreaError.value = MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_BSL_AREA_RNG
        break
      case 'treesPerHectare':
        treesPerHectareError.value = MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_TPH_RNG
        break
      case 'crownClosure':
        crownClosureError.value = MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_CROWN_CLOSURE_RNG
        break
    }
    return false
  }

  return true
}

const validateFormInputs = async (): Promise<boolean> => {
  if (!validateRange()) {
    return false
  }
  const isBALimitsValid = standInfoValidation.validateBALimits(
    selectedSiteSpecies.value,
    becZone.value,
    basalArea.value,
    spzHeight.value,
  )

  if (isBALimitsValid) {
    const validateTPHmessage = standInfoValidation.validateTPHLimits(
      basalArea.value,
      treesPerHectare.value,
      spzHeight.value,
      selectedSiteSpecies.value,
      becZone.value,
    )
    if (validateTPHmessage) {
      const confirmed = await alertDialogStore.openDialog(
        MESSAGE.MSG_DIALOG_TITLE.CONFIRM,
        validateTPHmessage,
        { width: 400 },
      )
      if (!confirmed) {
        return false
      }
    }
  } else {
    const confirmed = await alertDialogStore.openDialog(
      MESSAGE.MSG_DIALOG_TITLE.CONFIRM,
      MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_BSL_AREA_OVER_HEIGHT,
      { width: 400 },
    )
    if (!confirmed) {
      return false
    }
  }

  const validateQuadDiamMessage = standInfoValidation.validateQuadDiameter(
    basalArea.value,
    treesPerHectare.value,
    minDBHLimit.value,
  )
  if (validateQuadDiamMessage) {
    const confirmed = await alertDialogStore.openDialog(
      MESSAGE.MSG_DIALOG_TITLE.CONFIRM,
      validateQuadDiamMessage,
      { width: 400 },
    )
    if (!confirmed) {
      return false
    }
  }

  return true
}

const formattingValues = (): void => {
  if (basalArea.value) {
    basalArea.value = Number.parseFloat(basalArea.value).toFixed(
      CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_DECIMAL_NUM,
    )
  }

  if (treesPerHectare.value) {
    treesPerHectare.value = Number.parseFloat(treesPerHectare.value).toFixed(
      CONSTANTS.NUM_INPUT_LIMITS.TPH_DECIMAL_NUM,
    )
  }
}

const onConfirm = async () => {
  percentStockableAreaError.value = ''
  crownClosureError.value = ''
  basalAreaError.value = ''
  treesPerHectareError.value = ''

  if (percentStockableArea.value === null || percentStockableArea.value === '') {
    percentStockableAreaError.value = MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_PCT_STCB_AREA_REQ
    return
  }

  const isFormValid = await validateFormInputs()

  if (!isFormValid) {
    return
  }

  if (form.value) {
    form.value.validate()
  } else {
    console.warn(VALIDATION_WARN.FORM_REF_NULL)
  }

  if (isEmptyOrZero(percentStockableArea.value)) {
    percentStockableArea.value = '0'
  }

  formattingValues()

  // Save projection (create or update) before confirming the panel
  appStore.isSavingProjection = true
  try {
    await saveProjectionOnPanelConfirm(modelParameterStore, panelName)
  } catch (error) {
    console.error(PROJECTION_ERR.SAVE_ERROR_LOG, error)
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, PROJECTION_ERR.SAVE_FAILED_TITLE)
    return
  } finally {
    appStore.isSavingProjection = false
  }

  if (!isConfirmed.value) {
    modelParameterStore.confirmPanel(panelName)
  }
}

// Edit button in header
const isHeaderEditActive = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) return false
  return isConfirmed.value && !modelParameterStore.panelState[panelName].editable
})

const editTooltipText = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.READY) {
    return MESSAGE.EDIT_SECTION_TOOLTIP.RESTRICTED_BY_STATUS(status)
  }
  if (isConfirmed.value && !modelParameterStore.panelState[panelName].editable) {
    return MESSAGE.EDIT_SECTION_TOOLTIP.CLICK_TO_EDIT
  }
  return ''
})

const getEditablePanel = (): string | null => {
  const panelsToCheck = [
    CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO,
    CONSTANTS.MANUAL_INPUT_PANEL.SITE_INFO,
    CONSTANTS.MANUAL_INPUT_PANEL.STAND_INFO,
    CONSTANTS.MANUAL_INPUT_PANEL.REPORT_SETTINGS,
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
          console.error(PROJECTION_ERR.REVERT_ERROR_LOG, error)
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

const onCancel = async () => {
  appStore.isSavingProjection = true
  try {
    await revertPanelToSaved(panelName)
  } catch (error) {
    console.error(PROJECTION_ERR.REVERT_ERROR_LOG, error)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
  } finally {
    appStore.isSavingProjection = false
  }
}
</script>

<style scoped>

.action-panel {
  margin-top: 16px;
}

.stand-fields-row {
  row-gap: 16px;
}

.stand-field-col {
  flex: 1 1 100%;
  min-width: 0;
}

@media (min-width: 600px) {
  .stand-field-col {
    flex: 0 0 calc(50% - 8px);
    max-width: calc(50% - 8px);
  }
}

@media (min-width: 960px) {
  .stand-field-col {
    flex: 1;
    max-width: none;
  }
}

.min-dbh-label {
  font-size: var(--typography-font-size-label);
  color: var(--typography-color-secondary);
}

.min-dbh-value {
  font-size: var(--typography-font-size-body);
  color: var(--typography-color-primary);
  margin-top: 4px;
  margin-left: 4px;
  padding-left: 4px;
}

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
</style>
