<template>
  <v-card class="elevation-0">
    <v-expansion-panels v-model="panelOpenStates.reportDetails">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title class="details-panel-title">
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.reportDetails === CONSTANTS.PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">{{ CONSTANTS.MANUAL_INPUT_PANEL_LABEL.REPORT_DETAILS }}</span>
            </v-col>
            <PanelEditControl
              :is-read-only="isReadOnly"
              :editable="modelParameterStore.panelState[panelName].editable"
              :is-header-edit-active="isHeaderEditActive"
              :edit-tooltip-text="editTooltipText"
              @edit="onHeaderEdit"
            />
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text">
          <v-form ref="form">
            <v-row no-gutters class="form-fields-row">
              <v-col cols="12" sm="6">
                <label class="bcds-text-field-label report-title-label" for="manualReportTitle">
                  Report Title (Required)
                </label>
                <v-text-field
                  id="manualReportTitle"
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
              <v-col cols="12" sm="4" class="projection-type-col">
                <label class="bcds-radio-label projection-type-label projection-type-label-mobile" :class="{ 'bcds-radio-label--disabled': isInputDisabled }" for="manualProjectionType">
                  Projection Type
                </label>
                <v-radio-group
                  id="manualProjectionType"
                  v-model="localProjectionType"
                  inline
                  hide-details
                  :disabled="isInputDisabled"
                >
                  <v-radio
                    v-for="option in OPTIONS.projectionTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  ></v-radio>
                </v-radio-group>
              </v-col>
            </v-row>
            <v-row class="mt-fields">
              <v-col cols="12" class="description-textarea-col">
                <div class="bcds-textarea" :data-disabled="isInputDisabled || undefined">
                  <label class="bcds-textarea-label" for="manualReportDescription">
                    Description
                  </label>
                  <div class="bcds-textarea-container">
                    <textarea
                      id="manualReportDescription"
                      class="bcds-textarea-input"
                      v-model="localReportDescription"
                      placeholder="Provide a description of this Projection..."
                      :disabled="isInputDisabled"
                      :maxlength="500"
                      rows="3"
                    ></textarea>
                  </div>
                  <div class="bcds-textarea-description description-counter">
                    <span class="counter">{{ descriptionLength }}/500</span>
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
import { ref, computed, watch, nextTick } from 'vue'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { ActionPanel, PanelEditControl } from '@/components/projection'
import { CONSTANTS, MESSAGE, OPTIONS } from '@/constants'
import { saveProjectionOnPanelConfirm, revertPanelToSaved, hasPanelUnsavedChanges } from '@/services/projection/modelParameterService'
import type { PanelName } from '@/types/types'
import { PROJECTION_ERR } from '@/constants/message'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const notificationStore = useNotificationStore()
const alertDialogStore = useAlertDialogStore()

const panelName = CONSTANTS.MANUAL_INPUT_PANEL.REPORT_DETAILS
const panelOpenStates = computed(() => modelParameterStore.panelOpenStates)

const isReadOnly = computed(() => appStore.isReadOnly)
const isConfirmEnabled = computed(
  () => !isReadOnly.value && modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(() => modelParameterStore.panelState[panelName].confirmed)
const isInputDisabled = computed(
  () => isReadOnly.value || !modelParameterStore.panelState[panelName].editable,
)

// Edit button in header
const isHeaderEditActive = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.QUEUED || status === CONSTANTS.PROJECTION_STATUS.READY) return false
  return isConfirmed.value && !modelParameterStore.panelState[panelName].editable
})

const editTooltipText = computed(() => {
  const status = appStore.currentProjectionStatus
  if (status === CONSTANTS.PROJECTION_STATUS.RUNNING || status === CONSTANTS.PROJECTION_STATUS.QUEUED || status === CONSTANTS.PROJECTION_STATUS.READY) {
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

const localReportTitle = ref<string | null>(modelParameterStore.reportTitle)
const localProjectionType = ref<string | null>(
  modelParameterStore.projectionType ?? CONSTANTS.PROJECTION_TYPE.VOLUME,
)
const localReportDescription = ref<string | null>(modelParameterStore.reportDescription)

const titleError = ref<string>('')

const descriptionLength = computed(() =>
  localReportDescription.value ? localReportDescription.value.length : 0,
)

const isDirty = ref(false)
let suppressDirtyTracking = false
const markDirty = () => { if (!suppressDirtyTracking) isDirty.value = true }

watch(() => modelParameterStore.panelState[panelName].editable, (editable, wasEditable) => {
  if (editable && !wasEditable) isDirty.value = false
})

// Sync store -> local
watch(() => modelParameterStore.reportTitle, (v) => { localReportTitle.value = v })
watch(() => modelParameterStore.projectionType, (v) => {
  localProjectionType.value = v ?? CONSTANTS.PROJECTION_TYPE.VOLUME
})
watch(() => modelParameterStore.reportDescription, (v) => { localReportDescription.value = v })

watch(localReportTitle, (v) => { modelParameterStore.reportTitle = v; markDirty() })
watch(localProjectionType, (v) => {
  modelParameterStore.projectionType = v
  if (v === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS || v === CONSTANTS.PROJECTION_TYPE.BOTH) {
    modelParameterStore.isBySpeciesEnabled = false
  }
  markDirty()
})
watch(localReportDescription, (v) => { modelParameterStore.reportDescription = v; markDirty() })

const validateTitle = () => {
  if (!localReportTitle.value || localReportTitle.value.trim() === '') {
    titleError.value = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_REPORT_TITLE_REQ
    return false
  }
  titleError.value = ''
  return true
}

const onConfirm = async () => {
  if (!validateTitle()) return

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

.projection-type-col {
  margin-top: 0px;
}

.projection-type-label {
  padding-bottom: 8px;
}

.mt-fields {
  margin-top: 0;
  padding-bottom: 0px;
}

.report-title-label {
  padding-top: 0px;
}

.description-textarea-col {
  padding-bottom: 6px;
}

.description-counter {
  padding-bottom: 0px;
}

.action-panel {
  margin-top: 16px;
}

@media (max-width: 600px) {
  .projection-type-col {
    margin-top: 0;
  }

  .projection-type-label-mobile {
    padding-bottom: 0px;
  }

  .mt-fields {
    margin-top: -8px;
    padding-bottom: 0px;
  }
}
</style>
