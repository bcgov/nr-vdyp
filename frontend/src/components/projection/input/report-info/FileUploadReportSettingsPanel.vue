<template>
  <v-card class="elevation-0">
    <v-expansion-panels v-model="fileUploadStore.reportSettingsPanelOpenState">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title class="report-settings-panel-title">
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                fileUploadStore.reportSettingsPanelOpenState === CONSTANTS.PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">{{ CONSTANTS.FILE_UPLOAD_PANEL_LABEL.REPORT_SETTINGS }}</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text">
          <DebugModeSelector
            :model-value="fileUploadStore.isDebugModeEnabled"
            :disabled="isDebugModeDisabled"
            :warning-message="warningMessage"
            @update:model-value="onDebugModeChange"
          />
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/projection/appStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { DebugModeSelector } from '@/components/projection'
import { saveDebugModeSelection } from '@/services/projectionService'
import { CONSTANTS, MESSAGE } from '@/constants'
import { PROJECTION_ERR } from '@/constants/message'

const appStore = useAppStore()
const fileUploadStore = useFileUploadStore()
const notificationStore = useNotificationStore()

const isReadOnly = computed(() => appStore.isReadOnly)

const isDebugModeDisabled = computed(
  () => isReadOnly.value || !fileUploadStore.isDebugModeAvailable,
)

const warningMessage = computed(() => {
  if (isReadOnly.value) return ''
  if (fileUploadStore.hasTooManyPolygons) return MESSAGE.DEBUG_MODE.SINGLE_POLYGON_ONLY
  // A polygon file restored from an earlier session cannot be re-read to count its polygons
  if (fileUploadStore.polygonFileInfo !== null && fileUploadStore.polygonCount === null) {
    return MESSAGE.DEBUG_MODE.POLYGON_COUNT_UNKNOWN
  }
  return ''
})

const onDebugModeChange = async (enabled: boolean) => {
  fileUploadStore.isDebugModeEnabled = enabled
  // Without a projection yet, the selection is saved when the projection is created
  const projectionGUID = appStore.getCurrentProjectionGUID
  if (!projectionGUID) return

  appStore.isSavingProjection = true
  try {
    await saveDebugModeSelection(projectionGUID, enabled)
  } catch (error) {
    console.error(PROJECTION_ERR.SAVE_ERROR_LOG, error)
    fileUploadStore.isDebugModeEnabled = !enabled
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, PROJECTION_ERR.SAVE_FAILED_TITLE)
  } finally {
    appStore.isSavingProjection = false
  }
}
</script>

<style scoped>
/* Match collapsed height with other panels that have an Edit button */
.report-settings-panel-title {
  min-height: 48px !important;
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
</style>
