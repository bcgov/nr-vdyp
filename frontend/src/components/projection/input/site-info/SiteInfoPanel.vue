<template>
  <v-card class="elevation-0">
    <v-expansion-panels v-model="panelOpenStates.siteInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.siteInfo === CONSTANTS.PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Site Information</span>
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
            <div>
              <v-row no-gutters class="form-fields-row mb-4">
                <v-col cols="12" sm="6" md="3" class="bec-col">
                  <label class="bcds-select-label" for="bec-zone-select">BEC Zone (Required)</label>
                  <v-select
                    id="bec-zone-select"
                    :items="OPTIONS.becZoneOptions"
                    v-model="becZone"
                    item-title="label"
                    item-value="value"
                    hide-details="auto"
                    persistent-placeholder
                    placeholder="Select Bec Zone"
                    :disabled="isInputDisabled"
                    append-inner-icon="mdi-chevron-down"
                    :error="!!becZoneError"
                    :error-messages="becZoneError"
                    @update:modelValue="becZoneError = ''"
                  ></v-select>
                </v-col>
                <v-col cols="12" sm="6" md="3" class="eco-col">
                  <label class="bcds-select-label" for="eco-zone-select">Eco Zone</label>
                  <v-select
                    id="eco-zone-select"
                    :items="OPTIONS.ecoZoneOptions"
                    v-model="ecoZone"
                    item-title="label"
                    item-value="value"
                    clearable
                    hide-details="auto"
                    persistent-placeholder
                    placeholder="Select Eco Zone"
                    :disabled="isInputDisabled"
                    append-inner-icon="mdi-chevron-down"
                  ></v-select>
                </v-col>
                <v-col cols="12" md="6" class="site-index-container">
                  <label class="bcds-radio-label" for="siteIndex">Site Index:</label>
                  <v-radio-group
                    id="siteIndex"
                    v-model="siteSpeciesValues"
                    inline
                    hide-details
                    :disabled="isSiteSpeciesValueDisabled || !isConfirmEnabled"
                    @update:modelValue="siteIndexError = ''"
                  >
                    <v-radio
                      :key="OPTIONS.siteSpeciesValuesOptions[0].value"
                      :label="OPTIONS.siteSpeciesValuesOptions[0].label"
                      :value="OPTIONS.siteSpeciesValuesOptions[0].value"
                    ></v-radio>
                    <v-radio
                      :key="OPTIONS.siteSpeciesValuesOptions[1].value"
                      :label="OPTIONS.siteSpeciesValuesOptions[1].label"
                      :value="OPTIONS.siteSpeciesValuesOptions[1].value"
                    ></v-radio>
                  </v-radio-group>
                  <div v-if="siteIndexError" class="site-info-error">{{ siteIndexError }}</div>
                </v-col>
              </v-row>
              <template v-if="!showNewSiteIndicesFeature">
                <div class="hr-line"></div>
                <v-row class="mt-0 mb-3">
                  <v-col cols="6">
                    <v-row>
                      <v-col cols="6">
                        <label class="bcds-select-label" for="site-species-select">Site Species</label>
                        <v-select
                          id="site-species-select"
                          :items="siteSpeciesOptions"
                          v-model="selectedSiteSpecies"
                          item-title="label"
                          item-value="value"
                          hide-details="auto"
                          persistent-placeholder
                          placeholder="Select..."
                          data-testid="selected-site-species"
                          disabled
                          append-inner-icon="mdi-chevron-down"
                        ></v-select>
                      </v-col>
                    </v-row>
                  </v-col>
                </v-row>
                <div class="hr-line"></div>
                <v-row class="mt-1">
                  <v-col cols="auto">
                    <label class="bcds-radio-label" for="ageYears">Age Years:</label>
                    <v-radio-group
                      id="ageYears"
                      v-model="ageType"
                      inline
                      hide-details
                      :disabled="isAgeTypeDisabled || !isConfirmEnabled"
                    >
                      <v-radio
                        :key="OPTIONS.ageTypeOptions[0].value"
                        :label="OPTIONS.ageTypeOptions[0].label"
                        :value="OPTIONS.ageTypeOptions[0].value"
                      ></v-radio>
                      <v-radio
                        :key="OPTIONS.ageTypeOptions[1].value"
                        :label="OPTIONS.ageTypeOptions[1].label"
                        :value="OPTIONS.ageTypeOptions[1].value"
                      ></v-radio>
                    </v-radio-group>
                  </v-col>
                </v-row>
                <v-row no-gutters class="form-fields-row mt-0">
                  <v-col cols="6">
                    <v-row no-gutters class="form-fields-row mb-2">
                      <v-col cols="6">
                        <AppSpinField
                          label="Years"
                          :model-value="spzAge"
                          :max="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MAX"
                          :min="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MIN"
                          :step="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_STEP"
                          :persistent-placeholder="true"
                          :placeholder="spzAgePlaceholder"
                          :hideDetails="true"
                          :disabled="isSpzAgeDisabled || !isConfirmEnabled"
                          :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                          :decimalAllowNumber="
                            CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_DECIMAL_NUM
                          "
                          data-testid="spz-age"
                          @update:modelValue="handleSpzAgeUpdate"
                        />
                        <v-label
                          v-show="isZeroValue(spzAge)"
                          style="font-size: var(--typography-font-size-label)"
                          >{{
                            MESSAGE.MDL_PRM_INPUT_HINT.SITE_ZERO_NOT_KNOW
                          }}</v-label
                        >
                      </v-col>
                      <v-col>
                        <AppSpinField
                          label="Height in Meters"
                          :model-value="spzHeight"
                          :max="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MAX"
                          :min="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MIN"
                          :step="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_STEP"
                          :persistent-placeholder="true"
                          :placeholder="spzHeightPlaceholder"
                          :hideDetails="true"
                          :disabled="isSpzHeightDisabled || !isConfirmEnabled"
                          :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                          :decimalAllowNumber="
                            CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_DECIMAL_NUM
                          "
                          data-testid="spz-height"
                          @update:modelValue="handleSpzHeightUpdate"
                        />
                        <v-label
                          v-show="isZeroValue(spzHeight)"
                          style="font-size: var(--typography-font-size-label)"
                          >{{
                            MESSAGE.MDL_PRM_INPUT_HINT.SITE_ZERO_NOT_KNOW
                          }}</v-label
                        >
                      </v-col>
                    </v-row>
                  </v-col>
                  <v-col>
                    <v-row>
                      <v-col cols="6">
                        <AppSpinField
                          label="BHA 50 Site Index"
                          :model-value="bha50SiteIndex"
                          :max="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX"
                          :min="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN"
                          :step="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_STEP"
                          :persistent-placeholder="true"
                          :placeholder="bha50SiteIndexPlaceholder"
                          :hideDetails="true"
                          :disabled="
                            isBHA50SiteIndexDisabled || !isConfirmEnabled
                          "
                          :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                          :decimalAllowNumber="
                            CONSTANTS.NUM_INPUT_LIMITS
                              .BHA50_SITE_INDEX_DECIMAL_NUM
                          "
                          data-testid="bha-50-site-index"
                          @update:modelValue="handleBha50SiteIndexUpdate"
                        />
                        <v-label
                          v-show="isZeroValue(bha50SiteIndex)"
                          style="font-size: var(--typography-font-size-label)"
                          >{{
                            MESSAGE.MDL_PRM_INPUT_HINT.SITE_ZERO_NOT_KNOW
                          }}</v-label
                        >
                      </v-col>
                    </v-row>
                  </v-col>
                </v-row>
                <div v-if="siteFieldsError" class="site-info-error mt-2">{{ siteFieldsError }}</div>
              </template>
              <template v-else>
                <SiteIndicesTable :is-confirm-enabled="isConfirmEnabled" />
                <div v-if="siteFieldsError" class="site-info-error mt-2">{{ siteFieldsError }}</div>
              </template>
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
import { AppSpinField, AppButton } from '@/components'
import {
  ActionPanel,
} from '@/components/projection'
import SiteIndicesTable from './SiteIndicesTable.vue'
import type { SpeciesGroup, SiteIndexSpeciesRow } from '@/interfaces/interfaces'
import { CONSTANTS, OPTIONS, DEFAULTS, MESSAGE } from '@/constants'
import { PROJECTION_ERR } from '@/constants/message'
import { siteInfoValidation } from '@/validation'
import { isZeroValue, isEmptyOrZero } from '@/utils/util'
import { saveProjectionOnPanelConfirm, revertPanelToSaved, hasPanelUnsavedChanges } from '@/services/projection/modelParameterService'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import type { PanelName } from '@/types/types'

import { env } from '@/env'

const showNewSiteIndicesFeature = env.VITE_SITE_INDICES_TABLE_ENABLED === 'true'

const form = ref<HTMLFormElement>()

const modelParameterStore = useModelParameterStore()
const appStore = useAppStore()
const notificationStore = useNotificationStore()
const alertDialogStore = useAlertDialogStore()

// Check if we're in read-only (view) mode
const isReadOnly = computed(() => appStore.isReadOnly)

const becZoneError = ref<string>('')
const siteIndexError = ref<string>('')
const siteFieldsError = ref<string>('')

const {
  panelOpenStates,
  derivedBy,
  speciesGroups,
  highestPercentSpecies,
  selectedSiteSpecies,
  becZone,
  ecoZone,
  siteSpeciesValues,
  ageType,
  spzAge,
  spzHeight,
  bha50SiteIndex,
  siteIndexRows,
} = storeToRefs(modelParameterStore)

const panelName = CONSTANTS.MANUAL_INPUT_PANEL.SITE_INFO
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

const siteSpeciesOptions = computed(() =>
  speciesGroups.value.map((group: SpeciesGroup) => ({
    label: group.siteSpecies,
    value: group.siteSpecies,
  })),
)

const isSiteSpeciesValueDisabled = ref(false)
const isAgeTypeDisabled = ref(false)
const isSpzAgeDisabled = ref(false)
const isSpzHeightDisabled = ref(false)
const isBHA50SiteIndexDisabled = ref(false)

const spzAgePlaceholder = ref('')
const spzHeightPlaceholder = ref('')
const bha50SiteIndexPlaceholder = ref('')

const handleSiteSpeciesValuesState = (newSiteSpeciesValues: string | null, force: boolean = false) => {
  if (newSiteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED) {
    isAgeTypeDisabled.value = false
    isSpzAgeDisabled.value = false
    isSpzHeightDisabled.value = false
    isBHA50SiteIndexDisabled.value = true

    if (force || spzAge.value === null) {
      spzAge.value = DEFAULTS.DEFAULT_VALUES.SPZ_AGE
    }
    if (force || spzHeight.value === null) {
      spzHeight.value = DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
    }
    bha50SiteIndex.value = ''

    spzAgePlaceholder.value = ''
    spzHeightPlaceholder.value = ''
    bha50SiteIndexPlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.COMPUTED
  } else if (newSiteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED) {
    isAgeTypeDisabled.value = true
    isSpzAgeDisabled.value = true
    isSpzHeightDisabled.value = true
    isBHA50SiteIndexDisabled.value = false

    spzAge.value = null
    spzHeight.value = null
    if (force || bha50SiteIndex.value === null) {
      bha50SiteIndex.value = DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
    }

    spzAgePlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.NA
    spzHeightPlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.NA
    bha50SiteIndexPlaceholder.value = ''
  }
}

const handleDerivedByChange = (
  newDerivedBy: string | null,
  newSiteSpecies: string | null,
  newSiteSpeciesValues: string | null,
) => {
  if (!newDerivedBy) return

  if (newDerivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA) {
    isSiteSpeciesValueDisabled.value =
      newSiteSpecies !== highestPercentSpecies.value
  }
  handleSiteSpeciesValuesState(newSiteSpeciesValues)
}

watch(isConfirmEnabled, (enabled) => {
  if (enabled && siteSpeciesValues.value === null) {
    siteSpeciesValues.value = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
  }
}, { immediate: true })

watch(
  [derivedBy, selectedSiteSpecies, siteSpeciesValues],
  ([newDerivedBy, newSiteSpecies, newSiteSpeciesValues]) => {
    handleDerivedByChange(newDerivedBy, newSiteSpecies, newSiteSpeciesValues)
  },
  { immediate: true },
)

const handleSpzAgeUpdate = (value: string | null) => {
  spzAge.value = value
}

const handleSpzHeightUpdate = (value: string | null) => {
  spzHeight.value = value
}

const handleBha50SiteIndexUpdate = (value: string | null) => {
  bha50SiteIndex.value = value
}

const formattingValues = (): void => {
  if (bha50SiteIndex.value) {
    bha50SiteIndex.value = Number.parseFloat(bha50SiteIndex.value).toFixed(
      CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_DECIMAL_NUM,
    )
  }
}

const syncPrimaryRowToStore = () => {
  const primaryRow = siteIndexRows.value[0]
  if (!primaryRow) return

  ageType.value = primaryRow.ageType

  if (siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED) {
    if (primaryRow.computedValue === CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX) {
      spzAge.value = primaryRow.age
      spzHeight.value = primaryRow.height
      bha50SiteIndex.value = ''
    } else if (primaryRow.computedValue === CONSTANTS.COMPUTED_VALUE.HEIGHT) {
      spzAge.value = primaryRow.age
      spzHeight.value = null
      bha50SiteIndex.value = primaryRow.bhaSiteIndex
    } else if (primaryRow.computedValue === CONSTANTS.COMPUTED_VALUE.TOTAL_AGE) {
      spzAge.value = null
      spzHeight.value = primaryRow.height
      bha50SiteIndex.value = primaryRow.bhaSiteIndex
    }
  } else if (siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED) {
    spzAge.value = null
    spzHeight.value = null
    bha50SiteIndex.value = primaryRow.bhaSiteIndex
  }
}

const getPreConfirmErrorMessage = (): string | null => {
  const result = siteInfoValidation.validatePreConfirmFields(siteSpeciesValues.value, becZone.value)
  if (result.isValid) return null
  if (result.errorType === 'siteIndex') return MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SITE_INDEX_REQ
  if (result.errorType === 'becZone') return MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_BEC_ZONE_REQ
  return null
}

const isRowDisabledInNewUI = (index: number): boolean =>
  siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED &&
  derivedBy.value === CONSTANTS.DERIVED_BY.VOLUME &&
  index > 0

const getRequiredPairError = (
  missingPrimary: boolean,
  missingBha: boolean,
  speciesCode: string,
): string | null => {
  if (missingBha && !missingPrimary) return MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_SI_VAL(speciesCode)
  if (missingPrimary) return MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_VALS_SUP(speciesCode)
  return null
}

// The field matching computedValue is intentionally null (Calc.) - skip it, validate the other two
const getComputedRowError = (row: SiteIndexSpeciesRow): string | null => {
  const cv = row.computedValue
  if (cv === CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX) {
    if (isEmptyOrZero(row.age) || isEmptyOrZero(row.height))
      return MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_VALS_SUP(row.speciesCode)
  } else if (cv === CONSTANTS.COMPUTED_VALUE.HEIGHT) {
    return getRequiredPairError(isEmptyOrZero(row.age), isEmptyOrZero(row.bhaSiteIndex), row.speciesCode)
  } else if (cv === CONSTANTS.COMPUTED_VALUE.TOTAL_AGE) {
    return getRequiredPairError(isEmptyOrZero(row.height), isEmptyOrZero(row.bhaSiteIndex), row.speciesCode)
  }
  return null
}

const getNewUIRequiredError = (): string | null => {
  const isComputed = siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
  for (let i = 0; i < siteIndexRows.value.length; i++) {
    if (isRowDisabledInNewUI(i)) continue
    const row = siteIndexRows.value[i]
    if (isComputed) {
      const err = getComputedRowError(row)
      if (err) return err
    } else if (isEmptyOrZero(row.bhaSiteIndex)) {
      return MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_SI_VAL(row.speciesCode)
    }
  }
  return null
}

const getRequiredFieldsErrorMessage = (): string | null => {
  if (!showNewSiteIndicesFeature) {
    // Old UI: COMPUTED needs Age + Height, SUPPLIED needs BHA
    const result = siteInfoValidation.validateRequiredFields(
      siteSpeciesValues.value,
      spzAge.value,
      spzHeight.value,
      bha50SiteIndex.value,
    )
    if (result.isValid) return null
    return siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      ? MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_VALS_SUP(selectedSiteSpecies.value)
      : MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_SI_VAL(selectedSiteSpecies.value)
  }
  return getNewUIRequiredError()
}

const getRangeErrorFromType = (errorType: string | undefined): string | null => {
  if (errorType === 'spzAge') return MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_AGE_RNG
  if (errorType === 'spzHeight') return MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_HIGHT_RNG
  if (errorType === 'bha50SiteIndex') return MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SI_RNG
  return null
}

const getNewUIRangeError = (): string | null => {
  for (let i = 0; i < siteIndexRows.value.length; i++) {
    if (isRowDisabledInNewUI(i)) continue
    const row = siteIndexRows.value[i]
    const result = siteInfoValidation.validateRange(row.age, row.height, row.bhaSiteIndex)
    if (!result.isValid) return getRangeErrorFromType(result.errorType)
  }
  return null
}

const getRangeErrorMessage = (): string | null => {
  if (showNewSiteIndicesFeature) return getNewUIRangeError()
  const result = siteInfoValidation.validateRange(spzAge.value, spzHeight.value, bha50SiteIndex.value)
  if (result.isValid) return null
  return getRangeErrorFromType(result.errorType)
}

const onConfirm = async () => {
  if (showNewSiteIndicesFeature) syncPrimaryRowToStore()

  siteIndexError.value = ''
  siteFieldsError.value = ''

  if (!becZone.value) {
    becZoneError.value = MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_BEC_ZONE_REQ
    return
  }
  becZoneError.value = ''

  const preConfirmError = getPreConfirmErrorMessage()
  if (preConfirmError) {
    siteIndexError.value = preConfirmError
    return
  }

  const requiredError = getRequiredFieldsErrorMessage()
  if (requiredError) {
    siteFieldsError.value = requiredError
    return
  }

  const rangeError = getRangeErrorMessage()
  if (rangeError) {
    siteFieldsError.value = rangeError
    return
  }

  if (form.value) form.value.validate()
  else console.warn('Form reference is null. Validation skipped.')

  formattingValues()

  // Save projection (create or update) before confirming the panel
  appStore.isSavingProjection = true
  try {
    await saveProjectionOnPanelConfirm(modelParameterStore, panelName)
  } catch (error) {
    console.error('Error saving projection:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, PROJECTION_ERR.SAVE_FAILED_TITLE)
    return
  } finally {
    appStore.isSavingProjection = false
  }

  if (!isConfirmed.value) modelParameterStore.confirmPanel(panelName)
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
    return `This section may not be edited with a status of ${status}`
  }
  if (isConfirmed.value && !modelParameterStore.panelState[panelName].editable) {
    return 'Click Edit to make changes to this section'
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

const onCancel = async () => {
  appStore.isSavingProjection = true
  try {
    await revertPanelToSaved(panelName)
  } catch (error) {
    console.error('Error reverting panel to saved state:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
  } finally {
    appStore.isSavingProjection = false
  }
}

</script>

<style scoped>
.site-info-error {
  color: rgb(var(--v-theme-error));
  font-size: 0.75rem;
  font-weight: 400;
  line-height: 1rem;
  white-space: pre-line;
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

.action-panel {
  margin-top: 16px;
}

.site-index-container {
  margin-top: 16px !important;
}

@media (min-width: 960px) {
  .bec-col {
    max-width: 375px;
    flex: 0 0 360px !important;
  }
  .eco-col {
    max-width: 230px;
  }
  .site-index-container {
    flex: 1;
  }
}

@media (max-width: 959px) {
  .site-index-container {
    margin-top: 0;
  }
}

@media (min-width: 600px) and (max-width: 959px) {
  .bec-col {
    max-width: calc(50% - 8px) !important;
    flex-basis: calc(50% - 8px) !important;
  }
  .eco-col {
    max-width: calc(50% - 8px) !important;
    flex-basis: calc(50% - 8px) !important;
  }
  .site-index-container {
    margin-top: 12px;
  }
}

@media (min-width: 600px) {
  .eco-col {
    padding-left: 8px;
  }
}

@media (max-width: 600px) {
  .site-index-container {
    margin-top: 0px !important;
  }
}

@media (max-width: 390px) {
  .bec-col :deep(.v-field__input),
  .bec-col :deep(.v-select__selection-text) {
    font-size: 0.91rem;
  }
}

@media (max-width: 375px) {
  .bec-col :deep(.v-field__input),
  .bec-col :deep(.v-select__selection-text) {
    font-size: 0.87rem;
  }
}

@media (max-width: 360px) {
  .bec-col :deep(.v-field__input),
  .bec-col :deep(.v-select__selection-text) {
    font-size: 0.83rem;
  }
}

@media (max-width: 344px) {
  .bec-col :deep(.v-field__input),
  .bec-col :deep(.v-select__selection-text) {
    font-size: 0.80rem;
  }
}
</style>
