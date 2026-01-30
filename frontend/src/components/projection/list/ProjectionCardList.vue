<template>
  <div class="cards-container">
    <!-- Sort Dropdown for Card View -->
    <label class="bcds-select-label" for="cardSortBy">Sort By:</label>
    <v-select
      id="cardSortBy"
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
      <template #item="{ item, props }">
        <v-list-item v-bind="props" class="sort-option-item">
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
    <v-card v-if="projections.length === 0" class="empty-state-card" elevation="1">
      <div class="empty-state-content">
        <span class="empty-state-message">
          No projections found. Create a new projection to build your history.
        </span>
      </div>
    </v-card>

    <!-- Cards -->
    <v-card
      v-for="projection in projections"
      :key="projection.projectionGUID"
      class="projection-card clickable-card"
      elevation="1"
      @click="handleCardClick($event, projection)"
    >
      <!-- Header Section with gray background -->
      <div class="card-header-section">
        <div class="card-header-row">
          <span class="card-title">{{ projection.title }}</span>
          <ProjectionStatusBadge :status="projection.status" />
        </div>
        <div class="card-last-updated">
          Last Updated: {{ formatDateTimeDisplay(projection.lastUpdated) }}
        </div>
      </div>

      <!-- Content Section with white background -->
      <v-card-text class="card-content">
        <!-- Info Row: Method, Range Type, Expiration -->
        <div class="card-info-section">
          <div class="card-info-row">
            <div class="card-info-item">
              <span class="card-info-label">Method</span>
              <span class="card-info-value">{{ projection.method }}</span>
            </div>
            <div class="card-info-item">
              <span class="card-info-label">Range Type</span>
              <span class="card-info-value">{{ projection.projectionType }}</span>
            </div>
            <div class="card-info-item">
              <span class="card-info-label">Expiration</span>
              <span class="card-info-value">{{ formatDateDisplay(projection.expiration) }}</span>
            </div>
            <!-- Description inline on larger screens -->
            <div class="card-info-item card-info-description card-info-description-inline">
              <span class="card-info-label">Description</span>
              <span class="card-info-value">{{ projection.description }}</span>
            </div>
          </div>
          <!-- Description on separate row on smaller screens -->
          <div class="card-description-row card-description-row-responsive">
            <span class="card-info-label">Description</span>
            <span class="card-description-value">{{ projection.description }}</span>
          </div>
        </div>
      </v-card-text>

      <!-- Action Buttons based on status -->
      <v-card-actions class="card-actions">
        <div class="card-action-buttons">
          <!-- Draft: Edit, Duplicate, Delete -->
          <template v-if="projection.status === PROJECTION_STATUS.DRAFT">
            <AppButton
              label="Edit"
              variant="tertiary"
              icon-position="top"
              :icon-src="EditIcon"
              @click="$emit(PROJECTION_USER_ACTION.EDIT, projection.projectionGUID)"
            />
            <AppButton
              label="Duplicate"
              variant="tertiary"
              icon-position="top"
              :icon-src="DuplicateIcon"
              @click="$emit(PROJECTION_USER_ACTION.DUPLICATE, projection.projectionGUID)"
            />
            <AppButton
              label="Delete"
              variant="tertiary"
              icon-position="top"
              :icon-src="DeleteIcon"
              @click="$emit(PROJECTION_USER_ACTION.DELETE, projection.projectionGUID)"
            />
          </template>

          <!-- Failed: Edit, Duplicate, Download, Delete -->
          <template v-else-if="projection.status === PROJECTION_STATUS.FAILED">
            <AppButton
              label="Edit"
              variant="tertiary"
              icon-position="top"
              :icon-src="EditIcon"
              @click="$emit(PROJECTION_USER_ACTION.EDIT, projection.projectionGUID)"
            />
            <AppButton
              label="Duplicate"
              variant="tertiary"
              icon-position="top"
              :icon-src="DuplicateIcon"
              @click="$emit(PROJECTION_USER_ACTION.DUPLICATE, projection.projectionGUID)"
            />
            <AppButton
              label="Download"
              variant="tertiary"
              icon-position="top"
              :icon-src="DownloadIcon"
              @click="$emit(PROJECTION_USER_ACTION.DOWNLOAD, projection.projectionGUID)"
            />
            <AppButton
              label="Delete"
              variant="tertiary"
              icon-position="top"
              :icon-src="DeleteIcon"
              @click="$emit(PROJECTION_USER_ACTION.DELETE, projection.projectionGUID)"
            />
          </template>

          <!-- Ready: View, Duplicate, Download, Delete -->
          <template v-else-if="projection.status === PROJECTION_STATUS.READY">
            <AppButton
              label="View"
              variant="tertiary"
              icon-position="top"
              :icon-src="ViewIcon"
              @click="$emit(PROJECTION_USER_ACTION.VIEW, projection.projectionGUID)"
            />
            <AppButton
              label="Duplicate"
              variant="tertiary"
              icon-position="top"
              :icon-src="DuplicateIcon"
              @click="$emit(PROJECTION_USER_ACTION.DUPLICATE, projection.projectionGUID)"
            />
            <AppButton
              label="Download"
              variant="tertiary"
              icon-position="top"
              :icon-src="DownloadIcon"
              @click="$emit(PROJECTION_USER_ACTION.DOWNLOAD, projection.projectionGUID)"
            />
            <AppButton
              label="Delete"
              variant="tertiary"
              icon-position="top"
              :icon-src="DeleteIcon"
              @click="$emit(PROJECTION_USER_ACTION.DELETE, projection.projectionGUID)"
            />
          </template>

          <!-- Running: Cancel, Delete -->
          <template v-else-if="projection.status === PROJECTION_STATUS.RUNNING">
            <AppButton
              label="Cancel"
              variant="tertiary"
              icon-position="top"
              :icon-src="CancelIcon"
              @click="$emit(PROJECTION_USER_ACTION.CANCEL, projection.projectionGUID)"
            />
            <AppButton
              label="Delete"
              variant="tertiary"
              icon-position="top"
              :icon-src="DeleteIcon"
              @click="$emit(PROJECTION_USER_ACTION.DELETE, projection.projectionGUID)"
            />
          </template>
        </div>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import type { Projection, SortOption } from '@/interfaces/interfaces'
import { PROJECTION_STATUS, PROJECTION_USER_ACTION } from '@/constants/constants'
import { formatDateTimeDisplay, formatDateDisplay } from '@/utils/util'
import { AppButton } from '@/components'
import { ProjectionStatusBadge } from '@/components/projection'
import { EditIcon, DuplicateIcon, DeleteIcon, ViewIcon, DownloadIcon, CancelIcon } from '@/assets/'

interface Props {
  projections: Projection[]
  sortOptions: SortOption[]
  sortValue: string
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'sort', value: string): void
  (e: 'view', projectionGUID: string): void
  (e: 'edit', projectionGUID: string): void
  (e: 'duplicate', projectionGUID: string): void
  (e: 'download', projectionGUID: string): void
  (e: 'cancel', projectionGUID: string): void
  (e: 'delete', projectionGUID: string): void
  (e: 'rowClick', projection: Projection): void
}>()

const handleSortChange = (value: string) => {
  emit('sort', value)
}

const handleCardClick = (event: MouseEvent, projection: Projection) => {
  // Don't trigger card click if clicking on the action buttons area
  const target = event.target as HTMLElement
  if (target.closest('.card-actions')) {
    return
  }
  emit('rowClick', projection)
}
</script>

<style scoped>
/* Card Styles */
.cards-container {
  display: flex;
  flex-direction: column;
  gap: var(--layout-margin-small);
}

.sort-dropdown {
  max-width: 300px;
}

.projection-card {
  border: 1px solid var(--surface-color-border-default);
  border-radius: var(--layout-border-radius-medium);
  background: var(--surface-color-forms-default);
}

.projection-card.clickable-card {
  cursor: pointer;
  transition: box-shadow 0.2s ease;
}

.projection-card.clickable-card:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.12);
}

/* Card Header Section with gray background */
.card-header-section {
  background-color: var(--surface-color-background-light-gray);
  padding: var(--layout-padding-medium);
  border-bottom: 1px solid var(--surface-color-border-default);
}

.card-content {
  padding: var(--layout-padding-medium);
}

/* Card Header Row - Title and Status */
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--layout-margin-xsmall);
  gap: var(--layout-padding-medium);
}

.card-title {
  font: var(--typography-bold-h5);
  color: var(--typography-color-primary);
  flex: 1;
  word-break: break-word;
}

/* Last Updated */
.card-last-updated {
  font: var(--typography-regular-small-body);
  color: var(--typography-color-secondary);
}

.card-info-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--layout-padding-large);
}

.card-info-item {
  display: flex;
  flex-direction: column;
  gap: var(--layout-padding-xsmall);
}

.card-info-description {
  flex: 1;
  min-width: 150px;
}

/* Description inline - shown on larger card widths */
.card-info-description-inline {
  display: flex;
}

/* Description on separate row - hidden on larger screens */
.card-description-row-responsive {
  display: none;
}

/* When screen is narrower, show description on separate row */
@media (max-width: 700px) {
  .card-info-description-inline {
    display: none;
  }

  .card-description-row-responsive {
    display: flex;
  }
}

.card-info-label {
  font: var(--typography-regular-small-body);
  color: var(--typography-color-secondary);
}

.card-info-value {
  font: var(--typography-bold-body);
  color: var(--typography-color-primary);
}

/* Description on separate row - base styles */
.card-description-row {
  margin-top: var(--layout-margin-medium);
  flex-direction: column;
  gap: var(--layout-padding-xsmall);
  width: 100%;
}

.card-description-value {
  font: var(--typography-bold-body);
  color: var(--typography-color-primary);
  word-break: break-word;
}

/* Card Actions with gray background */
.card-actions {
  padding: var(--layout-padding-medium);
  border-top: 1px solid var(--surface-color-border-default);
  justify-content: flex-end;
  background-color: var(--surface-color-background-light-gray);
}

.card-action-buttons {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-medium);
}

/* Empty state styles */
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
</style>
