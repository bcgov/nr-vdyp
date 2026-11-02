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
            <v-col cols="auto">
              <span class="text-h6">{{ CONSTANTS.FILE_UPLOAD_PANEL_LABEL.REPORT_SETTINGS }}</span>
            </v-col>
            <!-- Shown in the header so the automatic reset is visible while the panel is collapsed -->
            <v-col
              v-if="fileUploadStore.isDebugModeAutoDisabled"
              class="header-warning"
              data-testid="debug-mode-header-warning"
            >
              <v-icon size="small" class="header-warning-icon">mdi-alert-outline</v-icon>
              <span>{{ MESSAGE.DEBUG_MODE.DISABLED_MULTIPLE_POLYGONS }}</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text">
          <DebugModeSelector
            v-model="fileUploadStore.isDebugModeEnabled"
            :disabled="isDebugModeDisabled"
            :warning-message="warningMessage"
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
import { DebugModeSelector } from '@/components/projection'
import { CONSTANTS, MESSAGE } from '@/constants'

const appStore = useAppStore()
const fileUploadStore = useFileUploadStore()

const isReadOnly = computed(() => appStore.isReadOnly)

const isDebugModeDisabled = computed(
  () => isReadOnly.value || !fileUploadStore.isDebugModeAvailable,
)

const warningMessage = computed(() => {
  if (isReadOnly.value) return ''
  if (fileUploadStore.hasMultiplePolygons) return MESSAGE.DEBUG_MODE.SINGLE_POLYGON_ONLY
  // A polygon file restored from an earlier session cannot be re-read to count its polygons
  if (fileUploadStore.polygonFileInfo !== null && fileUploadStore.polygonCount === null) {
    return MESSAGE.DEBUG_MODE.POLYGON_COUNT_UNKNOWN
  }
  return ''
})
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
  flex-wrap: nowrap;
}

.header-warning {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-xsmall);
  margin-left: var(--layout-margin-medium);
  font: var(--typography-regular-small-body);
  color: var(--typography-color-primary);
  min-width: 0;
}

.header-warning-icon {
  color: var(--typography-color-primary);
}

@media (max-width: 600px) {
  .expander-header {
    flex-wrap: wrap;
  }

  .header-warning {
    flex-basis: 100%;
    margin-left: 0;
    margin-top: var(--layout-margin-small);
  }
}
</style>
