<template>
  <v-card class="elevation-0">
    <SpeciesSelectionModal
      v-model="modalOpen"
      :existingSpecies="existingSpeciesCodes"
      :maxSpecies="MAX_SPECIES"
      @confirm="handleModalConfirm"
    />
    <v-expansion-panels v-model="panelOpenStates.speciesInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">
                {{
                  panelOpenStates.speciesInfo === CONSTANTS.PANEL.OPEN
                    ? 'mdi-chevron-up'
                    : 'mdi-chevron-down'
                }}
              </v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">{{ CONSTANTS.MANUAL_INPUT_PANEL_LABEL.SPECIES_INFO }}</span>
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
            <!-- Derived By row + Add Species button -->
            <div class="top-controls mt-n1">
              <div class="derived-by-wrapper">
                <label class="bcds-radio-label mr-2" for="derivedBy">Species % Derived By:</label>
                <v-radio-group
                  id="derivedBy"
                  v-model="derivedBy"
                  inline
                  hide-details
                  :disabled="isInputDisabled"
                  class="mt-0 pt-0"
                >
                  <v-radio
                    v-for="option in OPTIONS.derivedByOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  ></v-radio>
                </v-radio-group>
              </div>
              <div v-if="!isReadOnly" class="add-species-btn-col">
                <AppButton
                  label="Add Species"
                  variant="primary"
                  mdi-name="mdi-plus"
                  :isDisabled="isInputDisabled || isAtSpeciesLimit"
                  @click="openModal"
                />
              </div>
            </div>

            <hr v-if="!hasSpecies" class="section-divider" />

            <!-- Cards + Summary row -->
            <div :class="hasSpecies ? 'mt-5' : 'mt-3'">
              <v-row>
                <v-col cols="12" :lg="hasSpecies ? 8 : 12" class="pt-0">
                  <SpeciesListInput
                    :speciesList="speciesList"
                    :isConfirmEnabled="!isInputDisabled"
                    @update:speciesList="handleSpeciesListUpdate"
                    @request-add="openModal"
                  />
                </v-col>
                <SpeciesGroupsDisplay v-if="hasSpecies" :speciesGroups="speciesGroups" />
              </v-row>
            </div>

            <!-- Total species percent + validation -->
            <v-row v-if="hasSpecies" class="mt-n1">
              <v-col cols="12" lg="8" class="pb-0">
                <div class="total-percent-row text-right">
                  <span class="text-body-2 font-weight-medium">
                    Total Species Percentage: {{ totalSpeciesPercent }}%
                  </span>
                  <div v-if="showPercentageError" class="percent-error-text mt-1">
                    Percentages must add up to 100%
                  </div>
                  <div v-if="duplicateErrorMessage" class="percent-error-text mt-1">
                    {{ duplicateErrorMessage }}
                  </div>
                  <div v-if="requiredErrorMessage" class="percent-error-text mt-1">
                    {{ requiredErrorMessage }}
                  </div>
                </div>
              </v-col>
            </v-row>

            <ActionPanel
              v-if="!isReadOnly"
              class="action-panel"
              :isConfirmEnabled="isConfirmEnabled"
              :isConfirmed="isConfirmed"
              :hideClearButton="true"
              :hideEditButton="true"
              :showCancelButton="true"
              :isCancelEnabled="isDirty"
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
import { ref, watch, computed, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import { AppButton } from '@/components'
import { ActionPanel, SpeciesListInput, SpeciesGroupsDisplay } from '@/components/projection'
import { BIZCONSTANTS, CONSTANTS, MESSAGE, OPTIONS } from '@/constants'
import { PROJECTION_ERR, VALIDATION_WARN } from '@/constants/message'
import type { SpeciesList } from '@/interfaces/interfaces'
import { speciesInfoValidation } from '@/validation'
import { cloneDeep } from 'lodash'
import {
  saveProjectionOnPanelConfirm,
  revertPanelToSaved,
  hasPanelUnsavedChanges,
} from '@/services/projection/modelParameterService'
import { useNotificationStore } from '@/stores/common/notificationStore'
import type { PanelName } from '@/types/types'
import SpeciesSelectionModal from './SpeciesSelectionModal.vue'

const MAX_SPECIES = 6

const form = ref<HTMLFormElement>()

const modelParameterStore = useModelParameterStore()
const appStore = useAppStore()
const alertDialogStore = useAlertDialogStore()
const notificationStore = useNotificationStore()

const isReadOnly = computed(() => appStore.isReadOnly)


const {
  panelOpenStates,
  derivedBy,
  speciesList,
  speciesGroups,
  totalSpeciesPercent,
  totalSpeciesGroupPercent,
} = storeToRefs(modelParameterStore)

const panelName = CONSTANTS.MANUAL_INPUT_PANEL.SPECIES_INFO
const isConfirmEnabled = computed(
  () => !isReadOnly.value && modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(() => modelParameterStore.panelState[panelName].confirmed)

const isInputDisabled = computed(
  () => isReadOnly.value || !modelParameterStore.panelState[panelName].editable,
)

const hasSpecies = computed(() => speciesList.value.length > 0)

const isAtSpeciesLimit = computed(() => speciesList.value.length >= MAX_SPECIES)

const existingSpeciesCodes = computed(() =>
  speciesList.value.map((s) => s.species).filter(Boolean) as string[],
)

const duplicateErrorMessage = ref<string | null>(null)
const requiredErrorMessage = ref<string | null>(null)

const showPercentageError = computed(
  () =>
    hasSpecies.value &&
    !speciesInfoValidation.validateTotalSpeciesPercent(
      totalSpeciesPercent.value,
      totalSpeciesGroupPercent.value,
    ).isValid,
)

const updateSpeciesGroup = modelParameterStore.updateSpeciesGroup

watch(speciesList, () => updateSpeciesGroup(), { deep: true })

const isDirty = ref(false)
let suppressDirtyTracking = false
const markDirty = () => { if (!suppressDirtyTracking) isDirty.value = true }

watch(() => modelParameterStore.panelState[panelName].editable, (editable, wasEditable) => {
  if (editable && !wasEditable) isDirty.value = false
})

watch(speciesList, markDirty, { deep: true })
watch(derivedBy, markDirty)

// --- Modal ---
const modalOpen = ref(false)

const openModal = () => {
  if (!isInputDisabled.value && !isAtSpeciesLimit.value) {
    modalOpen.value = true
  }
}

const handleModalConfirm = (selectedCodes: string[]) => {
  const existing = new Map(speciesList.value.map((s) => [s.species, s.percent]))
  const newList: SpeciesList[] = selectedCodes.map((code) => ({
    species: code,
    percent: existing.get(code) ?? '0.0',
  }))
  // Sort by percent descending
  newList.sort(
    (a, b) => Number.parseFloat(b.percent || '0') - Number.parseFloat(a.percent || '0'),
  )
  speciesList.value = newList
}

// --- Species list updates from cards ---
const handleSpeciesListUpdate = (updatedList: SpeciesList[]) => {
  const isDifferent =
    updatedList.length !== speciesList.value.length ||
    updatedList.some(
      (item, index) =>
        item.species !== speciesList.value[index]?.species ||
        item.percent !== speciesList.value[index]?.percent,
    )
  if (isDifferent) {
    speciesList.value = cloneDeep(updatedList)
  }
}

const onConfirm = async () => {
  // validation - duplicate species (safety net)
  const duplicateResult = speciesInfoValidation.validateDuplicateSpecies(speciesList.value)
  if (!duplicateResult.isValid) {
    const dupCode = duplicateResult.duplicateSpecies as keyof typeof BIZCONSTANTS.SPECIES_MAP
    const speciesLabel = (
      Object.keys(BIZCONSTANTS.SPECIES_MAP) as Array<keyof typeof BIZCONSTANTS.SPECIES_MAP>
    ).includes(dupCode)
      ? BIZCONSTANTS.SPECIES_MAP[dupCode]
      : ''
    duplicateErrorMessage.value = speciesLabel
      ? MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_DUP_W_LABEL(dupCode, speciesLabel)
      : MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_DUP_WO_LABEL(dupCode)
    return
  }
  duplicateErrorMessage.value = null

  // validation - total percent
  const totalPercentResult = speciesInfoValidation.validateTotalSpeciesPercent(
    totalSpeciesPercent.value,
    totalSpeciesGroupPercent.value,
  )
  if (!totalPercentResult.isValid) {
    return
  }

  // validation - required fields
  const requiredResult = speciesInfoValidation.validateRequired(derivedBy.value)
  if (!requiredResult.isValid) {
    requiredErrorMessage.value = MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_MISSING_DERIVED_BY
    return
  }
  requiredErrorMessage.value = null

  if (form.value) {
    form.value.validate()
  } else {
    console.warn(VALIDATION_WARN.FORM_REF_NULL)
  }

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

  isDirty.value = false
  if (!isConfirmed.value) {
    modelParameterStore.confirmPanel(panelName)
  }
}

const isHeaderEditActive = computed(() => {
  const status = appStore.currentProjectionStatus
  if (
    status === CONSTANTS.PROJECTION_STATUS.RUNNING  || 
    status === CONSTANTS.PROJECTION_STATUS.QUEUED   ||
    status === CONSTANTS.PROJECTION_STATUS.READY
  )
    return false
  return isConfirmed.value && !modelParameterStore.panelState[panelName].editable
})

const editTooltipText = computed(() => {
  const status = appStore.currentProjectionStatus
  if (
    status === CONSTANTS.PROJECTION_STATUS.RUNNING  || 
    status === CONSTANTS.PROJECTION_STATUS.QUEUED   ||
    status === CONSTANTS.PROJECTION_STATUS.READY
  ) {
    return MESSAGE.EDIT_SECTION_TOOLTIP.RESTRICTED_BY_STATUS(status)
  }
  if (isConfirmed.value && !modelParameterStore.panelState[panelName].editable) {
    return MESSAGE.EDIT_SECTION_TOOLTIP.CLICK_TO_EDIT
  }
  return ''
})

const getEditablePanel = (): string | null => {
  const panelsToCheck = [
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
          notificationStore.showErrorMessage(
            PROJECTION_ERR.LOAD_FAILED,
            PROJECTION_ERR.LOAD_FAILED_TITLE,
          )
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
  if (isDirty.value) {
    const proceed = await alertDialogStore.openDialog(
      MESSAGE.UNSAVED_CHANGES_DIALOG.TITLE,
      MESSAGE.UNSAVED_CHANGES_DIALOG.MESSAGE,
      { variant: 'warning' },
    )
    if (!proceed) return
  }
  suppressDirtyTracking = true
  appStore.isSavingProjection = true
  try {
    await revertPanelToSaved(panelName)
    await nextTick()
    isDirty.value = false
  } catch (error) {
    console.error(PROJECTION_ERR.REVERT_ERROR_LOG, error)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
  } finally {
    appStore.isSavingProjection = false
    suppressDirtyTracking = false
  }
}

</script>

<style scoped>
.action-panel {
  margin-top: 16px;
}


.top-controls {
  display: flex;
  align-items: center;
}

.derived-by-wrapper {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  flex-shrink: 0;
}

.derived-by-wrapper :deep(.bcds-radio-label) {
  white-space: nowrap;
}

.derived-by-wrapper :deep(.v-selection-control-group) {
  flex-wrap: nowrap !important;
  gap: 0;
}

.derived-by-wrapper :deep(.v-selection-control) {
  white-space: nowrap;
  min-width: unset;
  padding-inline-end: 0;
  margin-inline-end: -8px;
}

.add-species-btn-col {
  display: flex;
  align-items: center;
  margin-left: auto;
}

@media (max-width: 599px) {
  .top-controls {
    flex-direction: column;
    align-items: flex-start;
  }
  .add-species-btn-col {
    margin-left: 0;
    margin-top: 8px;
    align-self: flex-end;
  }
}

@media (max-width: 360px) {
  .derived-by-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }
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

.section-divider {
  border: none;
  border-top: 1px solid #d8d8d8;
  margin: 6px 0 0 0;
}

.total-percent-row {
  padding: 4px 0 0 0;
}

.percent-error-text {
  font: var(--typography-regular-small-body);
  color: var(--typography-color-danger);
}
</style>
