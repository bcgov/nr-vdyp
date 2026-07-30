<template>
  <div class="cards-container">
    <!-- Sort Dropdown for Card View -->
    <label class="bcds-select-label" for="adminCardSortBy">Sort By:</label>
    <v-select
      id="adminCardSortBy"
      :model-value="sortValue"
      :items="sortOptions"
      item-title="title"
      item-value="value"
      label="Sort by"
      variant="outlined"
      density="compact"
      class="sort-dropdown"
      append-inner-icon="mdi-chevron-down"
      @update:model-value="handleSortChange"
    >
      <template #selection="{ item }">
        <span>{{ item.title }}</span>
      </template>
      <template #item="{ item, props: itemProps }">
        <v-list-item v-bind="itemProps" class="sort-option-item">
          <template #title>
            <div class="sort-option">
              <span class="sort-option-text">{{ item.raw.title }}</span>
              <v-icon
                size="small"
                class="sort-check-icon"
                :class="{ invisible: item.raw.value !== sortValue }"
              >
                mdi-check
              </v-icon>
            </div>
          </template>
        </v-list-item>
      </template>
    </v-select>

    <!-- Empty state when no projections -->
    <v-card v-if="projections.length === 0" class="empty-state-card" elevation="0">
      <div class="empty-state-content">
        <span class="empty-state-message">No running projections found.</span>
      </div>
    </v-card>

    <!-- Cards -->
    <v-card
      v-for="projection in projections"
      :key="projection.projectionGUID"
      class="admin-projection-card"
      elevation="0"
    >
      <!-- Header Section with gray background -->
      <div class="card-header-section">
        <div class="card-header-row">
          <span class="card-title">{{ projection.title }}</span>
          <span
            v-if="projection.status === PROJECTION_STATUS.RUNNING"
            class="status-badge status-running"
          >
            Running
          </span>
        </div>
      </div>

      <!-- Content Section with white background -->
      <v-card-text class="card-content">
        <div class="card-info-row">
          <div class="card-info-item">
            <span class="card-info-label">Owner</span>
            <span class="card-info-value">{{ projection.ownerDisplayName }}</span>
          </div>
          <div class="card-info-item">
            <span class="card-info-label">User Type</span>
            <span class="card-info-value">{{ projection.userType || '-' }}</span>
          </div>
          <div class="card-info-item">
            <span class="card-info-label">Elapsed</span>
            <span class="card-info-value">{{ formatElapsedTime(projection.startDate) }}</span>
          </div>
          <div class="card-info-item">
            <span class="card-info-label">Threads</span>
            <span class="card-info-value">{{ projection.workerCount }}</span>
          </div>
        </div>

        <div class="card-progress-row">
          <div class="card-info-item card-progress-item">
            <span class="card-info-label">Progress</span>
            <div class="progress-cell">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :style="{ width: `${getProgressPercent(projection)}%` }"
                />
              </div>
              <span class="progress-percent">{{ getProgressPercent(projection) }}%</span>
            </div>
          </div>
          <div class="card-info-item">
            <span class="card-info-label">Polygons</span>
            <span class="card-info-value polygons-value">
              {{ formatNumber(projection.completedPolygonCount) }}
              <span class="polygons-total">/ {{ formatNumber(projection.polygonCount) }}</span>
            </span>
          </div>
        </div>
      </v-card-text>

      <!-- Action Buttons -->
      <v-card-actions class="card-actions">
        <AppButton
          label="Cancel"
          variant="secondary"
          size="small"
          mdi-name="mdi-stop-circle-outline"
          class="cancel-button"
          @click="$emit('cancel', projection.projectionGUID)"
        />
      </v-card-actions>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import type { AdminProjection, SortOption } from '@/interfaces/interfaces'
import { PROJECTION_STATUS } from '@/constants/constants'
import { formatNumber } from '@/utils/util'
import { AppButton } from '@/components'

interface Props {
  projections: AdminProjection[]
  sortOptions: SortOption[]
  sortValue: string
  now: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'sort', value: string): void
  (e: 'cancel', projectionGUID: string): void
}>()

const handleSortChange = (value: string) => {
  emit('sort', value)
}

const getProgressPercent = (projection: AdminProjection): number => {
  if (!projection.polygonCount) return 0
  return Math.min(
    100,
    Math.round((projection.completedPolygonCount / projection.polygonCount) * 100),
  )
}

const formatElapsedTime = (startDate: string | null): string => {
  if (!startDate) return '-'
  const startMs = new Date(startDate).getTime()
  if (Number.isNaN(startMs)) return '-'

  const elapsedSeconds = Math.max(0, Math.floor((props.now - startMs) / 1000))
  const hours = Math.floor(elapsedSeconds / 3600)
  const minutes = Math.floor((elapsedSeconds % 3600) / 60)
  const seconds = elapsedSeconds % 60
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}
</script>

<style scoped>
.cards-container {
  display: flex;
  flex-direction: column;
  gap: var(--layout-margin-small);
  width: 100%;
}

.sort-dropdown {
  max-width: 300px;
}

/* Increase dropdown max-height to prevent scrollbar */
.sort-dropdown + .v-overlay__content,
.v-overlay__content.v-select__content {
  max-height: 400px !important;
}

.v-list .sort-option-item {
  padding: 0 !important;
}

.v-list .sort-option-item .v-list-item__content {
  padding: 0;
}

.v-list .sort-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 8px 16px;
  cursor: pointer;
}

.v-list .sort-option-text {
  flex: 1;
}

.v-list .sort-option .sort-check-icon {
  width: 20px;
  min-width: 20px;
  margin-left: 8px;
}

.v-list .sort-option .sort-check-icon.invisible {
  visibility: hidden;
}

.admin-projection-card {
  border: 1px solid var(--surface-color-border-default);
  border-radius: var(--layout-border-radius-medium);
  background: var(--surface-color-forms-default);
}

.card-header-section {
  background-color: var(--surface-color-background-light-gray);
  padding: var(--layout-padding-medium);
  border-bottom: 1px solid var(--surface-color-border-default);
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--layout-padding-medium);
}

.card-title {
  font: var(--typography-bold-h5);
  color: var(--typography-color-primary);
  flex: 1;
  overflow-wrap: break-word;
}

.status-badge {
  display: flex;
  align-items: center;
  height: 24px;
  padding: var(--layout-margin-hair) var(--layout-padding-small);
  gap: var(--layout-margin-small);
  border-radius: var(--layout-border-radius-small);
  font: var(--typography-regular-small-body);
  color: var(--typography-color-primary);
  white-space: nowrap;
}

.status-badge.status-running {
  border: 1px solid var(--support-border-color-success);
  background: var(--support-surface-color-success);
}

.card-content {
  padding: var(--layout-padding-medium);
  display: flex;
  flex-direction: column;
  gap: var(--layout-margin-medium);
}

.card-info-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--layout-padding-large);
}

.card-progress-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--layout-padding-large);
}

.card-info-item {
  display: flex;
  flex-direction: column;
  gap: var(--layout-padding-xsmall);
  min-width: 100px;
}

.card-progress-item {
  flex: 1;
  min-width: 160px;
}

.card-info-label {
  font: var(--typography-regular-small-body);
  color: var(--typography-color-secondary);
}

.card-info-value {
  font: var(--typography-bold-body);
  color: var(--typography-color-primary);
}

.polygons-value {
  white-space: nowrap;
}

.polygons-total {
  font: var(--typography-regular-small-body);
  color: var(--typography-color-secondary);
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-small);
}

.progress-track {
  flex: 1;
  height: 8px;
  background-color: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #003366;
  border-radius: 4px;
  transition: width 0.4s ease;
}

.progress-percent {
  font: var(--typography-regular-small-body);
  white-space: nowrap;
}

.card-actions {
  padding: var(--layout-padding-medium);
  border-top: 1px solid var(--surface-color-border-default);
  justify-content: flex-end;
  background-color: var(--surface-color-background-light-gray);
}

.cancel-button {
  border-color: var(--support-border-color-danger) !important;
  color: var(--support-border-color-danger) !important;
}

.cancel-button :deep(.v-icon) {
  color: var(--support-border-color-danger);
}

.empty-state-card {
  border: 1px solid var(--surface-color-border-default);
  border-radius: var(--layout-border-radius-medium);
  background: var(--surface-color-forms-default);
}

.empty-state-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--layout-padding-xlarge) var(--layout-padding-medium);
  gap: var(--layout-margin-small);
}

.empty-state-message {
  font: var(--typography-regular-body);
  color: var(--typography-color-secondary);
  text-align: center;
}
</style>
