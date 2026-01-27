<template>
  <v-card class="elevation-4">
    <AppMessageDialog
      :dialog="messageDialog.dialog"
      :title="messageDialog.title"
      :message="messageDialog.message"
      :dialogWidth="messageDialog.dialogWidth"
      :btnLabel="messageDialog.btnLabel"
      :variant="messageDialog.variant"
      @update:dialog="(value) => (messageDialog.dialog = value)"
      @close="handleDialogClose"
    />

    <v-expansion-panels v-model="panelOpenStates.standInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <!-- Place an arrow icon to the left of the title -->
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
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text mt-n2">
          <v-form ref="form">
            <v-row style="height: 70px !important">
              <v-col cols="6">
                <v-row class="mb-2">
                  <v-col cols="6">
                    <AppSpinField
                      label="% Stockable Area"
                      :model-value="percentStockableArea"
                      :max="
                        CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX
                      "
                      :min="
                        CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN
                      "
                      :step="
                        CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_STEP
                      "
                      :persistent-placeholder="true"
                      placeholder=""
                      :hideDetails="true"
                      :disabled="isInputDisabled"
                      :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                      :decimalAllowNumber="
                        CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_DECIMAL_NUM
                      "
                      data-testid="percent-stockable-area"
                      @update:modelValue="handlePercentStockableAreaUpdate"
                    />
                    <v-label
                      v-show="isEmptyOrZero(percentStockableArea)"
                      style="font-size: var(--typography-font-size-label)"
                      >{{
                        MESSAGE.MDL_PRM_INPUT_HINT.SITE_DFT_COMPUTED
                      }}</v-label
                    >
                  </v-col>
                  <v-col class="col-space-6" />
                  <v-col>
                    <AppSpinField
                      label="Crown Closure (%)"
                      :model-value="crownClosure"
                      :max="CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MAX"
                      :min="CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MIN"
                      :step="CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_STEP"
                      :persistent-placeholder="true"
                      :placeholder="crownClosurePlaceholder"
                      :hideDetails="true"
                      :disabled="isCrownClosureDisabled || !isConfirmEnabled"
                      :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                      :decimalAllowNumber="
                        CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_DECIMAL_NUM
                      "
                      data-testid="crown-closure"
                      @update:modelValue="handleCrownClosureUpdate"
                    />
                    <v-label
                      v-show="
                        isZeroValue(crownClosure) && !isCrownClosureDisabled
                      "
                      style="font-size: var(--typography-font-size-label)"
                      >{{
                        MESSAGE.MDL_PRM_INPUT_HINT.DENSITY_PCC_APPLY_DFT
                      }}</v-label
                    >
                  </v-col>
                </v-row>
              </v-col>
            </v-row>
            <v-row class="mt-10">
              <v-col cols="6">
                <v-row class="mb-2">
                  <v-col cols="6">
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
                      :decimalAllowNumber="
                        CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_DECIMAL_NUM
                      "
                      data-testid="basal-area"
                      @update:modelValue="handleBasalAreaUpdate"
                    >
                    </AppSpinField>
                    <v-label
                      v-show="
                        siteSpeciesValues ===
                        CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
                      "
                      style="font-size: var(--typography-font-size-label)"
                      >{{ MESSAGE.MDL_PRM_INPUT_HINT.DENSITY_WO_AGE }}</v-label
                    >
                  </v-col>
                  <v-col class="col-space-6" />
                  <v-col>
                    <AppSpinField
                      label="TPH (tree/ha)"
                      :model-value="treesPerHectare"
                      :max="CONSTANTS.NUM_INPUT_LIMITS.TPH_MAX"
                      :min="CONSTANTS.NUM_INPUT_LIMITS.TPH_MIN"
                      :step="CONSTANTS.NUM_INPUT_LIMITS.TPH_STEP"
                      :persistent-placeholder="true"
                      :placeholder="treesPerHectarePlaceholder"
                      :hideDetails="true"
                      :disabled="isTreesPerHectareDisabled || !isConfirmEnabled"
                      :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                      :decimalAllowNumber="
                        CONSTANTS.NUM_INPUT_LIMITS.TPH_DECIMAL_NUM
                      "
                      data-testid="trees-per-hectare"
                      @update:modelValue="handleTreesPerHectareUpdate"
                    />
                  </v-col>
                </v-row>
              </v-col>
              <v-col class="col-space-6" />
              <v-col>
                <v-row>
                  <v-col cols="6">
                    <label class="bcds-text-field-label" for="minDBHLimit">Min DBH Limit</label>
                    <v-text-field
                      id="minDBHLimit"
                      label="Min DBH Limit"
                      :model-value="minDBHLimit"
                      variant="underlined"
                      disabled
                      data-testid="min-dbh-limit"
                    ></v-text-field>
                  </v-col>
                  <v-col class="col-space-6" />
                  <v-col v-if="isCurrentDiameterVisibled">
                    <label class="bcds-text-field-label" for="current-diameter">Current Diameter</label>
                    <v-text-field
                      id="current-diameter"
                      label="Current Diameter"
                      :model-value="currentDiameter"
                      variant="underlined"
                      disabled
                      data-testid="current-diameter"
                    ></v-text-field>
                  </v-col>
                </v-row>
              </v-col>
            </v-row>
            <ActionPanel
              v-if="!isReadOnly"
              :isConfirmEnabled="isConfirmEnabled"
              :isConfirmed="isConfirmed"
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
import { ref, computed, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import { AppMessageDialog, AppSpinField } from '@/components'
import {
  ActionPanel,
} from '@/components/projection'
import { CONSTANTS, DEFAULTS, MESSAGE } from '@/constants'
import type { MessageDialog } from '@/interfaces/interfaces'
import { standInfoValidation } from '@/validation'
import { isEmptyOrZero, isZeroValue } from '@/utils/util'

const form = ref<HTMLFormElement>()

const modelParameterStore = useModelParameterStore()
const appStore = useAppStore()
const alertDialogStore = useAlertDialogStore()

// Check if we're in read-only (view) mode
const isReadOnly = computed(() => appStore.isReadOnly)

const messageDialog = ref<MessageDialog>({
  dialog: false,
  title: '',
  message: '',
})

const {
  panelOpenStates,

  derivedBy,
  becZone,
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

const panelName = CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO
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
const isCurrentDiameterVisibled = ref(false)

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

const updateCurrentDiameterState = (
  isBasalArea: boolean,
  isComputed: boolean,
) => {
  isCurrentDiameterVisibled.value = isBasalArea && isComputed
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
  updateCurrentDiameterState(isBasalArea, isComputed)
}

watch(
  [derivedBy, siteSpeciesValues],
  ([newDerivedBy, newSiteSpeciesValues]) => {
    updateStates(newDerivedBy, newSiteSpeciesValues)
  },
  { immediate: true },
)

watch([basalArea, treesPerHectare], ([newBA, newTPH]) => {
  let fDiam = 0

  const fBA = Number.parseFloat(newBA ?? '0') || 0
  const fTPH = Number.parseFloat(newTPH ?? '0') || 0

  try {
    if (fBA > 0 && fTPH > 0) {
      fDiam = Math.sqrt(fBA / fTPH / 0.00007854)
    }

    // Format diameter to 1 decimal place and append ' cm' (mimicking Format$)
    currentDiameter.value = fDiam.toFixed(1) + ' cm'
  } catch (error) {
    console.warn('Error calculating diameter:', error)
    currentDiameter.value = '0.0 cm' // Fallback to 0 on error
  }
})

const handlePercentStockableAreaUpdate = (value: string | null) => {
  percentStockableArea.value = value
}

const handleCrownClosureUpdate = (value: string | null) => {
  crownClosure.value = value
}

const handleBasalAreaUpdate = (value: string | null) => {
  basalArea.value = value
}

const handleTreesPerHectareUpdate = (value: string | null) => {
  treesPerHectare.value = value
}

const validateRange = (): boolean => {
  const result = standInfoValidation.validateRange(
    percentStockableArea.value,
    basalArea.value,
    treesPerHectare.value,
    crownClosure.value,
  )

  if (!result.isValid) {
    let message = ''
    switch (result.errorType) {
      case 'percentStockableArea':
        message = MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_PCT_STCB_AREA_RNG
        break
      case 'basalArea':
        message = MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_BSL_AREA_RNG
        break
      case 'treesPerHectare':
        message = MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_TPH_RNG
        break
      case 'crownClosure':
        message = MESSAGE.MDL_PRM_INPUT_ERR.DENSITY_VLD_CROWN_CLOSURE_RNG
        break
    }
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: message,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      variant: 'error',
    }
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
  const isFormValid = await validateFormInputs()

  if (!isFormValid) {
    return
  }

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
  }

  if (isEmptyOrZero(percentStockableArea.value)) {
    percentStockableArea.value = '0'
  }

  formattingValues()

  // this panel is not in a confirmed state
  if (!isConfirmed.value) {
    modelParameterStore.confirmPanel(panelName)
  }
}

const onEdit = () => {
  // this panel has already been confirmed.
  if (isConfirmed.value) {
    modelParameterStore.editPanel(panelName)
  }
}

const onClear = () => {
  updateStates(derivedBy.value, siteSpeciesValues.value, true)
  percentStockableArea.value = DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA
}

const handleDialogClose = () => {}
</script>

<style scoped />
