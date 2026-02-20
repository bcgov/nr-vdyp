<template>
  <v-card>
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
              <span class="text-h6">Minimum DBH Limit by Species Group</span>
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
        <v-expansion-panel-text class="expansion-panel-text mt-n2">
          <v-container fluid class="mt-n2">
            <v-row v-for="(group, index) in fileUploadSpeciesGroups" :key="index">
              <v-col class="min-dbh-species-group-label" :class="{ 'min-dbh-disabled': isMinDBHDeactivated }">
                {{ group.group }}
              </v-col>
              <v-col cols="8" class="ml-n5">
                <v-slider
                  v-model="fileUploadUtilizationSliderValues[index]"
                  :min="0"
                  :max="4"
                  :ticks="utilizationSliderTickLabels"
                  show-ticks="always"
                  step="1"
                  thumb-size="12"
                  track-size="7"
                  track-color="transparent"
                  :disabled="isMinDBHDeactivated"
                  @update:model-value="updateFileUploadMinDBH(index, $event)"
                ></v-slider>
              </v-col>
            </v-row>
          </v-container>
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
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useAppStore } from '@/stores/projection/appStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { AppButton } from '@/components'
import { ActionPanel } from '@/components/projection'
import { CONSTANTS, OPTIONS } from '@/constants'
import { saveProjectionOnPanelConfirm, revertPanelToSaved } from '@/services/projection/fileUploadService'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { PROJECTION_ERR } from '@/constants/message'

const appStore = useAppStore()
const fileUploadStore = useFileUploadStore()
const notificationStore = useNotificationStore()

const panelName = CONSTANTS.FILE_UPLOAD_PANEL.MINIMUM_DBH

const panelOpenStates = computed(() => fileUploadStore.panelOpenStates)

const isReadOnly = computed(() => appStore.isReadOnly)

const isConfirmEnabled = computed(
  () => !isReadOnly.value && fileUploadStore.panelState[panelName].editable,
)

const isConfirmed = computed(
  () => fileUploadStore.panelState[panelName].confirmed,
)

const isMinDBHDeactivated = computed(() => {
  const isDisabled = isReadOnly.value || !fileUploadStore.panelState[panelName].editable
  const isCFSBiomass = fileUploadStore.projectionType === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
  return isDisabled || isCFSBiomass
})

// Edit button in header
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

const onHeaderEdit = () => {
  if (isConfirmed.value) {
    fileUploadStore.editPanel(panelName)
  }
}

// Species groups and slider logic
const utilizationClassOptions = OPTIONS.utilizationClassOptions
const fileUploadSpeciesGroups = computed(() => fileUploadStore.fileUploadSpeciesGroup)
const fileUploadUtilizationSliderValues = ref<number[]>([])

const utilizationSliderTickLabels = utilizationClassOptions.reduce(
  (acc, opt) => {
    acc[opt.index] = opt.label
    return acc
  },
  {} as Record<number, string>,
)

// Watch fileUploadSpeciesGroups for changes and sync utilization sliderValues
watch(
  fileUploadSpeciesGroups,
  (newGroups) => {
    fileUploadUtilizationSliderValues.value = newGroups.map((group) =>
      utilizationClassOptions.findIndex(
        (opt) => opt.value === group.minimumDBHLimit,
      ),
    )
  },
  { immediate: true, deep: true, flush: 'sync' },
)

// Watch fileUploadStore projectionType to update species groups and slider values when projection type changes
watch(
  () => fileUploadStore.projectionType,
  (newType, oldType) => {
    // Skip if this is an initial load (oldType is null) for existing projections (view/edit mode)
    if (oldType === null && appStore.viewMode !== CONSTANTS.PROJECTION_VIEW_MODE.CREATE) {
      return
    }

    fileUploadStore.updateSpeciesGroupsForProjectionType(newType)
    fileUploadUtilizationSliderValues.value = fileUploadSpeciesGroups.value.map(
      (group) =>
        utilizationClassOptions.findIndex(
          (opt) => opt.value === group.minimumDBHLimit,
        ),
    )
  },
)

const updateFileUploadMinDBH = (index: number, value: number) => {
  if (fileUploadSpeciesGroups.value[index]) {
    const enumValue = utilizationClassOptions[value]?.value
    if (enumValue !== undefined) {
      fileUploadSpeciesGroups.value[index].minimumDBHLimit = enumValue
    }
  }
}

const onConfirm = async () => {
  appStore.isSavingProjection = true
  try {
    await saveProjectionOnPanelConfirm(fileUploadStore, panelName)
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
.min-dbh-species-group-label {
  max-width: 5%;
  padding-top: 0px;
  padding-left: 20px;
}

.min-dbh-disabled {
  color: var(--typography-color-disabled) !important;
}

.edit-button-col {
  display: flex;
  align-items: center;
}
</style>
