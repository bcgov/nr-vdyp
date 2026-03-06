<template>
  <v-card class="elevation-0">
    <v-expansion-panels v-model="panelOpenStates.detailsInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title class="details-panel-title">
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.detailsInfo === CONSTANTS.PANEL.OPEN
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
                <label class="bcds-radio-label" for="manualProjectionType">
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
              <v-col cols="12">
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
                  <div class="bcds-textarea-description">
                    <span class="counter">{{ descriptionLength }}/500</span>
                  </div>
                </div>
              </v-col>
            </v-row>
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
import { ref, computed, watch } from 'vue'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { AppButton } from '@/components'
import { ActionPanel } from '@/components/projection'
import { CONSTANTS, OPTIONS } from '@/constants'
import { saveProjectionOnPanelConfirm, revertPanelToSaved } from '@/services/projection/modelParameterService'
import { PROJECTION_ERR } from '@/constants/message'

const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const notificationStore = useNotificationStore()

const panelName = CONSTANTS.MODEL_PARAMETER_PANEL.DETAILS_INFO
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

const onHeaderEdit = () => {
  if (isConfirmed.value) {
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

// Sync store -> local
watch(() => modelParameterStore.reportTitle, (v) => { localReportTitle.value = v })
watch(() => modelParameterStore.projectionType, (v) => {
  localProjectionType.value = v ?? CONSTANTS.PROJECTION_TYPE.VOLUME
})
watch(() => modelParameterStore.reportDescription, (v) => { localReportDescription.value = v })

// Sync local -> store
watch(localReportTitle, (v) => { modelParameterStore.reportTitle = v })
watch(localProjectionType, (v) => { modelParameterStore.projectionType = v })
watch(localReportDescription, (v) => { modelParameterStore.reportDescription = v })

const validateTitle = () => {
  if (!localReportTitle.value || localReportTitle.value.trim() === '') {
    titleError.value = 'Report Title is required.'
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

.projection-type-col {
  margin-top: 13px;
}

.mt-fields {
  margin-top: 0;
  padding-bottom: 12px;
}

.report-title-label {
  padding-top: 0px;
}

@media (max-width: 600px) {
  .projection-type-col {
    margin-top: 0;
  }

  .mt-fields {
    margin-top: -8px;
    padding-bottom: 0px;
  }
}
</style>
